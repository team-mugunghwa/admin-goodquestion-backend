# 관리자 백엔드 개발 가이드

> **목적**: 이 문서 하나로 프로젝트를 띄우고, 구조의 설계 의도를 이해하고,
> 자기 담당 API 개발에 착수할 수 있게 한다.

엔드포인트와 요청/응답 필드는 [API.md](API.md)가 원본이다.

---

## 1. 이 서비스가 무엇인가

굿퀘스천 운영자가 쓰는 관리자 콘솔의 백엔드다. **사용자 서비스와 같은
PostgreSQL을 본다.**

```
              +--------------------------+
              |  PostgreSQL (한 개)       |
              +--------------------------+
                  ▲                   ▲
                  |                   |
   goodquestion-backend        admin-goodquestion-backend
        :8080                          :8081
          ▲                              ▲
          |                              |
   goodquestion-frontend        admin-goodquestion-frontend
     (Flutter, 사용자)             (Flutter Web, 운영자)
```

관리자가 공지를 공개하면 사용자 앱의 공지 목록에 그대로 나간다. 복제나 동기화
계층이 없다 - 같은 테이블을 양쪽이 본다.

---

## 2. 로컬 개발 환경

### 요구 사항

- JDK 25
- PostgreSQL 17 (사용자 서비스와 같은 DB)
- Docker (테스트용)

### .env 파일

```bash
cp .env.example .env
```

`DB_URL`은 **goodquestion-backend와 같은 값**을 넣는다. `ADMIN_JWT_SECRET`은
서비스의 `JWT_SECRET`과 **반드시 다른 값**을 쓴다 - 같으면 보호자 앱이 받은
토큰의 서명이 여기서도 통과한다.

### 실행

```bash
./gradlew bootRun     # http://localhost:8081
```

기동 로그에 아래가 뜨면 정상이다.

```
Migrating schema "public" to version "1 - init admin schema"
Migrating schema "public" with repeatable migration "1 seed admin"
FCM 자격증명이 없어 푸시를 실제로 보내지 않습니다. ...
Started AdminGoodquestionBackendApplication
```

세 번째 줄은 **오류가 아니다.** Firebase 자격증명이 없을 때의 정상 동작이다
(6절 참고).

시드 관리자 계정은 `admin@goodquestion.kr` / `admin1234!` 다.
**운영 배포 전에 반드시 바꾼다.**

### 테스트

```bash
./gradlew test
```

Docker만 떠 있으면 된다. Testcontainers가 `postgres:17`을 띄우고 빈 DB에
마이그레이션을 처음부터 적용한다. 개발 DB는 건드리지 않는다.

관리자 마이그레이션은 서비스 테이블(`parents` 등)이 있는 상태를 전제하므로,
`src/test/resources/db/baseline/`이 그 상태를 테스트 DB에 재현한다. 자세한
내용은 그 폴더의 README에 있다.

---

## 3. DB 마이그레이션 - 가장 먼저 이해할 것

**두 애플리케이션이 한 DB를 공유하지만 Flyway 이력 테이블은 따로 쓴다.**

| 앱 | 이력 테이블 |
| --- | --- |
| goodquestion-backend | `flyway_schema_history` |
| admin-goodquestion-backend | `flyway_schema_history_admin` |

나누지 않으면 서로의 마이그레이션을 "적용되지 않은 것"으로 보고 다시 돌리려 든다.

### 공유 테이블은 양쪽에 같은 DDL이 들어 있다

`notices`, `guides`, `inquiries`, `inquiry_answers`, `notifications`,
`device_tokens`, `daily_visits`, `parents.status` 여덟 가지다.

| 파일 | 위치 |
| --- | --- |
| `V1__init_admin_schema.sql` 뒷부분 | 이 저장소 |
| `V11__add_service_desk.sql` | goodquestion-backend |
| `V2__add_parent_status.sql` / `V12__add_parent_status.sql` | 양쪽 |

**왜 중복인가.** 어느 앱이 먼저 기동할지 정해져 있지 않다. 그래서 "상대가 이미
만들었을 것"에 기댈 수 없고, 각자의 테스트는 빈 컨테이너에서 자기 마이그레이션만으로
스키마를 만들어야 한다. 전부 `if not exists`로 적어 먼저 도는 쪽이 만들고 나중에
도는 쪽은 조용히 넘어간다.

**컬럼을 바꿀 때는 반드시 양쪽에 같은 `V{n}` 파일을 넣는다.** 한쪽만 고치면 먼저
뜬 쪽 정의가 이기고 나머지는 아무 말 없이 무시된다. 실제로 한 번 어긋났다 -
`inquiry_answers.admin_id`에 외래키를 걸었다가, 서비스 쪽에는 `admin_accounts`가
없어 제약이 있기도 하고 없기도 한 스키마가 되는 것을 뒤늦게 발견하고 뺐다.

### baseline-on-migrate 주의

이미 서비스 테이블이 있는 DB에 이력 테이블만 새로 만드는 상황이라
`baseline-on-migrate: true`가 필요하다. 이때 **`baseline-version: 0`을 같이
둬야 한다.** 기본값 1로 두면 V1을 "이미 적용된 것"으로 표시하고 건너뛴다.

---

## 4. 패키지 구조

```
com.mugunghwa.goodquestion.admin
+-- global        공유 커널. 보안/오류/설정/감사 로그. 도메인을 모른다
+-- auth          관리자 계정과 로그인
+-- dashboard     조립 계층. 여러 도메인 집계를 합쳐 한 번에 내린다
+-- story         이야기/장면/캐릭터/주제
+-- member        보호자/아이/학습 세션/로그인 세션
+-- notice        공지사항
+-- guide         이용안내
+-- support       고객센터 문의와 답변
+-- notification  알림함과 푸시 발송
+-- content       공지/이용안내가 공유하는 노출 상태 enum
```

**서비스 백엔드와 엔티티를 공유하지 않는다.** 같은 테이블을 각자 매핑한다.
공유 라이브러리를 만들면 배포가 묶이고, 관리자 쪽이 필요 없는 컬럼까지 끌고 온다.
관리자 쪽 엔티티는 화면이 쓰는 컬럼만 매핑한다 - 비밀번호 해시와 소셜 식별자는
아예 매핑하지 않아 실수로 응답에 실릴 길이 없다.

---

## 5. 보안

**기본이 잠금이다.** 인증 없이 여는 경로는 헬스체크와 로그인/재발급 셋뿐이다.
새 컨트롤러를 추가할 때 `SecurityConfig`를 고칠 일이 없어야 정상이다.

| 항목 | 값 |
| --- | --- |
| 액세스 토큰 | 30분. 관리자 화면은 개인정보를 다루므로 짧게 둔다 |
| 리프레시 토큰 | 12시간. 쓸 때마다 회전하고 원문 대신 SHA-256 해시를 저장한다 |
| 로그인 실패 잠금 | 5회, 15분 고정 |
| 권한 | ADMIN / SUPER_ADMIN 둘. 갈리는 것은 "관리자 계정을 만들고 지울 수 있는가" 하나뿐 |

### 감사 로그

상태를 바꾸는 조작만 남긴다. 조회까지 남기면 목록 한 번 여는 것만으로 수십 건이
쌓여 정작 확인할 삭제와 정지가 묻힌다.

`AuditLogger`는 호출부의 트랜잭션에 그대로 참여한다. 조작이 롤백되면 로그도 같이
사라져야 한다 - 남아 있으면 "삭제했다"는 기록만 있고 데이터는 그대로인 상태가
되어 로그를 믿을 수 없게 된다.

---

## 6. 푸시 알림

고객센터 답변이 등록되면 사용자에게 푸시를 보낸다.

### 벤더 선택

| 후보 | 판단 |
| --- | --- |
| **FCM** | 발송량 무료, Flutter 공식 플러그인이 iOS/안드로이드/웹을 덮음. **채택** |
| OneSignal | 무료 구간에 사용자 수 제한, 안드로이드는 결국 FCM 경유 |
| Expo Push | Expo로 만든 React Native 앱 전용. Flutter에서 쓸 수 없음 |

`firebase-admin` SDK 대신 HTTP v1을 직접 부른다. 우리가 쓰는 기능은 "토큰 하나에
알림 한 건"뿐이라 SDK가 끌고 오는 gRPC와 Guava가 값을 하지 못한다. 액세스 토큰
발급만 `google-auth-library`에 맡긴다.

### 자격증명이 없으면

`push.fcm.credentials`가 비면 로그만 남기는 발송기가 뜬다. 로컬과 CI에 Firebase
키를 두지 않기 위해서다. **알림 자체는 DB에 쌓이므로 사용자는 앱 안에서 답변을
확인할 수 있다** - 푸시는 알리는 수단이지 전달 경로가 아니다.

반대로 자격증명이 있는데 못 읽으면 **기동을 막는다.** 넣었는데 조용히 안 나가는
상태가 제일 나쁘다.

### 발송 시점

답변 저장 -> 문의 상태 변경 -> 알림 생성까지가 한 트랜잭션이고, **커밋된 뒤에**
푸시가 비동기로 나간다. 트랜잭션 안에서 보내면 뒤에서 롤백이 나도 푸시는 되돌릴
수 없어, 사용자는 알림을 받고 들어왔는데 아무것도 없는 상태를 보게 된다.

---

## 7. 새 API를 추가할 때

1. DTO는 `record`로 만들고 해당 도메인의 `dto` 하위 패키지에 둔다
2. 컨트롤러는 관리자 식별자를 파라미터로 받지 않는다. `@CurrentAdmin AdminPrincipal admin`으로 주입받는다
3. 상태를 바꾸는 조작이면 `AuditLogger`로 기록을 남긴다
4. 새 오류 상황이면 `ErrorCode`에 등록하고 `BusinessException`으로 던진다
5. 목록은 `PageResponse`로 감싼다. 스프링의 `Page`를 그대로 직렬화하지 않는다
6. 계약이 바뀌면 [API.md](API.md)를 함께 고친다

### 리포지터리 인터페이스를 중첩하지 않는다

한 파일에 모으려고 `public interface X { interface ARepository ... }` 형태로
두면 **스프링 데이터가 스캔하지 못한다.** 기동 시 `NoSuchBeanDefinitionException`이
나는데 원인이 한눈에 보이지 않는다. 파일로 나눈다.

---

## 8. 조작 중 서비스에 영향을 주는 것들

관리자 콘솔의 조작 대부분은 사용자에게 즉시 보인다. 특히 아래 셋은 되돌리기
어려우므로 서버가 막는다.

| 조작 | 막는 이유 |
| --- | --- |
| 장면 없는 이야기 공개 | 사용자가 시작하자마자 빈 화면을 본다 |
| 진행 기록 있는 이야기 삭제 | 리포트가 그 이야기를 가리켜, 지우면 무엇을 하고 받은 평가인지 사라진다 |
| 장면이 쓰는 캐릭터 삭제 | DB는 `set null`이라 그냥 지워지고, 그 사실이 사용자가 그 장면에 도달할 때까지 드러나지 않는다 |

계정 정지는 서비스 백엔드가 로그인 경로에서 막는다. 관리자 쪽에서 상태만 바꾸고
그쪽이 보지 않으면 정지가 아무 일도 하지 않는다.
