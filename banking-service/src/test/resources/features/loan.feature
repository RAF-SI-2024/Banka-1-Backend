Feature: Loan Request 

    Scenario: Successful submission of loan request form
        Given customer is logged into the banking portal
        And customer navigates to the loans portal
        When customer presses the Apply for Loan button
        And customer fills out the loan request form
        And customer submits the loan request form
        Then the loan request should be successfully submitted

    Scenario: Unsuccessful submission of loan request form due to missing information
        Given customer is logged into the banking portal
        And customer navigates to the loans portal
        When customer presses the Apply for Loan button
        And customer fills out the loan request form with missing required information
        And customer submits the loan request form
        Then the loan request should not be submitted

    Scenario: Details of loan request are displayed
        Given customer is logged into the banking portal
        And customer navigates to the loans portal
        When customer presses the Details button