package org.example.ssj3pj.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RefreshStoreRedis {
  private final StringRedisTemplate redis;

  private String key(String userId, String deviceId) { return "rt:" + userId + ":" + deviceId; }
  private String idx(String userId) { return "rtidx:" + userId; }

  public void save(String userId, String deviceId, String jti, Instant expiresAt) {
    long ttl = Math.max(1, Duration.between(Instant.now(), expiresAt).getSeconds());
    redis.opsForValue().set(key(userId, deviceId), jti, Duration.ofSeconds(ttl));
    redis.opsForSet().add(idx(userId), deviceId);
  }

  public @Nullable String getJti(String userId, String deviceId) {
    return redis.opsForValue().get(key(userId, deviceId));
  }

  public void delete(String userId, String deviceId) {
    redis.delete(key(userId, deviceId));
    redis.opsForSet().remove(idx(userId), deviceId);
  }

  public void revokeAll(String userId) {
    Set<String> devices = redis.opsForSet().members(idx(userId));
    if (devices != null) for (String d : devices) redis.delete(key(userId, d));
    redis.delete(idx(userId));
  }
}
