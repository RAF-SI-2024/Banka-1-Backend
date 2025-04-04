package services

import (
	"banka1.com/dto"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
)

//const userServiceURL = "https://bank1.djues3.com/api/user/api/users/employees"

//const userServiceURL = "https://bank1.djues3.com/api/user/api/users/employees/filtered"

//func GetEmployees() ([]dto.EmployeeResponse, error) {
//	resp, err := http.Get(userServiceURL)
//
//	if err != nil {
//		return nil, err
//	}
//
//	defer resp.Body.Close()
//
//	if resp.StatusCode != http.StatusOK {
//		return nil, errors.New(fmt.Sprintf("Greska pri dohvatanju zaposlenih, status: %v", resp.StatusCode))
//	}
//
//	var employees []dto.EmployeeResponse
//	if err := json.NewDecoder(resp.Body).Decode(&employees); err != nil {
//		return nil, err
//	}
//
//	return employees, nil
//}

func GetEmployeesFiltered(name, surname, email, position string) ([]dto.EmployeeResponse, error) {
	// Preuzimanje base URL iz okruženja
	basePath := os.Getenv("USER_SERVICE")
	if basePath == "" {
		return nil, fmt.Errorf("USER_SERVICE environment variable is not set")
	}

	uuserServiceURL := basePath + "/api/users/employees/actuaries/filtered"

	// Kreiranje URL-a sa query parametrima
	req, err := http.NewRequest("GET", uuserServiceURL, nil)
	if err != nil {
		fmt.Println("ERROR: Nece request ", err)

		return nil, err
	}

	// Dodavanje query parametara u URL
	q := req.URL.Query()
	if name != "" {
		q.Add("firstName", name)
	}
	if surname != "" {
		q.Add("lastName", surname)
	}
	if email != "" {
		q.Add("email", email)
	}
	if position != "" {
		q.Add("position", position)
	}
	req.URL.RawQuery = q.Encode()

	token := "eyJhbGciOiJIUzI1NiJ9.eyJpZCI6NCwicG9zaXRpb24iOiJXT1JLRVIiLCJwZXJtaXNzaW9ucyI6WyJ1c2VyLmN1c3RvbWVyLnZpZXciLCJ1c2VyLmN1c3RvbWVyLmNyZWF0ZSIsInVzZXIuY3VzdG9tZXIuZGVsZXRlIiwidXNlci5jdXN0b21lci5saXN0IiwidXNlci5jdXN0b21lci5lZGl0Il0sImlzRW1wbG95ZWQiOnRydWUsImlzQWRtaW4iOmZhbHNlLCJkZXBhcnRtZW50IjoiU1VQRVJWSVNPUiIsImlhdCI6MTc0MzczODc2NCwiZXhwIjoxNzQzNzQwNTY0fQ.nuVfA_qortYSSnPmM0HYJizcQBcdHq8U2oBL67uXnn4" // Zamenite sa stvarnim tokenom
	req.Header.Add("Authorization", "Bearer "+token)

	// Ispisivanje URL-a koji se šalje
	fmt.Println("Request URL: ", req.URL.String()+"HEADER + "+req.Header.Get("Authorization")) // Ispisuje ceo URL sa parametrima

	// Slanje GET zahteva
	resp, err := http.Get(req.URL.String())
	if err != nil {
		fmt.Println("ERROR: Failed to send HTTP request:", err)
		return nil, err
	}
	defer func(Body io.ReadCloser) {
		err := Body.Close()
		if err != nil {
			fmt.Println("ERROR: Failed to close response body:", err)

		}
	}(resp.Body)

	if resp.StatusCode != http.StatusOK {
		fmt.Println("ERROR: Non-OK HTTP status:", resp.StatusCode)

		return nil, fmt.Errorf("Greska pri dohvatanju zaposlenih, status: %v", resp.StatusCode)
	}

	// Dekodiranje JSON odgovora
	var employees []dto.EmployeeResponse
	if err := json.NewDecoder(resp.Body).Decode(&employees); err != nil {
		fmt.Println("ERROR: Failed to decode response body:", err)

		return nil, err
	}

	return employees, nil
}
