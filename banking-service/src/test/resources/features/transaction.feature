Feature: Transaction service management

  Scenario: Successfully process an internal transfer
    Given internal transfer with id 1001 is pending and sender has sufficient balance
    When the internal transfer with id 1001 is processed
    Then internal transfer should be marked as COMPLETED
    And both accounts should be updated correctly
    And transactions should be created successfully

  Scenario: Fail to process internal transfer due to insufficient funds
    Given internal transfer with id 1002 is pending and sender has insufficient balance
    When the internal transfer with id 1002 is processed
    Then internal transfer should fail with message "Insufficient funds"
    And transfer status should be marked as FAILED

  Scenario: Successfully process an external transfer
    Given external transfer with id 2001 is pending and sender has sufficient balance
    When the external transfer with id 2001 is processed
    Then external transfer should be marked as COMPLETED
    And accounts should be updated correctly
    And transactions should be created successfully

  Scenario: Fail to process external transfer due to insufficient balance
    Given external transfer with id 2002 is pending and sender has insufficient balance
    When the external transfer with id 2002 is processed
    Then external transfer should fail with message "Insufficient balance for transfer"
    And transfer status should be marked as FAILED

  Scenario: Calculate loan installment amount correctly
    When calculating installment for loan amount 100000 with annual interest rate 6 and 12 installments
    Then the installment amount should be correctly calculated as 8606.64

  Scenario: Successfully process loan installment
    Given customer has sufficient funds for installment
    When processing loan installment payment
    Then loan installment should be processed successfully
    And accounts and transactions should be updated

  Scenario: Fail to process loan installment due to insufficient funds
    Given customer has insufficient funds for installment
    When processing loan installment payment
    Then loan installment payment should fail

