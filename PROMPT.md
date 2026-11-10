# 사용자 프롬프트 및 커밋 연결

사용자 메시지는 오탈자·경로·공백을 포함한 원문을 보존한다. 환경 컨텍스트 및 도구 출력은 사용자 작업 프롬프트에 포함하지 않는다.

## P001

```text
kotlin으로 식단 관리 프로그램 만들건데 계획, 데이터 선별부터 하자 앞으로 작업시 PROMPT.md에 내가 말한 프롬프트 전문과 관련 커밋 해시 매칭해서 써놔 데이터 repository branch, 영양소 계산 로직 branch, ui branch 분리해서 작업하자 커밋은 한글로 하고 작업 단위로 해 jetpack compose로 만들거고 /Users/jm/Downloads/20251229_음식DB\ 19495건.xlsx 출처는 https://various.foodsafetykorea.go.kr/nutrient/general/down/historyList.do 이고 /Users/jm/Downloads/전국통합식품영양성분정보_음식_표준데이터.json 출처는 https://www.data.go.kr/data/15100070/standard.do 두 데이터 합쳐서 json으로 만들자 결측치 없는 항목들만 이용해 출처는 md에 기록해놓고
```

관련 커밋: 구현 커밋 생성 후 아래 표에 기록한다.

## P002

```text
앱 공통 필수 필드에 결측치 없는 행 사용해
```

관련 커밋: 구현 커밋 생성 후 아래 표에 기록한다.

## 커밋 매핑

| 프롬프트 | 전체 커밋 해시 | 작업 |
| --- | --- | --- |

커밋 트레일러 `Prompt-ID`로도 조회한다. 이 매핑만 갱신하는 커밋은 자기 해시를 기록하지 않는다(`git log -- PROMPT.md`).
