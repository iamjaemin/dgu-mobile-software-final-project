"""Run with: python3 -m unittest discover -s scripts -p 'test_*.py' -v"""
import copy
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

from build_food_dataset import FIELDS, NUTRIENTS, merge, normalize, number, reference_date, xlsx_rows


def source_row(**changes):
    row = {value: "0" for value in NUTRIENTS.values()}
    row.update(dict(zip(FIELDS.values(), ["D001", "음식", "밥류", "가정식", "100g", "2026-09-29"])))
    row.update(changes)
    return row


def candidate(source_id, index, day="2026-09-29", energy="100"):
    row, errors = normalize(source_row(**{"데이터기준일자": day, "에너지(kcal)": energy}))
    assert not errors
    row["sourceRefs"] = [{"sourceId": source_id, "row": index}]
    return row


class SelectionTest(unittest.TestCase):
    def test_zero_is_measurement(self):
        row, errors = normalize(source_row())
        self.assertEqual(errors, [])
        self.assertEqual(row["nutrients"]["fatG"], 0)

    def test_missing_trace_invalid_negative_are_not_zero(self):
        for value in [None, "", " ", "-", "N/A", "NaN", "Infinity", "-1", "<0.1", "Tr", "1,2"]:
            with self.subTest(value=value), self.assertRaises(ValueError):
                number(value)

    def test_optional_fields_do_not_exclude_food(self):
        _, errors = normalize(source_row(**{"비타민 D(μg)": "", "1인(회)분량 참고량": ""}))
        self.assertEqual(errors, [])

    def test_each_required_field_is_checked(self):
        for field in list(FIELDS.values()) + list(NUTRIENTS.values()):
            with self.subTest(field=field):
                _, errors = normalize(source_row(**{field: ""}))
                self.assertTrue(errors)

    def test_volume_is_preserved_and_invalid_basis_rejected(self):
        row, errors = normalize(source_row(**{"영양성분함량기준량": "100ml"}))
        self.assertEqual(errors, [])
        self.assertEqual((row["basisAmount"], row["basisUnit"]), (100, "mL"))
        for basis in ["100oz", "0g", "1인분"]:
            _, errors = normalize(source_row(**{"영양성분함량기준량": basis}))
            self.assertTrue(errors)

    def test_date_and_excel_serial(self):
        self.assertEqual(reference_date("46020"), "2025-12-29")
        self.assertEqual(reference_date("2026-09-29"), "2026-09-29")
        with self.assertRaises(ValueError):
            reference_date("2026-02-30")

    def test_latest_complete_row_and_provenance(self):
        old = candidate("mfds_xlsx", 2, "2025-12-29", "100")
        new = candidate("data_go_kr_json", 1, energy="200")
        foods, conflicts = merge([old, new])
        self.assertEqual(len(foods), 1)
        self.assertEqual(foods[0]["nutrients"]["energyKcal"], 200)
        self.assertEqual(sum(r["selected"] for r in foods[0]["sourceRefs"]), 1)
        self.assertEqual(len(conflicts), 1)
        self.assertIn("nutrients", conflicts[0]["otherVersions"][0]["differentFields"])

    def test_same_date_tie_and_independent_same_name_codes(self):
        a = candidate("mfds_xlsx", 2)
        b = candidate("data_go_kr_json", 1, energy="200")
        c = copy.deepcopy(a)
        c["code"] = "D002"
        foods, _ = merge([a, b, c])
        self.assertEqual(len(foods), 2)
        self.assertEqual(foods[0]["nutrients"]["energyKcal"], 200)

    def test_input_order_does_not_change_merge(self):
        a = candidate("data_go_kr_json", 3, energy="200")
        b = candidate("data_go_kr_json", 1, energy="100")
        self.assertEqual(merge([a, b]), merge([b, a]))

    def test_xlsx_sparse_cells_and_shared_strings(self):
        columns = list(FIELDS.values()) + list(NUTRIENTS.values())
        columns.append("비타민")
        # Skip a nutrient cell intentionally: the next column must not shift left.
        letters = [chr(65 + i) for i in range(len(columns))]
        header = "".join(f'<c r="{col}1" t="inlineStr"><is><t>{name}</t></is></c>'
                         for col, name in zip(letters, columns))
        raw = source_row()
        values = "".join(f'<c r="{col}2" t="inlineStr"><is><t>{raw.get(name, "")}</t></is></c>'
                         for col, name in zip(letters, columns) if name != "지방(g)")
        values = values.replace('<c r="B2" t="inlineStr"><is><t>음식</t></is></c>', '<c r="B2" t="s"><v>0</v></c>')
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "fixture.xlsx"
            with zipfile.ZipFile(path, "w") as z:
                z.writestr("xl/workbook.xml", '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="foods" sheetId="1" r:id="rId1"/></sheets></workbook>')
                z.writestr("xl/_rels/workbook.xml.rels", '<Relationships><Relationship Id="rId1" Target="worksheets/sheet1.xml"/></Relationships>')
                z.writestr("xl/sharedStrings.xml", '<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><si><t>음식</t></si></sst>')
                z.writestr("xl/worksheets/sheet1.xml", f'<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData><row r="1">{header}</row><row r="2">{values}</row></sheetData></worksheet>')
            index, result = list(xlsx_rows(path))[0]
            self.assertEqual(index, 2)
            self.assertEqual(result["식품명"], "음식")
            self.assertEqual(result["지방(g)"], "")
            self.assertEqual(result["당류(g)"], "0")


class GeneratedDatasetTest(unittest.TestCase):
    def test_catalog_contract_and_reconciliation(self):
        root = Path(__file__).resolve().parents[1] / "data/processed"
        catalog = json.loads((root / "foods.json").read_text())
        manifest = json.loads((root / "manifest.json").read_text())
        report = json.loads((root / "quality_report.json").read_text())
        foods = catalog["foods"]
        self.assertTrue(foods)
        self.assertEqual(catalog["schemaVersion"], 1)
        self.assertEqual(len({f["code"] for f in foods}), len(foods))
        self.assertEqual([f["code"] for f in foods], sorted(f["code"] for f in foods))
        for f in foods:
            self.assertEqual(set(f), {"code", "name", "category", "origin", "referenceDate", "basisAmount", "basisUnit", "nutrients", "sourceRefs"})
            self.assertTrue(all(f[key] for key in ["code", "name", "category", "origin", "referenceDate"]))
            self.assertGreater(f["basisAmount"], 0)
            self.assertEqual(set(f["nutrients"]), set(NUTRIENTS))
            self.assertTrue(all(type(v) in (int, float) and v >= 0 for v in f["nutrients"].values()))
            self.assertEqual(sum(r["selected"] for r in f["sourceRefs"]), 1)
        summary = manifest["summary"]
        self.assertEqual(summary["outputFoods"], len(foods))
        self.assertEqual(summary["inputRows"], summary["acceptedRowsBeforeDeduplication"] + summary["rejectedRows"])
        self.assertEqual(summary["acceptedRowsBeforeDeduplication"], len(foods) + summary["duplicateRowsRemoved"])
        self.assertEqual(summary["rejectedRows"], len(report["rejectedRows"]))
        self.assertEqual(summary["codesWithConflictingVersions"], len(report["conflicts"]))
        self.assertEqual(sum(summary["selectedRowsBySource"].values()), len(foods))
        self.assertEqual(manifest["outputSha256"], hashlib.sha256((root / "foods.json").read_bytes()).hexdigest())


if __name__ == "__main__":
    unittest.main()
