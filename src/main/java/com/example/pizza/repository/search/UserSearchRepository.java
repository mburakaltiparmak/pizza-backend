package com.example.pizza.repository.search;

import com.example.pizza.constants.user.Role;
import com.example.pizza.constants.user.UserStatus;
import com.example.pizza.entity.user.UserDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSearchRepository extends ElasticsearchRepository<UserDocument, Long> {

    Page<UserDocument> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<UserDocument> findBySurnameContainingIgnoreCase(String surname, Pageable pageable);
    Page<UserDocument> findByNameContainingOrSurnameContainingAllIgnoreCase(
            String name,
            String surname,
            Pageable pageable
    );

    Page<UserDocument> findByEmail(String email, Pageable pageable);
    Page<UserDocument> findByRole(Role role, Pageable pageable);
    Page<UserDocument> findByStatus(UserStatus status, Pageable pageable);
    Page<UserDocument> findByNameContainingOrSurnameContainingAllIgnoreCaseAndRole(
            String name,
            String surname,
            Role role,
            Pageable pageable
    );

    Page<UserDocument> findByNameContainingOrSurnameContainingAllIgnoreCaseAndStatus(
            String name,
            String surname,
            UserStatus status,
            Pageable pageable
    );

    Page<UserDocument> findByRoleAndStatus(Role role, UserStatus status, Pageable pageable);
}