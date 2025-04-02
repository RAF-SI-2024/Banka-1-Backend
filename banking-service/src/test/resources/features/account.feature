Feature: Account Creation

    Scenario: Successful account creation
        Given employee is logged into the account portal
        When employee navigates to account creation portal
        And employee enters the account information
        And employee presses the Create button for account
        Then the account should be succesfully created

    Scenario: Unsuccessful account creation
        Given employee is logged into the account portal
        When employee navigates to account creation portal
        And employee does not enter the account information
        And employee presses the Create button for account
        Then the account should not be succesfully created

        #