package tax

import (
	"banka1.com/db"
	"banka1.com/middlewares"
	"banka1.com/types"
	"github.com/gofiber/fiber/v2"
)

func GetTaxForAllUsers(c *fiber.Ctx) error {
	rows, err := db.DB.Raw(`WITH max_created_at AS (SELECT user_id, MAX(created_at) AS c FROM tax GROUP BY user_id)
SELECT user_id, taxable_profit, tax_amount, is_paid, actuary.id IS NOT NULL
FROM tax LEFT JOIN actuary USING (user_id)
WHERE month_year = (SELECT MAX(month_year) FROM tax)
AND created_at = (SELECT c FROM max_created_at WHERE max_created_at.user_id = tax.user_id)
AND NOT is_paid;`).Rows()
	defer rows.Close()
	if err != nil {
		return c.Status(400).JSON(types.Response{
			Success: false,
			Error:   "Neuspeo zahtev: " + err.Error(),
		})
	}
	responses := make([]types.TaxResponse, 0)
	for rows.Next() {
		var response types.TaxResponse
		err := rows.Scan(&response.UserID, &response.TaxableProfit, &response.TaxAmount, &response.IsPaid, &response.IsActuary)
		if err != nil {
			return c.Status(500).JSON(types.Response{
				Success: false,
				Error:   "Greska prilikom citanja redova iz baze: " + err.Error(),
			})
		}
		responses = append(responses, response)
	}
	return c.JSON(types.Response{
		Success: true,
		Data:    responses,
	})
}

func RunTax(c *fiber.Ctx) error {
	return c.Status(500).JSON(types.Response{
		Success: false,
		Error:   "Nije implementirano.",
	})
}

func InitRoutes(app *fiber.App) {
	app.Get("/tax", middlewares.Auth, middlewares.DepartmentCheck("SUPERVISOR"), GetTaxForAllUsers)
	app.Post("/tax/run", middlewares.Auth, middlewares.DepartmentCheck("SUPERVISOR"), RunTax)
}
