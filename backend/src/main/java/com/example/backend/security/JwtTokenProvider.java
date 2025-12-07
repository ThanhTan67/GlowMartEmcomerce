package com.example.backend.security;

import com.example.backend.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtTokenProvider {
    private final JwtConfig jwtConfig;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        if (jwtConfig == null || jwtConfig.getSecret() == null || jwtConfig.getSecret().isBlank()) {
            throw new IllegalStateException("JWT secret must be configured (set jwt.secret in application.properties/yml)");
        }
        try {
            this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtConfig.getSecret()));
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid JWT secret format - must be valid base64 with sufficient length (256+ bits)", ex);
        }
    }

    public String generateAccessToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        return generateAccessToken(userPrincipal.getUsername(), userPrincipal.getId(), userPrincipal.getTokenVersion());
    }

    public String generateAccessToken(String username, UUID userId, int tokenVersion) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getAccessTokenExpirationMs());

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId != null ? userId.toString() : null)
                .claim("tokenVersion", tokenVersion)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateRefreshToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        return generateRefreshToken(userPrincipal.getUsername(), userPrincipal.getId(), userPrincipal.getTokenVersion());
    }

    public String generateRefreshToken(String username, UUID userId, int tokenVersion) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getRefreshTokenExpirationMs());

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId.toString())
                .claim("tokenVersion", tokenVersion)
                .setId(UUID.randomUUID().toString()) // jti - để sau này có thể blacklist nếu muốn
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims == null ? null : claims.getSubject();
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        if (claims == null) return null;
        String userId = claims.get("userId", String.class);
        if (userId == null || userId.isBlank()) return null;
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public int getTokenVersionFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        if (claims == null) return 0;
        Integer version = claims.get("tokenVersion", Integer.class);
        return version != null ? version : 0;
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) return false;
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }
}