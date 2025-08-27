package org.example.ssj3pj.redis;
import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.request.UserRequestData;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VideoRequestService {
    private final RedisTemplate<String, Object> redisTemplate;

    public void saveUserRequest(Long userId, String imageKey, String locationCode) {
        String key = "video:request:" + userId;
        UserRequestData data = new UserRequestData(userId, imageKey, locationCode);
        redisTemplate.opsForValue().set(key, data);
    }
    public UserRequestData getUserRequest(Long userId) {
        return (UserRequestData) redisTemplate.opsForValue().get("video:request:" + userId);
    }
}
