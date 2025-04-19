package com.banka1.banking.bootstrap;

import com.banka1.banking.dto.CreateEventDTO;
import com.banka1.banking.dto.interbank.InterbankMessageDTO;
import com.banka1.banking.dto.interbank.InterbankMessageType;
import com.banka1.banking.dto.interbank.newtx.ForeignBankIdDTO;
import com.banka1.banking.dto.interbank.newtx.InterbankTransactionDTO;
import com.banka1.banking.dto.interbank.newtx.PostingDTO;
import com.banka1.banking.dto.interbank.newtx.TxAccountDTO;
import com.banka1.banking.dto.interbank.newtx.assets.CurrencyAsset;
import com.banka1.banking.dto.interbank.newtx.assets.MonetaryAssetDTO;
import com.banka1.banking.dto.interbank.rollbacktx.RollbackTransactionDTO;
import com.banka1.banking.models.Event;
import com.banka1.banking.models.helper.IdempotenceKey;
import com.banka1.banking.services.CurrencyService;
import com.banka1.banking.services.EventExecutorService;
import com.banka1.banking.services.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class BootstrapExchangeRateLoader implements CommandLineRunner {

    private final CurrencyService currencyService;
    private final EventService eventService;
    private final EventExecutorService eventExecutorService;

    public BootstrapExchangeRateLoader(CurrencyService currencyService,
                                       EventService eventService,
                                       EventExecutorService eventExecutorService) {
        this.currencyService = currencyService;
        this.eventService = eventService;
        this.eventExecutorService = eventExecutorService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== Fetching exchange rates on startup ===");
        currencyService.fetchExchangeRates();
        System.out.println("=== Exchange rates fetched successfully ===");


//         test events

        System.out.println("=== Testing event creation ===");
        InterbankMessageDTO<InterbankTransactionDTO> message = new InterbankMessageDTO<>();

        message.setMessageType(InterbankMessageType.NEW_TX);
        IdempotenceKey idempotenceKey = new IdempotenceKey();
        idempotenceKey.setRoutingNumber(444);
        idempotenceKey.setLocallyGeneratedKey(UUID.randomUUID().toString());
        message.setIdempotenceKey(idempotenceKey);

        InterbankTransactionDTO transactionDTO = new InterbankTransactionDTO();
        transactionDTO.setTransactionId(new IdempotenceKey(
            444, UUID.randomUUID().toString()
        ));

        TxAccountDTO account = new TxAccountDTO();
        account.setType("ACCOUNT");
        account.setNum("RS123456789");

        ForeignBankIdDTO foreignId = new ForeignBankIdDTO();
        foreignId.setRoutingNumber(444);
        foreignId.setUserId("user123");
        account.setId(foreignId);

        PostingDTO posting = new PostingDTO();
        posting.setAccount(account);
        posting.setAmount(1000); // pozitivan = ulaz na taj račun
        posting.setAsset(new MonetaryAssetDTO() {{
            setType("MONAS");
            setAsset(new CurrencyAsset("EUR"));
        }});

// Posting 2 (izlaz sa drugog računa)

        TxAccountDTO account2 = new TxAccountDTO();
        account2.setType("ACCOUNT");
        account2.setNum("RS987654321");
        ForeignBankIdDTO foreignId2 = new ForeignBankIdDTO();
        foreignId2.setRoutingNumber(444);
        foreignId2.setUserId("user456");
        account2.setId(foreignId2);
        PostingDTO posting2 = new PostingDTO();
        posting2.setAccount(account2);
        posting2.setAmount(-1000); // negativan = izlaz
        posting2.setAsset(new MonetaryAssetDTO() {{
            setType("MONAS");
            setAsset(new CurrencyAsset("EUR"));
        }});

// Dodavanje postinga
        transactionDTO.setPostings(List.of(posting, posting2));

        message.setMessage(transactionDTO);

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(message.getMessage());
        System.out.println(json);

        Event event = eventService.createEvent(new CreateEventDTO(
                InterbankMessageType.NEW_TX,
                json,
                "http://localhost:8082/interbank"
        ));

        System.out.println("=== Executing event ===");

        eventExecutorService.attemptEventAsync(event);

        System.out.println("=== Event executed successfully ===");
    }
}
