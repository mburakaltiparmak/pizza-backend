package com.example.pizza.repository;

import com.example.pizza.constants.user.Role;
import com.example.pizza.constants.user.UserStatus;
import com.example.pizza.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ============================================================================
    // EXISTING METHODS (MUST KEEP - Used in codebase)
    // ============================================================================

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.addresses WHERE u.email = :email")
    Optional<User> findByEmailWithAddresses(@Param("email") String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findBySupabaseId(String supabaseId);

    List<User> findByStatus(UserStatus status);

    void deleteByEmail(String email);

    boolean existsByEmail(String email);

    // ============================================================================
    // STATISTICS METHODS (Existing)
    // ============================================================================

    @Query("SELECT COUNT(u) FROM User u WHERE u.status = 'ACTIVE'")
    long countActiveUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.status = 'PENDING'")
    long countPendingUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") Role role);

    // ============================================================================
    // PAGINATION METHODS
    // ============================================================================

    /**
     * Tüm kullanıcıları sayfalı olarak getir
     *
     * JpaRepository'den otomatik gelir:
     * Page<User> findAll(Pageable pageable);
     *
     * Kullanım:
     * Pageable pageable = PageRequest.of(0, 20, Sort.by("orderDate").descending());
     * Page<User> users = repository.findAll(pageable);
     */
    // Otomatik gelir, override etmeye gerek yok

    /**
     * Status'e göre kullanıcıları sayfalı getir
     *
     * NOT: Mevcut findByStatus(UserStatus) metodu korundu backward compatibility için.
     * Bu yeni paginated version.
     *
     * Kullanım:
     * Pageable pageable = PageRequest.of(0, 20, Sort.by("orderDate"));
     * Page<User> users = repository.findByStatus(UserStatus.PENDING, pageable);
     *
     * @param status Kullanıcı durumu
     * @param pageable Pagination parametreleri
     * @return Paginated users by status
     */
    Page<User> findByStatus(UserStatus status, Pageable pageable);

    /**
     * Role'e göre kullanıcıları sayfalı getir
     *
     * Kullanım:
     * Pageable pageable = PageRequest.of(0, 20, Sort.by("email"));
     * Page<User> users = repository.findByRole(Role.CUSTOMER, pageable);
     *
     * @param role Kullanıcı rolü
     * @param pageable Pagination parametreleri
     * @return Paginated users by role
     */
    Page<User> findByRole(Role role, Pageable pageable);

    /**
     * Email'e göre kullanıcı arama (sayfalı)
     *
     * Case-insensitive partial match yapılır.
     *
     * Kullanım:
     * Pageable pageable = PageRequest.of(0, 20);
     * Page<User> users = repository.searchByEmail("gmail", pageable);
     *
     * @param email Aranacak email (partial match)
     * @param pageable Pagination parametreleri
     * @return Paginated search results
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))")
    Page<User> searchByEmail(@Param("email") String email, Pageable pageable);

    /**
     * İsme göre kullanıcı arama (sayfalı)
     *
     * Case-insensitive partial match yapılır.
     *
     * @param name Aranacak isim (partial match)
     * @param pageable Pagination parametreleri
     * @return Paginated search results
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<User> searchByName(@Param("name") String name, Pageable pageable);

    /**
     * Role ve status'e göre kullanıcıları getir (sayfalı)
     *
     * @param role Kullanıcı rolü
     * @param status Kullanıcı durumu
     * @param pageable Pagination parametreleri
     * @return Paginated users by role and status
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.status = :status")
    Page<User> findByRoleAndStatus(
            @Param("role") Role role,
            @Param("status") UserStatus status,
            Pageable pageable
    );
}