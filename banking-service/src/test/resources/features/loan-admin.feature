Feature: Loan Approval/Denial

    Scenario: Loan Denial
        Given employee is logged into the banking portal
        And employee navigates to the pending loans portal
        When employee presses the Deny button
        Then the loan should be succesfully denied

    Scenario: Loan Approval
      Given employee is logged into the banking portal
      And employee navigates to the pending loans portal
      When employee presses the Approve button
      Then the loan should be succesfully approved