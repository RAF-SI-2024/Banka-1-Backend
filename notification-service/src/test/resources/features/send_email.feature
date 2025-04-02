Feature: Send Email Notification

  Scenario: Successfully sending an email notification
    Given an email notification request with valid details
    When the notification is processed
    Then an email should be sent successfully
    And the notification should be stored in the database
