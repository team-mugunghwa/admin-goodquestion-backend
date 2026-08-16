# Railway 배포 가이드

관리자 백엔드를 Railway에 배포하는 절차.

## 가장 중요한 것

**서비스 백엔드와 같은 PostgreSQL을 봐야 한다.** DB를 새로 만들면 관리자가 고친
공지와 이야기가 사용자 앱에 전혀 보이지 않는다. 배포가 성공했는데 아무것도
연동되지 않는 형태로 나타나므로 알아채기 어렵다.

그래서 **goodquestion-backend가 이미 떠 있는 Railway 프로젝트 안에 서비스를
추가**한다. 새 프로젝트를 만들면 내부 네트워크가 갈려 그 Postgres를 참조할 수 없다.

## 사전 준비

- Railway 계정과 goodquestion-backend가 배포된 기존 프로젝트
- 관리자 토큰 서명 키. `openssl rand -base64 32` 로 만든다
- FCM 서비스 계정 JSON (푸시를 쓸 경우. 없어도 배포는 된다)

## 1. 서비스 추가

1. 기존 Railway 프로젝트를 연다. `goodquestion-backend` 와 `Postgres` 가 있어야 한다
2. New -> GitHub Repo -> `admin-goodquestion-backend` 선택
3. Railway가 저장소 루트의 `Dockerfile` 과 `railway.toml` 을 감지해 빌드한다
4. 첫 배포는 환경변수가 없어 실패한다. 정상이다

첫 배포는 빌드까지 성공하고 **헬스체크에서 멈춘다.** Deploy Logs를 열면 무엇이
빠졌는지 한글로 나온다. 빠진 값을 한 번에 다 보여주므로 하나씩 고쳐가며 다시
배포할 필요가 없다.

```
======================================================================
관리자 백엔드를 시작할 수 없습니다. 환경변수를 확인하세요.
======================================================================

- DB_URL 이 비어 있습니다.
  서비스 백엔드와 같은 DB 를 가리켜야 합니다. jdbc: 로 시작해야 합니다.
  ...
```

## 2. 환경변수

서비스 -> Variables 탭에 넣는다. `${{ Postgres.VAR }}` 는 같은 프로젝트의 다른
서비스 변수를 참조하는 Railway 문법이다.

```
DB_URL=jdbc:postgresql://${{ Postgres.PGHOST }}:${{ Postgres.PGPORT }}/${{ Postgres.PGDATABASE }}
DB_USERNAME=${{ Postgres.PGUSER }}
DB_PASSWORD=${{ Postgres.PGPASSWORD }}

ADMIN_JWT_SECRET=<openssl rand -base64 32 결과>
ADMIN_JWT_EXPIRATION_MS=1800000
ADMIN_JWT_REFRESH_EXPIRATION_MS=43200000

CORS_ALLOWED_ORIGIN_PATTERNS=https://admin-goodquestion-frontend.vercel.app,https://admin-goodquestion-frontend-*-team-mugunghwa.vercel.app

SERVICE_BASE_URL=https://goodquestion-frontend.vercel.app
```

`DB_URL` 은 `jdbc:` 로 시작해야 한다. Postgres 서비스가 주는 `DATABASE_URL` 은
`postgresql://` 스킴이라 Spring이 읽지 못한다.

`${{ Postgres.PGHOST }}` 의 `Postgres` 는 **그 프로젝트에 있는 서비스 이름과 정확히
같아야 한다.** 이름이 다르면 Railway는 오류를 내지 않고 **조용히 빈 값으로 바꾼다.**
그러면 `DB_URL` 이 `jdbc:postgresql://:/` 가 되고, 드라이버는 "claims to not accept
jdbcUrl" 이라고만 말해서 원인이 보이지 않는다. 기동 시점 검사가 이 경우를 잡아
어디가 비었는지 알려 준다.

Variables 탭에서 값이 실제 호스트명으로 치환돼 보이는지 눈으로 확인하는 것이 가장 빠르다.

**`ADMIN_JWT_SECRET` 은 서비스 백엔드의 `JWT_SECRET` 과 반드시 다른 값을 쓴다.**
같으면 보호자 앱이 받은 토큰의 서명이 관리자 API에서도 통과한다.

### 푸시를 쓸 경우

```
FCM_CREDENTIALS=<서비스 계정 JSON 원문 전체>
FCM_PROJECT_ID=<Firebase 프로젝트 id>
```

`FCM_CREDENTIALS` 는 파일 경로와 JSON 원문을 모두 받는다. Railway는 파일을 올릴
수 없으므로 JSON을 통째로 붙여 넣는다.

**비워 두면 푸시만 나가지 않고 앱은 정상 기동한다.** 알림은 DB에 쌓이므로
사용자는 앱 안 알림함에서 답변을 확인할 수 있다. 반대로 값이 있는데 읽지
못하면 기동을 막는다. 넣었는데 조용히 안 나가는 상태가 제일 나쁘기 때문이다.

## 3. 공개 주소 만들기

서비스 -> Settings -> Networking -> Generate Domain.
`admin-goodquestion-backend-production.up.railway.app` 형태의 주소가 나온다.

관리자 콘솔(Vercel)의 `API_BASE_URL` 에 넣을 값은 여기에 `/api/admin` 을 붙인 것이다.

## 4. 확인

```bash
curl https://<주소>/actuator/health
```

기동 로그에서 확인할 것.

```
Migrating schema "public" to version "1 - init admin schema"
Successfully applied N migrations
Started AdminGoodquestionBackendApplication
```

FCM 자격증명을 안 넣었다면 아래 경고가 함께 뜬다. 오류가 아니다.

```
FCM 자격증명이 없어 푸시를 실제로 보내지 않습니다.
```

## 5. 배포 순서에 관한 주의

관리자 백엔드와 서비스 백엔드는 **공유 테이블 DDL을 양쪽에 중복으로 갖고 있고
둘 다 `if not exists`** 다. 어느 쪽이 먼저 떠도 된다. 자세한 배경은
[admin-backend-guide.md](admin-backend-guide.md) 3절에 있다.

다만 **서비스 백엔드를 먼저 최신으로 배포**하는 편이 낫다. 그쪽의
`V11__add_service_desk.sql`, `V12__add_parent_status.sql` 이 적용돼 있어야
사용자 앱의 공지와 문의 화면이 함께 동작한다.

## 6. 시드 관리자 계정을 반드시 바꾼다

첫 배포 때 `R__1_seed_admin.sql` 이 아래 두 계정을 만든다.

| 이메일 | 비밀번호 |
| --- | --- |
| admin@goodquestion.kr | admin1234! |
| cs@goodquestion.kr | admin1234! |

**공개 주소가 생기는 순간 누구나 이 값으로 로그인할 수 있다.** 배포 직후
로그인해서 내 계정 화면에서 비밀번호를 바꾸고, 쓰지 않는 계정은 관리자 계정
화면에서 지운다.

## 재배포

`develop` 브랜치에 push하면 자동으로 다시 배포된다. 서비스 백엔드와 같은 설정이다.

## 기동이 안 될 때

앱은 뜨기 전에 `DB_URL` 과 `ADMIN_JWT_SECRET` 을 확인하고, 문제가 있으면 Deploy
Logs에 한글 안내를 남기고 종료한다(`RequiredEnvironmentListener`). 스택트레이스
없이 안내만 나오므로 로그 맨 위를 보면 된다.

| 안내 문장 | 뜻 |
| --- | --- |
| `DB_URL 이 비어 있습니다` | 변수를 넣지 않았다 |
| `DB_URL 이 jdbc: 로 시작하지 않습니다` | `DATABASE_URL` 을 그대로 붙여 넣었다 |
| `DB_URL 의 Railway 변수 참조가 풀리지 않았습니다` | 다른 프로젝트에 서비스를 만들었다 |
| `DB_URL 에 호스트(PGHOST) ... 이(가) 비어 있습니다` | 참조가 빈 값으로 바뀌었다. Postgres 서비스 이름이 다르다 |
| `ADMIN_JWT_SECRET 이 비어 있습니다` | 서명 키를 넣지 않았다 |
| `ADMIN_JWT_SECRET 이 너무 짧습니다` | HS256은 32바이트 이상이 필요하다 |

**이 안내가 없는데 헬스체크만 실패한다면** 환경변수 문제가 아니다. DB 접속
실패(`password authentication failed`, 연결 타임아웃)나 Flyway 마이그레이션
오류를 로그에서 찾는다.

`DB_USERNAME` 과 `DB_PASSWORD` 는 검사하지 않는다. 비워 두고 쓰는 로컬 설정이
있어서다. 값이 틀리면 드라이버가 분명하게 말해 준다.
