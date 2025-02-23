package com.banka1.user.security;

import com.banka1.user.service.BlackListTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private final BlackListTokenService blackListTokenService;

    public JwtUtil(BlackListTokenService blackListTokenService) {
        this.blackListTokenService = blackListTokenService;
    }

    // Generise kriptografski kljuc za potpisivanje i validaciju JWT tokena.
    // Osigurava da token moze biti validiran samo pomocu poznatog tajnog kljuca.
    private Key getSigningKey(){
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // Kreira jwt sa korisnickim ID-em,rolom i permisijama.
    public String generateToken(Long userId, String role, List<String> permissions) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("role", role)
                .claim("permissions", permissions)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }


    // Proverava da li je token validan i nije istekao.
    public boolean validateToken(String token) {
        if (blackListTokenService.isTokenBlacklisted(token)) {
            return false;
        }
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }


    // Izvlaci podatke iz tokena.
    // Omogucava citanje korisnickih podataka iz tokena bez dodatnih upita prema bazi.
    public Claims getClaimsFromToken(String token){
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public void setJwtSecret(String secretKey) {
    }

    public void setJwtExpiration(long expirationTime) {
    }
}