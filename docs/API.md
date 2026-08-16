# 관리자 API 명세

기본 경로는 `/api/admin` 이다. 로그인과 재발급을 뺀 모든 요청에
`Authorization: Bearer {accessToken}` 이 필요하다.

## 공통

### 목록 응답

```json
{ "content": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0 }
```

`page` 는 0부터 시작한다. 화면의 "1페이지"가 여기서는 0이다.

### 오류 응답

```json
{ "code": "STORY_IN_USE", "message": "이미 진행된 기록이 있는 이야기는 삭제할 수 없습니다. 보관 처리해 주세요." }
```

**클라이언트는 HTTP 상태나 메시지 문자열이 아니라 `code` 로 분기한다.**

| code | 상태 | 뜻 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 없음/만료 |
| `FORBIDDEN` | 403 | 권한 부족 |
| `NOT_FOUND` | 404 | 리소스 없음 |
| `INVALID_REQUEST` | 400 | 입력 검증 실패 |
| `INVALID_CREDENTIALS` | 401 | 이메일/비밀번호 불일치 |
| `ACCOUNT_LOCKED` | 423 | 로그인 실패 누적으로 잠김 |
| `ACCOUNT_SUSPENDED` | 403 | 정지된 관리자 계정 |
| `SELF_MODIFICATION_DENIED` | 409 | 자기 계정에는 못 하는 조작 |
| `STORY_IN_USE` | 409 | 진행 기록이 있어 삭제 불가 |
| `INCOMPLETE_DIALOGUE_SCENE` | 422 | 대화 장면의 필수 항목 누락 |
| `ANSWER_ALREADY_EXISTS` | 409 | 이미 답변된 문의 |
| `INQUIRY_CLOSED` | 409 | 종료된 문의 |

---

## 1. 인증 `/auth`

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| POST | `/auth/login` | 로그인. `{email, password}` -> 토큰과 관리자 정보 |
| POST | `/auth/refresh` | 재발급. 쓴 토큰은 폐기되고 새 값이 나간다(회전) |
| POST | `/auth/logout` | 본문에 토큰을 실으면 그 기기만, 비우면 모든 기기 |
| GET | `/auth/me` | 현재 관리자 |
| PATCH | `/auth/password` | 비밀번호 변경. 성공하면 이 계정의 모든 리프레시 토큰이 끊긴다 |

## 2. 관리자 계정 `/accounts` - 최고관리자 전용

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/accounts` | 목록 |
| POST | `/accounts` | 생성 |
| PATCH | `/accounts/{adminId}` | 이름/권한/상태 변경 |
| DELETE | `/accounts/{adminId}` | 삭제 |

자기 계정의 권한/상태 변경과 삭제는 `SELF_MODIFICATION_DENIED` 로 막는다.
마지막 최고관리자가 사라지면 DB를 직접 고치는 것 말고는 되돌릴 방법이 없다.

## 3. 대시보드 `/dashboard`

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/dashboard` | 화면 전체를 한 번에 |

`users`(총 사용자/오늘 방문자/신규 가입/진행 중 세션), `content`(이야기/공지/
이용안내/미답변 문의), `visitTrend`(최근 2주, 방문 없는 날도 0으로 채움),
`recentActivities`(최근 관리자 조작 10건).

"오늘"은 한국 시간 기준이다.

## 4. 이야기 `/stories`

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/stories` | 목록. `status`, `keyword`, `page`, `size` |
| GET | `/stories/{storyId}` | 상세. `sceneCount`, `sessionCount` 포함 |
| POST | `/stories` | 생성 |
| PATCH | `/stories/{storyId}` | 수정. `topics` 는 null이면 유지, 빈 배열이면 전부 삭제 |
| DELETE | `/stories/{storyId}` | 삭제. 진행 기록이 있으면 `STORY_IN_USE` |
| GET/POST | `/stories/{storyId}/scenes` | 장면 목록/추가 |
| PATCH/DELETE | `/stories/{storyId}/scenes/{sceneId}` | 장면 수정/삭제 |
| PUT | `/stories/{storyId}/scenes/order` | 순서 일괄 변경. 배열의 위치가 곧 순서 |
| GET/POST | `/stories/{storyId}/characters` | 캐릭터 목록/추가 |
| PATCH/DELETE | `/stories/{storyId}/characters/{characterId}` | 캐릭터 수정/삭제 |

- 장면이 0개인 이야기를 `PUBLISHED` 로 바꾸면 400
- 대화 장면은 캐릭터/첫 대사/장면 목표/확인 요소/턴 수가 모두 필요하다
- 장면을 쓰는 캐릭터는 삭제되지 않는다

## 5. 주제 `/topics`

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/topics` | 목록 |
| GET | `/topics/{topicId}/usage` | 이 주제를 쓰는 이야기 수 |
| POST/PATCH/DELETE | `/topics`, `/topics/{topicId}` | 생성/수정/삭제 |

이야기 저장에서 없는 주제 이름을 보내면 자동으로 만들어진다. 오타로 늘어난
주제를 정리하는 곳이 이 화면이다.

## 6. 사용자 `/members`

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/members` | 목록. `status`, `keyword` |
| GET | `/members/{parentId}` | 상세. 아이/로그인 세션/문의 수 |
| GET | `/members/{parentId}/sessions` | 학습 세션 |
| POST | `/members/{parentId}/suspend` | 정지. `{reason}` 필수. 로그인 세션도 함께 끊긴다 |
| POST | `/members/{parentId}/restore` | 정지 해제. 로그인 실패 잠금도 함께 풀린다 |
| POST | `/members/{parentId}/login-sessions/revoke` | 로그인 세션만 종료 |

**생성/삭제 API가 없다.** 가입은 사용자가 하고 탈퇴는 사용자의 권리다.

## 7. 공지사항 `/notices`

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/notices` | 목록. `status`, `keyword` |
| GET/POST | `/notices`, `/notices/{noticeId}` | 상세/작성 |
| PATCH/DELETE | `/notices/{noticeId}` | 수정/삭제 |

`DRAFT` -> `PUBLISHED` 로 바뀌는 순간에만 공개 시각을 찍는다. 이후 수정에는
갱신하지 않는다 - 오타 수정 하나에 사용자 목록 맨 위로 올라오면 안 된다.

## 8. 이용안내 `/guides`

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/guides` | 목록. `category`, `status`. **페이징 없음** |
| POST | `/guides` | 생성. 순서를 비우면 해당 분류 맨 아래 |
| PATCH/DELETE | `/guides/{guideId}` | 수정/삭제 |
| PUT | `/guides/order` | 순서 일괄 변경. 분류 전체를 다시 번호 매긴다 |

순서를 보며 편집하는 화면이라 페이징하지 않는다.

## 9. 고객센터 `/inquiries`

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/inquiries` | 목록. `status`, `category`, `keyword`. 미답변이 먼저, 그 안에서 오래된 순 |
| GET | `/inquiries/{inquiryId}` | 상세 |
| POST | `/inquiries/{inquiryId}/answer` | **답변 등록. 알림 생성 + 푸시 발송** |
| PATCH | `/inquiries/{inquiryId}/answer` | 답변 수정. **알림을 다시 보내지 않는다** |
| POST | `/inquiries/{inquiryId}/close` | 종료 |
| POST | `/inquiries/{inquiryId}/reopen` | 재개 |

문의 생성 API가 없다. 문의는 사용자 앱만 만든다.

답변 등록 한 번에 세 가지가 한 트랜잭션에서 일어난다 - 답변 저장, 문의 상태
변경, 사용자 알림 생성. 커밋된 뒤 푸시가 비동기로 나간다.

## 10. 감사 로그 `/audit-logs`

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/audit-logs` | 목록. `targetType` 으로 거를 수 있다 |

쓰기 API가 없다. 기록은 각 서비스가 조작할 때 남긴다.

## 11. 데이터베이스 `/database`

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/database/tables` | 테이블 목록. 분류/설명/컬럼 수/대략적인 행 수 |
| GET | `/database/tables/{tableName}` | 구조. 컬럼, 타입, 기본키, 외래키, 인덱스 |
| GET | `/database/tables/{tableName}/rows` | 값. `page`, `size`, `sortColumn`, `sortDirection`, `filterColumn`, `keyword` |

**읽기 전용이다.** 쓰기 API를 두지 않았다. 관리자 콘솔은 사용자 서비스와 같은
운영 DB를 보므로, 여기서 값을 고칠 수 있게 되면 서비스 규칙을 건너뛴 데이터가
들어간다. 값을 바꿔야 하면 그 도메인의 관리 API를 쓴다.

컬럼과 테이블의 설명은 DB에 심어 둔 주석(`comment on table/column`)을 그대로
읽어 온다. 주석은 `R__2_schema_comments.sql` 이 관리하며, 반복 마이그레이션이라
파일을 고치면 다음 기동에 다시 적용된다. **새 컬럼을 만들면 이 파일에 설명을
같이 적는다.**

### 안전장치

| 항목 | 내용 |
| --- | --- |
| 테이블/컬럼 이름 | `information_schema` 에 실제로 있는 이름만 통과시킨다. 정렬/검색 컬럼도 같다 |
| 값 가리기 | `password_hash`, `token_hash`, `token`, `idempotency_key` 는 `(가려짐)` 으로 내려간다 |
| 조회 시간 | 5초를 넘기면 끊는다 |
| 페이지 크기 | 최대 200 |
| 감사 로그 | 개인정보가 든 테이블의 값을 열면 `READ_DATA` 로 남는다 |

조회를 감사 로그에 남기는 것은 이 API 하나뿐이다. 다른 화면의 조회는 남기지
않는다 - 여기서는 아이 발화 원문처럼 무게가 다른 값을 원본 그대로 보기 때문이다.
