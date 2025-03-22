package com.banka1.banking.services;

import com.banka1.banking.dto.CustomerDTO;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.Company;
import com.banka1.banking.models.helper.AccountStatus;
import com.banka1.banking.models.helper.AccountSubtype;
import com.banka1.banking.models.helper.AccountType;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.CompanyRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

//@RequiredArgsConstructor
//@Service
public class BankAccountsInitializer {

//    private final UserServiceCustomer userServiceCustomer;
//    private final AccountRepository accountRepository;
//    private final CompanyRepository companyRepository;
//
//    @PostConstruct
//    public void init(){
//        CustomerDTO customer = userServiceCustomer.getCustomerByEmail("bankabanka@banka1.com");
//        Company company = new Company();
//        company.setName("Naša Banka");
//        company.setAddress("Bulevar Banka 1");
//        company.setVatNumber("111111111");
//        company.setCompanyNumber("11111111");
//
//        for(CurrencyType currency : CurrencyType.values()){
//            boolean exists = accountRepository.existsByOwnerIDAndCurrencyType(customer.getId(), currency);
//            if(!exists){
//                Account account = new Account();
//                account.setOwnerID(customer.getId());
//                account.setCurrencyType(currency);
//                if (currency == CurrencyType.RSD){
//                    account.setBalance(10_000_000_000.0);
//                } else {
//                    account.setBalance(100_000_000.0);
//                }
//                account.setReservedBalance(0.0);
//                account.setType(AccountType.BANK);
//                account.setSubtype(AccountSubtype.STANDARD);
//                account.setAccountNumber(generateAccountNumber(AccountType.BANK,AccountSubtype.STANDARD,currency));
//                account.setCreatedDate(System.currentTimeMillis());
//                account.setExpirationDate(System.currentTimeMillis() + 4L * 365 * 24 * 60 * 60 * 1000);
//                account.setDailyLimit(100000000.0);
//                account.setMonthlyLimit(1000000000.0);
//                account.setDailySpent(0.0);
//                account.setMonthlySpent(0.0);
//                account.setStatus(AccountStatus.ACTIVE);
//                account.setEmployeeID(1L);
//                account.setMonthlyMaintenanceFee(0.0);
//                Company bankCompany = companyRepository.findByName("Naša Banka")
//                        .orElseGet(() -> companyRepository.save(company));
//                account.setCompany(bankCompany);
//
//                accountRepository.save(account);
//            }
//        }
//
//
//    }
//
//    private String generateAccountNumber(AccountType type, AccountSubtype subtype, CurrencyType currencyType) {
//        String bankCode = "111";
//        String branchCode = "0001";
//        String randomPart = String.format("%09d", (int)(Math.random() * 1_000_000_000));
//
//        String typeCode = "99";
//
//        String rawAccountNumber = bankCode + branchCode + randomPart + typeCode;
//
//        int sum = 0;
//        for (char c : rawAccountNumber.toCharArray()) {
//            sum += Character.getNumericValue(c);
//        }
//        if (sum % 11 != 0) {
//            return generateAccountNumber(type, subtype, currencyType);
//        }
//
//        return rawAccountNumber;
//    }


}
