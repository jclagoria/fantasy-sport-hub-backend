package com.fantasysporthub.domain.service;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Password hashing and verification using Argon2id.
 */
@Slf4j
@Service
public class PasswordService {

    private final Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);

    @Value("${security.password.argon2.iterations:3}")
    private int iterations;

    @Value("${security.password.argon2.memory:65536}")
    private int memory;

    @Value("${security.password.argon2.parallelism:4}")
    private int parallelism;

    /**
     * Hash password using Argon2id.
     * Runs on bounded elastic scheduler to avoid blocking reactor threads.
     */
    public Mono<String> hashPassword(String plainPassword) {
        return Mono.fromCallable(() -> {
                    char[] chars = plainPassword.toCharArray();
                    try {
                        return argon2.hash(iterations, memory, parallelism, chars);
                    } finally {
                        // Clear sensitive data from memory
                        argon2.wipeArray(chars);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(hash -> log.debug("Password hashed successfully"))
                .doOnError(e -> log.error("Failed to hash password", e));
    }

    /**
     * Verify password against hash.
     * Constant-time comparison to prevent timing attacks.
     */
    public Mono<Boolean> verifyPassword(String plainPassword, String hash) {
        return Mono.fromCallable(() -> {
                    char[] chars = plainPassword.toCharArray();
                    try {
                        return argon2.verify(hash, chars);
                    } finally {
                        // Clear sensitive data from memory
                        argon2.wipeArray(chars);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(result -> log.debug("Password verification completed: {}", result))
                .doOnError(e -> log.error("Failed to verify password", e));
    }

}
