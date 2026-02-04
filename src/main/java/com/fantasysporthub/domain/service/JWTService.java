package com.fantasysporthub.domain.service;

import com.fantasysporthub.domain.model.user.UserEntity;
import com.fantasysporthub.domain.util.RoleConverter;
import com.fantasysporthub.infrastructure.config.JWTConfig;
import com.fantasysporthub.security.JWTClaims;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT token generation and validation service with RS256.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JWTService {

    private final JWTConfig jwtConfig;
    private final RSAKey rsaKey;

    /**
     * Generate short-lived access token.
     *
     * @param userEntity User entity from database
     * @param deviceFingerprint Client device identifier (Phase 1: fraud detection)
     * @return Signed JWT token string
     */
    public Mono<String> generateAccessToken(
            UserEntity userEntity,
            String deviceFingerprint
    ) {
        return Mono.fromCallable(() -> {
            var now = Instant.now();
            var expiresAt = now.plus(jwtConfig.getAccessTokenDuration());

            // Convert JSONB roles to List<String> for JWT claim
            var rolesList = RoleConverter.jsonNodeToList(userEntity.getRoles());

            var claimsSet = new JWTClaimsSet.Builder()
                    .issuer(jwtConfig.getIssuer())
                    .subject(userEntity.getId().toString())
                    .claim("email", userEntity.getEmail())
                    .claim("roles", rolesList)
                    .claim("deviceFingerprint", deviceFingerprint)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiresAt))
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            var signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(rsaKey.getKeyID())
                            .build(),
                    claimsSet
            );

            // Sign with RSA private key
            var signer = new RSASSASigner(rsaKey.toRSAPrivateKey());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        })
                .doOnSuccess(token ->
                        log.info("Generated access token for user: {}", userEntity.getEmail()))
                .doOnError(e ->
                        log.error("Failed to generate access token for user: {}", userEntity.getEmail(), e));
    }

    /**
     * Validate and decode JWT token.
     *
     * @param token JWT token string
     * @return JWTClaims if valid
     */
    public Mono<JWTClaims> validateToken(String token) {
        return Mono.fromCallable(() -> {
            // Parse JWT
            var signedJWT = SignedJWT.parse(token);

            // Verify signature with RSA public key
            var verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());
            if (!signedJWT.verify(verifier)) {
                throw new JWTVerificationException("Invalid JWT signature");
            }

            var claims = signedJWT.getJWTClaimsSet();

            if (!jwtConfig.getIssuer().equals(claims.getIssuer())) {
                throw new JWTVerificationException("Invalid JWT issuer");
            }

            if (claims.getExpirationTime().before(new Date())) {
                throw new JWTVerificationException("JWT token expired");
            }

            var rolesSet = RoleConverter.claimToSet(claims.getStringListClaim("roles"));

            return new JWTClaims(
                    UUID.fromString(claims.getSubject()),
                    claims.getStringClaim("email"),
                    rolesSet,
                    claims.getClaims(),
                    claims.getIssueTime().toInstant(),
                    claims.getExpirationTime().toInstant(),
                    claims.getJWTID(),
                    claims.getStringClaim("deviceFingerprint")
            );

        })
                .doOnSuccess(claims ->
                        log.info("Validated token for user: {}", claims.email()))
                .doOnError(e -> log.warn("Token validation failed: {}", e.getMessage()));
    }

    /**
     * Custom JWT verification exception.
     */
    public static class JWTVerificationException extends RuntimeException {
        public JWTVerificationException(String message) {
            super(message);
        }
    }

}
