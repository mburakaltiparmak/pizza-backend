package com.example.pizza.service.user;

import com.example.pizza.dto.address.UserAddressDto;
import com.example.pizza.constants.user.Role;
import com.example.pizza.entity.user.User;
import com.example.pizza.entity.user.UserAddress;
import com.example.pizza.constants.user.UserStatus;
import com.example.pizza.entity.token.VerificationToken;
import com.example.pizza.exceptions.common.ResourceNotFoundException;
import com.example.pizza.exceptions.user.UserRegistrationException;
import com.example.pizza.repository.UserRepository;
import com.example.pizza.repository.VerificationTokenRepository;
import com.example.pizza.service.logic.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final VerificationTokenRepository tokenRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(\\+90|0)?[0-9]{10}$"
    );

    @Transactional(rollbackFor = Exception.class)
    public User registerUser(User user) {
        log.info("Registering new user: {}", user.getEmail());

        if (user == null) {
            throw new IllegalArgumentException("Kullanıcı bilgisi boş olamaz");
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email adresi gereklidir");
        }

        if (!EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
            throw new IllegalArgumentException("Geçersiz email formatı");
        }

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new UserRegistrationException("Bu email zaten kullanılıyor");
        }

        if (user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty()) {
            if (!PHONE_PATTERN.matcher(user.getPhoneNumber()).matches()) {
                throw new IllegalArgumentException("Geçersiz telefon numarası formatı");
            }

            if (userRepository.findByPhoneNumber(user.getPhoneNumber()).isPresent()) {
                throw new UserRegistrationException("Bu telefon numarası zaten kullanılıyor");
            }
        }

        if (user.getName() == null || user.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("İsim gereklidir");
        }

        if (user.getName().length() < 2 || user.getName().length() > 50) {
            throw new IllegalArgumentException("İsim 2-50 karakter arasında olmalıdır");
        }

        if (user.getSurname() == null || user.getSurname().trim().isEmpty()) {
            throw new IllegalArgumentException("Soyisim gereklidir");
        }

        if (user.getSurname().length() < 2 || user.getSurname().length() > 50) {
            throw new IllegalArgumentException("Soyisim 2-50 karakter arasında olmalıdır");
        }

        if (user.getPassword() != null) {
            if (user.getPassword().length() < 6) {
                throw new IllegalArgumentException("Şifre en az 6 karakter olmalıdır");
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        user.setRole(Role.CUSTOMER);
        user.setStatus(UserStatus.PENDING);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        if (user.getRole() != Role.GUEST) {
            String token = UUID.randomUUID().toString();
            VerificationToken verificationToken = new VerificationToken();
            verificationToken.setToken(token);
            verificationToken.setUser(savedUser);
            verificationToken.setExpiryDate(LocalDateTime.now().plusHours(24));
            tokenRepository.save(verificationToken);
        if("${spring.profiles.active}".equals("prod") ) {}
            emailService.sendVerificationEmail(savedUser, token);
        }

        log.info("User registered successfully: {}", savedUser.getEmail());
        return savedUser;
    }

    @Transactional(rollbackFor = Exception.class)
    public User addAddress(Long userId, UserAddressDto addressDto) {
        log.info("Adding address for user ID: {}", userId);

        // Validation
        if (addressDto == null) {
            throw new IllegalArgumentException("Adres bilgisi boş olamaz");
        }

        if (addressDto.getFullAddress() == null || addressDto.getFullAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Adres detayı gereklidir");
        }

        if (addressDto.getCity() == null || addressDto.getCity().trim().isEmpty()) {
            throw new IllegalArgumentException("Şehir bilgisi gereklidir");
        }

        if (addressDto.getDistrict() == null || addressDto.getDistrict().trim().isEmpty()) {
            throw new IllegalArgumentException("İlçe bilgisi gereklidir");
        }

        if (addressDto.getFullAddress().length() < 10) {
            throw new IllegalArgumentException("Adres çok kısa (minimum 10 karakter)");
        }

        if (addressDto.getFullAddress().length() > 500) {
            throw new IllegalArgumentException("Adres çok uzun (maximum 500 karakter)");
        }

        if (addressDto.getPhoneNumber() != null && !addressDto.getPhoneNumber().trim().isEmpty()) {
            if (!PHONE_PATTERN.matcher(addressDto.getPhoneNumber()).matches()) {
                throw new IllegalArgumentException("Geçersiz telefon numarası formatı");
            }
        }

        User user = getUserById(userId);

        // Create new address with sequence-generated ID
        UserAddress newAddress = new UserAddress();
        newAddress.setId(generateAddressId());  // ✅ FIXED: Uses database sequence
        newAddress.setFullAddress(addressDto.getFullAddress().trim());
        newAddress.setCity(addressDto.getCity().trim());
        newAddress.setDistrict(addressDto.getDistrict().trim());
        newAddress.setPostalCode(addressDto.getPostalCode());
        newAddress.setAddressTitle(addressDto.getAddressTitle());
        newAddress.setPhoneNumber(addressDto.getPhoneNumber());
        newAddress.setRecipientName(addressDto.getRecipientName());
        newAddress.setCreatedAt(LocalDateTime.now());
        newAddress.setUpdatedAt(LocalDateTime.now());

        // Set as default if first address or explicitly requested
        if (user.getAddresses().isEmpty() || Boolean.TRUE.equals(addressDto.getIsDefault())) {
            user.getAddresses().forEach(address -> address.setIsDefault(false));
            newAddress.setIsDefault(true);
        } else {
            newAddress.setIsDefault(false);
        }

        user.getAddresses().add(newAddress);
        User savedUser = userRepository.save(user);

        log.info("Address added successfully for user: {} with ID: {}",
                user.getEmail(), newAddress.getId());
        return savedUser;
    }
    @Transactional(rollbackFor = Exception.class)
    public User updateAddress(Long userId, int addressId, UserAddressDto addressDto) {
        log.info("Updating address ID {} for user ID: {}", addressId, userId);

        if (addressDto == null) {
            throw new IllegalArgumentException("Güncellenecek adres bilgisi boş olamaz");
        }

        if (addressId <= 0) {
            throw new IllegalArgumentException("Geçersiz adres ID'si");
        }

        User user = getUserById(userId);
        UserAddress existingAddress = findAddressById(user, (long) addressId);

        if (addressDto.getFullAddress() != null) {
            String trimmedAddress = addressDto.getFullAddress().trim();
            if (trimmedAddress.isEmpty()) {
                throw new IllegalArgumentException("Adres boş olamaz");
            }
            if (trimmedAddress.length() < 10) {
                throw new IllegalArgumentException("Adres çok kısa (minimum 10 karakter)");
            }
            if (trimmedAddress.length() > 500) {
                throw new IllegalArgumentException("Adres çok uzun (maximum 500 karakter)");
            }
            existingAddress.setFullAddress(trimmedAddress);
        }

        if (addressDto.getCity() != null) {
            if (addressDto.getCity().trim().isEmpty()) {
                throw new IllegalArgumentException("Şehir bilgisi boş olamaz");
            }
            existingAddress.setCity(addressDto.getCity().trim());
        }

        if (addressDto.getDistrict() != null) {
            if (addressDto.getDistrict().trim().isEmpty()) {
                throw new IllegalArgumentException("İlçe bilgisi boş olamaz");
            }
            existingAddress.setDistrict(addressDto.getDistrict().trim());
        }

        if (addressDto.getPostalCode() != null) {
            existingAddress.setPostalCode(addressDto.getPostalCode());
        }

        if (addressDto.getAddressTitle() != null) {
            existingAddress.setAddressTitle(addressDto.getAddressTitle());
        }

        if (addressDto.getPhoneNumber() != null) {
            if (!addressDto.getPhoneNumber().trim().isEmpty() &&
                    !PHONE_PATTERN.matcher(addressDto.getPhoneNumber()).matches()) {
                throw new IllegalArgumentException("Geçersiz telefon numarası formatı");
            }
            existingAddress.setPhoneNumber(addressDto.getPhoneNumber());
        }

        if (addressDto.getRecipientName() != null) {
            existingAddress.setRecipientName(addressDto.getRecipientName());
        }

        if (Boolean.TRUE.equals(addressDto.getIsDefault())) {
            user.getAddresses().forEach(address -> address.setIsDefault(false));
            existingAddress.setIsDefault(true);
        }

        existingAddress.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        log.info("Address updated successfully for user: {}", user.getEmail());
        return savedUser;
    }

    @Transactional(rollbackFor = Exception.class)
    public User removeAddress(Long userId, int addressId) {
        log.info("Removing address ID {} for user ID: {}", addressId, userId);

        if (addressId <= 0) {
            throw new IllegalArgumentException("Geçersiz adres ID'si");
        }

        User user = getUserById(userId);

        UserAddress addressToRemove = null;
        int indexToRemove = -1;

        for (int i = 0; i < user.getAddresses().size(); i++) {
            UserAddress address = user.getAddresses().get(i);
            if (address.getId() != null && address.getId() == addressId) {
                addressToRemove = address;
                indexToRemove = i;
                break;
            }
        }

        if (addressToRemove == null) {
            throw new ResourceNotFoundException("Adres bulunamadı: ID " + addressId);
        }

        user.getAddresses().remove(indexToRemove);

        if (Boolean.TRUE.equals(addressToRemove.getIsDefault()) && !user.getAddresses().isEmpty()) {
            user.getAddresses().get(0).setIsDefault(true);
        }

        User savedUser = userRepository.save(user);
        log.info("Address removed successfully for user: {}", user.getEmail());
        return savedUser;
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        log.info("Changing password for user ID: {}", userId);

        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Mevcut şifre gereklidir");
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Yeni şifre gereklidir");
        }

        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Yeni şifre en az 6 karakter olmalıdır");
        }

        if (currentPassword.equals(newPassword)) {
            throw new IllegalArgumentException("Yeni şifre mevcut şifre ile aynı olamaz");
        }

        User user = getUserById(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mevcut şifre yanlış");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", user.getEmail());
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<User> getPendingUsers() {
        log.debug("Fetching pending users");
        return userRepository.findByStatus(UserStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        log.debug("Finding user by ID: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı: ID " + id));
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı: " + email));
    }

    @Transactional(readOnly = true)
    public User getUserByEmailWithAddresses(String email) {
        log.debug("Finding user with addresses by email: {}", email);
        return userRepository.findByEmailWithAddresses(email)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı: " + email));
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    @Transactional(readOnly = true)
    public UserAddress findAddressById(User user, Long addressId) {
        for (UserAddress address : user.getAddresses()) {
            if (address.getId() != null && address.getId().equals(addressId)) {
                return address;
            }
        }
        throw new ResourceNotFoundException("Adres bulunamadı: ID " + addressId);
    }

    private Long generateAddressId() {
        try {
            // Query sequence for next value
            String sql = "SELECT nextval('pizza.user_addresses_seq')";
            Long id = jdbcTemplate.queryForObject(sql, Long.class);

            if (id == null) {
                log.error("Sequence returned null - database configuration error");
                throw new IllegalStateException("Failed to generate address ID: sequence returned null");
            }

            log.debug("Generated address ID from sequence: {}", id);
            return id;

        } catch (DataAccessException e) {
            log.error("Failed to generate address ID from sequence", e);
            throw new IllegalStateException(
                    "Address ID generation failed. Ensure database sequence 'pizza.user_addresses_seq' exists.",
                    e
            );
        }
    }

    @Transactional(readOnly = true)
    public boolean isOAuthUser(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        return userOpt.isPresent() &&
                userOpt.get().getOauthProvider() != null &&
                !userOpt.get().getOauthProvider().isEmpty();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateLoginTime(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            log.debug("Updated login time for user: {}", email);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean sendPasswordResetEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        String token = UUID.randomUUID().toString();

        VerificationToken resetToken = new VerificationToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusHours(1));
        tokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user, token);

        log.info("Password reset email sent to: {}", email);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(String token, String newPassword) {
        VerificationToken resetToken = tokenRepository.findByToken(token);

        if (resetToken == null) {
            log.warn("Invalid reset token: {}", token);
            return false;
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.warn("Expired reset token: {}", token);
            return false;
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Yeni şifre boş olamaz");
        }

        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Yeni şifre en az 6 karakter olmalıdır");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepository.delete(resetToken);

        log.info("Password reset successfully for user: {}", user.getEmail());
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public User approveUser(Long userId) {
        User user = getUserById(userId);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public User rejectUser(Long userId) {
        User user = getUserById(userId);
        user.setStatus(UserStatus.REJECTED);
        return userRepository.save(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public User updateUserRole(Long userId, Role newRole) {
        User user = getUserById(userId);
        user.setRole(newRole);
        return userRepository.save(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public User updateUserProfile(Long userId, User updatedUser) {
        User user = getUserById(userId);

        if (updatedUser.getName() != null && !updatedUser.getName().trim().isEmpty()) {
            user.setName(updatedUser.getName());
        }

        if (updatedUser.getSurname() != null && !updatedUser.getSurname().trim().isEmpty()) {
            user.setSurname(updatedUser.getSurname());
        }

        if (updatedUser.getPhoneNumber() != null && !updatedUser.getPhoneNumber().trim().isEmpty()) {
            user.setPhoneNumber(updatedUser.getPhoneNumber());
        }

        return userRepository.save(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public User setDefaultAddress(Long userId, int addressId) {
        User user = getUserById(userId);
        UserAddress addressToSetDefault = findAddressById(user, (long) addressId);

        user.getAddresses().forEach(address -> address.setIsDefault(false));
        addressToSetDefault.setIsDefault(true);

        return userRepository.save(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean verifyEmail(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token);

        if (verificationToken == null) {
            throw new ResourceNotFoundException("Geçersiz doğrulama linki");
        }

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Doğrulama linki süresi dolmuş");
        }

        User user = verificationToken.getUser();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        tokenRepository.delete(verificationToken);

        log.info("Email verified successfully for user: {}", user.getEmail());
        return true;
    }
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        log.debug("Fetching paginated users - page: {}, size: {}, sort: {}",
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort());

        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> getPendingUsers(Pageable pageable) {
        log.debug("Fetching paginated pending users - page: {}, size: {}",
                pageable.getPageNumber(),
                pageable.getPageSize());

        return userRepository.findByStatus(UserStatus.PENDING, pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> getUsersByRole(Role role, Pageable pageable) {
        log.debug("Fetching paginated users by role: {} - page: {}, size: {}",
                role,
                pageable.getPageNumber(),
                pageable.getPageSize());

        return userRepository.findByRole(role, pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> getUsersByStatus(UserStatus status, Pageable pageable) {
        log.debug("Fetching paginated users by status: {} - page: {}, size: {}",
                status,
                pageable.getPageNumber(),
                pageable.getPageSize());

        return userRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> searchByEmail(String email, Pageable pageable) {
        log.debug("Searching users by email: '{}' - page: {}, size: {}",
                email,
                pageable.getPageNumber(),
                pageable.getPageSize());

        if (email == null || email.trim().isEmpty()) {
            log.warn("Empty search term provided, returning all users");
            return getAllUsers(pageable);
        }

        return userRepository.searchByEmail(email, pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> searchByName(String name, Pageable pageable) {
        log.debug("Searching users by name: '{}' - page: {}, size: {}",
                name,
                pageable.getPageNumber(),
                pageable.getPageSize());

        if (name == null || name.trim().isEmpty()) {
            log.warn("Empty search term provided, returning all users");
            return getAllUsers(pageable);
        }

        return userRepository.searchByName(name, pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> getUsersByRoleAndStatus(Role role, UserStatus status, Pageable pageable) {
        log.debug("Fetching users by role: {} and status: {} - page: {}, size: {}",
                role,
                status,
                pageable.getPageNumber(),
                pageable.getPageSize());

        return userRepository.findByRoleAndStatus(role, status, pageable);
    }

}