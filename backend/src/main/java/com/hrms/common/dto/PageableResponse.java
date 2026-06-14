// Pagination envelope wrapping Spring Data Page objects; used as the data field inside ApiResponse
package com.hrms.common.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class PageableResponse<T> {

    private final List<T> content;
    private final int     page;
    private final int     size;
    private final long    totalElements;
    private final int     totalPages;
    private final boolean last;
    private final boolean first;

    public static <T> PageableResponse<T> of(Page<T> springPage) {
        return PageableResponse.<T>builder()
                .content(springPage.getContent())
                .page(springPage.getNumber())
                .size(springPage.getSize())
                .totalElements(springPage.getTotalElements())
                .totalPages(springPage.getTotalPages())
                .last(springPage.isLast())
                .first(springPage.isFirst())
                .build();
    }
}
