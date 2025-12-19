package com.example.pizza.dto.paginate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SortMetadata {

    private boolean sorted;
    private boolean unsorted;
    private String property;
    private String direction;
}