Feature: Receiver management service

  Scenario: Successfully create a new receiver
    Given receiver with account number "123456789012345678" does not exist for account 1001
    When user creates a receiver with name "Marko Markovic", address "Beograd, Srbija", account number "123456789012345678" for account 1001
    Then receiver should be successfully created

  Scenario: Fail to create receiver due to duplicate account number
    Given receiver with account number "123456789012345678" already exists for account 1001
    When user creates a receiver with name "Marko Markovic", address "Beograd, Srbija", account number "123456789012345678" for account 1001
    Then receiver creation should fail with message "Primalac sa ovim brojem računa već postoji za datog korisnika."

  Scenario: Successfully update an existing receiver
    Given receiver with id 2001 exists
    When user updates receiver with id 2001 with new account number "1111222233334444", name "Petar Petrovic", and address "Novi Sad, Srbija"
    Then receiver should be successfully updated

  Scenario: Fail to update non-existing receiver
    Given receiver with id 9999 does not exist
    When user updates receiver with id 9999 with new account number "1111222233334444", name "Petar Petrovic", and address "Novi Sad, Srbija"
    Then receiver update should fail with message "Primalac sa ID 9999 ne postoji."

  Scenario: Successfully delete existing receiver
    Given receiver with id 3001 exists
    When user deletes receiver with id 3001
    Then receiver should be successfully deleted

  Scenario: Fail to delete non-existing receiver
    Given receiver with id 8888 does not exist
    When user deletes receiver with id 8888
    Then receiver deletion should fail with message "Primalac sa ID 8888 ne postoji."

