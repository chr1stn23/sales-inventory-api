package com.christn.salesinventoryapi.repository;

import com.christn.salesinventoryapi.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUserIdAndRevokedAtIsNull(Long userId);

    @Modifying
    @Query("""
            DELETE FROM RefreshToken rf
            WHERE (rf.expiresAt < :now OR rf.revokedAt IS NOT NULL)
                AND rf.createdAt < :threshold
            """)
    void deleteExpiredOrRevoked(@Param("now") LocalDateTime now, @Param("threshold") LocalDateTime threshold);


    @Modifying
    @Query(""" 
            UPDATE RefreshToken rt
            SET rt.revokedAt = :now
            WHERE rt.userId = :userId
                AND rt.revokedAt IS NULL
            """)
    void revokeAllByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            UPDATE RefreshToken rt
            SET rt.revokedAt = :now
            WHERE rt.tokenHash = :tokenHash
                AND rt.userId = :userId
                AND rt.revokedAt IS NULL
            """)
    void revokeByTokenHashAndUserId(
            @Param("tokenHash") String tokenHash,
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);
}
