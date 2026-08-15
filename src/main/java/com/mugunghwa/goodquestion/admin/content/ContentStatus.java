package com.mugunghwa.goodquestion.admin.content;

/**
 * 공지와 이용안내가 공유하는 노출 상태.
 *
 * <p>ARCHIVED를 두고 삭제를 쓰지 않는 이유는, 내린 글을 다시 올릴 일이 자주 있고
 * 지워 버리면 무엇이 언제 걸려 있었는지 확인할 수 없기 때문이다.
 */
public enum ContentStatus {
    /** 작성 중. 사용자 앱에 나가지 않는다. */
    DRAFT,
    /** 공개. 사용자 앱 목록에 나간다. */
    PUBLISHED,
    /** 내림. 공개했다가 거둔 상태. */
    ARCHIVED
}
