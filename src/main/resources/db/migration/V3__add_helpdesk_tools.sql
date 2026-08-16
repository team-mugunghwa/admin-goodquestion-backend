-- ------------------------------------------------------------
-- 고객센터 운영 도구
--
-- 셋 다 관리자 콘솔만 쓰는 테이블이라 서비스 백엔드 쪽에 짝 마이그레이션이
-- 필요 없다. inquiries 를 참조하지만 그 테이블은 양쪽 V1/V11 이 이미
-- if not exists 로 만들어 둔다.
-- ------------------------------------------------------------

-- 1. 자주 쓰는 답변 템플릿.
--    변수 치환({보호자} 등)은 화면이 한다. 여기는 원문 그대로 저장한다.
create table if not exists admin_reply_templates (
    id         uuid        primary key default gen_random_uuid(),
    title      varchar(100) not null,
    body       text        not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- 2. 문의 내부 메모. 사용자에게 보이지 않는 팀 안의 기록이다.
--    작성자 이메일을 함께 저장한다 - 계정이 지워져도 누가 남겼는지는 남아야 한다.
create table if not exists admin_inquiry_notes (
    id              uuid        primary key default gen_random_uuid(),
    inquiry_id      uuid        not null references inquiries(id) on delete cascade,
    author_admin_id uuid        references admin_accounts(id) on delete set null,
    author_email    varchar(255) not null,
    body            text        not null,
    created_at      timestamptz not null default now()
);

create index if not exists idx_admin_inquiry_notes_inquiry
    on admin_inquiry_notes(inquiry_id);

-- 3. 문의 담당자. 문의 하나에 담당자 한 명이므로 inquiry_id 가 기본키다.
--    두 명이 같은 문의에 동시에 답하는 사고를 막는 것이 목적이다.
create table if not exists admin_inquiry_assignees (
    inquiry_id  uuid        primary key references inquiries(id) on delete cascade,
    admin_id    uuid        references admin_accounts(id) on delete set null,
    admin_email varchar(255) not null,
    assigned_at timestamptz not null default now()
);
