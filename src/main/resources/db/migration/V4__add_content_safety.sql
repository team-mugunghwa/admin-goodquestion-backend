-- ------------------------------------------------------------
-- 공지 안전장치: 예약 공개, 수정 이력
--
-- 둘 다 관리자 콘솔만 쓰는 테이블이다. notices 자체에 컬럼을 더하지 않는 것이
-- 요점이다 - notices 는 서비스 백엔드와 공유하는 테이블이라 컬럼을 바꾸면
-- 양쪽 엔티티를 함께 고쳐야 한다. 옆에 붙는 테이블이면 이쪽만 안다.
-- ------------------------------------------------------------

-- 1. 예약 공개. 시각이 되면 관리자 백엔드의 스케줄러가 공개로 바꾸고 행을 지운다.
--    공지 하나에 예약 하나라 notice_id 가 기본키다.
create table if not exists admin_notice_schedules (
    notice_id        uuid         primary key references notices(id) on delete cascade,
    publish_at       timestamptz  not null,
    created_by_email varchar(255) not null,
    created_at       timestamptz  not null default now()
);

-- 2. 수정 이력. 저장할 때마다 바꾸기 전의 내용을 남겨서 되돌릴 수 있게 한다.
create table if not exists admin_notice_revisions (
    id              uuid         primary key default gen_random_uuid(),
    -- 순서 보장용. created_at 은 한 트랜잭션 안에서 같은 값이라(now() 가
    -- 트랜잭션 시각) 연달아 저장하면 "최신"을 가릴 수 없다.
    seq             bigint       generated always as identity,
    notice_id       uuid         not null references notices(id) on delete cascade,
    title           varchar(200) not null,
    content         text         not null,
    category        varchar(20)  not null,
    pinned          boolean      not null,
    edited_by_email varchar(255) not null,
    created_at      timestamptz  not null default now()
);

create index if not exists idx_admin_notice_revisions_notice
    on admin_notice_revisions(notice_id, seq desc);
