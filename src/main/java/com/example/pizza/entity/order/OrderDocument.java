package com.example.pizza.entity.order;

import com.example.pizza.constants.order.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "orders")
public class OrderDocument {

    @Id
    @Field(type = FieldType.Long)
    private Long id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String userEmail;

    @Field(type = FieldType.Keyword)
    private OrderStatus orderStatus;

    @Field(type = FieldType.Double)
    private Double totalAmount;

    @Field(type = FieldType.Date)
    private LocalDate orderDate;
}
