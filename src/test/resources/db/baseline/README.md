# 테스트용 서비스 스키마 스냅숏

여기 있는 `V0_*.sql`은 **goodquestion-backend의 `db/migration/V1~V8`을 그대로 복사한
것**이다. 파일 안을 고치지 않는다.

## 왜 필요한가

관리자 콘솔은 서비스 백엔드와 같은 DB를 본다. 실제 환경에서는 서비스 백엔드가 이미
`parents`, `stories`, `story_sessions` 같은 테이블을 만들어 둔 상태이고, 관리자 쪽
마이그레이션(`db/migration/V1__init_admin_schema.sql`)은 그 위에 관리자 테이블과 공유
테이블을 얹는다. 관리자 마이그레이션의 `inquiries.parent_id`가 `parents(id)`를 참조하는
것이 그 전제 위에서 성립한다.

테스트는 빈 PostgreSQL 컨테이너에서 시작하므로 그 전제가 없다. 서비스 테이블이 없는
DB에 관리자 마이그레이션만 돌리면 외래키에서 바로 실패한다. 이 폴더가 "서비스 백엔드가
이미 돌아 있는 상태"를 테스트 DB에 재현한다.

버전을 `V0_1` ~ `V0_8`로 붙인 것은 관리자 마이그레이션 `V1`보다 먼저 돌게 하면서
버전 번호가 겹치지 않게 하려는 것이다(0.1 < 1). 이 폴더는 `src/test/resources` 아래에
있어서 운영 기동에는 포함되지 않는다 -- `application-test.yml`의
`spring.flyway.locations`가 테스트에서만 이 경로를 추가한다.

## 서비스 스키마가 바뀌면

goodquestion-backend에 `V9`, `V10`이 추가되면 여기에도 `V0_9`, `V0_10`으로 복사한다.
복사하지 않아도 테스트는 통과할 수 있지만, 그때부터 테스트 DB와 운영 DB가 갈리기
시작하고 관리자 쪽에서만 안 잡히는 문제가 생긴다.

단, **공유 테이블**(notices, guides, inquiries, inquiry_answers, notifications,
device_tokens, daily_visits)을 만드는 서비스 쪽 마이그레이션은 복사하지 않는다. 그건
관리자 `V1__init_admin_schema.sql`이 이미 만든다. 양쪽 다 `if not exists`라 겹쳐도
문제는 없지만, 복사해 두면 어느 쪽이 원본인지 알 수 없게 된다.
