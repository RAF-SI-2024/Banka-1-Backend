package com.banka1.banking.cucumber;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features", // Lokacija feature fajlova
        glue = {"com.banka1.banking.cucumber", "com.banka1.banking.steps"}, // Lokacija step definicija
        plugin = {
                "pretty",  // Lepši ispis u konzoli
                "html:target/cucumber-reports.html", // HTML izveštaj u target folderu
                "json:target/cucumber.json" // JSON izveštaj
        },
        monochrome = true // Poboljšava čitljivost izlaza u terminalu
)
public class CucumberTest {
}
