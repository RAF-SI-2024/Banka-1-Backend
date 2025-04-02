package com.banka1.banking.cucumber;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Import;

@Import(CucumberSpringConfiguration.class)
@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features",
        glue = "com.banka1.banking.cucumber"
)


public class CucumberTest {
    
}
