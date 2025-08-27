package org.example.ssj3pj.repository;

import jakarta.persistence.LockModeType;
import org.example.ssj3pj.entity.User.RedditToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RedditTokenRepository extends JpaRepository<RedditToken, Long> {

    // user_id로 단건 조회 (유니크)
    @Query("select t from RedditToken t where t.user.id = :userId")
    Optional<RedditToken> findByUserId(@Param("userId") Long userId);

    // 만료 임박 토큰 배치 갱신용(선택)
    @Query("select t from RedditToken t where t.expiresAt <= :threshold and t.refreshToken is not null")
    List<RedditToken> findExpiringSoon(@Param("threshold") Instant threshold);

    // 갱신 시 충돌 방지(선택): PESSIMISTIC_WRITE
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RedditToken t where t.user.id = :userId")
    Optional<RedditToken> findByUserIdForUpdate(@Param("userId") Long userId);
}
