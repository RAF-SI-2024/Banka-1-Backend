package services

import (
    "banka1.com/dto"
    "encoding/json"
    "errors"
    "fmt"
    "net/http"
    "net/url"
)

//const userServiceURL = "https://bank1.djues3.com/api/user/api/users/employees"

const userServiceURL = "https://bank1.djues3.com/api/user/api/users/employees/filtered"

func GetEmployees() ([]dto.EmployeeResponse, error) {
    resp, err := http.Get(userServiceURL)

    if err != nil {
        return nil, err
    }

    defer resp.Body.Close()

    if resp.StatusCode != http.StatusOK {
        return nil, errors.New(fmt.Sprintf("Greska pri dohvatanju zaposlenih, status: %v", resp.StatusCode))
    }

    var employees []dto.EmployeeResponse
    if err := json.NewDecoder(resp.Body).Decode(&employees); err != nil {
        return nil, err
    }

    return employees, nil
}

func GetEmployeesFiltered(name, surname, email, position string) ([]dto.EmployeeResponse, error) {
    params := url.Values{}
    if name != "" {
        params.Add("name", name)
    }
    if surname != "" {
        params.Add("surname", surname)
    }
    if email != "" {
        params.Add("email", email)
    }
    if position != "" {
        params.Add("position", position)
    }

    url := fmt.Sprintf("%s?%s", userServiceURL, params.Encode())
    resp, err := http.Get(url)
    if err != nil {
        return nil, err
    }

    defer resp.Body.Close()
    if resp.StatusCode != http.StatusOK {
        return nil, errors.New(fmt.Sprintf("Greska pri dohvatanju zaposlenih, status: %v", resp.StatusCode))
    }

    var employees []dto.EmployeeResponse
    if err := json.NewDecoder(resp.Body).Decode(&employees); err != nil {
        return nil, err
    }

    return employees, nil
}