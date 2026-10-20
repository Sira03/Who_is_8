# Who Is... Riot API + JSP

이 프로젝트는 JSP 화면과 Java 백엔드(Spring Boot)만으로 동작합니다. 플레이어 검색, 최근 전적, 경기 팀 분석, 챔피언 상세 정보, 랭킹 기능을 Java에서 Riot API와 Data Dragon/CommunityDragon 데이터로 처리합니다. 화면 마크업과 `Style.css`는 기존 구성을 유지했습니다.

## 준비
- Java 17 이상
- Maven
- Riot Developer Portal에서 발급한 Riot API Key
- Google AI Studio에서 발급한 Gemini API Key (AI 분석/채팅 기능 사용 시)

## API 키 설정
프로젝트 루트에 `.env` 파일을 만들고 다음 값을 넣습니다. Spring Boot가 실행 시 이 파일을 읽습니다.

```env
RIOT_API_KEY=발급받은_API_키
GEMINI_API_KEY=발급받은_Gemini_API_키
```

Gemini 모델은 기본으로 `gemini-3.8-flash`를 사용합니다. 503 혼잡 응답이 지속되면 `gemini-3.7-flash`, `gemini-3.6-flash`, `gemini-3.5-flash-lite` 순서로 자동 전환합니다. 모델을 바꾸려면 `.env`에 `GEMINI_MODEL=모델_ID`와 필요 시 `GEMINI_FALLBACK_MODELS=모델_ID1,모델_ID2`를 설정할 수 있습니다. Gemini API 키는 브라우저에 전달되지 않고 Java 서버에서만 사용됩니다.

PowerShell에서 환경 변수로 직접 지정할 수도 있습니다.

```powershell
$env:RIOT_API_KEY = "발급받은_API_키"
```

AI 분석을 사용할 때는 `GEMINI_API_KEY` 환경 변수도 설정하세요.

```powershell
$env:GEMINI_API_KEY = "발급받은_Gemini_API_키"
```

## 실행
프로젝트 폴더에서 다음 명령을 실행합니다.

```powershell
mvn spring-boot:run
```

브라우저에서 `http://localhost:8080`을 엽니다.

## WAR 배포
```powershell
mvn package
```

생성된 `target/riot-jsp.war`는 JSP 지원이 활성화된 Tomcat에 배포할 수 있습니다. 배포 서버에도 `RIOT_API_KEY` 환경 변수를 설정하세요.

Riot API Key를 브라우저 코드에 넣거나 공개 저장소에 올리지 마세요.
Gemini API Key도 브라우저 코드나 공개 저장소에 넣지 마세요.
