-- ============================================================
-- 관리자 콘솔 스키마
--
-- 이 파일은 두 부류의 테이블을 만든다.
--
--   1) 관리자 전용    admin_accounts, admin_refresh_tokens, admin_audit_logs
--      관리자 콘솔만 읽고 쓴다. 서비스 백엔드는 존재조차 몰라도 된다.
--
--   2) 서비스 공유    notices, guides, inquiries, inquiry_answers,
--                     notifications, device_tokens, daily_visits
--      관리자가 쓰고 사용자 앱이 읽는다(또는 그 반대). 그래서 서비스 백엔드
--      (goodquestion-backend)의 V9__add_service_desk.sql에도 같은 정의가 있다.
--
-- 2)가 양쪽에 중복으로 들어있는 이유:
--   두 앱이 한 DB를 공유하지만 Flyway 이력 테이블은 따로 쓴다(서비스는
--   flyway_schema_history, 관리자는 flyway_schema_history_admin). 어느 쪽이 먼저
--   기동할지 정해져 있지 않으므로 "상대가 이미 만들었을 것"에 기댈 수 없고,
--   각자의 테스트는 빈 컨테이너에서 자기 마이그레이션만으로 스키마를 만들어야 한다.
--   그래서 공유 테이블 DDL을 전부 if not exists로 적어 먼저 도는 쪽이 만들고
--   나중에 도는 쪽은 조용히 넘어가게 했다.
--
--   대신 두 파일의 공유 테이블 정의는 글자 단위로 같아야 한다. 한쪽만 고치면
--   먼저 뜬 쪽 정의가 이기고 나머지는 아무 말 없이 무시된다 - 컬럼을 바꿀 때는
--   반드시 양쪽에 같은 V{n} 파일을 넣는다.
--
-- 코드값은 서비스 백엔드와 같은 대문자 스네이크케이스를 쓴다.
-- 적용된 뒤에는 수정하지 않는다(체크섬). 스키마 변경은 V2, V3...로 추가한다.
-- ============================================================

create extension if not exists "pgcrypto";

-- ------------------------------------------------------------
-- 1. admin_accounts - 관리자 계정
--    보호자(parents)와 완전히 분리한다. 한 테이블에 역할 컬럼만 얹으면
--    보호자 로그인 경로의 버그가 곧바로 관리자 권한 문제가 된다.
-- ------------------------------------------------------------
create table if not exists admin_accounts (
    id                    uuid         primary key default gen_random_uuid(),
    email                 varchar(255) not null unique,
    password_hash         varchar(100) not null,
    name                  varchar(50)  not null,
    -- SUPER_ADMIN만 관리자 계정을 만들고 지울 수 있다.
    role                  varchar(20)  not null default 'ADMIN'
        check (role in ('ADMIN', 'SUPER_ADMIN')),
    status                varchar(20)  not null default 'ACTIVE'
        check (status in ('ACTIVE', 'SUSPENDED')),
    failed_login_attempts smallint     not null default 0,
    locked_until          timestamptz,
    last_login_at         timestamptz,
    last_login_ip         varchar(45),
    created_at            timestamptz  not null default now(),
    updated_at            timestamptz  not null default now()
);

-- ------------------------------------------------------------
-- 2. admin_refresh_tokens - 관리자 리프레시 토큰
--    원문은 저장하지 않고 해시만 둔다. 서비스의 refresh_tokens와 같은 패턴이다.
-- ------------------------------------------------------------
create table if not exists admin_refresh_tokens (
    id          uuid         primary key default gen_random_uuid(),
    admin_id    uuid         not null references admin_accounts(id) on delete cascade,
    token_hash  varchar(100) not null unique,
    expires_at  timestamptz  not null,
    revoked_at  timestamptz,
    created_at  timestamptz  not null default now()
);

create index if not exists idx_admin_refresh_tokens_admin_id on admin_refresh_tokens(admin_id);

-- ------------------------------------------------------------
-- 3. admin_audit_logs - 관리자 조작 감사 로그
--    관리자 콘솔은 남의 개인정보를 열람하고 콘텐츠를 지울 수 있다.
--    "누가 무엇을 했는가"가 남지 않으면 사고가 났을 때 확인할 방법이 없다.
--    조회는 남기지 않고 상태를 바꾸는 조작만 남긴다 - 조회까지 남기면
--    로그가 조작 기록을 덮어 정작 볼 것이 묻힌다.
-- ------------------------------------------------------------
create table if not exists admin_audit_logs (
    id           uuid        primary key default gen_random_uuid(),
    admin_id     uuid        references admin_accounts(id) on delete set null,
    -- 로그가 남은 뒤 계정이 지워져도 누구였는지는 알아야 한다.
    admin_email  varchar(255) not null,
    action       varchar(40) not null,
    target_type  varchar(40) not null,
    target_id    varchar(64),
    summary      text,
    ip           varchar(45),
    created_at   timestamptz not null default now()
);

create index if not exists idx_admin_audit_logs_created_at on admin_audit_logs(created_at desc);
create index if not exists idx_admin_audit_logs_admin_id on admin_audit_logs(admin_id);

-- ============================================================
-- 여기부터 서비스 공유 테이블.
-- goodquestion-backend/src/main/resources/db/migration/V9__add_service_desk.sql와
-- 같은 내용이어야 한다.
-- ============================================================

-- ------------------------------------------------------------
-- 4. notices - 공지사항
--    관리자가 쓰고 사용자 앱이 읽는다. PUBLISHED만 사용자에게 나간다.
-- ------------------------------------------------------------
create table if not exists notices (
    id            uuid         primary key default gen_random_uuid(),
    title         varchar(200) not null,
    content       text         not null,
    category      varchar(20)  not null default 'GENERAL'
        check (category in ('GENERAL', 'UPDATE', 'EVENT', 'MAINTENANCE')),
    -- 목록 맨 위에 고정. 점검 공지처럼 기간이 지나면 내려야 하는 것에 쓴다.
    pinned        boolean      not null default false,
    status        varchar(20)  not null default 'DRAFT'
        check (status in ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    -- 공개 시점. 작성과 공개가 갈리므로 created_at으로 대신할 수 없다.
    published_at  timestamptz,
    view_count    integer      not null default 0,
    author_name   varchar(50),
    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now(),

    -- 공개 상태라면 공개 시각이 반드시 있어야 목록 정렬이 성립한다.
    constraint ck_notices_published_at check (status <> 'PUBLISHED' or published_at is not null)
);

-- 사용자 목록 조회: 공개분을 고정 먼저, 그다음 최신순
create index if not exists idx_notices_published on notices(status, pinned desc, published_at desc);

-- ------------------------------------------------------------
-- 5. guides - 이용안내
--    "이야기 시작하기", "별가루란?" 같은 도움말 문서. 카테고리 안에서
--    관리자가 정한 순서대로 노출되므로 display_order가 정렬의 원본이다.
-- ------------------------------------------------------------
create table if not exists guides (
    id             uuid         primary key default gen_random_uuid(),
    category       varchar(20)  not null default 'BASIC'
        check (category in ('BASIC', 'ACCOUNT', 'PLAY', 'REWARD', 'TROUBLE')),
    title          varchar(200) not null,
    content        text         not null,
    display_order  smallint     not null default 0,
    status         varchar(20)  not null default 'DRAFT'
        check (status in ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    created_at     timestamptz  not null default now(),
    updated_at     timestamptz  not null default now()
);

create index if not exists idx_guides_published on guides(status, category, display_order);

-- ------------------------------------------------------------
-- 6. inquiries - 고객센터 문의
--    사용자 앱이 쓰고 관리자가 읽는다. 답변은 inquiry_answers에 따로 둔다.
--
--    parent_id는 on delete cascade다. 탈퇴한 보호자의 문의를 남겨 두면
--    개인정보만 남고 응대할 대상은 사라진다.
-- ------------------------------------------------------------
create table if not exists inquiries (
    id           uuid         primary key default gen_random_uuid(),
    parent_id    uuid         not null references parents(id) on delete cascade,
    category     varchar(20)  not null default 'ETC'
        check (category in ('ACCOUNT', 'PAYMENT', 'CONTENT', 'BUG', 'SUGGESTION', 'ETC')),
    title        varchar(200) not null,
    content      text         not null,
    status       varchar(20)  not null default 'PENDING'
        check (status in ('PENDING', 'ANSWERED', 'CLOSED')),
    answered_at  timestamptz,
    created_at   timestamptz  not null default now(),
    updated_at   timestamptz  not null default now(),

    constraint ck_inquiries_answered_at check (status <> 'ANSWERED' or answered_at is not null)
);

create index if not exists idx_inquiries_parent_id on inquiries(parent_id, created_at desc);
-- 관리자 목록의 기본 화면이 "미답변 오래된 순"이라 상태를 앞에 둔다.
create index if not exists idx_inquiries_status on inquiries(status, created_at);

-- ------------------------------------------------------------
-- 7. inquiry_answers - 문의 답변
--    문의당 한 건으로 제한한다(unique). 여러 건을 허용하면 사용자 화면이
--    "어느 것이 최종 답변인가"를 판단해야 하고, 수정과 추가답변이 구분되지 않는다.
--    내용을 고치는 것은 같은 행의 update다.
-- ------------------------------------------------------------
create table if not exists inquiry_answers (
    id           uuid        primary key default gen_random_uuid(),
    inquiry_id   uuid        not null unique references inquiries(id) on delete cascade,
    admin_id     uuid        references admin_accounts(id) on delete set null,
    admin_name   varchar(50) not null,
    content      text        not null,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);

-- ------------------------------------------------------------
-- 8. notifications - 사용자 알림함
--    푸시는 기기가 꺼져 있거나 권한이 없으면 도착하지 않는다. 앱 안에서
--    다시 볼 수 있어야 "답변이 등록되면 사용자가 확인할 수 있다"가 성립하므로
--    푸시와 별개로 알림을 여기 쌓는다. 푸시는 이 행을 알리는 수단일 뿐이다.
-- ------------------------------------------------------------
create table if not exists notifications (
    id          uuid         primary key default gen_random_uuid(),
    parent_id   uuid         not null references parents(id) on delete cascade,
    type        varchar(30)  not null
        check (type in ('INQUIRY_ANSWERED', 'NOTICE', 'SYSTEM')),
    title       varchar(200) not null,
    body        text         not null,
    -- 앱이 이동할 화면 경로. 예: /support/{inquiryId}
    link_path   varchar(200),
    read_at     timestamptz,
    created_at  timestamptz  not null default now()
);

-- 알림함 조회: 보호자별 최신순. 안 읽은 개수 배지도 같은 인덱스로 처리된다.
create index if not exists idx_notifications_parent on notifications(parent_id, created_at desc);

-- ------------------------------------------------------------
-- 9. device_tokens - 푸시 기기 토큰 (FCM 등록 토큰)
--    토큰은 앱 재설치/복원으로 다른 보호자에게 재발급될 수 있어 token이 유일키다.
--    같은 토큰이 다시 들어오면 소유자를 갱신한다(upsert).
-- ------------------------------------------------------------
create table if not exists device_tokens (
    id            uuid         primary key default gen_random_uuid(),
    parent_id     uuid         not null references parents(id) on delete cascade,
    token         varchar(255) not null unique,
    platform      varchar(20)  not null
        check (platform in ('ANDROID', 'IOS', 'WEB')),
    -- 발송이 UNREGISTERED로 실패하면 여기에 시각을 남기고 대상에서 뺀다.
    -- 즉시 지우지 않는 이유는 왜 안 보내지는지 확인할 근거를 남기기 위해서다.
    disabled_at   timestamptz,
    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now()
);

create index if not exists idx_device_tokens_parent on device_tokens(parent_id) where disabled_at is null;

-- ------------------------------------------------------------
-- 10. daily_visits - 일자별 방문 기록
--     대시보드의 "오늘 방문자"는 로그인 수가 아니라 순 방문자 수다.
--     (보호자, 날짜)를 유일키로 두면 하루에 몇 번을 들어와도 1로 집계된다.
--     방문 이벤트를 그대로 쌓으면 집계 때마다 distinct가 필요하고 행이 급격히 는다.
-- ------------------------------------------------------------
create table if not exists daily_visits (
    parent_id   uuid        not null references parents(id) on delete cascade,
    visit_date  date        not null,
    visit_count integer     not null default 1,
    -- 서비스 이용 시간대를 보려면 마지막 시각이 필요하다.
    last_seen_at timestamptz not null default now(),

    primary key (parent_id, visit_date)
);

create index if not exists idx_daily_visits_date on daily_visits(visit_date);

-- ------------------------------------------------------------
-- 11. stories 확장 - 관리자 편집 시각
--     기존 stories에는 created_at만 있어 "언제 고쳤는가"를 보여줄 수 없다.
--     서비스 백엔드의 Story 엔티티는 이 컬럼을 매핑하지 않지만
--     ddl-auto=validate는 매핑되지 않은 여분 컬럼을 문제 삼지 않는다.
-- ------------------------------------------------------------
alter table stories add column if not exists updated_at timestamptz not null default now();
