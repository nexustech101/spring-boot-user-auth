package com.example.auth.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    RedisSerializer.json()
                )
            );
            
        return RedisCacheManager.builder(factory)
            .cacheDefaults(base)
            .withCacheConfiguration("usersById", base.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration("usersByUsername", base.entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration("usersByEmail", base.entryTtl(Duration.ofMinutes(10)))
            .build();
    }
}