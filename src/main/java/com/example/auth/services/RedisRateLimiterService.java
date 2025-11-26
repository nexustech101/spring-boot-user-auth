package com.example.auth.services;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class RedisRateLimiterService {
    private final StringRedisTemplate redis;
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    public RedisRateLimiterService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean isAllowed(String username) {
        String key = "rl:signin:" + username;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, WINDOW);
        }
        return count != null && count <= MAX_ATTEMPTS;
    }
}