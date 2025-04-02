package routes

import (
	"banka1.com/controllers"
	"github.com/gofiber/fiber/v2"
)

func Setup(app *fiber.App) {

	actuaryController := controllers.NewActuaryController()

	portfolioController := controllers.NewPortfolioController()

	app.Post("/actuaries", actuaryController.CreateActuary)

	app.Get("/actuaries/all", actuaryController.GetAllActuaries)

	app.Put("/actuaries/:ID/reset-used-limit", actuaryController.ResetActuaryLimit)

	app.Put("actuaries/:ID/limit", actuaryController.ChangeAgentLimits)

	app.Get("/actuaries/filter", actuaryController.FilterActuaries)

	app.Get("/securities/:id", portfolioController.GetUserSecurities)
}
