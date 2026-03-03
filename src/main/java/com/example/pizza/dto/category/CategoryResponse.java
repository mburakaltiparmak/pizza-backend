package com.example.pizza.dto.category;

import java.io.Serializable;

public record CategoryResponse(
                Long id,
                String name,
                String img,
                int productCount

) implements Serializable {
}
