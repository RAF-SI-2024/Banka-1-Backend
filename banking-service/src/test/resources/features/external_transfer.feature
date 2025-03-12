Feature: External Transfer Processing
  Scenario: Successful external transfer
    Given there is an external transfer request
    And the sender has enough balance
    When the external transfer is processed
    Then the transfer status should be "COMPLETED"
    And the sender's balance should decrease
    And the receiver's balance should increase

  Scenario: Failed external transfer due to insufficient balance
    Given there is an external transfer request
    And the sender does not have enough balance
    When the external transfer is processed
    Then the transfer status should be "FAILED"
    And an error message "Insufficient balance for transfer" should be returned
