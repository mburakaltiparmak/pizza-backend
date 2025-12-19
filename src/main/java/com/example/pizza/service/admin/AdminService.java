package com.example.pizza.service.admin;

import com.example.pizza.dto.admin.DashboardResponseDTO;
import com.example.pizza.constants.user.Role;
import com.example.pizza.entity.user.User;
import com.example.pizza.constants.user.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface AdminService {
    DashboardResponseDTO getDashboardStatistics();

    List<User> getAllUsers();

    List<User> getPendingUsers();

    User approveUser(Long userId);

    User rejectUser(Long userId);

    User updateUserRole(Long userId, Role newRole);

    // ============================================================================
    // PAGINATION METHODS (ADD TO ADMINSERVICE INTERFACE)
    // ============================================================================

    Page<User> getAllUsers(Pageable pageable);

    Page<User> getPendingUsers(Pageable pageable);

    Page<User> getUsersByRole(Role role, Pageable pageable);

    Page<User> getUsersByStatus(UserStatus status, Pageable pageable);

    // ============================================================================
    // DASHBOARD ANALYTICS METHODS
    // ============================================================================

    // Revenue Analytics
    Double calculateTotalRevenue();

    // Order Analytics
    long countOrdersByStatus(String status);

    // Stock Analytics
    long countOutOfStockProducts();

    long countLowStockProducts(int threshold);

    long calculateTotalStockByCategory(Long categoryId);

    // User Analytics
    long countPendingUsers();

    long countUsersByRole(String role);
}