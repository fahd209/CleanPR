package com.fahd.cleanPR.security;

import com.fahd.cleanPR.model.Account;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * This class is for
 * Generating tokens and
 * validating client tokens
 * */
@Service
public class JwtService {

    @Value("${jwt.secret.key}")
    private String JWT_SECRET;

    public String generateToken(Account account) {
        return Jwts
                .builder()
                .setSubject(String.valueOf(account.getEmail()))
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 24 hours
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Date extractExpirationDate(String jwt) {
        return extractClaim(jwt, Claims::getExpiration);
    }

    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token, String currentSubject) {
        try {
            String subject = extractSubject(token);
            Date expirationDate = extractExpirationDate(token);
            return subject.equals(currentSubject) && new Date(System.currentTimeMillis()).before(expirationDate);
        } catch (Exception e) {
            return false;
        }
    }

    public SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(JWT_SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
