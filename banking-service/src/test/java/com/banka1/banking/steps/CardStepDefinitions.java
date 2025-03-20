package com.banka1.banking.steps;

import com.banka1.banking.BankingServiceApplication;
import com.banka1.banking.dto.CreateCardDTO;
import com.banka1.banking.dto.UpdateCardDTO;
import com.banka1.banking.mapper.CardMapper;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.Card;
import com.banka1.banking.models.helper.AccountSubtype;
import com.banka1.banking.models.helper.AccountType;
import com.banka1.banking.models.helper.CardBrand;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.CardRepository;
import com.banka1.banking.services.CardService;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import org.junit.jupiter.api.Assertions;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;


import java.util.List;
import java.util.Optional;

@SpringBootTest
@ContextConfiguration(classes = BankingServiceApplication.class)
public class CardStepDefinitions {

    @InjectMocks
    private CardService cardService; // Osigurava da koristi @Mock instance

    @Mock
    private CardRepository cardRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CardMapper cardMapper;

    private Card card;
    private Exception exception;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this); // Inicijalizuje Mockito mockove
    }



    @Given("account with id {long} exists with subtype PERSONAL and has less than 2 cards")
    public void accountWithPersonalSubtype(Long accountId) {
        Account account = new Account();
        account.setId(accountId);
        account.setSubtype(AccountSubtype.PERSONAL);

        Mockito.when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        Mockito.when(cardRepository.findByAccountId(accountId)).thenReturn(Optional.of(List.of())); // Pravi prazan spisak kartica
    }

    @Given("account with id {long} exists with type FOREIGN_CURRENCY")
    public void accountForeignCurrency(Long accountId) {
        Account account = new Account();
        account.setId(accountId);
        account.setType(AccountType.FOREIGN_CURRENCY);

        Mockito.when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
    }

    @When("creating a {word} card for account {long}")
    public void createCard(String brand, Long accountId) {
        CreateCardDTO dto = new CreateCardDTO();
        dto.setAccountID(accountId);
        dto.setCardBrand(CardBrand.valueOf(brand));

        try {
            card = cardService.createCard(dto);
        } catch (Exception e) {
            exception = e;
        }
    }

    @Then("card should be created successfully")
    public void cardCreatedSuccessfully() {
        Mockito.when(accountRepository.save(Mockito.any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0)); // vrati isti objekat koji je pozvan

        Assertions.assertNotNull(card);
        Mockito.verify(cardRepository).save(Mockito.any(Card.class));
    }

    @Then("card creation should fail with message {string}")
    public void cardCreationFail(String message) {
        Assertions.assertNotNull(exception);
        Assertions.assertTrue(exception.getMessage().contains(message));
    }

    @Given("card with id {long} exists")
    public void cardExists(Long cardId) {
        Card mockCard = new Card();
        mockCard.setId(cardId);

        Mockito.when(cardRepository.findById(cardId)).thenReturn(Optional.of(mockCard));
    }

    @When("blocking card {long}")
    public void blockCard(Long cardId) {
        UpdateCardDTO updateCardDTO = new UpdateCardDTO();
        updateCardDTO.setStatus(true);

        cardService.blockCard(cardId.intValue(), updateCardDTO);
    }

    @Then("card should be blocked successfully")
    public void cardBlockedSuccessfully() {
        Mockito.verify(cardRepository).save(Mockito.any(Card.class));
    }
}
