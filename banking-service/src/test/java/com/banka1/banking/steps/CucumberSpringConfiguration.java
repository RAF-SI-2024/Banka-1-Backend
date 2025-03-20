package com.banka1.banking.steps;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import com.banka1.banking.BankingServiceApplication;

@CucumberContextConfiguration
@SpringBootTest(classes = BankingServiceApplication.class)
public class CucumberSpringConfiguration {
}
