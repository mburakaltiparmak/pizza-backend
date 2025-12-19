// ============================================================================
// AdminServiceImpl.java - FIXED LazyInitializationException
// ============================================================================
// Location: src/main/java/com/example/pizza/service/AdminServiceImpl.java
// ============================================================================

package com.example.pizza.service.admin;

import com.example.pizza.constants.user.UserStatus;
import com.example.pizza.constants.user.Role;
import com.example.pizza.constants.order.OrderStatus;
import com.example.pizza.dto.admin.DashboardResponseDTO;
import com.example.pizza.dto.category.CategoryResponse;
import com.example.pizza.dto.category.CategoryWithProductsDTO;
import com.example.pizza.dto.product.ProductResponse;
import com.example.pizza.dto.product.ProductSummaryDTO;
import com.example.pizza.entity.user.User;
import com.example.pizza.repository.ProductRepository;
import com.example.pizza.repository.OrderRepository;
import com.example.pizza.repository.UserRepository;
import com.example.pizza.service.category.CategoryService;
import com.example.pizza.service.product.ProductService;
import com.example.pizza.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final CategoryService categoryService;
    private final ProductService productService;
    private final UserService userService;

    // Repositories for analytics
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    private static final int RECENT_PRODUCTS_LIMIT = 5;

    // =========================================================================
    // DASHBOARD
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public DashboardResponseDTO getDashboardStatistics() {
        try {
            List<CategoryResponse> categories = categoryService.getAllCategories();
            List<ProductResponse> products = productService.getAllProducts();
            List<User> users = userService.getAllUsers();

            return DashboardResponseDTO.builder()
                    .totalCategories(categories.size())
                    .totalProducts(products.size())
                    .totalStock(calculateTotalStock(products))
                    .totalUsers(users.size())
                    .categories(mapCategoriesToDTOs(categories, products))
                    .recentProducts(getRecentProducts(products))
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Dashboard verileri alınırken hata oluştu: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // CATEGORY + PRODUCT DTO MAP'LERİ
    // =========================================================================

    private List<CategoryWithProductsDTO> mapCategoriesToDTOs(
            List<CategoryResponse> categories,
            List<ProductResponse> allProducts) {

        return categories.stream()
                .map(category -> {
                    List<ProductResponse> categoryProducts = allProducts.stream()
                            .filter(p -> p.categoryId().equals(category.id()))
                            .collect(Collectors.toList());

                    List<ProductSummaryDTO> productDTOs = categoryProducts.stream()
                            .map(this::mapProductToSummaryDTO)
                            .collect(Collectors.toList());

                    long totalStock = productDTOs.stream()
                            .mapToLong(p -> p.getStock() != null ? p.getStock() : 0)
                            .sum();

                    return CategoryWithProductsDTO.builder()
                            .id(category.id())
                            .name(category.name())
                            .img(category.img())
                            .productCount(productDTOs.size())
                            .totalStock(totalStock)
                            .products(productDTOs)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private ProductSummaryDTO mapProductToSummaryDTO(ProductResponse product) {
        return ProductSummaryDTO.builder()
                .id(product.id())
                .name(product.name())
                .rating(product.rating())
                .stock(product.stock())
                .price(product.price())
                .img(product.img())
                .description(product.description())
                .categoryId(product.categoryId())
                .build();
    }

    private List<ProductSummaryDTO> getRecentProducts(List<ProductResponse> products) {
        return products.stream()
                .sorted(Comparator.comparing(ProductResponse::id).reversed())
                .limit(RECENT_PRODUCTS_LIMIT)
                .map(this::mapProductToSummaryDTO)
                .collect(Collectors.toList());
    }

    private long calculateTotalStock(List<ProductResponse> products) {
        return products.stream()
                .mapToLong(ProductResponse::stock)
                .sum();
    }

    // =========================================================================
    // USER OPERASYONLARI - FIXED: Addresses fetch edilmiyor
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        // FIXED: getAllUsers() zaten addresses'siz user döner
        // UserMapper addresses koleksiyonuna erişmeye çalışırsa
        // isEmpty() kontrolü yapmalı veya addresses'i null bırakmalı
        return userService.getAllUsers();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getPendingUsers() {
        return userService.getPendingUsers();
    }

    @Override
    @Transactional
    public User approveUser(Long userId) {
        return userService.approveUser(userId);
    }

    @Override
    @Transactional
    public User rejectUser(Long userId) {
        return userService.rejectUser(userId);
    }

    @Override
    @Transactional
    public User updateUserRole(Long userId, Role newRole) {
        return userService.updateUserRole(userId, newRole);
    }

    // =========================================================================
    // PAGINATION METHODS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userService.getAllUsers(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getPendingUsers(Pageable pageable) {
        return userService.getPendingUsers(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getUsersByRole(Role role, Pageable pageable) {
        return userService.getUsersByRole(role, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getUsersByStatus(UserStatus status, Pageable pageable) {
        return userService.getUsersByStatus(status, pageable);
    }

    // =========================================================================
    // DASHBOARD ANALYTICS METHODS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Double calculateTotalRevenue() {
        return orderRepository.calculateTotalRevenue();
    }

    @Override
    @Transactional(readOnly = true)
    public long countOrdersByStatus(String status) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            return orderRepository.countByOrderStatus(orderStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order status: " + status);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countOutOfStockProducts() {
        return productRepository.countOutOfStockProducts();
    }

    @Override
    @Transactional(readOnly = true)
    public long countLowStockProducts(int threshold) {
        return productRepository.countLowStockProducts(threshold);
    }

    @Override
    @Transactional(readOnly = true)
    public long calculateTotalStockByCategory(Long categoryId) {
        return productRepository.calculateTotalStockByCategory(categoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingUsers() {
        return userRepository.countPendingUsers();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsersByRole(String role) {
        try {
            Role userRole = Role.valueOf(role.toUpperCase());
            return userRepository.countByRole(userRole);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
    }
}