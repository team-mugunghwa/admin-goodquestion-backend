package com.mugunghwa.goodquestion.admin.global.web;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 목록 응답의 공통 봉투.
 *
 * <p>스프링의 {@code Page}를 그대로 직렬화하지 않는다. 그쪽은 pageable, sort, first,
 * numberOfElements까지 20개 남짓한 필드를 내보내고 그 모양이 스프링 버전에 따라 바뀐다.
 * 관리자 콘솔이 실제로 쓰는 다섯 개만 우리 이름으로 고정한다.
 *
 * @param page 0부터 시작한다. 화면의 "1페이지"가 여기서는 0이다.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <E, T> PageResponse<T> of(Page<E> source, Function<E, T> mapper) {
        return new PageResponse<>(
                source.getContent().stream().map(mapper).toList(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages());
    }

    public static <T> PageResponse<T> of(Page<T> source) {
        return of(source, Function.identity());
    }
}
