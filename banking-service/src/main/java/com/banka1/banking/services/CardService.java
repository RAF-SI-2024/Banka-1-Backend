package com.banka1.banking.services;

import com.banka1.banking.dto.CreateCardDTO;
import com.banka1.banking.dto.UpdateCardDTO;
import com.banka1.banking.dto.UpdateCardLimitDTO;
import com.banka1.banking.dto.request.UpdateCardNameDTO;
import com.banka1.banking.mapper.CardMapper;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.AuthorizedPerson;
import com.banka1.banking.models.Card;
import com.banka1.banking.models.helper.AccountSubtype;
import com.banka1.banking.models.helper.AccountType;
import com.banka1.banking.models.helper.CardBrand;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.AuthorizedPersonRepository;
import com.banka1.banking.repository.CardRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CardService {
    private final CardRepository cardRepository;
    private final AuthorizedPersonRepository authorizedPersonRepository;
    private final AccountRepository accountRepository;

    private final CardMapper cardMapper;
    private final CompanyService companyService;

    public CardService(CardRepository cardRepository, AuthorizedPersonRepository authorizedPersonRepository, AccountRepository accountRepository, CardMapper cardMapper, CompanyService companyService) {
        this.cardRepository = cardRepository;
        this.authorizedPersonRepository = authorizedPersonRepository;
        this.accountRepository = accountRepository;
        this.cardMapper = cardMapper;
        this.companyService = companyService;
    }

    public Card findById(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Kartica sa ID-jem " + cardId + " nije pronađena"));
    }

    public List<Card> findAllByAccountId(int accountId) {
        return cardRepository.findByAccountId((long) accountId).isPresent() ? cardRepository.findByAccountId((long) accountId).get() : null;
    }

    public Card createCard(CreateCardDTO createCardDTO) {
        Card card = cardMapper.dtoToCard(createCardDTO);

        Account account = accountRepository.findById(createCardDTO.getAccountID())
                .orElseThrow(() -> new RuntimeException("Racun nije pronadjen"));
        card.setAccount(account);

        String cardName = account.getSubtype() + " kartica";
        card.setCardName(cardName);

        if(!account.getSubtype().equals(AccountSubtype.BUSINESS) && cardRepository.findByAccountId(createCardDTO.getAccountID()).isPresent() && cardRepository.findByAccountId(createCardDTO.getAccountID()).get().size() == 2){
            throw new RuntimeException("Privatni racun moze biti povezan sa najvise dve kartice!");
        }
        else if(account.getSubtype().equals(AccountSubtype.BUSINESS) && cardRepository.findByAccountId(createCardDTO.getAccountID()).isPresent() && cardRepository.findByAccountId(createCardDTO.getAccountID()).get().size() == 5){
            throw new RuntimeException("Poslovni racun moze biti povezan sa najvise pet kartica!");
        }

        if (createCardDTO.getAuthorizedPerson() != null) {
            Optional<AuthorizedPerson> authorizedPerson = authorizedPersonRepository.findByFirstNameAndLastNameAndPhoneNumber(createCardDTO.getAuthorizedPerson().getFirstName(), createCardDTO.getAuthorizedPerson().getLastName(), createCardDTO.getAuthorizedPerson().getPhoneNumber());

            if(authorizedPerson.isPresent()) {
                card.setAuthorizedPerson(authorizedPerson.get());
            } else {
                AuthorizedPerson person = cardMapper.dtoToAuthorizedPerson(createCardDTO.getAuthorizedPerson());

                authorizedPersonRepository.save(person);
            }
        }

        if (account.getSubtype().equals(AccountSubtype.BUSINESS) && createCardDTO.getCompany()!= null) {
            companyService.createCompany(createCardDTO.getCompany());
        }

        if(card.getAccount().getType().equals(AccountType.FOREIGN_CURRENCY) && card.getCardBrand().equals(CardBrand.DINA_CARD)){
            throw new RuntimeException("Dina kartica moze biti jedino povezana sa tekucim racunom");
        } else if(card.getAccount().getType().equals(AccountType.CURRENT) && card.getCardBrand().equals(CardBrand.AMERICAN_EXPRESS)){
            throw new RuntimeException("American Express moze biti jedino povezan sa deviznim racunom");
        }

        long currentTimeMillis = System.currentTimeMillis();
        card.setCreatedAt(currentTimeMillis);
        card.setExpirationDate(currentTimeMillis + (3L * 365 * 24 * 60 * 60 * 1000)); // +3 godine

        return cardRepository.save(card);
    }

    public void blockCard(int cardId, UpdateCardDTO updateCardDTO) {
        Card card = cardRepository.findById((long) cardId)
                .orElseThrow(() -> new RuntimeException("Kartica nije pronađena"));

        card.setBlocked(updateCardDTO.isStatus());

        cardRepository.save(card);
    }

    public void activateCard(int cardId, UpdateCardDTO updateCardDTO) {
        Card card = cardRepository.findById((long) cardId)
                .orElseThrow(() -> new RuntimeException("Kartica nije pronađena"));

        if(card.getActive().equals(false)){
            throw new RuntimeException("Kartica je deaktivirana i ne moze biti aktivirana");
        }

        card.setActive(updateCardDTO.isStatus());

        cardRepository.save(card);
    }

    public void updateCardLimit(Long cardId, UpdateCardLimitDTO updateCardLimitDTO) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Kartica nije pronađena"));

        card.setCardLimit(updateCardLimitDTO.getNewLimit());
        cardRepository.save(card);
    }

    public void updateCardName(Long cardId, UpdateCardNameDTO updateCardNameDTO) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Kartica nije pronađena"));

        card.setCardName(updateCardNameDTO.getName());
        cardRepository.save(card);
    }

}
