package controllers

import (
	"banka1.com/db"
	"bytes"
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"banka1.com/dto"
	"banka1.com/types"
	"github.com/gofiber/fiber/v2"
	"github.com/stretchr/testify/assert"
)

func TestInitActuaryRoutes(t *testing.T) {
	// Setup
	app := fiber.New()

	// Execute
	InitActuaryRoutes(app)

	// Verify routes are registered
	findRoute := func(method, path string) bool {
		for _, routes := range app.Stack() {
			for _, route := range routes {
				if route.Method == method && strings.Contains(route.Path, path) {
					return true
				}
			}
		}
		return false
	}

	// Check expected routes
	assert.True(t, findRoute("POST", "/actuaries"))
	assert.True(t, findRoute("GET", "/actuaries/all"))
	assert.True(t, findRoute("GET", "/actuaries/filter"))
	assert.True(t, findRoute("GET", "/actuaries/profits"))
	assert.True(t, findRoute("GET", "/actuaries/:ID"))
	assert.True(t, findRoute("PUT", "/actuaries/:ID/limit"))
	assert.True(t, findRoute("PUT", "/actuaries/:ID/reset-used-limit"))
}

func TestCreateActuaryInvalidRequest(t *testing.T) {
	// Setup
	app := fiber.New()
	controller := NewActuaryController()
	app.Post("/actuaries", controller.CreateActuary)

	// Create test request with invalid JSON
	reqBody := `{"user_id": "invalid" "not_a_valid_field": true}`
	req := httptest.NewRequest(http.MethodPost, "/actuaries", bytes.NewBufferString(reqBody))
	req.Header.Set("Content-Type", "application/json")
	resp, _ := app.Test(req)

	// Verify
	assert.Equal(t, 400, resp.StatusCode)

	// Check response body
	body, _ := io.ReadAll(resp.Body)
	var response types.Response
	json.Unmarshal(body, &response)

	assert.False(t, response.Success)
	assert.Contains(t, response.Error, "Format zahteva nije ispravan")
}

func TestCreateActuaryInvalidData(t *testing.T) {
	// Mock validate.Struct to return an error
	oldValidate := validate
	defer func() { validate = oldValidate }()

	// Setup
	app := fiber.New()
	controller := NewActuaryController()
	app.Post("/actuaries", controller.CreateActuary)

	// Create test request with data that will fail validation
	actuaryDTO := dto.ActuaryDTO{
		UserID:       123,
		Department:   "Trading",
		LimitAmount:  1000,
		UsedLimit:    0,
		NeedApproval: false,
	}
	reqBody, _ := json.Marshal(actuaryDTO)

	req := httptest.NewRequest(http.MethodPost, "/actuaries", bytes.NewBuffer(reqBody))
	req.Header.Set("Content-Type", "application/json")
	resp, _ := app.Test(req)

	// Verify
	assert.Equal(t, 400, resp.StatusCode)

	// Check response body
	body, _ := io.ReadAll(resp.Body)
	var response types.Response
	json.Unmarshal(body, &response)

	assert.False(t, response.Success)
	assert.Contains(t, response.Error, "Poslati podaci nisu validni")
}

func TestGetActuaryByIDInvalidID(t *testing.T) {
	// Setup
	app := fiber.New()
	controller := NewActuaryController()
	app.Get("/actuaries/:id", controller.GetActuaryByID)

	// Execute with non-existent ID
	req := httptest.NewRequest(http.MethodGet, "/actuaries/999999", nil)
	resp, _ := app.Test(req)

	// Verify - Since we're not mocking DB, we expect 500 for DB error
	assert.Equal(t, 404, resp.StatusCode)

	body, _ := io.ReadAll(resp.Body)
	var response types.Response
	json.Unmarshal(body, &response)

	assert.False(t, response.Success)
	assert.Contains(t, response.Error, "Aktuar nije pronadjen")
}

func TestChangeAgentLimitsInvalidID(t *testing.T) {
	// Setup
	app := fiber.New()
	controller := NewActuaryController()
	app.Put("/actuaries/:ID/limit", controller.ChangeAgentLimits)

	// Execute with non-existent ID
	updateData := dto.UpdateActuaryDTO{
		LimitAmount: stringPtr("5000"),
		ResetLimit:  true,
	}
	reqBody, _ := json.Marshal(updateData)

	req := httptest.NewRequest(http.MethodPut, "/actuaries/999999/limit", bytes.NewBuffer(reqBody))
	req.Header.Set("Content-Type", "application/json")
	resp, _ := app.Test(req)

	// Verify
	assert.Equal(t, 404, resp.StatusCode)

	body, _ := io.ReadAll(resp.Body)
	var response types.Response
	json.Unmarshal(body, &response)

	assert.False(t, response.Success)
	assert.Contains(t, response.Error, "Aktuar nije pronadjen")
}

func TestChangeAgentLimitsInvalidFormat(t *testing.T) {
	// Setup

	db.DB.Create(&types.Actuary{
		UserID:       123,
		Department:   "Finance",
		LimitAmount:  1000.0,
		UsedLimit:    100.0,
		NeedApproval: false,
	})

	app := fiber.New()
	controller := NewActuaryController()
	app.Put("/actuaries/:ID/limit", controller.ChangeAgentLimits)

	// Invalid JSON format
	reqBody := `{"limit_amount": "not-a-number" "reset_limit": true}`

	req := httptest.NewRequest(http.MethodPut, "/actuaries/1/limit", bytes.NewBufferString(reqBody))
	req.Header.Set("Content-Type", "application/json")
	resp, _ := app.Test(req)

	// Verify
	assert.Equal(t, 400, resp.StatusCode)

	body, _ := io.ReadAll(resp.Body)
	var response types.Response
	json.Unmarshal(body, &response)

	assert.False(t, response.Success)
	assert.Contains(t, response.Error, "Neispravan format podataka")
}

func TestResetActuaryLimitInvalidID(t *testing.T) {
	// Setup
	app := fiber.New()
	controller := NewActuaryController()
	app.Put("/actuaries/:ID/reset-used-limit", controller.ResetActuaryLimit)

	// Execute with non-existent ID
	req := httptest.NewRequest(http.MethodPut, "/actuaries/999999/reset-used-limit", nil)
	resp, _ := app.Test(req)

	// Verify
	assert.Equal(t, 404, resp.StatusCode)

	body, _ := io.ReadAll(resp.Body)
	var response types.Response
	json.Unmarshal(body, &response)

	assert.False(t, response.Success)
	assert.Contains(t, response.Error, "Aktuar nije pronadjen")
}

func TestNewActuaryController(t *testing.T) {
	// Verify that the constructor returns a non-nil controller
	controller := NewActuaryController()
	assert.NotNil(t, controller)
}

// Helper function and types

type mockValidator struct {
	shouldFail bool
}

func (m *mockValidator) Struct(s interface{}) error {
	if m.shouldFail {
		return &mockValidationError{}
	}
	return nil
}

type mockValidationError struct{}

func (m *mockValidationError) Error() string {
	return "validation error"
}

func stringPtr(s string) *string {
	return &s
}
