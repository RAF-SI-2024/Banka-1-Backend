package com.banka1.banking.steps;

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
import io.cucumber.java.en.*;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Optional;

public class CardStepDefinitions {

    @Autowired
    private CardService cardService;

    @MockBean
    private CardRepository cardRepository;
    @MockBean
    private AccountRepository accountRepository;
    @MockBean
    private CardMapper cardMapper;

    private Card card;
    private Exception exception;

    @Given("account with id {long} exists with subtype PERSONAL and has less than 2 cards")
    public void accountWithPersonalSubtype(Long accountId) {
        Account account = new Account();
        account.setSubtype(AccountSubtype.PERSONAL);
        Mockito.when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        Mockito.when(cardRepository.findByAccountId(accountId)).thenReturn(Optional.of(List.of()));
    }

    @Given("account with id {long} exists with type FOREIGN_CURRENCY")
    public void accountForeignCurrency(Long accountId) {
        Account account = new Account();
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
        Mockito.when(cardRepository.findById(cardId)).thenReturn(Optional.of(new Card()));
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
