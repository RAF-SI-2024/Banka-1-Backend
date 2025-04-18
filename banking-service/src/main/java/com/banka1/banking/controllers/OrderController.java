package com.banka1.banking.controllers;

import com.banka1.banking.dto.OrderTransactionInitiationDTO;
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
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    private final AuthService authService;
    private final OrderService orderService;

    @PostMapping("/execute/{token}")
    public ResponseEntity<?> executeOrder(@PathVariable String token) {
        Claims claims = authService.parseToken(token);

        try {
            String direction = claims.get("direction", String.class);
            Long accountId = Long.valueOf(claims.get("accountId", Integer.class));
            Long userId = Long.valueOf(claims.get("userId", Integer.class));
            Double amount = Double.valueOf(claims.get("amount", String.class));
            Double fee = Double.parseDouble((String) claims.get("fee"));

            if(direction == null)
                throw new Exception();

            Double finalAmount = orderService.executeOrder(direction, userId, accountId, amount, fee);

            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("finalAmount", finalAmount), null);
        } catch (IllegalArgumentException e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.FORBIDDEN), false, null, "Nedovoljna sredstva");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, "Nevalidni podaci");
        }
    }

    @PostMapping("/initiate/{token}")
    public ResponseEntity<?> initiateOrderTransaction(@PathVariable String token) {
        Claims claims = authService.parseToken(token);
        try {
            String uid = claims.get("uid", String.class);
            Long buyerAccountId = Long.valueOf(claims.get("buyerAccountId", Integer.class));
            Long sellerAccountId = Long.valueOf(claims.get("sellerAccountId", Integer.class));
            Double amount = Double.valueOf(claims.get("amount", String.class));

            OrderTransactionInitiationDTO dto = new OrderTransactionInitiationDTO();
            dto.setUid(uid);
            dto.setBuyerAccountId(buyerAccountId);
            dto.setSellerAccountId(sellerAccountId);
            dto.setAmount(amount);

            orderService.processOrderTransaction(dto);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Nevalidni podaci u tokenu za initiate transakciju");
        }
    }

}
