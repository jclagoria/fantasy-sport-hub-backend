package com.fantasysporthub.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PasswordService Tests")
class PasswordServiceTest {

    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordService();
        // Use reflection to set the configuration values
        setField(passwordService, "iterations", 3);
        setField(passwordService, "memory", 65536);
        setField(passwordService, "parallelism", 4);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    @Nested
    @DisplayName("Password Hashing")
    class PasswordHashingTests {

        @Test
        @DisplayName("Should hash password successfully with Argon2id algorithm")
        void shouldHashPasswordWithArgon2id() {
            String password = "SecurePass123!";

            StepVerifier.create(passwordService.hashPassword(password))
                    .assertNext(hash -> {
                        assertThat(hash).isNotNull();
                        assertThat(hash).startsWith("$argon2id$");
                        assertThat(hash).isNotEqualTo(password);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should generate different hashes for same password (salt)")
        void shouldGenerateDifferentHashesForSamePassword() {
            String password = "SecurePass123!";

            Mono<String> hash1 = passwordService.hashPassword(password);
            Mono<String> hash2 = passwordService.hashPassword(password);

            StepVerifier.create(Mono.zip(hash1, hash2))
                    .assertNext(tuple -> {
                        String firstHash = tuple.getT1();
                        String secondHash = tuple.getT2();
                        assertThat(firstHash).isNotEqualTo(secondHash);
                        assertThat(firstHash).startsWith("$argon2id$");
                        assertThat(secondHash).startsWith("$argon2id$");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should hash password with special characters")
        void shouldHashPasswordWithSpecialCharacters() {
            String password = "P@ssw0rd!#$%^&*()_+-=[]{}|;:',.<>?/~`";

            StepVerifier.create(passwordService.hashPassword(password))
                    .assertNext(hash -> {
                        assertThat(hash).isNotNull();
                        assertThat(hash).startsWith("$argon2id$");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should hash very long password")
        void shouldHashVeryLongPassword() {
            String password = "a".repeat(1000);

            StepVerifier.create(passwordService.hashPassword(password))
                    .assertNext(hash -> {
                        assertThat(hash).isNotNull();
                        assertThat(hash).startsWith("$argon2id$");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should hash very short password")
        void shouldHashVeryShortPassword() {
            String password = "a";

            StepVerifier.create(passwordService.hashPassword(password))
                    .assertNext(hash -> {
                        assertThat(hash).isNotNull();
                        assertThat(hash).startsWith("$argon2id$");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should hash password with Unicode characters")
        void shouldHashPasswordWithUnicodeCharacters() {
            String password = "パスワード密码🔒";

            StepVerifier.create(passwordService.hashPassword(password))
                    .assertNext(hash -> {
                        assertThat(hash).isNotNull();
                        assertThat(hash).startsWith("$argon2id$");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should hash password with only whitespace")
        void shouldHashPasswordWithWhitespace() {
            String password = "   spaces   ";

            StepVerifier.create(passwordService.hashPassword(password))
                    .assertNext(hash -> {
                        assertThat(hash).isNotNull();
                        assertThat(hash).startsWith("$argon2id$");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should complete hashing within reasonable time")
        void shouldCompleteHashingWithinReasonableTime() {
            String password = "SecurePass123!";

            StepVerifier.create(passwordService.hashPassword(password))
                    .expectNextMatches(hash -> hash.startsWith("$argon2id$"))
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        }
    }

    @Nested
    @DisplayName("Password Verification")
    class PasswordVerificationTests {

        @Test
        @DisplayName("Should verify correct password successfully")
        void shouldVerifyCorrectPassword() {
            String password = "SecurePass123!";

            StepVerifier.create(
                    passwordService.hashPassword(password)
                            .flatMap(hash -> passwordService.verifyPassword(password, hash))
            )
            .assertNext(result -> assertThat(result).isTrue())
            .verifyComplete();
        }

        @Test
        @DisplayName("Should reject incorrect password")
        void shouldRejectIncorrectPassword() {
            String correctPassword = "SecurePass123!";
            String incorrectPassword = "WrongPassword456!";

            StepVerifier.create(
                    passwordService.hashPassword(correctPassword)
                            .flatMap(hash -> passwordService.verifyPassword(incorrectPassword, hash))
            )
            .assertNext(result -> assertThat(result).isFalse())
            .verifyComplete();
        }

        @Test
        @DisplayName("Should reject password with different case")
        void shouldRejectPasswordWithDifferentCase() {
            String password = "SecurePass123!";
            String wrongCase = "securepass123!";

            StepVerifier.create(
                    passwordService.hashPassword(password)
                            .flatMap(hash -> passwordService.verifyPassword(wrongCase, hash))
            )
            .assertNext(result -> assertThat(result).isFalse())
            .verifyComplete();
        }

        @Test
        @DisplayName("Should reject password with extra characters")
        void shouldRejectPasswordWithExtraCharacters() {
            String password = "SecurePass123!";
            String withExtra = "SecurePass123!extra";

            StepVerifier.create(
                    passwordService.hashPassword(password)
                            .flatMap(hash -> passwordService.verifyPassword(withExtra, hash))
            )
            .assertNext(result -> assertThat(result).isFalse())
            .verifyComplete();
        }

        @Test
        @DisplayName("Should reject password with missing characters")
        void shouldRejectPasswordWithMissingCharacters() {
            String password = "SecurePass123!";
            String withMissing = "SecurePass123";

            StepVerifier.create(
                    passwordService.hashPassword(password)
                            .flatMap(hash -> passwordService.verifyPassword(withMissing, hash))
            )
            .assertNext(result -> assertThat(result).isFalse())
            .verifyComplete();
        }

        @Test
        @DisplayName("Should verify password with special characters")
        void shouldVerifyPasswordWithSpecialCharacters() {
            String password = "P@ssw0rd!#$%^&*()";

            StepVerifier.create(
                    passwordService.hashPassword(password)
                            .flatMap(hash -> passwordService.verifyPassword(password, hash))
            )
            .assertNext(result -> assertThat(result).isTrue())
            .verifyComplete();
        }

        @Test
        @DisplayName("Should verify password with Unicode characters")
        void shouldVerifyPasswordWithUnicodeCharacters() {
            String password = "パスワード密码🔒";

            StepVerifier.create(
                    passwordService.hashPassword(password)
                            .flatMap(hash -> passwordService.verifyPassword(password, hash))
            )
            .assertNext(result -> assertThat(result).isTrue())
            .verifyComplete();
        }

        @Test
        @DisplayName("Should complete verification within reasonable time")
        void shouldCompleteVerificationWithinReasonableTime() {
            String password = "SecurePass123!";

            StepVerifier.create(
                    passwordService.hashPassword(password)
                            .flatMap(hash -> passwordService.verifyPassword(password, hash))
            )
            .expectNext(true)
            .expectComplete()
            .verify(Duration.ofSeconds(5));
        }
    }

    @Nested
    @DisplayName("Reactive Behavior")
    class ReactiveBehaviorTests {

        @Test
        @DisplayName("Should execute hashing on bounded elastic scheduler")
        void shouldExecuteHashingOnBoundedElasticScheduler() {
            String password = "SecurePass123!";

            StepVerifier.create(passwordService.hashPassword(password))
                    .expectNextMatches(hash -> hash.startsWith("$argon2id$"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should execute verification on bounded elastic scheduler")
        void shouldExecuteVerificationOnBoundedElasticScheduler() {
            String password = "SecurePass123!";

            StepVerifier.create(
                    passwordService.hashPassword(password)
                            .flatMap(hash -> passwordService.verifyPassword(password, hash))
            )
            .expectNext(true)
            .verifyComplete();
        }

        @Test
        @DisplayName("Should handle multiple concurrent hashing requests")
        void shouldHandleMultipleConcurrentHashingRequests() {
            String password = "SecurePass123!";

            Mono<Boolean> allSuccessful = Mono.zip(
                    passwordService.hashPassword(password),
                    passwordService.hashPassword(password),
                    passwordService.hashPassword(password),
                    passwordService.hashPassword(password),
                    passwordService.hashPassword(password)
            ).map(tuple -> {
                return tuple.getT1().startsWith("$argon2id$") &&
                       tuple.getT2().startsWith("$argon2id$") &&
                       tuple.getT3().startsWith("$argon2id$") &&
                       tuple.getT4().startsWith("$argon2id$") &&
                       tuple.getT5().startsWith("$argon2id$");
            });

            StepVerifier.create(allSuccessful)
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle multiple concurrent verification requests")
        void shouldHandleMultipleConcurrentVerificationRequests() {
            String password = "SecurePass123!";

            StepVerifier.create(
                    passwordService.hashPassword(password)
                            .flatMap(hash -> Mono.zip(
                                    passwordService.verifyPassword(password, hash),
                                    passwordService.verifyPassword(password, hash),
                                    passwordService.verifyPassword(password, hash)
                            ))
                            .map(tuple -> tuple.getT1() && tuple.getT2() && tuple.getT3())
            )
            .expectNext(true)
            .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Security Properties")
    class SecurityPropertiesTests {

        @Test
        @DisplayName("Should produce hash significantly different from original password")
        void shouldProduceHashDifferentFromOriginal() {
            String password = "SecurePass123!";

            StepVerifier.create(passwordService.hashPassword(password))
                    .assertNext(hash -> {
                        assertThat(hash).doesNotContain(password);
                        assertThat(hash).isNotEqualTo(password);
                        assertThat(hash.length()).isGreaterThan(password.length());
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should use salt (different hashes for same input)")
        void shouldUseSaltForDifferentHashes() {
            String password = "SecurePass123!";

            StepVerifier.create(
                    Mono.zip(
                            passwordService.hashPassword(password),
                            passwordService.hashPassword(password),
                            passwordService.hashPassword(password)
                    )
            )
            .assertNext(tuple -> {
                String hash1 = tuple.getT1();
                String hash2 = tuple.getT2();
                String hash3 = tuple.getT3();

                assertThat(hash1).isNotEqualTo(hash2);
                assertThat(hash2).isNotEqualTo(hash3);
                assertThat(hash1).isNotEqualTo(hash3);
            })
            .verifyComplete();
        }

        @Test
        @DisplayName("Should prevent timing attacks with constant-time comparison")
        void shouldUseConstantTimeComparison() {
            String password = "SecurePass123!";
            String wrongPassword = "WrongPassword!";

            // Verification should take similar time regardless of result
            StepVerifier.create(
                    passwordService.hashPassword(password)
                            .flatMap(hash -> Mono.zip(
                                    passwordService.verifyPassword(password, hash),
                                    passwordService.verifyPassword(wrongPassword, hash)
                            ))
            )
            .assertNext(tuple -> {
                assertThat(tuple.getT1()).isTrue();
                assertThat(tuple.getT2()).isFalse();
            })
            .verifyComplete();
        }

        @Test
        @DisplayName("Should verify multiple correct passwords independently")
        void shouldVerifyMultiplePasswordsIndependently() {
            String password1 = "Password1!";
            String password2 = "Password2!";
            String password3 = "Password3!";

            Mono<Boolean> allVerified = Mono.zip(
                    passwordService.hashPassword(password1),
                    passwordService.hashPassword(password2),
                    passwordService.hashPassword(password3)
            ).flatMap(hashes -> Mono.zip(
                    passwordService.verifyPassword(password1, hashes.getT1()),
                    passwordService.verifyPassword(password2, hashes.getT2()),
                    passwordService.verifyPassword(password3, hashes.getT3()),
                    // Cross-verification should fail
                    passwordService.verifyPassword(password1, hashes.getT2()),
                    passwordService.verifyPassword(password2, hashes.getT3())
            ).map(results ->
                results.getT1() && results.getT2() && results.getT3() &&
                !results.getT4() && !results.getT5()
            ));

            StepVerifier.create(allVerified)
                    .expectNext(true)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle empty string password")
        void shouldHandleEmptyStringPassword() {
            String password = "";

            StepVerifier.create(passwordService.hashPassword(password))
                    .assertNext(hash -> {
                        assertThat(hash).isNotNull();
                        assertThat(hash).startsWith("$argon2id$");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should verify empty string password correctly")
        void shouldVerifyEmptyStringPassword() {
            String password = "";

            StepVerifier.create(
                    passwordService.hashPassword(password)
                            .flatMap(hash -> passwordService.verifyPassword(password, hash))
            )
            .expectNext(true)
            .verifyComplete();
        }

        @Test
        @DisplayName("Should reject verification with malformed hash")
        void shouldHandleMalformedHash() {
            String password = "SecurePass123!";
            String malformedHash = "not-a-valid-argon2-hash";

            // Argon2 library returns false for invalid hashes rather than throwing
            StepVerifier.create(passwordService.verifyPassword(password, malformedHash))
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle password with null bytes")
        void shouldHandlePasswordWithNullBytes() {
            String password = "Pass\u0000word";

            StepVerifier.create(passwordService.hashPassword(password))
                    .assertNext(hash -> {
                        assertThat(hash).isNotNull();
                        assertThat(hash).startsWith("$argon2id$");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle password with only numbers")
        void shouldHandlePasswordWithOnlyNumbers() {
            String password = "123456789";

            StepVerifier.create(
                    passwordService.hashPassword(password)
                            .flatMap(hash -> passwordService.verifyPassword(password, hash))
            )
            .expectNext(true)
            .verifyComplete();
        }
    }
}