Feature: Searching
  Scenario Outline: Search
    Given an administrator is logged in
    When one <type> search query is sent for "<field>" containing "<filter>"
    Then it's <ret> that results are returned

  Examples:
    | type      | field     | filter  | ret   |
    | customers |           |         | true  |
    | customers | firstName | invalid | false |
    | employees |           |         | true  |
    | employees | firstName | Jovana  | true  |
    | customers | birthDate | 1992    | false |
    | employees | lastName  | Petr    | true  |
