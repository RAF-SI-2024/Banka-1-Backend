package controllers

import (
	"banka1.com/db"
	"banka1.com/types"
	"bytes"
	"encoding/json"
	"fmt"
	"github.com/gofiber/fiber/v2"
	"github.com/stretchr/testify/assert"
	"io"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestInitPortfolioRoutes(t *testing.T) {
	app := fiber.New()
	InitPortfolioRoutes(app)

	findRoute := func(method, path string) bool {
		for methodRoutes := range app.Stack() {
			for _, route := range app.Stack()[methodRoutes] {
				if route.Method == method && route.Path == path {
					return true
				}
			}
		}
		return false
	}

	assert.True(t, findRoute("PUT", "/securities/public-count"))
	assert.True(t, findRoute("GET", "/portfolios"))
}

func TestUpdatePublicCountInvalidRequestBody(t *testing.T) {
	app := fiber.New()
	app.Put("/securities/public-count", NewPortfolioController().UpdatePublicCount)

	req := httptest.NewRequest(http.MethodPut, "/securities/public-count", bytes.NewBufferString(`{invalid json}`))
	req.Header.Set("Content-Type", "application/json")
	resp, _ := app.Test(req)

	assert.Equal(t, 400, resp.StatusCode)

	body, _ := io.ReadAll(resp.Body)
	var response types.Response
	_ = json.Unmarshal(body, &response)

	assert.False(t, response.Success)
	assert.Contains(t, response.Error, "Invalid request body")
}

func TestUpdatePublicCountNegativeValue(t *testing.T) {
	app := fiber.New()
	app.Put("/securities/public-count", NewPortfolioController().UpdatePublicCount)

	db.InitTestDatabase()
	portfolio := types.Portfolio{UserID: 10, Quantity: 5}
	db.DB.Create(&portfolio)

	payload := map[string]interface{}{
		"portfolio_id": portfolio.ID,
		"public":       -1,
	}
	body, _ := json.Marshal(payload)
	req := httptest.NewRequest(http.MethodPut, "/securities/public-count", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")

	resp, _ := app.Test(req)
	assert.Equal(t, 400, resp.StatusCode)
}

func TestUpdatePublicCountTooHigh(t *testing.T) {
	app := fiber.New()
	app.Put("/securities/public-count", NewPortfolioController().UpdatePublicCount)

	db.InitTestDatabase()
	portfolio := types.Portfolio{UserID: 11, Quantity: 5}
	db.DB.Create(&portfolio)

	payload := map[string]interface{}{
		"portfolio_id": portfolio.ID,
		"public":       10,
	}
	body, _ := json.Marshal(payload)
	req := httptest.NewRequest(http.MethodPut, "/securities/public-count", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")

	resp, _ := app.Test(req)
	assert.Equal(t, 400, resp.StatusCode)
}

func TestUpdatePublicCountSuccess(t *testing.T) {
	app := fiber.New()
	app.Put("/securities/public-count", NewPortfolioController().UpdatePublicCount)

	db.InitTestDatabase()
	// Make index be higher than anything the other tests use
	security := types.Security{ID: 10000001, Type: "Stock"}
	db.DB.Create(&security)
	portfolio := types.Portfolio{UserID: 12, Quantity: 5, PublicCount: 2, Security: security}
	db.DB.Create(&portfolio)

	payload := map[string]interface{}{
		"portfolio_id": portfolio.ID,
		"public":       3,
	}
	body, _ := json.Marshal(payload)
	req := httptest.NewRequest(http.MethodPut, "/securities/public-count", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")

	resp, _ := app.Test(req)
	assert.Equal(t, 200, resp.StatusCode)
	var response types.Response
	_ = json.NewDecoder(resp.Body).Decode(&response)
	fmt.Printf("Response: %+v\n", response)
	assert.True(t, response.Success)
	assert.Contains(t, response.Data, "Updated public count")
}

func TestUpdatePublicCountPortfolioNotFound(t *testing.T) {
	app := fiber.New()
	app.Put("/securities/public-count", NewPortfolioController().UpdatePublicCount)

	db.InitTestDatabase()

	payload := map[string]interface{}{
		"portfolio_id": 99999,
		"public":       1,
	}
	body, _ := json.Marshal(payload)
	req := httptest.NewRequest(http.MethodPut, "/securities/public-count", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")

	resp, _ := app.Test(req)
	assert.Equal(t, 404, resp.StatusCode)
}
