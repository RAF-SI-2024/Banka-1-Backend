package com.banka1.banking.bootstrap;

import com.banka1.banking.models.*;
import com.banka1.banking.models.helper.*;
import com.banka1.banking.repository.*;
import com.banka1.banking.services.CurrencyService;
import com.banka1.banking.services.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BootstrapData implements CommandLineRunner {

    private final CurrencyRepository currencyRepository;
    private final CurrencyService currencyService;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferRepository transferRepository;
    private final ReceiverRepository receiverRepository;
    private final LoanRepository loanRepository;

    @Autowired
    public BootstrapData(CurrencyRepository currencyRepository, CurrencyService currencyService, AccountRepository accountRepository, TransactionRepository transactionRepository, TransferRepository transferRepository, ReceiverRepository receiverRepository, LoanRepository loanRepository) {
        this.currencyRepository = currencyRepository;
        this.currencyService = currencyService;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transferRepository = transferRepository;
        this.receiverRepository = receiverRepository;
        this.loanRepository = loanRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("============== Loading Data ==============");

//        Account account = new Account();
//        account.setAccountNumber("1234567890123456");
//        account.setBalance(1000.0);
//        account.setCompany(null);
//        account.setDailyLimit(10000.0);
//        account.setMonthlyLimit(100000.0);
//        account.setDailySpent(0.0);
//        account.setMonthlySpent(0.0);
//        account.setCurrencyType(CurrencyType.RSD);
//        account.setExpirationDate(1630454400000L);
//        account.setCreatedDate(1627776000000L);
//        account.setEmployeeID(1L);
//        account.setMonthlyMaintenanceFee(0.0);
//        account.setReservedBalance(0.0);
//        account.setOwnerID(1L);
//        account.setStatus(AccountStatus.ACTIVE);
//        account.setType(AccountType.CURRENT);
//        account.setSubtype(AccountSubtype.BUSINESS);
//        account.setId(1L);
//
//        accountRepository.save(account);
//
//        Receiver receiver = new Receiver();
//        receiver.setAddress("Nemanjina 11");
//        receiver.setAccountNumber("1234567890123456");
//        receiver.setOwnerAccountId(1L);
//        receiver.setFirstName("John");
//        receiver.setLastName("Doe");
//
//        receiverRepository.save(receiver);

//        Transfer transfer = new Transfer();
//        transfer.setAmount(100.0);
//        transfer.setFromCurrency(rsd);
//        transfer.setToCurrency(rsd);
//        transfer.setStatus(TransferStatus.COMPLETED);
//        transfer.setFromAccountId(account);
//        transfer.setToAccountId(account);
//        transfer.setCompletedAt(1627776000000L);
//        transfer.setCreatedAt(1627776000000L);
//        transfer.setOtp("123456");
//        transfer.setPaymentCode("123456");
//        transfer.setAdress("Nemanjina 11");
//        transfer.setPaymentReference("123456");
//        transfer.setType(TransferType.INTERNAL);
//        transfer.setPaymentDescription("Initial deposit");
//        transfer.setReceiver("John Doe");
//
//        transferRepository.save(transfer);
//
//        Transaction transaction = new Transaction();
//        transaction.setAmount(100.0);
//        transaction.setCurrency(rsd);
//        transaction.setDescription("Initial deposit");
//        transaction.setTimestamp(1627776000000L);
//        transaction.setFromAccountId(account);
//        transaction.setToAccountId(account);
//        transaction.setTransfer(transfer);
//        transaction.setId(1L);
//
//        transactionRepository.save(transaction);

        if (currencyRepository.count() == 0) {
            List<Currency> currencies = new ArrayList<>();

            Currency rsd = new Currency();
            rsd.setCode(CurrencyType.RSD);
            rsd.setName("Serbian Dinar");
            rsd.setCountry("Serbia");
            rsd.setSymbol("дин.");
            currencies.add(rsd);

            Currency eur = new Currency();
            eur.setCode(CurrencyType.EUR);
            eur.setName("Euro");
            eur.setCountry("European Union");
            eur.setSymbol("€");
            currencies.add(eur);

            Currency usd = new Currency();
            usd.setCode(CurrencyType.USD);
            usd.setName("US Dollar");
            usd.setCountry("United States");
            usd.setSymbol("$");
            currencies.add(usd);

            Currency gbp = new Currency();
            gbp.setCode(CurrencyType.GBP);
            gbp.setName("British Pound");
            gbp.setCountry("United Kingdom");
            gbp.setSymbol("£");
            currencies.add(gbp);

            Currency chf = new Currency();
            chf.setCode(CurrencyType.CHF);
            chf.setName("Swiss Franc");
            chf.setCountry("Switzerland");
            chf.setSymbol("Fr");
            currencies.add(chf);

            Currency jpy = new Currency();
            jpy.setCode(CurrencyType.JPY);
            jpy.setName("Japanese Yen");
            jpy.setCountry("Japan");
            jpy.setSymbol("¥");
            currencies.add(jpy);

            Currency cad = new Currency();
            cad.setCode(CurrencyType.CAD);
            cad.setName("Canadian Dollar");
            cad.setCountry("Canada");
            cad.setSymbol("C$");
            currencies.add(cad);

            Currency aud = new Currency();
            aud.setCode(CurrencyType.AUD);
            aud.setName("Australian Dollar");
            aud.setCountry("Australia");
            aud.setSymbol("A$");
            currencies.add(aud);

            // Čuvanje u bazi
            currencyRepository.saveAll(currencies);
            currencyService.fetchExchangeRates();
            System.out.println("Currencies have been initialized in the database.");

        }

        Account jovanTekuci = new Account();
        jovanTekuci.setAccountNumber("111000100000000110");
        jovanTekuci.setBalance(100000.0);
        jovanTekuci.setCompany(null);
        jovanTekuci.setDailyLimit(10000.0);
        jovanTekuci.setMonthlyLimit(100000.0);
        jovanTekuci.setDailySpent(0.0);
        jovanTekuci.setMonthlySpent(0.0);
        jovanTekuci.setCurrencyType(CurrencyType.RSD);
        jovanTekuci.setExpirationDate(1630454400000L);
        jovanTekuci.setCreatedDate(2025030500000L);
        jovanTekuci.setEmployeeID(1L);
        jovanTekuci.setMonthlyMaintenanceFee(0.0);
        jovanTekuci.setReservedBalance(0.0);
        jovanTekuci.setOwnerID(3L);
        jovanTekuci.setStatus(AccountStatus.ACTIVE);
        jovanTekuci.setType(AccountType.CURRENT);
        jovanTekuci.setSubtype(AccountSubtype.STANDARD);

        accountRepository.save(jovanTekuci);

        Account jovanTekuci2 = new Account();
        jovanTekuci2.setAccountNumber("111000100011000110");
        jovanTekuci2.setBalance(1000000.0);
        jovanTekuci2.setCompany(null);
        jovanTekuci2.setDailyLimit(0.0);
        jovanTekuci2.setMonthlyLimit(0.0);
        jovanTekuci2.setDailySpent(0.0);
        jovanTekuci2.setMonthlySpent(0.0);
        jovanTekuci2.setCurrencyType(CurrencyType.RSD);
        jovanTekuci2.setExpirationDate(1630454400000L);
        jovanTekuci2.setCreatedDate(2025030500000L);
        jovanTekuci2.setEmployeeID(2L);
        jovanTekuci2.setMonthlyMaintenanceFee(0.0);
        jovanTekuci2.setReservedBalance(0.0);
        jovanTekuci2.setOwnerID(3L);
        jovanTekuci2.setStatus(AccountStatus.ACTIVE);
        jovanTekuci2.setType(AccountType.CURRENT);
        jovanTekuci2.setSubtype(AccountSubtype.SAVINGS);

        accountRepository.save(jovanTekuci2);

        Account jovanEUR = new Account();
        jovanEUR.setAccountNumber("111000100000000120");
        jovanEUR.setBalance(1000.0);
        jovanEUR.setCompany(null);
        jovanEUR.setDailyLimit(200.0);
        jovanEUR.setMonthlyLimit(10000.0);
        jovanEUR.setDailySpent(0.0);
        jovanEUR.setMonthlySpent(0.0);
        jovanEUR.setCurrencyType(CurrencyType.EUR);
        jovanEUR.setExpirationDate(1630454400000L);
        jovanEUR.setCreatedDate(2025030500000L);
        jovanEUR.setEmployeeID(1L);
        jovanEUR.setMonthlyMaintenanceFee(0.0);
        jovanEUR.setReservedBalance(0.0);
        jovanEUR.setOwnerID(3L);
        jovanEUR.setStatus(AccountStatus.ACTIVE);
        jovanEUR.setType(AccountType.FOREIGN_CURRENCY);
        jovanEUR.setSubtype(AccountSubtype.STANDARD);

        accountRepository.save(jovanEUR);

        Account jovanEUR2= new Account();
        jovanEUR2.setAccountNumber("111000100220000120");
        jovanEUR2.setBalance(1000.0);
        jovanEUR2.setCompany(null);
        jovanEUR2.setDailyLimit(100.0);
        jovanEUR2.setMonthlyLimit(1000.0);
        jovanEUR2.setDailySpent(0.0);
        jovanEUR2.setMonthlySpent(0.0);
        jovanEUR2.setCurrencyType(CurrencyType.EUR);
        jovanEUR2.setExpirationDate(1630454400000L);
        jovanEUR2.setCreatedDate(2025030500000L);
        jovanEUR2.setEmployeeID(1L);
        jovanEUR2.setMonthlyMaintenanceFee(0.0);
        jovanEUR2.setReservedBalance(0.0);
        jovanEUR2.setOwnerID(3L);
        jovanEUR2.setStatus(AccountStatus.ACTIVE);
        jovanEUR2.setType(AccountType.FOREIGN_CURRENCY);
        jovanEUR2.setSubtype(AccountSubtype.PENSION);

        accountRepository.save(jovanEUR2);

        Account jovanUSD = new Account();
        jovanUSD.setAccountNumber("111000100000000320");
        jovanUSD.setBalance(1000.0);
        jovanUSD.setCompany(null);
        jovanUSD.setDailyLimit(200.0);
        jovanUSD.setMonthlyLimit(10000.0);
        jovanUSD.setDailySpent(0.0);
        jovanUSD.setMonthlySpent(0.0);
        jovanUSD.setCurrencyType(CurrencyType.USD);
        jovanUSD.setExpirationDate(1630454400000L);
        jovanUSD.setCreatedDate(2025030500000L);
        jovanUSD.setEmployeeID(1L);
        jovanUSD.setMonthlyMaintenanceFee(0.0);
        jovanUSD.setReservedBalance(0.0);
        jovanUSD.setOwnerID(3L);
        jovanUSD.setStatus(AccountStatus.ACTIVE);
        jovanUSD.setType(AccountType.FOREIGN_CURRENCY);
        jovanUSD.setSubtype(AccountSubtype.STANDARD);

        accountRepository.save(jovanUSD);

        Account nemanjaTekuci = new Account();
        nemanjaTekuci.setAccountNumber("111000100000000210");
        nemanjaTekuci.setBalance(100000.0);
        nemanjaTekuci.setCompany(null);
        nemanjaTekuci.setDailyLimit(10000.0);
        nemanjaTekuci.setMonthlyLimit(100000.0);
        nemanjaTekuci.setDailySpent(0.0);
        nemanjaTekuci.setMonthlySpent(0.0);
        nemanjaTekuci.setCurrencyType(CurrencyType.RSD);
        nemanjaTekuci.setExpirationDate(1630454400000L);
        nemanjaTekuci.setCreatedDate(2025030500000L);
        nemanjaTekuci.setEmployeeID(1L);
        nemanjaTekuci.setMonthlyMaintenanceFee(0.0);
        nemanjaTekuci.setReservedBalance(0.0);
        nemanjaTekuci.setOwnerID(4L);
        nemanjaTekuci.setStatus(AccountStatus.ACTIVE);
        nemanjaTekuci.setType(AccountType.CURRENT);
        nemanjaTekuci.setSubtype(AccountSubtype.STANDARD);

        accountRepository.save(nemanjaTekuci);

        Account nemanjaTekuci2 = new Account();
        nemanjaTekuci2.setAccountNumber("111000100000222210");
        nemanjaTekuci2.setBalance(300000.0);
        nemanjaTekuci2.setCompany(null);
        nemanjaTekuci2.setDailyLimit(10000.0);
        nemanjaTekuci2.setMonthlyLimit(100000.0);
        nemanjaTekuci2.setDailySpent(0.0);
        nemanjaTekuci2.setMonthlySpent(0.0);
        nemanjaTekuci2.setCurrencyType(CurrencyType.RSD);
        nemanjaTekuci2.setExpirationDate(1630454400000L);
        nemanjaTekuci2.setCreatedDate(2025030500000L);
        nemanjaTekuci2.setEmployeeID(1L);
        nemanjaTekuci2.setMonthlyMaintenanceFee(0.0);
        nemanjaTekuci2.setReservedBalance(0.0);
        nemanjaTekuci2.setOwnerID(4L);
        nemanjaTekuci2.setStatus(AccountStatus.ACTIVE);
        nemanjaTekuci2.setType(AccountType.CURRENT);
        nemanjaTekuci2.setSubtype(AccountSubtype.SAVINGS);

        accountRepository.save(nemanjaTekuci2);

        Account nemanjaEur = new Account();
        nemanjaEur.setAccountNumber("111000100000000220");
        nemanjaEur.setBalance(1000.0);
        nemanjaEur.setCompany(null);
        nemanjaEur.setDailyLimit(200.0);
        nemanjaEur.setMonthlyLimit(10000.0);
        nemanjaEur.setDailySpent(0.0);
        nemanjaEur.setMonthlySpent(0.0);
        nemanjaEur.setCurrencyType(CurrencyType.EUR);
        nemanjaEur.setExpirationDate(1630454400000L);
        nemanjaEur.setCreatedDate(2025030500000L);
        nemanjaEur.setEmployeeID(1L);
        nemanjaEur.setMonthlyMaintenanceFee(0.0);
        nemanjaEur.setReservedBalance(0.0);
        nemanjaEur.setOwnerID(4L);
        nemanjaEur.setStatus(AccountStatus.ACTIVE);
        nemanjaEur.setType(AccountType.FOREIGN_CURRENCY);
        nemanjaEur.setSubtype(AccountSubtype.STANDARD);

        accountRepository.save(nemanjaEur);

        Account nemanjaGBP = new Account();
        nemanjaGBP.setAccountNumber("111000100000000420");
        nemanjaGBP.setBalance(1000.0);
        nemanjaGBP.setCompany(null);
        nemanjaGBP.setDailyLimit(200.0);
        nemanjaGBP.setMonthlyLimit(10000.0);
        nemanjaGBP.setDailySpent(0.0);
        nemanjaGBP.setMonthlySpent(0.0);
        nemanjaGBP.setCurrencyType(CurrencyType.GBP);
        nemanjaGBP.setExpirationDate(1630454400000L);
        nemanjaGBP.setCreatedDate(2025030500000L);
        nemanjaGBP.setEmployeeID(1L);
        nemanjaGBP.setMonthlyMaintenanceFee(0.0);
        nemanjaGBP.setReservedBalance(0.0);
        nemanjaGBP.setOwnerID(4L);
        nemanjaGBP.setStatus(AccountStatus.ACTIVE);
        nemanjaGBP.setType(AccountType.FOREIGN_CURRENCY);
        nemanjaGBP.setSubtype(AccountSubtype.STANDARD);

        accountRepository.save(nemanjaGBP);

        Account nemanjaEur2 = new Account();
        nemanjaEur2.setAccountNumber("111000100222220220");
        nemanjaEur2.setBalance(5000.0);
        nemanjaEur2.setCompany(null);
        nemanjaEur2.setDailyLimit(200.0);
        nemanjaEur2.setMonthlyLimit(10000.0);
        nemanjaEur2.setDailySpent(0.0);
        nemanjaEur2.setMonthlySpent(0.0);
        nemanjaEur2.setCurrencyType(CurrencyType.EUR);
        nemanjaEur2.setExpirationDate(2029030500000L);
        nemanjaEur2.setCreatedDate(2025030500000L);
        nemanjaEur2.setEmployeeID(1L);
        nemanjaEur2.setMonthlyMaintenanceFee(0.0);
        nemanjaEur2.setReservedBalance(0.0);
        nemanjaEur2.setOwnerID(4L);
        nemanjaEur2.setStatus(AccountStatus.ACTIVE);
        nemanjaEur2.setType(AccountType.FOREIGN_CURRENCY);
        nemanjaEur2.setSubtype(AccountSubtype.STANDARD);

        accountRepository.save(nemanjaEur2);

        Account nikolaTekuci1 = new Account();
        nikolaTekuci1.setAccountNumber("111000100330222210");
        nikolaTekuci1.setBalance(300000.0);
        nikolaTekuci1.setCompany(null);
        nikolaTekuci1.setDailyLimit(10000.0);
        nikolaTekuci1.setMonthlyLimit(100000.0);
        nikolaTekuci1.setDailySpent(0.0);
        nikolaTekuci1.setMonthlySpent(0.0);
        nikolaTekuci1.setCurrencyType(CurrencyType.RSD);
        nikolaTekuci1.setExpirationDate(1630454400000L);
        nikolaTekuci1.setCreatedDate(2025030500000L);
        nikolaTekuci1.setEmployeeID(1L);
        nikolaTekuci1.setMonthlyMaintenanceFee(0.0);
        nikolaTekuci1.setReservedBalance(0.0);
        nikolaTekuci1.setOwnerID(5L);
        nikolaTekuci1.setStatus(AccountStatus.ACTIVE);
        nikolaTekuci1.setType(AccountType.FOREIGN_CURRENCY);
        nikolaTekuci1.setSubtype(AccountSubtype.STANDARD);

        accountRepository.save(nikolaTekuci1);

        Account nikolaEUR = new Account();
        nikolaEUR.setAccountNumber("111000100335112220");
        nikolaEUR.setBalance(4000.0);
        nikolaEUR.setCompany(null);
        nikolaEUR.setDailyLimit(1000.0);
        nikolaEUR.setMonthlyLimit(10000.0);
        nikolaEUR.setDailySpent(0.0);
        nikolaEUR.setMonthlySpent(0.0);
        nikolaEUR.setCurrencyType(CurrencyType.EUR);
        nikolaEUR.setExpirationDate(1630454400000L);
        nikolaEUR.setCreatedDate(2025030500000L);
        nikolaEUR.setEmployeeID(1L);
        nikolaEUR.setMonthlyMaintenanceFee(0.0);
        nikolaEUR.setReservedBalance(0.0);
        nikolaEUR.setOwnerID(5L);
        nikolaEUR.setStatus(AccountStatus.ACTIVE);
        nikolaEUR.setType(AccountType.FOREIGN_CURRENCY);
        nikolaEUR.setSubtype(AccountSubtype.STANDARD);

        accountRepository.save(nikolaEUR);

        Account jelenaTekuci = new Account();
        jelenaTekuci.setAccountNumber("111000100335672210");
        jelenaTekuci.setBalance(300000.0);
        jelenaTekuci.setCompany(null);
        jelenaTekuci.setDailyLimit(10000.0);
        jelenaTekuci.setMonthlyLimit(100000.0);
        jelenaTekuci.setDailySpent(0.0);
        jelenaTekuci.setMonthlySpent(0.0);
        jelenaTekuci.setCurrencyType(CurrencyType.RSD);
        jelenaTekuci.setExpirationDate(1630454400000L);
        jelenaTekuci.setCreatedDate(2025030500000L);
        jelenaTekuci.setEmployeeID(1L);
        jelenaTekuci.setMonthlyMaintenanceFee(0.0);
        jelenaTekuci.setReservedBalance(0.0);
        jelenaTekuci.setOwnerID(6L);
        jelenaTekuci.setStatus(AccountStatus.ACTIVE);
        jelenaTekuci.setType(AccountType.CURRENT);
        jelenaTekuci.setSubtype(AccountSubtype.STANDARD);

        accountRepository.save(jelenaTekuci);

        Account jelenaEUR = new Account();
        nikolaEUR.setAccountNumber("111000100366112220");
        nikolaEUR.setBalance(4000.0);
        nikolaEUR.setCompany(null);
        nikolaEUR.setDailyLimit(1000.0);
        nikolaEUR.setMonthlyLimit(10000.0);
        nikolaEUR.setDailySpent(0.0);
        nikolaEUR.setMonthlySpent(0.0);
        nikolaEUR.setCurrencyType(CurrencyType.EUR);
        nikolaEUR.setExpirationDate(1630454400000L);
        nikolaEUR.setCreatedDate(2025030500000L);
        nikolaEUR.setEmployeeID(1L);
        nikolaEUR.setMonthlyMaintenanceFee(0.0);
        nikolaEUR.setReservedBalance(0.0);
        nikolaEUR.setOwnerID(6L);
        nikolaEUR.setStatus(AccountStatus.ACTIVE);
        nikolaEUR.setType(AccountType.FOREIGN_CURRENCY);
        nikolaEUR.setSubtype(AccountSubtype.STANDARD);

        accountRepository.save(nikolaEUR);


        Receiver receiverJovanTekuci1 = new Receiver();
        receiverJovanTekuci1.setOwnerAccountId(1L);
        receiverJovanTekuci1.setAccountNumber("111000100000000210");
        receiverJovanTekuci1.setFirstName("Nemanja");
        receiverJovanTekuci1.setLastName("Marjanov");
        receiverRepository.save(receiverJovanTekuci1);

        Receiver receiverJovanTekuci2 = new Receiver();
        receiverJovanTekuci2.setOwnerAccountId(1L);
        receiverJovanTekuci2.setAccountNumber("111000100330222210");
        receiverJovanTekuci2.setFirstName("Nikola");
        receiverJovanTekuci2.setLastName("Nikolic");
        receiverRepository.save(receiverJovanTekuci2);

        Receiver receiverJovanTekuci3 = new Receiver();
        receiverJovanTekuci3.setOwnerAccountId(1L);
        receiverJovanTekuci3.setAccountNumber("111000100335672210");
        receiverJovanTekuci3.setFirstName("Jelena");
        receiverJovanTekuci3.setLastName("Jovanovic");
        receiverRepository.save(receiverJovanTekuci3);

        Receiver receiverJovanEUR1 = new Receiver();
        receiverJovanEUR1.setOwnerAccountId(3L);
        receiverJovanEUR1.setAccountNumber("111000100000000220");
        receiverJovanEUR1.setFirstName("Nemanja");
        receiverJovanEUR1.setLastName("Marjanov");
        receiverRepository.save(receiverJovanEUR1);

        Receiver receiverJovanEUR2 = new Receiver();
        receiverJovanEUR2.setOwnerAccountId(3L);
        receiverJovanEUR2.setAccountNumber("111000100366112220");
        receiverJovanEUR2.setFirstName("Jelena");
        receiverJovanEUR2.setLastName("Jovanovic");
        receiverRepository.save(receiverJovanEUR2);


        Loan loan = new Loan();
        loan.setLoanType(LoanType.CASH);
        loan.setCurrencyType(CurrencyType.RSD);
        loan.setInterestType(InterestType.FIXED);
        loan.setPaymentStatus(PaymentStatus.PENDING);
        loan.setNominalRate(5.5);
        loan.setEffectiveRate(6.0);
        loan.setLoanAmount(500000.0);
        loan.setDuration(24); // 24 months
        loan.setCreatedDate(System.currentTimeMillis());
        loan.setAllowedDate(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)); // Allowed in 7 days
        loan.setMonthlyPayment(22000.0);
        loan.setNextPaymentDate(System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)); // Next payment in a month
        loan.setRemainingAmount(500000.0);
        loan.setLoanReason("Home renovation");
        loan.setAccount(jovanTekuci);

        Loan loan1 = new Loan();
        loan1.setLoanType(LoanType.CASH);
        loan1.setCurrencyType(CurrencyType.RSD);
        loan1.setInterestType(InterestType.FIXED);
        loan1.setPaymentStatus(PaymentStatus.PENDING);
        loan1.setNominalRate(5.5);
        loan1.setEffectiveRate(6.0);
        loan1.setLoanAmount(550000.0);
        loan1.setDuration(24); // 24 months
        loan1.setCreatedDate(System.currentTimeMillis());
        loan1.setAllowedDate(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)); // Allowed in 7 days
        loan1.setMonthlyPayment(22000.0);
        loan1.setNextPaymentDate(System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)); // Next payment in a month
        loan1.setRemainingAmount(500000.0);
        loan1.setLoanReason("Home renovation, attempt 2");
        loan1.setAccount(jovanTekuci);

        loanRepository.save(loan);
        loanRepository.save(loan1);

        System.out.println("============== Data Loaded ==============");
    }
}
