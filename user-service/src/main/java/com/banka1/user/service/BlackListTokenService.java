package com.banka1.user.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BlackListTokenService {

    private final Map<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();

    // Dodaje token u blacklist sa trenutnim vremenom.
    public void blacklistToken(String token){
        blacklistedTokens.put(token,Instant.now());
    }

    // Proverava da li je token na blacklisti.
    public boolean isTokenBlacklisted(String token){
        return blacklistedTokens.containsKey(token);
    }

    // Brise sve tokene sa blacklist liste koji su stariji od 30 minuta
    // Pokretanje ove metode na svakih 5 minuta
    @Scheduled(fixedRate = 300000)
    public void cleanExpiredTokens(){
        Instant deletion = Instant.now().minusSeconds(1800); // Tokeni stariji od 30 minuta
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(deletion));
    }

}
