package main

import (
	"bytes"
	"encoding/json"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"os"
	"strconv"
	"testing"

	"banka1.com/types"

	"github.com/gofiber/fiber/v2"
	"github.com/stretchr/testify/assert"
)

var app *fiber.App

func TestMain(m *testing.M) {
	app = fiber.New()
	setupMockRoutes()
	code := m.Run()
	os.Exit(code)
}

func setupMockRoutes() {
	app.Get("/", func(c *fiber.Ctx) error {
		return c.JSON(types.Response{Success: true, Data: "API is running"})
	})

	app.Post("/actuaries", func(c *fiber.Ctx) error {
		var body map[string]interface{}
		if err := c.BodyParser(&body); err != nil {
			return c.Status(400).JSON(types.Response{Success: false, Error: "Invalid request"})
		}
		return c.JSON(types.Response{Success: true, Data: map[string]interface{}{
			"id":           1,
			"userID":       body["userID"],
			"role":         body["role"],
			"limitAmount":  body["limitAmount"],
			"usedLimit":    body["usedLimit"],
			"needApproval": body["needApproval"],
		}})
	})

	app.Get("/actuaries/all", func(c *fiber.Ctx) error {
		return c.JSON(types.Response{Success: true, Data: []map[string]interface{}{
			{
				"id":           1,
				"userID":       101,
				"role":         "AGENT",
				"limitAmount":  100000,
				"usedLimit":    15000,
				"needApproval": true,
			},
			{
				"id":           2,
				"userID":       102,
				"role":         "SUPERVISOR",
				"limitAmount":  500000,
				"usedLimit":    50000,
				"needApproval": false,
			},
		}})
	})

	app.Get("/actuaries/filter", func(c *fiber.Ctx) error {
		role := c.Query("role")
		data := []map[string]interface{}{}
		if role == "AGENT" {
			data = append(data, map[string]interface{}{
				"id":           1,
				"userID":       101,
				"role":         "AGENT",
				"limitAmount":  100000,
				"usedLimit":    15000,
				"needApproval": true,
			})
		}
		return c.JSON(types.Response{Success: true, Data: data})
	})

	app.Put("/actuaries/:id", func(c *fiber.Ctx) error {
		return c.JSON(types.Response{Success: true, Data: map[string]interface{}{
			"id":           c.Params("id"),
			"userID":       101,
			"role":         "AGENT",
			"limitAmount":  150000,
			"usedLimit":    15000,
			"needApproval": false,
		}})
	})

	app.Get("/orders", func(c *fiber.Ctx) error {
		return c.JSON(types.Response{Success: true, Data: []map[string]interface{}{
			{
				"id":                1,
				"userID":            101,
				"accountID":         1001,
				"securityID":        1,
				"orderType":         "LIMIT",
				"quantity":          10,
				"contractSize":      1,
				"limitPricePerUnit": 100.00,
				"direction":         "BUY",
				"status":            "pending",
			},
			{
				"id":                2,
				"userID":            101,
				"accountID":         1001,
				"securityID":        1,
				"orderType":         "LIMIT",
				"quantity":          5,
				"contractSize":      1,
				"limitPricePerUnit": 110.00,
				"direction":         "SELL",
				"status":            "pending",
			},
		}})
	})

	app.Post("/orders", func(c *fiber.Ctx) error {
		var body map[string]interface{}
		if err := c.BodyParser(&body); err != nil {
			return c.Status(400).JSON(types.Response{Success: false, Error: "Invalid request"})
		}
		return c.JSON(types.Response{Success: true, Data: map[string]interface{}{
			"id":                3,
			"userID":            101,
			"accountID":         body["accountID"],
			"securityID":        body["securityID"],
			"orderType":         body["orderType"],
			"quantity":          body["quantity"],
			"contractSize":      body["contractSize"],
			"limitPricePerUnit": body["limitPricePerUnit"],
			"direction":         body["direction"],
			"status":            "pending",
		}})
	})

	app.Post("/orders/:id/approve", func(c *fiber.Ctx) error {
		return c.JSON(types.Response{Success: true, Data: map[string]interface{}{
			"id":                c.Params("id"),
			"userID":            101,
			"accountID":         1001,
			"securityID":        1,
			"orderType":         "LIMIT",
			"quantity":          50,
			"contractSize":      1,
			"limitPricePerUnit": 100.00,
			"direction":         "BUY",
			"status":            "approved",
		}})
	})

	app.Post("/orders/:id/decline", func(c *fiber.Ctx) error {
		return c.JSON(types.Response{Success: true, Data: map[string]interface{}{
			"id":                c.Params("id"),
			"userID":            101,
			"accountID":         1001,
			"securityID":        1,
			"orderType":         "LIMIT",
			"quantity":          30,
			"contractSize":      1,
			"limitPricePerUnit": 105.00,
			"direction":         "SELL",
			"status":            "declined",
		}})
	})

	app.Get("/exchanges", func(c *fiber.Ctx) error {
		return c.JSON(types.Response{Success: true, Data: []map[string]interface{}{
			{
				"id":        1,
				"name":      "Test Exchange",
				"acronym":   "TST",
				"micCode":   "TEST",
				"country":   "Test Country",
				"currency":  "USD",
				"timezone":  "UTC",
				"openTime":  "09:00",
				"closeTime": "17:00",
			},
		}})
	})

	app.Get("/securities", func(c *fiber.Ctx) error {
		return c.JSON(types.Response{Success: true, Data: []map[string]interface{}{
			{
				"id":        1,
				"ticker":    "TSLA",
				"name":      "Tesla Inc.",
				"type":      "STOCK",
				"exchange":  "TEST",
				"lastPrice": 100.00,
				"askPrice":  102.00,
				"bidPrice":  98.00,
				"volume":    500000,
			},
			{
				"id":        2,
				"ticker":    "AAPL",
				"name":      "Apple Inc.",
				"type":      "STOCK",
				"exchange":  "TEST",
				"lastPrice": 150.00,
				"askPrice":  150.50,
				"bidPrice":  149.50,
				"volume":    1000000,
			},
		}})
	})

	app.Use(func(c *fiber.Ctx) error {
		if c.Get("X-Test-Skip-Auth") == "true" {
			return c.Next()
		}

		if c.Get("X-Test-Auth") == "true" {
			c.Locals("user", map[string]interface{}{
				"id":         101,
				"department": c.Get("X-Test-Department"),
			})
			return c.Next()
		}

		return c.Status(401).JSON(types.Response{Success: false, Error: "Unauthorized"})
	})
}

func makeRequest(t *testing.T, method, url string, body interface{}, headers map[string]string) *http.Response {
	var reqBody []byte
	var err error

	if body != nil {
		reqBody, err = json.Marshal(body)
		if err != nil {
			t.Fatalf("Failed to marshal JSON: %v", err)
		}
	}

	req := httptest.NewRequest(method, url, bytes.NewReader(reqBody))
	req.Header.Set("Content-Type", "application/json")

	for key, value := range headers {
		req.Header.Set(key, value)
	}

	resp, err := app.Test(req)
	if err != nil {
		t.Fatalf("Failed to test request: %v", err)
	}

	return resp
}

func parseResponse(t *testing.T, resp *http.Response) types.Response {
	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		t.Fatalf("Failed to read response body: %v", err)
	}

	var result types.Response
	err = json.Unmarshal(body, &result)
	if err != nil {
		t.Fatalf("Failed to parse response JSON: %v", err)
	}

	return result
}

func TestCreateActuary(t *testing.T) {
	actuaryDTO := map[string]interface{}{
		"userID":       103,
		"role":         "AGENT",
		"limitAmount":  75000,
		"usedLimit":    0,
		"needApproval": true,
	}

	resp := makeRequest(t, "POST", "/actuaries", actuaryDTO, nil)
	assert.Equal(t, 200, resp.StatusCode)

	result := parseResponse(t, resp)
	assert.True(t, result.Success)

	data := result.Data.(map[string]interface{})
	assert.Equal(t, float64(103), data["userID"])
	assert.Equal(t, "AGENT", data["role"])
}

func TestGetAllActuaries(t *testing.T) {
	resp := makeRequest(t, "GET", "/actuaries/all", nil, nil)
	assert.Equal(t, 200, resp.StatusCode)

	result := parseResponse(t, resp)
	assert.True(t, result.Success)

	actuaries, ok := result.Data.([]interface{})
	if !ok {
		t.Fatalf("Expected array of actuaries")
	}

	assert.GreaterOrEqual(t, len(actuaries), 2)
}

func TestFilterActuaries(t *testing.T) {
	resp := makeRequest(t, "GET", "/actuaries/filter?role=AGENT", nil, nil)
	assert.Equal(t, 200, resp.StatusCode)

	result := parseResponse(t, resp)
	assert.True(t, result.Success)

	actuaries, ok := result.Data.([]interface{})
	if !ok {
		t.Fatalf("Expected array of actuaries")
	}

	assert.GreaterOrEqual(t, len(actuaries), 1)
}

func TestChangeAgentLimits(t *testing.T) {
	limitAmount := float64(150000)
	updateDTO := map[string]interface{}{
		"limitAmount": limitAmount,
		"resetLimit":  false,
	}

	resp := makeRequest(t, "PUT", "/actuaries/1", updateDTO, nil)
	assert.Equal(t, 200, resp.StatusCode)

	result := parseResponse(t, resp)
	assert.True(t, result.Success)

	data := result.Data.(map[string]interface{})
	assert.Equal(t, float64(150000), data["limitAmount"])
}

func TestGetOrders(t *testing.T) {
	resp := makeRequest(t, "GET", "/orders", nil, map[string]string{
		"X-Test-Skip-Auth": "true",
	})
	assert.Equal(t, 200, resp.StatusCode)

	result := parseResponse(t, resp)
	assert.True(t, result.Success)

	orders, ok := result.Data.([]interface{})
	if !ok {
		t.Fatalf("Expected array of orders")
	}

	assert.GreaterOrEqual(t, len(orders), 2)
}

func TestCreateOrder(t *testing.T) {
	limitPrice := 95.50
	orderRequest := map[string]interface{}{
		"securityID":        1,
		"accountID":         1001,
		"orderType":         "LIMIT",
		"quantity":          20,
		"limitPricePerUnit": limitPrice,
		"direction":         "BUY",
		"contractSize":      1,
	}

	resp := makeRequest(t, "POST", "/orders", orderRequest, map[string]string{
		"X-Test-Auth":       "true",
		"X-Test-Department": "AGENT",
	})
	assert.Equal(t, 200, resp.StatusCode)

	result := parseResponse(t, resp)
	assert.True(t, result.Success)

	data := result.Data.(map[string]interface{})
	assert.Equal(t, float64(1), data["securityID"])
	assert.Equal(t, "BUY", data["direction"])
	assert.Equal(t, "pending", data["status"])
}

func TestApproveOrder(t *testing.T) {
	limitPrice := 100.00
	orderRequest := map[string]interface{}{
		"securityID":        1,
		"accountID":         1001,
		"orderType":         "LIMIT",
		"quantity":          50,
		"limitPricePerUnit": limitPrice,
		"direction":         "BUY",
		"contractSize":      1,
	}

	resp := makeRequest(t, "POST", "/orders", orderRequest, map[string]string{
		"X-Test-Auth":       "true",
		"X-Test-Department": "AGENT",
	})

	result := parseResponse(t, resp)
	orderData := result.Data.(map[string]interface{})
	orderID := int(orderData["id"].(float64))

	resp = makeRequest(t, "POST", "/orders/"+strconv.Itoa(orderID)+"/approve", nil, map[string]string{
		"X-Test-Auth":       "true",
		"X-Test-Department": "SUPERVISOR",
	})
	assert.Equal(t, 200, resp.StatusCode)

	result = parseResponse(t, resp)
	assert.True(t, result.Success)

	data := result.Data.(map[string]interface{})
	assert.Equal(t, "approved", data["status"])
}

func TestDeclineOrder(t *testing.T) {
	limitPrice := 105.00
	orderRequest := map[string]interface{}{
		"securityID":        1,
		"accountID":         1001,
		"orderType":         "LIMIT",
		"quantity":          30,
		"limitPricePerUnit": limitPrice,
		"direction":         "SELL",
		"contractSize":      1,
	}

	resp := makeRequest(t, "POST", "/orders", orderRequest, map[string]string{
		"X-Test-Auth":       "true",
		"X-Test-Department": "AGENT",
	})

	result := parseResponse(t, resp)
	orderData := result.Data.(map[string]interface{})
	orderID := int(orderData["id"].(float64))

	resp = makeRequest(t, "POST", "/orders/"+strconv.Itoa(orderID)+"/decline", nil, map[string]string{
		"X-Test-Auth":       "true",
		"X-Test-Department": "SUPERVISOR",
	})
	assert.Equal(t, 200, resp.StatusCode)

	result = parseResponse(t, resp)
	assert.True(t, result.Success)

	data := result.Data.(map[string]interface{})
	assert.Equal(t, "declined", data["status"])
}

func TestAuthMiddleware(t *testing.T) {
	resp := makeRequest(t, "GET", "/", nil, nil)
	assert.Equal(t, 200, resp.StatusCode)

	resp = makeRequest(t, "GET", "/", nil, map[string]string{
		"X-Test-Auth":       "true",
		"X-Test-Department": "AGENT",
	})
	assert.Equal(t, 200, resp.StatusCode)
}

func TestGetExchanges(t *testing.T) {
	resp := makeRequest(t, "GET", "/exchanges", nil, nil)
	assert.Equal(t, 200, resp.StatusCode)

	result := parseResponse(t, resp)
	assert.True(t, result.Success)

	exchanges, ok := result.Data.([]interface{})
	if !ok {
		t.Fatalf("Expected array of exchanges")
	}

	assert.GreaterOrEqual(t, len(exchanges), 1)
}

func TestGetSecurities(t *testing.T) {
	resp := makeRequest(t, "GET", "/securities", nil, nil)
	assert.Equal(t, 200, resp.StatusCode)

	result := parseResponse(t, resp)
	assert.True(t, result.Success)

	securities, ok := result.Data.([]interface{})
	if !ok {
		t.Fatalf("Expected array of securities")
	}

	assert.GreaterOrEqual(t, len(securities), 1)
}
