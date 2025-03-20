Feature: Exchange Service Management

  Scenario: Successfully validate and create exchange transfer
    Given accounts 1001 and 1002 exist, belong to the same user, and have different currencies
    When creating an exchange transfer from account 1001 to account 1002 with amount 1000
    Then transfer should be successfully created
    And OTP code should be generated and sent to user email

  Scenario: Fail to validate exchange transfer due to same currencies
    Given accounts 1003 and 1004 exist, belong to the same user, but have the same currency
    When validating exchange transfer from account 1003 to account 1004
    Then transfer validation should fail

  Scenario: Successfully calculate RSD to EUR preview exchange
    Given exchange rate from RSD to EUR is 117.5
    When calculating exchange from RSD to EUR for amount 11750
    Then exchange preview should return correct converted amount and fee
