package com.fantasysporthub.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Token blacklist service using Redis for logout functionality.
 */
@Slf4j
@Service
public class TokenBlacklistService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public TokenBlacklistService(@Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final String BLACKLIST_PREFIX = "blacklist:";

    /**
     * Blacklist a token (during logout).
     *
     * @param tokenId JWT token ID
     * @param ttl Time to live (until original expiration)
     */
    public Mono<Void> blackListToken(String tokenId, Duration ttl) {
        var key = BLACKLIST_PREFIX + tokenId;

        return redisTemplate.opsForValue()
                .set(key, "revoked", ttl)
                .doOnSuccess(result -> log.info("Blacklisted token: {}", tokenId))
                .doOnError(e -> log.error("Failed to blacklist token: {}", tokenId, e))
                .then();
    }

    /**
     * Check if token is blacklisted.
     *
     * @param tokenId JWT token ID
     * @return true if blacklisted
     */
    public Mono<Boolean> isBlackListed(String tokenId) {
        var key = BLACKLIST_PREFIX + tokenId;

        return redisTemplate.hasKey(key)
                .defaultIfEmpty(Boolean.FALSE);
    }

}
