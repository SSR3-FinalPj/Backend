package org.example.ssj3pj.redis;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.request.UserRequestData;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VideoRequestService {
    private final RedisTemplate<String, Object> redisTemplate;

    public void saveJobRequest(Long jobId, Long userId, String imageKey, String locationCode) {
        String key = "video:request:job:" + jobId;
        UserRequestData data = new UserRequestData(jobId, userId, imageKey, locationCode);
        redisTemplate.opsForValue().set(key, data);
    }

    public UserRequestData getJobRequest(Long jobId) {
        return (UserRequestData) redisTemplate.opsForValue().get("video:request:job:" + jobId);
    }
}

