# 식단 관리 Android 앱

Kotlin + Jetpack Compose 기반 식단 관리 앱 **오늘을 담다**. 흰색·주황색 카드 UI로 매일 먹은 음식과 영양 균형을 기록한다.

- 입력: 날짜·시간·끼니 종류, 선택한 사진, 음식과 반찬 여러 항목, 섭취량, 탄단지 및 kcal 실시간 미리보기.
- 식사 보기: 사진과 날짜별 목록, 일별 열량·끼니 수, 상세 영양소, 기록 삭제.
- 분석: 선택한 하루와 최근 7일 영양 균형, 최근 한 달 열량 및 조식·중식·석식·간식/음료별 영양소.
- 오프라인 저장: 식사 JSON과 사진을 앱 내부에 보관. 데이터 출처 화면에서 두 원본 제공처 링크 확인.

- [개발 계획](docs/PLAN.md)
- [출처 및 데이터 통합 기준](docs/DATA_SOURCES.md)
- [요청 원문과 커밋 이력](PROMPT.md)
- [작업 규칙](AGENTS.md)
- [기능 명세 및 검증](docs/APP_SPEC.md)

통합 앱 브랜치는 `feature/ui-compose`이며 저장소와 계산 작업은 각각의 기능 브랜치에 커밋되어 있다. main에는 자동 병합하지 않는다.

Android Studio에서 프로젝트를 열고 Gradle 동기화 후 app을 실행한다. Android SDK 37, JDK 25가 필요하다. 최소 Android 버전은 API 24이며 java.time을 desugaring한다. 앱 기본 데이터는 2,784개다. g 음식과 mL 음료는 각 단위로 입력한다.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
# 기기 또는 에뮬레이터가 연결되어 있을 때
./gradlew :app:connectedDebugAndroidTest
python3 -m unittest discover -s scripts -p 'test_*.py' -v
```

APK는 `app/build/outputs/apk/debug/app-debug.apk`에 생성된다. 데이터 재생성 방법은 출처 문서를 참고한다. 원본을 갱신하면 `data/processed/foods.json`과 앱의 `app/src/main/assets/foods.json`을 함께 갱신한다.
