Feature: Loan Service Management

  Scenario: Successfully create a valid loan request
    Given customer account with id 1001 exists
    When customer requests a CASH loan with amount 100000 and 24 installments
    Then loan should be created with status "PENDING"

  Scenario: Fail to create loan due to invalid number of installments
    Given customer account with id 1001 exists
    When customer requests a MORTGAGE loan with amount 500000 and invalid 10 installments
    Then loan creation should fail with message "Invalid number of installments"

  Scenario: Approve a pending loan request successfully
    Given a pending loan request with id 5001 exists
    When employee approves loan request with id 5001
    Then loan status should be updated to "APPROVED"
    And customer should receive notification "Vaš kredit je odobren."

  Scenario: Reject a pending loan request
    Given a pending loan request with id 5002 exists
    When employee rejects loan request with id 5002 with reason "Insufficient income"
    Then loan status should be updated to "DENIED"
    And customer should receive notification "Vaš kredit je odbijen."

  Scenario: Process daily loan installments successfully
    Given there are installments due for today
    When scheduled loan installment processing is triggered
    Then all due installments should be processed and marked as paid

  Scenario: No installments due today
    Given there are no installments due for today
    When scheduled loan installment processing is triggered
    Then loan installment processing should fail with message "Nijedna rata nije danas na redu za naplatu"
