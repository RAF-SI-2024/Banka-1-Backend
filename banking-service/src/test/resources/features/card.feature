Feature: Card Service Management

  Scenario: Successfully create a new card
    Given account with id 2001 exists with subtype PERSONAL and has less than 2 cards
    When creating a VISA card for account 2001
    Then card should be created successfully

  Scenario: Fail to create DINA card for FOREIGN_CURRENCY account
    Given account with id 2002 exists with type FOREIGN_CURRENCY
    When creating a DINA_CARD card for account 2002
    Then card creation should fail with message "Dina kartica moze biti jedino povezana sa tekucim racunom"

  Scenario: Successfully block existing card
    Given card with id 3001 exists
    When blocking card 3001
    Then card should be blocked successfully
