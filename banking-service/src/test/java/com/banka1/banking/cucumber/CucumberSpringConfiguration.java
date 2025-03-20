package com.banka1.banking.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.banka1.banking.BankingServiceApplication;

@CucumberContextConfiguration
@SpringBootTest(classes = BankingServiceApplication.class)
@ActiveProfiles("test") // Aktivira test profil ako postoji application-test.properties
public class CucumberSpringConfiguration {
}

