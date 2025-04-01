package com.banka1.banking.controllers;

import com.banka1.banking.models.Account;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.services.AccountService;
import com.banka1.banking.services.BankAccountUtils;
import com.banka1.banking.services.OrderService;
import com.banka1.banking.services.implementation.AuthService;
import com.banka1.banking.utils.ResponseTemplate;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    private final AuthService authService;
    private final OrderService orderService;

    @PostMapping("/execute")
    @Transactional
    public ResponseEntity<?> executeOrder(@RequestBody String token) {
        Claims claims = authService.parseToken(token);

        try {
            String direction = claims.get("direction", String.class);
            Long accountId = claims.get("accountId", Long.class);
            Long userId = claims.get("userId", Long.class);
            Double amount = claims.get("amount", Double.class);

            if(direction == null || accountId == null || userId == null || amount == null)
                throw new Exception();

            Double finalAmount = orderService.executeOrder(direction, userId, accountId, amount);

            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("finalAmount", finalAmount), null);
        } catch (IllegalArgumentException e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.FORBIDDEN), false, null, "Nedovoljna sredstva");
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, "Nevalidni podaci");
        }
    }
}
