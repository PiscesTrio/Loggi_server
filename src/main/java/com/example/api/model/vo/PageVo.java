package com.example.api.model.vo;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * One page of results, and enough to ask for the next.
 *
 * <p>Spring Data's {@code Page} serialises to something much larger, including the {@code Pageable}
 * that produced it and a {@code Sort} object per page - and its shape is not guaranteed across
 * versions, which is a poor property for a published contract. This carries the four numbers a
 * client needs and the items.
 */
public record PageVo<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <E, T> PageVo<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageVo<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
