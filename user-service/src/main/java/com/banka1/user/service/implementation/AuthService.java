package com.banka1.user.service.implementation;

import com.banka1.user.DTO.request.LoginRequest;
import com.banka1.user.model.Customer;
import com.banka1.user.model.Employee;
import com.banka1.user.model.helper.Permission;
import com.banka1.user.model.helper.Position;
import com.banka1.user.repository.CustomerRepository;
import com.banka1.user.repository.EmployeeRepository;
import com.banka1.user.service.IAuthService;
import com.banka1.user.utils.ResponseMessage;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
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
public class AuthService implements IAuthService {
    @Value("${oauth.jwt.secret}")
    private String secret;
    @Value("${oauth.jwt.expiration}")
    private long jwtExpiration;

    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;

    @Autowired
    public AuthService(CustomerRepository customerRepository, EmployeeRepository employeeRepository) {
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    }

    // Provera validnosti lozinke koriscenjem BCrypt-a kombinovanjem sa salt-om.
    private boolean verifyPassword(String rawPassword, String hashedPassword, String salt){
        if (rawPassword == null || hashedPassword == null || salt == null)
            return false;
        return BCrypt.checkpw(rawPassword + salt, hashedPassword);
    }

    // Generiše token bez zahtevanja enum tipova
    private String generateTokenRaw(Long userId, String position, List<String> permissions, Boolean isAdmin) {
        return Jwts.builder()
                .claim("id", userId)
                .claim("position", position)
                .claim("permissions", permissions)
                .claim("isAdmin", isAdmin)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public String getToken(String authHeader) {
        if(authHeader != null && authHeader.startsWith("Bearer"))
            return authHeader.split(" ")[1];
        return null;
    }

    @Override
    public Claims parseToken(String token) {
        try {
            return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
        } catch (Exception e) {
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
                                    claims.get("isAdmin", Boolean.class));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String generateToken(Long userId, Position position, List<Permission> permissions, Boolean isAdmin) {
        return generateTokenRaw(userId, position.toString(), permissions.stream().map(Permission::toString).collect(Collectors.toList()), isAdmin);
    }

    @Override
    public String login(LoginRequest loginRequest) throws IllegalArgumentException {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        Customer customer = customerRepository.findByEmail(email).orElse(null);
        if(customer != null && verifyPassword(password, customer.getPassword(), customer.getSaltPassword()))
            return generateToken(customer.getId(), Position.NONE, customer.getPermissions(), false);

        Employee employee = employeeRepository.findByEmail(email).orElse(null);
        if(employee != null && verifyPassword(password, employee.getPassword(), employee.getSaltPassword()) && employee.getActive())
            return generateToken(employee.getId(), employee.getPosition(), employee.getPermissions(), employee.getIsAdmin());

        throw new IllegalArgumentException(ResponseMessage.INVALID_USER.toString());
    }
}
