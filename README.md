# admin-goodquestion-backend

굿퀘스천 관리자 콘솔의 백엔드.

관리자 콘솔 화면은 [admin-goodquestion-frontend](https://github.com/team-mugunghwa/admin-goodquestion-frontend),
사용자 서비스는 [goodquestion-backend](https://github.com/team-mugunghwa/goodquestion-backend) /
[goodquestion-frontend](https://github.com/team-mugunghwa/goodquestion-frontend)에 있다.

## 무엇을 하는가

| 메뉴 | 하는 일 |
| --- | --- |
| 대시보드 | 총 사용자, 오늘 방문자, 오늘 신규 가입자, 미답변 문의, 최근 활동 |
| 이야기 관리 | 이야기와 장면, 캐릭터, 주제 CRUD. 공개하면 사용자 앱 목록에 나간다 |
| 사용자 관리 | 보호자/아이 조회, 학습 세션 확인, 로그인 세션 강제 종료, 계정 정지 |
| 공지사항 관리 | 작성/공개/고정. 공개분이 사용자 앱 공지 목록에 나간다 |
| 고객센터 관리 | 사용자 문의 확인과 답변 등록. 답변하면 알림과 푸시가 나간다 |
| 이용안내 관리 | 도움말 문서 CRUD와 노출 순서 |

## 서비스 백엔드와의 관계

**같은 PostgreSQL을 본다.** 관리자가 고친 이야기/공지/이용안내가 그대로 사용자 앱에
보여야 하므로 복제나 동기화 계층을 두지 않았다.

애플리케이션은 따로 배포된다. Flyway 이력 테이블도 분리했고
(`flyway_schema_history_admin`), 공유 테이블 DDL은 양쪽에 `if not exists`로 들어 있다.
자세한 배경은 [docs/admin-backend-guide.md](docs/admin-backend-guide.md) 3절에 있다.

## 시작하기

```bash
cp .env.example .env   # 값을 채운다. DB_URL은 서비스 백엔드와 같은 DB를 가리킨다
./gradlew bootRun      # http://localhost:8081
```

시드 관리자 계정은 `admin@goodquestion.kr` / `admin1234!` 이다. **첫 로그인 후 바꾼다.**

자세한 셋업과 구조는 [docs/admin-backend-guide.md](docs/admin-backend-guide.md),
엔드포인트 목록은 [docs/API.md](docs/API.md)에 있다.
