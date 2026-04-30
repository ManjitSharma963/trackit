package com.trackit.auth.service;

import com.trackit.auth.dto.*;
import com.trackit.auth.model.*;
import com.trackit.auth.repository.*;
import com.trackit.auth.security.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final String REQUIRED_ALG = "HS256";
    private static final String ACCOUNT_ID_CLAIM = "accountId";
    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = signingKey(properties.getSecret());
    }

    public String createAccessToken(Long userId) {
        return createAccessToken(userId, null);
    }

    public String createAccessToken(Long userId, Long accountId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + properties.getExpirationMs());
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(exp)
                .signWith(key, Jwts.SIG.HS256);
        if (accountId != null) {
            builder.claim(ACCOUNT_ID_CLAIM, accountId);
        }
        return builder.compact();
    }

    public Long parseUserId(String token) {
        return parseAuthContext(token).userId();
    }

    public AuthContext parseAuthContext(String token) {
        try {
            var jws = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            String alg = jws.getHeader().getAlgorithm();
            if (!REQUIRED_ALG.equals(alg)) {
                throw new UnauthorizedException("Unsupported JWT algorithm");
            }
            Claims claims = jws.getPayload();
            Long userId = Long.parseLong(claims.getSubject());
            Long accountId = parseLongClaim(claims.get(ACCOUNT_ID_CLAIM));
            return new AuthContext(userId, accountId);
        } catch (ExpiredJwtException ex) {
            throw new UnauthorizedException("Token expired");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid token");
        }
    }

    private static Long parseLongClaim(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public record AuthContext(Long userId, Long accountId) {}

    private static SecretKey signingKey(String rawSecret) {
        byte[] bytes = String.valueOf(rawSecret).getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256.");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
