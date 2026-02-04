package com.fantasysporthub.domain.service;

import com.fantasysporthub.domain.model.user.RefreshToken;
import com.fantasysporthub.infrastructure.config.JWTConfig;
import com.fantasysporthub.infrastructure.persistence.repository.RefreshTokenRepository;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final JWTConfig jwtConfig;
    private final RSAKey rsaKey;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Generate and store refresh token.
     */
    public Mono<String> generateRefreshToken(UUID userId, String deviceFingerprint) {
        return Mono.fromCallable(() -> {
                    var now = Instant.now();
                    var expiresAt = now.plus(jwtConfig.getRefreshTokenDuration());
                    var tokenId = UUID.randomUUID();

                    var claimsSet = new JWTClaimsSet.Builder()
                            .issuer(jwtConfig.getIssuer())
                            .subject(userId.toString())
                            .issueTime(Date.from(now))
                            .expirationTime(Date.from(expiresAt))
                            .jwtID(tokenId.toString())
                            .build();

                    var signedJWT = new SignedJWT(
                            new JWSHeader.Builder(JWSAlgorithm.RS256)
                                    .keyID(rsaKey.getKeyID())
                                    .build(),
                            claimsSet
                    );

                    var signer = new RSASSASigner(rsaKey.toRSAPrivateKey());
                    signedJWT.sign(signer);

                    return signedJWT.serialize();
                })
                .flatMap(token -> {
                    // Store refresh token in database
                    var refreshToken = RefreshToken.builder()
                            .id(UUID.randomUUID())
                            .userId(userId)
                            .tokenHash(hashToken(token))
                            .deviceFingerprint(deviceFingerprint)
                            .expiresAt(Instant.now().plus(jwtConfig.getRefreshTokenDuration()))
                            .revoked(false)
                            .createdAt(Instant.now())
                            .build();

                    return refreshTokenRepository.save(refreshToken).thenReturn(token);
                })
                .doOnSuccess(token -> log.info("Generated refresh token for user: {}", userId))
                .doOnError(e -> log.error("Failed to generate refresh token", e));
    }

    /**
     * Validate refresh token and rotate.
     */
    public Mono<String> rotateRefreshToken(String oldToken) {
        // TODO: Implement token rotation
        return Mono.error(new UnsupportedOperationException("Phase 1 - Token rotation"));
    }

    /**
     * Revoke refresh token (logout).
     */
    public Mono<Void> revokeToken(String token) {
        return Mono.fromCallable(() -> hashToken(token))
                .flatMap(refreshTokenRepository::findByTokenHash)
                .flatMap(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshToken.setRevokedAt(Instant.now());
                    return refreshTokenRepository.save(refreshToken);
                })
                .then()
                .doOnSuccess(v -> log.info("Revoked refresh token"))
                .doOnError(e -> log.error("Failed to revoke token", e));
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

}
