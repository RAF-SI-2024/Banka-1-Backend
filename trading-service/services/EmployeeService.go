package services

import (
	"banka1.com/dto"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
)

const userServiceURL = "https://bank1.djues3.com/api/user/api/users/employees"

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
