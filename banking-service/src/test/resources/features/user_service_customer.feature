Feature: User Service Customer Management

  Scenario: Successfully retrieve customer data by ID
    Given customer with id 1001 exists
    When retrieving customer data by id 1001
    Then customer data should be returned successfully

  Scenario: Fail to retrieve customer data due to non-existing ID
    Given customer with id 9999 does not exist
    When retrieving customer data by id 9999
    Then retrieving customer data should fail with message "Korisnik nije pronađen ili API nije vratio očekivani format."
