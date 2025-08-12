package org.example.ssj3pj.repository;

import jakarta.persistence.LockModeType;
import org.example.ssj3pj.entity.User.GoogleToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GoogleTokenRepository extends JpaRepository<GoogleToken, Long> {

    Optional<GoogleToken> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GoogleToken g where g.user.id = :userId")
    Optional<GoogleToken> findByUserIdForUpdate(@Param("userId") Long userId);
}
