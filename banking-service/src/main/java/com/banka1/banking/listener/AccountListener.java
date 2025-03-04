package com.banka1.banking.listener;

import com.banka1.banking.dto.request.CreateAccountDTO;
import com.banka1.banking.services.AccountService;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountListener {
    private final AccountService accountService;
    private final MessageHelper messageHelper;

    @JmsListener(destination = "${destination.account}", concurrency = "5-10")
    public void onActivationMessage(Message message) throws JMSException {
        var dto = messageHelper.getMessage(message, CreateAccountDTO.class);
        try {
            if (dto != null)
                accountService.createAccount(dto);
        } catch (Exception e) {
            log.error("AccountListener: ", e);
        }
    }
}
