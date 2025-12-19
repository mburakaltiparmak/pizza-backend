package com.example.pizza.dto.paginate;

import com.example.pizza.dto.order.OrderResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> implements Serializable {

    private List<T> content;
    private PageMetadata page;
    private SortMetadata sort;

    public PagedResponse(List<OrderResponse> content, int number, int size, long totalElements, int totalPages, boolean last) {
    }

    public static <T> PagedResponse<T> of(Page<T> page) {
        // Sort metadata oluştur
        SortMetadata sortMetadata = null;
        if (page.getSort().isSorted()) {
            Sort.Order order = page.getSort().iterator().next();
            sortMetadata = SortMetadata.builder()
                    .sorted(true)
                    .unsorted(false)
                    .property(order.getProperty())
                    .direction(order.getDirection().name())
                    .build();
        } else {
            sortMetadata = SortMetadata.builder()
                    .sorted(false)
                    .unsorted(true)
                    .property(null)
                    .direction(null)
                    .build();
        }

        // Page metadata oluştur
        PageMetadata pageMetadata = PageMetadata.builder()
                .number(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();

        // PagedResponse oluştur
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(pageMetadata)
                .sort(sortMetadata)
                .build();
    }
}