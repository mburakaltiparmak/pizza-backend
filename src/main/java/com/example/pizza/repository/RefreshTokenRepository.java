package com.example.pizza.repository;

import com.example.pizza.entity.token.RefreshToken;
import com.example.pizza.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByUser(User user);
    List<RefreshToken> findByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
    int deleteByExpiryDateBefore(@Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.revoked = true")
    int deleteRevokedTokens();

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.token = :token")
    int deleteByToken(@Param("token") String token);

    @Query("SELECT CASE WHEN COUNT(rt) > 0 THEN true ELSE false END " +
            "FROM RefreshToken rt WHERE rt.token = :token AND rt.revoked = false")
    boolean existsByTokenAndNotRevoked(@Param("token") String token);

    @Query("SELECT rt FROM RefreshToken rt " +
            "WHERE rt.user.id = :userId " +
            "AND rt.revoked = false " +
            "AND rt.expiryDate > :now")
    List<RefreshToken> findValidTokensByUserId(
            @Param("userId") Long userId,
            @Param("now") Instant now
    );

    @Query("SELECT COUNT(rt) FROM RefreshToken rt " +
            "WHERE rt.user.id = :userId " +
            "AND rt.revoked = false " +
            "AND rt.expiryDate > :now")
    long countValidTokensByUserId(
            @Param("userId") Long userId,
            @Param("now") Instant now
    );

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId")
    int revokeAllUserTokens(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.token = :token")
    int revokeToken(@Param("token") String token);
}