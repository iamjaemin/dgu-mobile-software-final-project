# 2026 모바일 소프트웨어 학기말 프로젝트

매일 먹은 음식과 사진을 기록하고, 하루부터 한 달까지 열량과 영양 균형을 확인하는 **오프라인 식단 관리 Android 앱**입니다.

## 주요 기능

- **식사 입력**: 식사 날짜·시간 → 음식·반찬 및 섭취량 → 사진 → 영양소 미리보기의 4단계로 기록합니다. 한국어 달력과 시·분 휠을 사용합니다.
- **음식 검색**: 메뉴 분류 카드, 검색어·카테고리·식품 기원·단위 필터를 제공합니다. 음식 카드 선택 시 추가 완료 스낵바를 표시합니다.
- **식사 조회**: 사진 카드의 2열 그리드, 날짜별 총 열량·끼니 수, 식사 상세와 삭제 기능을 제공합니다.
- **식사 분석**: 1달(기본)·7일·14일·하루를 선택해 총 열량, 탄단지 비율, 시간대별 영양소, 날짜별 열량 그래프를 확인합니다.
- **로컬 보관**: 식사와 사진을 앱 내부에 저장하고 재실행 시 복원합니다. 앱의 데이터 출처 화면에서 원본 제공처를 확인할 수 있습니다.

## 기술 스택 및 구조

| 영역 | 사용 기술 |
| --- | --- |
| 언어·UI | Kotlin, Jetpack Compose, Material 3, Pretendard |
| 상태 관리 | Android ViewModel, StateFlow, Kotlin Coroutines |
| 데이터 저장 | JSON, Android AtomicFile, 앱 내부 사진 파일 |
| 계산·그래프 | 순수 Kotlin 계산 로직, java.time, Compose Canvas |
| 데이터 전처리 | Python 3 표준 라이브러리로 Excel·JSON 통합 |
| 빌드·검증 | Gradle, JUnit, AndroidX Test, Compose UI Test, Android lint |

단일 `app` 모듈에서 **UI → ViewModel → 저장소/계산 로직**으로 역할을 분리했습니다.

```text
app/src/main/java/com/example/dgumobilesoftwarefinalproject/
├── MainActivity.kt       # Compose 진입점
├── ui/                   # 화면, 테마, 차트, MealViewModel
├── model/                # Food, Meal, MealItem, Nutrients
├── data/                 # 음식 조회, 식사 JSON·사진 저장, 직렬화
└── domain/               # 섭취량 계산, 기간·시간대별 집계
```

ViewModel이 입력과 화면 상태를 관리하고, 파일 읽기·쓰기는 IO 코루틴에서 처리합니다. 식사는 저장 당시 영양소를 함께 보관해 이후 음식 DB가 바뀌어도 과거 기록의 계산값이 유지됩니다. AtomicFile로 저장 실패 시 기존 파일을 보호합니다.

## 열량·영양소 계산 방법

DB의 기준량과 실제 섭취량의 비율을 각 성분에 곱합니다.

```text
배율 = 섭취량 / DB 기준량
섭취 열량(kcal) = DB 열량 × 배율
섭취 영양소(g) = DB 탄수화물·단백질·지방 각각의 함량 × 배율
```

예를 들어 **국밥_돼지머리 100g당 137kcal**를 150g 먹으면 `137 × 1.5 = 205.5kcal`입니다. 같은 방식으로 탄수화물 23.91g, 단백질 10.05g, 지방 7.74g을 계산합니다.

- g 음식은 g, mL 음료는 mL로 계산하며 두 단위를 임의로 환산하지 않습니다. 입력은 0보다 큰 유한한 값만 저장합니다.
- 음식별 값을 합쳐 한 끼를 계산하고, 해당 기간의 식사를 합쳐 일간·주간·월간 값을 구합니다. 계산 중에는 반올림하지 않고 화면에 소수 첫째 자리까지 표시합니다.
- **총 열량은 DB kcal 값**을 사용합니다. 도넛 그래프의 탄단지 에너지 비율만 탄수화물·단백질 `4kcal/g`, 지방 `9kcal/g`으로 계산하며, 환산 합계가 0이면 비율도 0%입니다.
- 시간대는 실제 식사 시각에 따라 새벽(00–06시), 오전(06–12시), 오후(12–18시), 저녁(18–24시)으로 나눕니다. 각 구간은 시작 시각을 포함하고 종료 시각은 포함하지 않습니다.
- 최근 7일·14일은 오늘을 포함하며, 한 달은 `오늘.minusMonths(1).plusDays(1)`부터 오늘까지입니다. 기록이 없는 날짜도 그래프와 일평균에 0kcal로 포함합니다. 한 번 저장한 식사 기록을 1끼로 셉니다.

## 설치 및 실행 방법

**환경:** Android Studio, JDK 25, Android SDK Platform 37, Build Tools 36.0.0. 실행 기기는 Android 7.0(API 24) 이상입니다. Gradle JDK 또는 `JAVA_HOME`을 JDK 25로 지정합니다.

1. 프로젝트 루트 폴더를 Android Studio에서 열고 `feature/ui-compose` 브랜치를 선택합니다.
2. SDK 경로를 설정하고 Gradle 동기화를 완료합니다. 환경별 `local.properties`는 Android Studio에서 생성합니다.
3. Device Manager에서 에뮬레이터를 실행하거나 USB 디버깅을 켠 기기를 연결합니다.
4. `app` 구성을 Run으로 실행합니다. 음식 입력 화면에서 첫 기록을 작성합니다.

터미널에서는 프로젝트 루트에서 실행합니다. Windows에서는 `./gradlew` 대신 `gradlew.bat`를 사용합니다. 최초 빌드에는 의존성 다운로드를 위한 인터넷 연결이 필요합니다.

```sh
./gradlew :app:assembleDebug
# Android SDK platform-tools가 PATH에 있고 기기가 연결된 상태에서
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.dgumobilesoftwarefinalproject/.MainActivity
```

APK 경로는 `app/build/outputs/apk/debug/app-debug.apk`입니다. 파일을 기기로 옮겨 직접 설치할 수도 있습니다.

## 영양 데이터 출처

| 원본 자료 | 공식 출처 | 원본 행 수 |
| --- | --- | ---: |
| 식품의약품안전처 `20251229_음식DB 19495건.xlsx` | [K-FIND 음식 DB 내려받기](https://various.foodsafetykorea.go.kr/nutrient/general/down/historyList.do) | 19,495 |
| `전국통합식품영양성분정보_음식_표준데이터.json` | [공공데이터포털 15100070](https://www.data.go.kr/data/15100070/standard.do) | 19,738 |

두 원본에서 **앱 필수 필드와 9종 영양성분에 결측치가 없는 행**을 선별하고 식품코드로 중복을 정리했습니다. 최종 **2,784개**는 100g 기준 음식 2,493개와 100mL 기준 음료 291개입니다. 결측치를 0으로 채우지 않고 실제 측정값 0은 유지합니다.

같은 코드는 최신 기준일의 완전한 행을 선택합니다. 이번 자료는 JSON의 **2026-09-29** 기준 값이 선택되었고 Excel 참조도 보존했습니다. 앱에서는 선별된 자료 중 열량·탄수화물·단백질·지방을 표시합니다.

통합 데이터는 [foods.json](data/processed/foods.json), 앱 배포본은 [assets/foods.json](app/src/main/assets/foods.json)입니다. 원본 해시·선별 기준·재생성 절차는 [데이터 출처 문서](docs/DATA_SOURCES.md)에 기록했습니다. 폰트 출처와 라이선스는 [THIRD_PARTY.md](docs/THIRD_PARTY.md)를 참고합니다.

## 검증 및 작업 관리

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest  # 기기/에뮬레이터 연결 필요
python3 -m unittest discover -s scripts -p 'test_*.py' -v
```

저장소·계산·UI는 `feature/data-repository`, `feature/nutrition-calculation`, `feature/ui-compose`로 분리했습니다. 통합 앱은 UI 브랜치에 있습니다. 상세 검증은 [기능 명세](docs/APP_SPEC.md), 작업 이력은 [PROMPT.md](PROMPT.md)에서 확인할 수 있습니다.
