#!/usr/bin/env python3
"""Read two immutable sources and build a complete-case food catalog (stdlib only)."""
import argparse
from collections import Counter, defaultdict
from datetime import date, datetime, timedelta
from decimal import Decimal, InvalidOperation
import hashlib
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET
import zipfile

NS = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
FIELDS = {
    "code": "식품코드", "name": "식품명", "category": "식품대분류명",
    "origin": "식품기원명", "basis": "영양성분함량기준량",
    "referenceDate": "데이터기준일자",
}
NUTRIENTS = {
    "energyKcal": "에너지(kcal)", "carbohydrateG": "탄수화물(g)",
    "proteinG": "단백질(g)", "fatG": "지방(g)", "sugarsG": "당류(g)",
    "sodiumMg": "나트륨(mg)", "cholesterolMg": "콜레스테롤(mg)",
    "saturatedFatG": "포화지방산(g)", "transFatG": "트랜스지방산(g)",
}
URLS = {
    "mfds_xlsx": "https://various.foodsafetykorea.go.kr/nutrient/general/down/historyList.do",
    "data_go_kr_json": "https://www.data.go.kr/data/15100070/standard.do",
}
MISSING = {"", "-", "--", "n/a", "na", "null", "none", "nan", "미상", "해당없음"}


def clean(value):
    return "" if value is None else str(value).strip()


def number(value):
    text = clean(value)
    if text.lower() in MISSING:
        raise ValueError("missing")
    # A trace/below-limit value is not an exact measurement; do not impute zero.
    if not re.fullmatch(r"[+]?(?:\d+(?:\.\d*)?|\.\d+)", text):
        raise ValueError("invalid_number")
    try:
        result = Decimal(text)
    except InvalidOperation as exc:
        raise ValueError("invalid_number") from exc
    if not result.is_finite() or result < 0:
        raise ValueError("invalid_number")
    return int(result) if result == result.to_integral() else float(result)


def reference_date(value):
    text = clean(value)
    try:
        return date.fromisoformat(text).isoformat()
    except ValueError:
        # Excel Windows date system. Fractional/time values are not accepted.
        if re.fullmatch(r"\d{5}", text):
            return (datetime(1899, 12, 30) + timedelta(days=int(text))).date().isoformat()
        raise ValueError("invalid_date")


def xlsx_rows(path):
    with zipfile.ZipFile(path) as archive:
        workbook = ET.fromstring(archive.read("xl/workbook.xml"))
        props = workbook.find("s:workbookPr", NS)
        if props is not None and props.get("date1904") in {"1", "true"}:
            raise ValueError("1904-date-system workbook is unsupported")
        strings = []
        if "xl/sharedStrings.xml" in archive.namelist():
            root = ET.fromstring(archive.read("xl/sharedStrings.xml"))
            strings = ["".join(t.text or "" for t in item.findall(".//s:t", NS))
                       for item in root.findall("s:si", NS)]
        sheet = workbook.find("s:sheets/s:sheet", NS)
        relation_id = sheet.get("{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id")
        relations = ET.fromstring(archive.read("xl/_rels/workbook.xml.rels"))
        target = next(r.get("Target") for r in relations if r.get("Id") == relation_id)
        sheet_path = target.lstrip("/") if target.startswith("/") else "xl/" + target
        headers = None
        with archive.open(sheet_path) as stream:
            for _, row in ET.iterparse(stream, events=("end",)):
                if row.tag != "{" + NS["s"] + "}row":
                    continue
                cells = {}
                for cell in row.findall("s:c", NS):
                    column = re.sub(r"\d", "", cell.get("r"))
                    if cell.find("s:f", NS) is not None:
                        raise ValueError(f"Formula cell requires explicit review: {cell.get('r')}")
                    v = cell.find("s:v", NS)
                    value = v.text if v is not None else ""
                    if cell.get("t") == "s":
                        value = strings[int(value)]
                    elif cell.get("t") == "inlineStr":
                        value = "".join(t.text or "" for t in cell.findall(".//s:t", NS))
                    cells[column] = value
                if headers is None:
                    headers = cells
                    missing = set(FIELDS.values()) | set(NUTRIENTS.values())
                    missing -= set(headers.values())
                    if missing:
                        raise ValueError(f"Missing XLSX headers: {sorted(missing)}")
                else:
                    yield int(row.get("r")), {key: cells.get(col, "") for col, key in headers.items()}
                row.clear()


def json_rows(path):
    with path.open(encoding="utf-8-sig") as stream:
        data = json.load(stream)
    records = data["records"]
    for index, row in enumerate(records, 1):
        yield index, row


def normalize(row):
    errors = []
    result = {}
    for target, source in FIELDS.items():
        value = clean(row.get(source))
        if value.lower() in MISSING:
            errors.append(f"{source}:missing")
        result[target] = value
    match = re.fullmatch(r"(\d+(?:\.\d+)?)\s*(g|ml)", result.pop("basis"), re.I)
    if match and Decimal(match[1]) > 0:
        result["basisAmount"] = number(match[1])
        result["basisUnit"] = "g" if match[2].lower() == "g" else "mL"
    else:
        errors.append("영양성분함량기준량:unsupported_basis")
    try:
        result["referenceDate"] = reference_date(result["referenceDate"])
    except ValueError:
        errors.append("데이터기준일자:invalid_date")
    nutrients = {}
    for target, source in NUTRIENTS.items():
        try:
            nutrients[target] = number(row.get(source))
        except ValueError as exc:
            errors.append(f"{source}:{exc}")
    result["nutrients"] = nutrients
    return result, sorted(set(errors))


def payload(row):
    return {key: value for key, value in row.items() if key != "sourceRefs"}


def merge(candidates):
    groups = defaultdict(list)
    for row in candidates:
        groups[row["code"]].append(row)
    foods, conflicts = [], []
    for code, rows in sorted(groups.items()):
        # Newest complete row wins; JSON wins a same-date tie; earlier row is final tie-breaker.
        rows.sort(key=lambda r: (r["referenceDate"],
                  r["sourceRefs"][0]["sourceId"] == "data_go_kr_json",
                  -r["sourceRefs"][0]["row"]), reverse=True)
        winner = rows[0]
        selected = winner["sourceRefs"][0].copy()
        variants = []
        refs = []
        for row in rows:
            ref = row["sourceRefs"][0]
            refs.append({**ref, "selected": ref == selected})
            changed = [key for key in payload(winner) if row[key] != winner[key]]
            if changed:
                variants.append({"sourceRef": ref, "differentFields": changed, "values": payload(row)})
        if variants:
            conflicts.append({"code": code, "selectedSourceRef": selected,
                              "selectedValues": payload(winner), "otherVersions": variants})
        foods.append({**payload(winner), "sourceRefs": refs})
    return foods, conflicts


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2, allow_nan=False) + "\n", encoding="utf-8")


def build(xlsx_path, json_path, output):
    candidates, sources, rejections = [], {}, []
    for source_id, path, reader in [("mfds_xlsx", xlsx_path, xlsx_rows),
                                    ("data_go_kr_json", json_path, json_rows)]:
        counts = Counter()
        total = accepted = 0
        for row_index, raw in reader(path):
            total += 1
            row, errors = normalize(raw)
            if errors:
                counts.update(errors)
                rejections.append({"sourceId": source_id, "row": row_index,
                                   "code": clean(raw.get("식품코드")), "reasons": errors})
                continue
            row["sourceRefs"] = [{"sourceId": source_id, "row": row_index}]
            candidates.append(row)
            accepted += 1
        sources[source_id] = {
            "filename": path.name, "url": URLS[source_id],
            "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "rows": total, "acceptedRows": accepted, "rejectedRows": total - accepted,
            "rejectionReasons": dict(sorted(counts.items())),
        }
    foods, conflicts = merge(candidates)
    catalog = {"schemaVersion": 1, "foods": foods}
    write_json(output / "foods.json", catalog)
    manifest = {
        "schemaVersion": 1, "sources": sources,
        "requiredSourceFields": list(FIELDS.values()) + list(NUTRIENTS.values()),
        "summary": {
            "inputRows": sum(s["rows"] for s in sources.values()),
            "acceptedRowsBeforeDeduplication": len(candidates),
            "rejectedRows": len(rejections), "duplicateRowsRemoved": len(candidates) - len(foods),
            "outputFoods": len(foods), "codesWithConflictingVersions": len(conflicts),
            "selectedRowsBySource": dict(sorted(Counter(
                ref["sourceId"] for food in foods for ref in food["sourceRefs"] if ref["selected"]
            ).items())),
            "basisDistribution": dict(sorted(Counter(str(f["basisAmount"]) + f["basisUnit"] for f in foods).items())),
        },
        "outputSha256": hashlib.sha256((output / "foods.json").read_bytes()).hexdigest(),
        "rowNumbering": {"mfds_xlsx": "Excel row, header at 1", "data_go_kr_json": "records index + 1"},
    }
    write_json(output / "manifest.json", manifest)
    write_json(output / "quality_report.json", {"rejectedRows": rejections, "conflicts": conflicts})
    return manifest


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--xlsx", required=True, type=Path)
    parser.add_argument("--json", required=True, type=Path)
    parser.add_argument("--output", type=Path, default=Path("data/processed"))
    args = parser.parse_args()
    manifest = build(args.xlsx, args.json, args.output)
    print(json.dumps(manifest, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
