package com.banka1.notification.aspect;

import com.banka1.common.model.Permission;
import com.banka1.common.model.Position;
import com.banka1.notification.service.implementation.AuthService;
import com.banka1.notification.utils.ResponseMessage;
import com.banka1.notification.utils.ResponseTemplate;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Aspect
@Configuration
@EnableAspectJAutoProxy
public class AuthAspect {

    private final AuthService authService;

    public AuthAspect(AuthService authService) {
        this.authService = authService;
    }

    // Izvlači token iz prosledjenog autorizacionog parametra
    private String parseAuthToken(String[] parameterNames, Object[] args) {
        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals("authorization") && args[i] != null) {
                return authService.getToken(args[i].toString());
            }
        }
        return null;
    }

    private String getAuthTokenFromRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader("Authorization");
            return authService.getToken(authHeader);
        }
        return null;
    }

    @Around("@annotation(com.banka1.notification.aspect.Authorization)")
    public Object authorize(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();

        String token = parseAuthToken(methodSignature.getParameterNames(), joinPoint.getArgs());
        if(token == null)
            token = getAuthTokenFromRequest();

        if(token == null) {
            // Token ne postoji, tj. korisnik nije ulogovan
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.UNAUTHORIZED), false, null, ResponseMessage.INVALID_LOGIN.toString());
        }

        Claims claims = authService.parseToken(token);
        if(claims == null) {
            // Token postoji ali nije autentičan, verovatno je pokušaj neodobrenog pristupa
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.UNAUTHORIZED), false, null, ResponseMessage.INVALID_LOGIN.toString());
        }

        Authorization authorization = method.getAnnotation(Authorization.class);

        Set<String> necessaryPermissions = Arrays.stream(authorization.permissions()).map(Permission::toString).collect(Collectors.toSet());
        Set<String> grantedPermissions = new HashSet<>((List<String>) claims.get("permissions"));
        System.out.println(grantedPermissions);

        Set<String> possiblePositions = Arrays.stream(authorization.positions()).map(Position::toString).collect(Collectors.toSet());

        // Moguće je da korisnik ima i više permisija nego što je potrebno
        if(grantedPermissions.containsAll(necessaryPermissions) && (possiblePositions.isEmpty() || possiblePositions.contains(claims.get("position", String.class)))) {
            return joinPoint.proceed();
        }

        if(authorization.allowIdFallback()) {
            OptionalInt maybeIdIndex = IntStream.range(0, methodSignature.getParameterNames().length).filter(i -> methodSignature.getParameterNames()[i].compareTo("id") == 0).findFirst();
            if(maybeIdIndex.isPresent()) {
                Long givenId = Long.parseLong(joinPoint.getArgs()[maybeIdIndex.getAsInt()].toString());
                if (Objects.equals(claims.get("id", Long.class), givenId)) {
                    return joinPoint.proceed();
                }
            }
        }

        // ako je admin
        if(Objects.equals(claims.get("isAdmin", Boolean.class), true)) {
            return joinPoint.proceed();
        }

        return ResponseTemplate.create(ResponseEntity.status(HttpStatus.FORBIDDEN), false, null, ResponseMessage.FORBIDDEN.toString());
    }
}