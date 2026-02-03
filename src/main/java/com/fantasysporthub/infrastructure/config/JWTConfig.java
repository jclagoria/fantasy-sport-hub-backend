package com.fantasysporthub.infrastructure.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;

/**
 * JWT configuration for RS256 asymmetric token signing.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JWTConfig {

    private String issuer;
    private Duration accessTokenDuration;
    private Duration refreshTokenDuration;
    private String privateKeyPath;
    private String publicKeyPath;

    private final ResourceLoader resourceLoader;

    public JWTConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Load RSA private key from PEM file.
     */
    @Bean
    public RSAPrivateKey rsaPrivateKey() throws Exception {
        Resource resource = resourceLoader.getResource(privateKeyPath);
        String key = new String(Files.readAllBytes(resource.getFile().toPath()))
                .replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) keyFactory.generatePrivate(spec);
    }

    /**
     * Load RSA public key from PEM file.
     */
    @Bean
    public RSAPublicKey rsaPublicKey() throws Exception {
        Resource resource = resourceLoader.getResource(publicKeyPath);
        String key = new String(Files.readAllBytes(resource.getFile().toPath()))
                .replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(spec);
    }

    /**
     * RSA JWK for Nimbus JOSE JWT library.
     */
    @Bean
    public RSAKey rsaKey(RSAPublicKey rsaPublicKey, RSAPrivateKey rsaPrivateKey) {
        return new RSAKey.Builder(rsaPublicKey)
                .privateKey(rsaPrivateKey)
                .algorithm(JWSAlgorithm.RS256)
                .build();
    }

}
