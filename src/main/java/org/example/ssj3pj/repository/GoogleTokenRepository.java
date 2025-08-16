package org.example.ssj3pj.repository;

import jakarta.persistence.LockModeType;
import org.example.ssj3pj.entity.User.GoogleToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GoogleTokenRepository extends JpaRepository<GoogleToken, Long> {



    @Query("SELECT t FROM GoogleToken t " +
            "WHERE t.expiresAt <= :threshold AND t.refreshToken IS NOT NULL")
    List<GoogleToken> findExpiringSoon(@Param("threshold") Instant threshold);

    Optional<GoogleToken> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GoogleToken g where g.user.id = :userId")
    Optional<GoogleToken> findByUserIdForUpdate(@Param("userId") Long userId);
}
