package com.example.auth.services;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

/**
 * Distributed rate limiting service using Redis.
 * Limits login attempts to 3 per 10 minutes per username.
 * Shared state across multiple application instances.
 */
@Service
public class RedisRateLimiterService {
    private final StringRedisTemplate redis;
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    public RedisRateLimiterService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Checks if a login attempt is allowed using Redis counter.
     * Atomically increments attempt counter with 10-minute TTL.
     *
     * @param username the username to rate limit
     * @return true if allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String username) {
        String key = "rl:signin:" + username;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, WINDOW);
        }
        return count != null && count <= MAX_ATTEMPTS;
    }
}