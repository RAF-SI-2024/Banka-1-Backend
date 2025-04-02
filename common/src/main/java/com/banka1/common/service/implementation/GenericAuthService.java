package com.banka1.common.service.implementation;

import com.banka1.common.model.Department;
import com.banka1.common.model.Permission;
import com.banka1.common.model.Position;
import com.banka1.common.service.IAuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Standardna implementacija {@link IAuthService} interfejsa.
 */
@Service
@Slf4j
public abstract class GenericAuthService implements IAuthService {
    @Value("${oauth.jwt.secret}")
    protected String secret;
    @Value("${oauth.jwt.expiration}")
    protected long jwtExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    }

    // Generiše token bez zahtevanja enum tipova
    private String generateTokenRaw(Long userId, String position, List<String> permissions, Boolean isEmployed, Boolean isAdmin, Department department) {
        return Jwts.builder()
                .claim("id", userId)
                .claim("position", position)
                .claim("permissions", permissions)
                .claim("isEmployed", isEmployed)
                .claim("isAdmin", isAdmin)
                .claim("department", department)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public String getToken(String authHeader) {
        log.info("Grabbing token from header " + authHeader);
        if(authHeader != null && authHeader.startsWith("Bearer"))
            return authHeader.split(" ")[1];
        log.warn("Didn't grab token from header (make sure it starts with \"Bearer \" (" + authHeader + ")");
        return null;
    }

    @Override
    public Claims parseToken(String token) {
        try {
            log.info("Parsing token " + token);
            return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            log.warn("Did not parse token " + token);
            return null;
        }
    }

    @Override
    public String recreateToken(String token) {
        try {
            Claims claims = parseToken(token);

            return generateTokenRaw(claims.get("id", Long.class),
                                    claims.get("position", String.class),
                                    (List<String>) claims.get("permissions"),
                                    claims.get("isEmployed", Boolean.class),
                                    claims.get("isAdmin", Boolean.class),
                                    claims.get("department", Department.class));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String generateToken(Long userId, Position position, List<Permission> permissions, Boolean isEmployed, Boolean isAdmin, Department department) {
        return generateTokenRaw(userId, position.toString(), permissions.stream().map(Permission::toString).collect(Collectors.toList()), isEmployed, isAdmin, department);
    }
}
