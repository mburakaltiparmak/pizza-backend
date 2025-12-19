package com.example.pizza.repository;

import com.example.pizza.constants.order.OrderStatus;
import com.example.pizza.entity.order.Order;
import com.example.pizza.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // ============================================================================
    // LEGACY METHODS (Backward Compatibility)
    // ============================================================================

    /**
     * Tüm siparişleri detaylarıyla getir (WITHOUT PAGINATION)
     * N+1 problem çözümü için JOIN FETCH kullanılıyor
     *
     * @deprecated Use {@link #findAllWithDetails(Pageable)} for pagination
     */
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH o.items oi " +
            "LEFT JOIN FETCH oi.product " +
            "ORDER BY o.orderDate DESC")
    List<Order> findAllWithDetails();

    /**
     * Kullanıcıya göre siparişleri getir (WITHOUT PAGINATION)
     *
     * @deprecated Use {@link #findByUserWithDetails(User, Pageable)} for pagination
     */
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.items oi " +
            "LEFT JOIN FETCH oi.product " +
            "WHERE o.user = :user " +
            "ORDER BY o.orderDate DESC")
    List<Order> findByUserWithDetails(@Param("user") User user);

    /**
     * Statüye göre siparişleri getir (WITHOUT PAGINATION)
     *
     * @deprecated Use {@link #findByOrderStatusWithDetails(OrderStatus, Pageable)} for pagination
     */
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH o.items oi " +
            "LEFT JOIN FETCH oi.product " +
            "WHERE o.orderStatus = :status " +
            "ORDER BY o.orderDate DESC")
    List<Order> findByOrderStatusWithDetails(@Param("status") OrderStatus status);

    /**
     * Sipariş detaylarını ID ile getir
     */
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH o.items oi " +
            "LEFT JOIN FETCH oi.product " +
            "WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    /**
     * Belirli tarih aralığındaki siparişleri getir
     */
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    List<Order> findOrdersBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Toplam sipariş geliri
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o")
    Double calculateTotalRevenue();

    /**
     * Statüye göre sipariş sayısı
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus = :status")
    long countByOrderStatus(@Param("status") OrderStatus status);

    /**
     * Kullanıcıya göre sipariş sayısı
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.user = :user")
    long countByUser(@Param("user") User user);

    // ============================================================================
    // PAGINATION METHODS
    // ============================================================================

    /**
     * Tüm siparişleri sayfalı olarak getir (detaylarla)
     *
     * NOT: JOIN FETCH ile pagination kullanımı dikkatli olmalı!
     * Hibernate memory'de join yapıp sonra paginate eder.
     *
     * Kullanım:
     * Pageable pageable = PageRequest.of(0, 20, Sort.by("orderDate").descending());
     * Page<Order> orders = repository.findAllWithDetails(pageable);
     *
     * @param pageable Pagination parametreleri
     * @return Paginated orders with details
     */
    @Query(value = "SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH o.items oi " +
            "LEFT JOIN FETCH oi.product " +
            "ORDER BY o.orderDate DESC",
            countQuery = "SELECT COUNT(DISTINCT o) FROM Order o")
    Page<Order> findAllWithDetails(Pageable pageable);

    /**
     * Kullanıcıya göre siparişleri sayfalı getir
     *
     * Kullanım:
     * Pageable pageable = PageRequest.of(0, 10, Sort.by("orderDate").descending());
     * Page<Order> orders = repository.findByUserWithDetails(user, pageable);
     *
     * @param user Kullanıcı
     * @param pageable Pagination parametreleri
     * @return Paginated user orders
     */
    @Query(value = "SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.items oi " +
            "LEFT JOIN FETCH oi.product " +
            "WHERE o.user = :user " +
            "ORDER BY o.orderDate DESC",
            countQuery = "SELECT COUNT(DISTINCT o) FROM Order o WHERE o.user = :user")
    Page<Order> findByUserWithDetails(@Param("user") User user, Pageable pageable);

    /**
     * Statüye göre siparişleri sayfalı getir
     *
     * Kullanım:
     * Pageable pageable = PageRequest.of(0, 20, Sort.by("orderDate").descending());
     * Page<Order> orders = repository.findByOrderStatusWithDetails(OrderStatus.PENDING, pageable);
     *
     * @param status Sipariş durumu
     * @param pageable Pagination parametreleri
     * @return Paginated orders by status
     */
    @Query(value = "SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH o.items oi " +
            "LEFT JOIN FETCH oi.product " +
            "WHERE o.orderStatus = :status " +
            "ORDER BY o.orderDate DESC",
            countQuery = "SELECT COUNT(DISTINCT o) FROM Order o WHERE o.orderStatus = :status")
    Page<Order> findByOrderStatusWithDetails(@Param("status") OrderStatus status, Pageable pageable);

    /**
     * Tarih aralığına göre siparişleri sayfalı getir
     *
     * @param startDate Başlangıç tarihi
     * @param endDate Bitiş tarihi
     * @param pageable Pagination parametreleri
     * @return Paginated orders in date range
     */
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    Page<Order> findOrdersBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * Fiyat aralığına göre siparişleri sayfalı getir
     *
     * @param minPrice Minimum fiyat
     * @param maxPrice Maximum fiyat
     * @param pageable Pagination parametreleri
     * @return Paginated orders in price range
     */
    @Query("SELECT o FROM Order o WHERE o.totalAmount BETWEEN :minPrice AND :maxPrice")
    Page<Order> findByPriceRange(
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            Pageable pageable
    );
}