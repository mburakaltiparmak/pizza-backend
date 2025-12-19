package com.example.pizza.controller;

import com.example.pizza.constants.user.Role;
import com.example.pizza.constants.user.UserStatus;
import com.example.pizza.dto.admin.DashboardResponseDTO;
import com.example.pizza.dto.paginate.PagedResponse;
import com.example.pizza.dto.user.UserResponse;
import com.example.pizza.logic.mapper.UserMapper;
import com.example.pizza.service.admin.AdminService;
import com.example.pizza.service.user.UserSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminRestController {

    private final AdminService adminService;
    private final UserMapper userMapper;
    private final UserSearchService userSearchService;

    // ============================================================================
    // DASHBOARD & LEGACY
    // ============================================================================

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponseDTO> getDashboardStats() {
        try {
            return ResponseEntity.ok(adminService.getDashboardStatistics());
        } catch (Exception e) {
            log.error("Dashboard error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        try {
            return ResponseEntity.ok(adminService.getAllUsers().stream()
                    .map(userMapper::toUserResponse)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/users/{id}/approve")
    public ResponseEntity<UserResponse> approveUser(@PathVariable Long id) {
        try {
            log.info("Approving user ID: {}", id);
            return ResponseEntity.ok(userMapper.toUserResponse(adminService.approveUser(id)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/users/{id}/reject")
    public ResponseEntity<UserResponse> rejectUser(@PathVariable Long id) {
        try {
            log.info("Rejecting user ID: {}", id);
            return ResponseEntity.ok(userMapper.toUserResponse(adminService.rejectUser(id)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(@PathVariable Long id, @RequestParam Role role) {
        try {
            log.info("Updating user {} role to: {}", id, role);
            return ResponseEntity.ok(userMapper.toUserResponse(adminService.updateUserRole(id, role)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================================================
    // PAGINATION ENDPOINTS (DB - Mapped to DTO)
    // ============================================================================

    @GetMapping("/users/paged")
    public ResponseEntity<PagedResponse<UserResponse>> getAllUsersPaged(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        try {
            Page<UserResponse> responsePage = adminService.getAllUsers(pageable)
                    .map(userMapper::toUserResponse);
            return ResponseEntity.ok(PagedResponse.of(responsePage));
        } catch (Exception e) {
            log.error("Paged users error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/users/paged/pending")
    public ResponseEntity<PagedResponse<UserResponse>> getPendingUsersPaged(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        try {
            Page<UserResponse> responsePage = adminService.getPendingUsers(pageable)
                    .map(userMapper::toUserResponse);
            return ResponseEntity.ok(PagedResponse.of(responsePage));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/users/paged/role/{role}")
    public ResponseEntity<PagedResponse<UserResponse>> getUsersByRolePaged(
            @PathVariable Role role,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        try {
            Page<UserResponse> responsePage = adminService.getUsersByRole(role, pageable)
                    .map(userMapper::toUserResponse);
            return ResponseEntity.ok(PagedResponse.of(responsePage));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/users/paged/status/{status}")
    public ResponseEntity<PagedResponse<UserResponse>> getUsersByStatusPaged(
            @PathVariable UserStatus status,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        try {
            Page<UserResponse> responsePage = adminService.getUsersByStatus(status, pageable)
                    .map(userMapper::toUserResponse);
            return ResponseEntity.ok(PagedResponse.of(responsePage));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================================================
    // CENTRALIZED USER SEARCH ENDPOINT
    // ============================================================================

    /**
     * Merkezi Kullanıcı Arama Endpoint'i (Admin Only)
     *
     * URL Örnekleri:
     * - /api/admin/users/search?query=Ahmet
     * - /api/admin/users/search?role=CUSTOMER&status=ACTIVE
     * - /api/admin/users/search?email=test@mail.com
     */
    @GetMapping("/users/search")
    public ResponseEntity<PagedResponse<UserResponse>> searchUsers(
            @RequestParam(required = false) String query, // İsim, Soyisim veya Email (genel arama)
            @RequestParam(required = false) String email, // Tam eşleşme (opsiyonel)
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {

        Page<UserResponse> page = userSearchService.searchUsersDynamic(
                query, email, role, status, pageable);

        return ResponseEntity.ok(PagedResponse.of(page));
    }

    @PostMapping("/users/reindex")
    public ResponseEntity<String> reindexAllUsers() {
        userSearchService.indexAllUsers();
        return ResponseEntity.ok("All users reindexed successfully");
    }

    // ============================================================================
    // DASHBOARD ANALYTICS ENDPOINTS
    // ============================================================================

    /**
     * Get total revenue from all orders
     * GET /api/admin/analytics/revenue/total
     */
    @GetMapping("/analytics/revenue/total")
    public ResponseEntity<Double> getTotalRevenue() {
        try {
            Double revenue = adminService.calculateTotalRevenue();
            return ResponseEntity.ok(revenue);
        } catch (Exception e) {
            log.error("Error calculating total revenue: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get order count by status
     * GET /api/admin/analytics/orders/status/{status}/count
     * Example: /api/admin/analytics/orders/status/PENDING/count
     */
    @GetMapping("/analytics/orders/status/{status}/count")
    public ResponseEntity<Long> getOrderCountByStatus(@PathVariable String status) {
        try {
            long count = adminService.countOrdersByStatus(status);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            log.error("Invalid order status: {}", status);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error counting orders by status: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get count of out-of-stock products
     * GET /api/admin/analytics/stock/out-of-stock
     */
    @GetMapping("/analytics/stock/out-of-stock")
    public ResponseEntity<Long> getOutOfStockProductsCount() {
        try {
            long count = adminService.countOutOfStockProducts();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error counting out-of-stock products: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get count of low stock products
     * GET /api/admin/analytics/stock/low?threshold=10
     */
    @GetMapping("/analytics/stock/low")
    public ResponseEntity<Long> getLowStockProductsCount(
            @RequestParam(defaultValue = "10") int threshold) {
        try {
            long count = adminService.countLowStockProducts(threshold);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error counting low stock products: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get total stock by category
     * GET /api/admin/analytics/stock/category/{categoryId}
     */
    @GetMapping("/analytics/stock/category/{categoryId}")
    public ResponseEntity<Long> getTotalStockByCategory(@PathVariable Long categoryId) {
        try {
            long totalStock = adminService.calculateTotalStockByCategory(categoryId);
            return ResponseEntity.ok(totalStock);
        } catch (Exception e) {
            log.error("Error calculating total stock for category {}: ", categoryId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get count of pending users
     * GET /api/admin/analytics/users/pending/count
     */
    @GetMapping("/analytics/users/pending/count")
    public ResponseEntity<Long> getPendingUsersCount() {
        try {
            long count = adminService.countPendingUsers();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error counting pending users: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get user count by role
     * GET /api/admin/analytics/users/role/{role}/count
     * Example: /api/admin/analytics/users/role/CUSTOMER/count
     */
    @GetMapping("/analytics/users/role/{role}/count")
    public ResponseEntity<Long> getUserCountByRole(@PathVariable String role) {
        try {
            long count = adminService.countUsersByRole(role);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            log.error("Invalid role: {}", role);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error counting users by role: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}