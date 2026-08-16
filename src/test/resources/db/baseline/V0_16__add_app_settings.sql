-- 본 백엔드 V16__add_app_settings.sql 사본 (README 규칙: 본 스키마 변경을 따라간다)
create table app_settings (
    key        varchar(64)  primary key,
    value      varchar(128) not null,
    updated_at timestamptz  not null default now()
);
