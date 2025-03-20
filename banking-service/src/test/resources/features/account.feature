Feature: Account Service Management

  Scenario: Successfully create an account
    Given customer with id 1001 exists
    When employee with id 500 creates a CURRENT account in RSD currency for customer 1001
    Then the account should be created successfully
    And notification email should be sent to customer

  Scenario: Fail to create account due to invalid currency combination
    Given customer with id 1001 exists
    When employee with id 500 tries to create a CURRENT account in EUR currency for customer 1001
    Then account creation should fail with message "Nevalidna kombinacija vrste racuna i valute"

  Scenario: Successfully retrieve transactions for account
    Given account with id 2001 exists
    When retrieving transactions for account 2001
    Then transactions should be successfully returned
