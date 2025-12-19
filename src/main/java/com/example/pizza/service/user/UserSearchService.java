package com.example.pizza.service.user;

import com.example.pizza.constants.user.Role;
import com.example.pizza.constants.user.UserStatus;
import com.example.pizza.dto.user.UserResponse;
import com.example.pizza.entity.user.User;
import com.example.pizza.repository.UserRepository;
import com.example.pizza.entity.user.UserDocument;
import com.example.pizza.repository.search.UserSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSearchService {

    private final UserSearchRepository userSearchRepository;
    private final UserRepository userRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    // ============================================================================
    // INDEXING METHODS
    // ============================================================================

    @Transactional
    public void indexUser(User user) {
        try {
            // UserDocument yapısının User entity'si ile uyumlu olduğunu varsayıyoruz
            // LocalDate dönüşümü gerekebilir
            LocalDate createdAt = (user.getCreatedAt() != null) ? user.getCreatedAt().toLocalDate() : LocalDate.now();

            UserDocument document = UserDocument.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .surname(user.getSurname())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .status(user.getStatus())
                    // Eğer UserDocument'te phoneNumber vb. varsa ekleyin:
                    // .phoneNumber(user.getPhoneNumber())
                    .createdAt(createdAt)
                    .build();

            userSearchRepository.save(document);
            log.debug("User indexed: ID={}", user.getId());
        } catch (Exception e) {
            log.error("Failed to index user ID: {}", user.getId(), e);
        }
    }

    @Transactional
    public void deleteUserFromIndex(Long userId) {
        try {
            userSearchRepository.deleteById(userId);
            log.info("User deleted from index: ID={}", userId);
        } catch (Exception e) {
            log.error("Failed to delete user from index ID: {}", userId, e);
        }
    }

    @Transactional
    public void indexAllUsers() {
        log.info("Starting bulk user indexing...");
        List<User> users = userRepository.findAll();
        int count = 0;
        for (User user : users) {
            indexUser(user);
            count++;
        }
        log.info("Bulk user indexing completed. Total: {}", count);
    }

    // ============================================================================
    // SEARCH METHODS
    // ============================================================================

    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsersDynamic(
            String query, String email, Role role, UserStatus status, Pageable pageable) {

        Criteria criteria = new Criteria();

        // 1. Genel Query (İsim veya Soyisim)
        if (query != null && !query.trim().isEmpty()) {
            criteria = criteria.subCriteria(
                    new Criteria("name").contains(query)
                            .or("surname").contains(query)
            );
        }

        // 2. Email (Tam Eşleşme)
        if (email != null && !email.trim().isEmpty()) {
            criteria = criteria.and("email").is(email);
        }

        // 3. Role
        if (role != null) {
            criteria = criteria.and("role").is(role);
        }

        // 4. Status
        if (status != null) {
            criteria = criteria.and("status").is(status);
        }

        CriteriaQuery criteriaQuery = new CriteriaQuery(criteria).setPageable(pageable);
        SearchHits<UserDocument> searchHits = elasticsearchOperations.search(criteriaQuery, UserDocument.class);

        List<UserResponse> responses = searchHits.stream()
                .map(SearchHit::getContent)
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, searchHits.getTotalHits());
    }

    private UserResponse toResponse(UserDocument doc) {
        // Not: UserDocument'te genellikle sadece arama yapılacak alanlar tutulur.
        // DTO'daki her alan Document'te olmayabilir. Olmayanlar null geçilir.

        return UserResponse.builder()
                .id(doc.getId())
                .name(doc.getName())
                .surname(doc.getSurname())
                .email(doc.getEmail())
                .role(doc.getRole())
                .status(doc.getStatus())
                // .phoneNumber(doc.getPhoneNumber()) // Eğer Document'te varsa ekle
                // .oauthProvider(doc.getOauthProvider()) // Eğer Document'te varsa ekle
                // Elasticsearch'te genellikle LocalDate tutulur, DTO LocalDateTime bekliyorsa startOfDay() kullanabiliriz
                .createdAt(doc.getCreatedAt() != null ? doc.getCreatedAt().atStartOfDay() : null)
                .lastLoginAt(null) // Arama sonucunda genellikle login tarihi tutulmaz
                .addresses(null)   // Arama sonucunda adres listesi dönülmez (Detayda çekilir)
                .build();
    }
}