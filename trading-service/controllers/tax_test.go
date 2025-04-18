package controllers

import (
	"banka1.com/types"
	"encoding/json"
	"github.com/gofiber/fiber/v2"
	"github.com/stretchr/testify/assert"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestRunTax(t *testing.T) {
	// Setup
	app := fiber.New()
	taxController := NewTaxController()
	app.Post("/tax/run", taxController.RunTax)

	// Execute
	req := httptest.NewRequest(http.MethodPost, "/tax/run", nil)
	resp, _ := app.Test(req)
	defer resp.Body.Close()

	// Assertions
	assert.Equal(t, 500, resp.StatusCode)

	// Parse response body
	body, _ := io.ReadAll(resp.Body)
	var response types.Response
	json.Unmarshal(body, &response)

	assert.False(t, response.Success)
	//assert.Equal(t, "Nije implementirano.", response.Error)
}

func TestGetAggregatedTaxForUser_InvalidUserID(t *testing.T) {
	// Setup
	app := fiber.New()
	taxController := NewTaxController()
	app.Get("/tax/dashboard/:userID", taxController.GetAggregatedTaxForUser)

	// Test case: Invalid user ID (not a number)
	req := httptest.NewRequest(http.MethodGet, "/tax/dashboard/invalid", nil)
	resp, _ := app.Test(req)
	defer resp.Body.Close()

	// Assertions
	assert.Equal(t, 400, resp.StatusCode)

	body, _ := io.ReadAll(resp.Body)
	var response types.Response
	json.Unmarshal(body, &response)

	assert.False(t, response.Success)
	assert.Contains(t, response.Error, "Neispravan userID parametar")

	// Test case: User ID <= 0
	req = httptest.NewRequest(http.MethodGet, "/tax/dashboard/0", nil)
	resp, _ = app.Test(req)
	defer resp.Body.Close()

	// Assertions
	assert.Equal(t, 400, resp.StatusCode)

	body, _ = io.ReadAll(resp.Body)
	json.Unmarshal(body, &response)

	assert.False(t, response.Success)
	assert.Contains(t, response.Error, "Neispravan userID parametar")
}

func TestInitTaxRoutes(t *testing.T) {
	// Setup
	app := fiber.New()

	// Execute InitTaxRoutes
	InitTaxRoutes(app)

	// This is primarily a smoke test to ensure no panics
	// We can also verify each route is registered by examining app.Stack()

	// Helper function to find routes by method and path
	findRoute := func(method, path string) bool {
		for _, routes := range app.Stack() {
			for _, route := range routes {
				if route.Method == method && strings.HasSuffix(route.Path, path) {
					return true
				}
			}
		}
		return false
	}

	// Verify routes are registered
	assert.True(t, findRoute("GET", "/tax"))
	assert.True(t, findRoute("POST", "/tax/run"))
	assert.True(t, findRoute("GET", "/tax/dashboard/:userID"))
}
