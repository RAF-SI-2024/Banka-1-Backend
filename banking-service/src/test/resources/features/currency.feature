Feature: Currency exchange service management

  Scenario: Successfully fetching and updating exchange rates from API
    Given external exchange rates API is available
    And currencies RSD, EUR, USD exist in the database
    When the scheduled task to fetch exchange rates is triggered
    Then exchange rates should be successfully saved into the database

  Scenario: Failure when external API is unavailable
    Given external exchange rates API is not available
    When the scheduled task to fetch exchange rates is triggered
    Then no exchange rates should be saved
    And appropriate error message should be logged

  Scenario: Retrieve all exchange rates successfully
    Given there are exchange rates in the database
    When user requests all exchange rates
    Then all exchange rates should be returned successfully

  Scenario: Retrieve exchange rates with non-existing base currency
    Given there are exchange rates in the database
    When user requests exchange rates for a non-existing base currency "GBP"
    Then exchange rate retrieval should fail with error "Exchange rate for base currency GBP not found"
