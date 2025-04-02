package com.banka1.banking.service;

import com.banka1.banking.dto.AuthorizedPersonDTO;
import com.banka1.banking.dto.CreateCardDTO;
import com.banka1.banking.dto.UpdateCardDTO;
import com.banka1.banking.mapper.CardMapper;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.AuthorizedPerson;
import com.banka1.banking.models.Card;
import com.banka1.banking.models.helper.*;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.AuthorizedPersonRepository;
import com.banka1.banking.repository.CardRepository;
import com.banka1.banking.services.CardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private AuthorizedPersonRepository authorizedPersonRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private CardService cardService;

    @Test
    void testCreateCardSuccessfully() {
        CreateCardDTO createCardDTO = new CreateCardDTO();
        createCardDTO.setCardBrand(CardBrand.VISA);
        createCardDTO.setCardType(CardType.CREDIT);
        createCardDTO.setAccountID(1L);

        AuthorizedPersonDTO personDTO = new AuthorizedPersonDTO();
        personDTO.setFirstName("Milica");
        personDTO.setLastName("Grujic");
        personDTO.setPhoneNumber("123456789");
        createCardDTO.setAuthorizedPerson(personDTO);

        Card card = new Card();
        card.setCardBrand(CardBrand.VISA);
        when(cardMapper.dtoToCard(createCardDTO)).thenReturn(card);

        Account account = new Account();
        account.setType(AccountType.CURRENT);
        account.setSubtype(AccountSubtype.PERSONAL);

        when(accountRepository.findById(createCardDTO.getAccountID())).thenReturn(Optional.of(account));
        when(cardRepository.findByAccountId(createCardDTO.getAccountID())).thenReturn(Optional.of(new ArrayList<>()));

        when(authorizedPersonRepository.findByFirstNameAndLastNameAndPhoneNumber(
                personDTO.getFirstName(), personDTO.getLastName(), personDTO.getPhoneNumber()))
                .thenReturn(Optional.empty());

        AuthorizedPerson newAuthorizedPerson = new AuthorizedPerson();
        when(cardMapper.dtoToAuthorizedPerson(personDTO)).thenReturn(newAuthorizedPerson);

        when(cardRepository.save(card)).thenReturn(card);

        Card result = cardService.createCard(createCardDTO);

        assertNotNull(result);
        verify(authorizedPersonRepository, times(1)).save(newAuthorizedPerson);
        verify(cardRepository, times(1)).save(card);
    }

    @Test
    void testCreateCardAccountNotFound() {
        CreateCardDTO createCardDTO = new CreateCardDTO();
        createCardDTO.setCardBrand(CardBrand.VISA);
        createCardDTO.setCardType(CardType.CREDIT);
        createCardDTO.setAccountID(1L);

        Card card = new Card();
        card.setCardBrand(CardBrand.VISA);
        when(cardMapper.dtoToCard(createCardDTO)).thenReturn(card);

        when(accountRepository.findById(createCardDTO.getAccountID())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cardService.createCard(createCardDTO);
        });
        assertEquals("Racun nije pronadjen", exception.getMessage());

        verify(cardRepository, never()).save(any());
    }

    @Test
    void testCreateCardPrivateAccountExceedsLimit() {
        CreateCardDTO createCardDTO = new CreateCardDTO();
        createCardDTO.setCardBrand(CardBrand.VISA);
        createCardDTO.setCardType(CardType.CREDIT);
        createCardDTO.setAccountID(1L);

        Card card = new Card();
        card.setCardBrand(CardBrand.VISA);
        when(cardMapper.dtoToCard(createCardDTO)).thenReturn(card);

        Account account = new Account();
        account.setType(AccountType.CURRENT);
        account.setSubtype(AccountSubtype.PERSONAL);
        when(accountRepository.findById(createCardDTO.getAccountID())).thenReturn(Optional.of(account));

        List<Card> cards = new ArrayList<>();
        cards.add(new Card());
        cards.add(new Card());

        when(cardRepository.findByAccountId(createCardDTO.getAccountID())).thenReturn(Optional.of(cards));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cardService.createCard(createCardDTO);
        });
        assertEquals("Privatni racun moze biti povezan sa najvise dve kartice!", exception.getMessage());

        verify(cardRepository, never()).save(any());
    }

    @Test
    void testCreateCardBusinessAccountExceedsLimit() {
        CreateCardDTO createCardDTO = new CreateCardDTO();
        createCardDTO.setCardBrand(CardBrand.VISA);
        createCardDTO.setCardType(CardType.CREDIT);
        createCardDTO.setAccountID(1L);

        Card card = new Card();
        card.setCardBrand(CardBrand.VISA);
        when(cardMapper.dtoToCard(createCardDTO)).thenReturn(card);

        Account account = new Account();
        account.setType(AccountType.CURRENT);
        account.setSubtype(AccountSubtype.BUSINESS);
        when(accountRepository.findById(createCardDTO.getAccountID())).thenReturn(Optional.of(account));

        List<Card> cards = new ArrayList<>();
        cards.add(new Card());
        cards.add(new Card());
        cards.add(new Card());
        cards.add(new Card());
        cards.add(new Card());

        when(cardRepository.findByAccountId(createCardDTO.getAccountID())).thenReturn(Optional.of(cards));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cardService.createCard(createCardDTO);
        });
        assertEquals("Poslovni racun moze biti povezan sa najvise pet kartica!", exception.getMessage());

        verify(cardRepository, never()).save(any());
    }

    @Test
    void testCreateCardDinaCardWithForeignCurrencyAccount() {
        CreateCardDTO createCardDTO = new CreateCardDTO();
        createCardDTO.setCardBrand(CardBrand.DINA_CARD);
        createCardDTO.setCardType(CardType.CREDIT);
        createCardDTO.setAccountID(1L);

        Card card = new Card();
        card.setCardBrand(CardBrand.DINA_CARD);
        when(cardMapper.dtoToCard(createCardDTO)).thenReturn(card);

        Account account = new Account();
        account.setType(AccountType.FOREIGN_CURRENCY);
        account.setSubtype(AccountSubtype.PERSONAL);

        when(accountRepository.findById(createCardDTO.getAccountID())).thenReturn(Optional.of(account));
        when(cardRepository.findByAccountId(createCardDTO.getAccountID())).thenReturn(Optional.of(new ArrayList<>()));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cardService.createCard(createCardDTO);
        });
        assertEquals("Dina kartica moze biti jedino povezana sa tekucim racunom", exception.getMessage());

        verify(cardRepository, never()).save(any());
    }

    @Test
    void testCreateCardAmericanExpressWithCurrentAccount() {
        CreateCardDTO createCardDTO = new CreateCardDTO();
        createCardDTO.setCardBrand(CardBrand.AMERICAN_EXPRESS);
        createCardDTO.setCardType(CardType.CREDIT);
        createCardDTO.setAccountID(1L);

        Card card = new Card();
        card.setCardBrand(CardBrand.AMERICAN_EXPRESS);
        when(cardMapper.dtoToCard(createCardDTO)).thenReturn(card);

        Account account = new Account();
        account.setType(AccountType.CURRENT);
        account.setSubtype(AccountSubtype.PERSONAL);

        when(accountRepository.findById(createCardDTO.getAccountID())).thenReturn(Optional.of(account));
        when(cardRepository.findByAccountId(createCardDTO.getAccountID())).thenReturn(Optional.of(new ArrayList<>()));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cardService.createCard(createCardDTO);
        });
        assertEquals("American Express moze biti jedino povezan sa deviznim racunom", exception.getMessage());

        verify(cardRepository, never()).save(any());
    }

    @Test
    public void testUpdateCardBlockCard() {
        int cardId = 1;
        UpdateCardDTO updateCardDTO = new UpdateCardDTO();
        updateCardDTO.setStatus(true);

        Card card = new Card();
        card.setBlocked(false);

        when(cardRepository.findById((long) cardId)).thenReturn(Optional.of(card));

        cardService.blockCard(cardId, updateCardDTO);

        assertTrue(card.getBlocked());
        verify(cardRepository, times(1)).save(card);
    }

    @Test
    public void testUpdateCardActivateCard() {
        int cardId = 1;
        UpdateCardDTO updateCardDTO = new UpdateCardDTO();
        updateCardDTO.setStatus(true);

        Card card = new Card();
        card.setActive(false);

        when(cardRepository.findById((long) cardId)).thenReturn(Optional.of(card));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cardService.activateCard(cardId, updateCardDTO);
        });
        assertEquals("Kartica je deaktivirana i ne moze biti aktivirana", exception.getMessage());

        verify(cardRepository, never()).save(any());
    }
}
