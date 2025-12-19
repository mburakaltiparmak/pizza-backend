package com.example.pizza.service.user;

import com.example.pizza.dto.user.SupabaseUserDto;
import com.example.pizza.constants.user.Role;
import com.example.pizza.entity.user.User;
import com.example.pizza.constants.user.UserStatus;
import com.example.pizza.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class SupabaseUserService {
    private static final Logger logger = LoggerFactory.getLogger(SupabaseUserService.class);

    private final UserRepository userRepository;

    @Autowired
    public SupabaseUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User syncUserWithSupabase(String email, String name, String surname, String phoneNumber, String supabaseId) {
        logger.info("Syncing Supabase user with individual parameters");

        // SupabaseUserDto oluştur ve syncSupabaseUser metodunu çağır
        SupabaseUserDto userDto = new SupabaseUserDto();
        userDto.setEmail(email);
        userDto.setName(name);
        userDto.setSurname(surname);
        userDto.setPhoneNumber(phoneNumber);
        userDto.setSupabaseId(supabaseId);

        return syncSupabaseUser(userDto);
    }

    @Transactional
    public User syncSupabaseUser(SupabaseUserDto supabaseUser) {
        logger.info("Syncing Supabase user: {}", supabaseUser.getEmail());

        // Veri kontrolü ve loglamalar
        logSupabaseUserData(supabaseUser);

        try {
            // Email ile kullanıcı kontrolü
            Optional<User> existingUserByEmail = userRepository.findByEmail(supabaseUser.getEmail());

            // Supabase ID ile kullanıcı kontrolü (email değişmiş olabilir)
            Optional<User> existingUserById = Optional.empty();
            if (supabaseUser.getSupabaseId() != null) {
                existingUserById = userRepository.findBySupabaseId(supabaseUser.getSupabaseId());
            }

            // Mevcut kullanıcı bulundu mu?
            if (existingUserByEmail.isPresent()) {
                logger.info("Existing user found by email: {}", supabaseUser.getEmail());
                return updateExistingUser(existingUserByEmail.get(), supabaseUser);
            } else if (existingUserById.isPresent()) {
                logger.info("Existing user found by Supabase ID: {}", supabaseUser.getSupabaseId());
                return updateExistingUser(existingUserById.get(), supabaseUser);
            } else {
                // Yeni kullanıcı oluştur
                logger.info("Creating new user from Supabase: {}", supabaseUser.getEmail());
                return createNewUser(supabaseUser);
            }
        } catch (Exception e) {
            logger.error("Error syncing Supabase user", e);
            throw e;
        }
    }

    private User updateExistingUser(User existingUser, SupabaseUserDto supabaseUser) {
        // Email güncelleme (eğer değiştiyse)
        if (supabaseUser.getEmail() != null && !supabaseUser.getEmail().equals(existingUser.getEmail())) {
            existingUser.setEmail(supabaseUser.getEmail());
        }

        // İsim güncelleme
        if (supabaseUser.getName() != null) {
            existingUser.setName(supabaseUser.getName());
        }

        // Soyisim güncelleme - boş kontrolü ile
        String surname = supabaseUser.getSurname();
        if (surname != null && !surname.trim().isEmpty()) {
            existingUser.setSurname(surname);
        } else if (existingUser.getSurname() == null || existingUser.getSurname().trim().isEmpty()) {
            // Mevcut soyisim de boşsa, varsayılan değer ata veya isimden çıkar
            setDefaultSurname(existingUser, supabaseUser.getName());
        }

        // Telefon numarası güncelleme
        if (supabaseUser.getPhoneNumber() != null) {
            existingUser.setPhoneNumber(supabaseUser.getPhoneNumber());
        }

        // Supabase ID güncelleme
        if (supabaseUser.getSupabaseId() != null) {
            existingUser.setSupabaseId(supabaseUser.getSupabaseId());
        }

        // Son giriş zamanını güncelle
        existingUser.setLastLogin(LocalDateTime.now());

        // Diğer güncellemeler burada yapılabilir

        logger.info("Updated existing user: {}", existingUser.getEmail());
        return userRepository.save(existingUser);
    }

    private User createNewUser(SupabaseUserDto supabaseUser) {
        User newUser = new User();

        // Temel bilgiler
        newUser.setEmail(supabaseUser.getEmail());
        newUser.setName(supabaseUser.getName() != null ? supabaseUser.getName() : "Kullanıcı");

        // Soyisim kontrolü ve atama
        String surname = supabaseUser.getSurname();
        if (surname == null || surname.trim().isEmpty()) {
            setDefaultSurname(newUser, supabaseUser.getName());
        } else {
            newUser.setSurname(surname);
        }

        // Diğer alanları ayarla
        newUser.setPhoneNumber(supabaseUser.getPhoneNumber() != null ? supabaseUser.getPhoneNumber() : "");
        newUser.setPassword(UUID.randomUUID().toString()); // Rastgele güvenli şifre
        newUser.setRole(Role.CUSTOMER);
        newUser.setStatus(UserStatus.ACTIVE);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setLastLogin(LocalDateTime.now());
        newUser.setSupabaseId(supabaseUser.getSupabaseId());
        newUser.setOauthProvider("supabase");

        logger.info("Saving new user with role: {} and status: {}", newUser.getRole(), newUser.getStatus());
        return userRepository.save(newUser);
    }

    private void setDefaultSurname(User user, String fullName) {
        // İsimden soyisim çıkarmayı dene
        if (fullName != null && fullName.contains(" ")) {
            String[] nameParts = fullName.split(" ", 2);
            user.setName(nameParts[0]);
            user.setSurname(nameParts[1]);
            logger.info("Split full name into name: {} and surname: {}", nameParts[0], nameParts[1]);
        } else {
            // Varsayılan soyisim
            user.setSurname("Belirlenmedi");
            logger.info("Set default surname as 'Belirlenmedi'");
        }
    }

    private void logSupabaseUserData(SupabaseUserDto supabaseUser) {
        logger.info("Sync data - email: {}", supabaseUser.getEmail());
        logger.info("Sync data - supabaseId: {}", supabaseUser.getSupabaseId());
        logger.info("Sync data - name: {}", supabaseUser.getName());
        logger.info("Sync data - surname: {}", supabaseUser.getSurname());
        logger.info("Sync data - phoneNumber: {}", supabaseUser.getPhoneNumber());
    }
}