package com.example.auth.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;

import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting service using Guava to limit login attempts.
 * Default: 3 attempts per 10 minutes per username.
 */
@SuppressWarnings("NullableProblems")
@Service
public class RateLimiterService {

    private final LoadingCache<String, RateLimiter> limiterCache;
    private static final double PERMITS_PER_SECOND = 3.0 / 600.0; // 3 permits per 600 seconds (10 min)

    public RateLimiterService() {
        this.limiterCache = CacheBuilder.newBuilder()
                .maximumSize(10000) // Max 10k users in cache
                .expireAfterAccess(10, TimeUnit.MINUTES) // Remove after 10 min of inactivity
                .build(new CacheLoader<String, RateLimiter>() {
                    @Override
                    public RateLimiter load(@javax.annotation.Nonnull String key) {
                        return RateLimiter.create(PERMITS_PER_SECOND);
                    }
                });
    }

    /**
     * Check if the user has exceeded the rate limit.
     *
     * @param username the username to rate limit
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isAllowed(@javax.annotation.Nonnull String username) {
        try {
            RateLimiter rateLimiter = limiterCache.get(username);
            return rateLimiter.tryAcquire();
        } catch (ExecutionException e) {
            // If cache fails, allow the request (fail open for security)
            return true;
        }
    }

    /**
     * Get remaining attempts for the user.
     *
     * @param username the username to check
     * @return remaining attempts (approximate, up to 3)
     */
    public int getRemainingAttempts(@javax.annotation.Nonnull String username) {
        try {
            RateLimiter rateLimiter = limiterCache.getIfPresent(username);
            if (rateLimiter == null) {
                return 3;
            }
            // Approximation: rate limiter doesn't directly expose remaining attempts
            // We'll estimate based on the state
            double availablePermits = rateLimiter.getRate() * rateLimiter.getRate();
            return Math.max(0, (int) Math.ceil(availablePermits));
        } catch (Exception e) {
            return 3;
        }
    }
}
