package com.example.pizza.entity.user;

import com.example.pizza.constants.user.Role;
import com.example.pizza.constants.user.UserStatus;
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
@Document(indexName = "users")
public class UserDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String surname;

    @Field(type = FieldType.Keyword)
    private String email;

    @Field(type = FieldType.Keyword)
    private String phoneNumber;

    @Field(type = FieldType.Keyword)
    private Role role;

    @Field(type = FieldType.Keyword)
    private UserStatus status;

    @Field(type = FieldType.Date)
    private LocalDate createdAt;

    @Field(type = FieldType.Date)
    private LocalDate lastLogin;

    // ========================================================================
    // SECURITY NOTES
    // ========================================================================
    // - Password is NOT indexed (security)
    // - User addresses are NOT indexed (privacy)
    // - User orders are NOT indexed (performance + privacy)
    // - Google ID, Supabase ID are NOT indexed (privacy)
    // - OAuth provider is NOT indexed (privacy)
}