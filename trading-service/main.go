package main

import (
	"banka1.com/routes"
	fiberSwagger "github.com/swaggo/fiber-swagger"

	"banka1.com/controllers/orders"

	"fmt"
	"os"
	"time"

	"banka1.com/cron"

	"banka1.com/middlewares"

	"banka1.com/broker"
	"banka1.com/db"
	"banka1.com/types"
	"github.com/gofiber/fiber/v2"
	"github.com/joho/godotenv"

	_ "banka1.com/docs"

	"log"
)

//	@title			Trading Service
//	@version		1.0
//	@description	Trading Service API

// @securityDefinitions.apikey	BearerAuth
// @in							header
// @name						Authorization
// @description				Unesite token. Primer: "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
func main() {
	err := godotenv.Load()
	fmt.Println("Do not merge this")
	if err != nil {
		panic("Error loading .env file")
	}

	broker.Connect(os.Getenv("MESSAGE_BROKER_NETWORK"), os.Getenv("MESSAGE_BROKER_HOST"))
	db.Init()

	cron.StartScheduler()

	broker.FailAllOTC()

	broker.StartListeners()

	app := fiber.New()

	app.Use(func(c *fiber.Ctx) error {
		c.Set("Access-Control-Allow-Origin", "*")
		c.Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE")
		return c.Next()
	})

	routes.SetupRoutes(app)

	app.Get("/", middlewares.Auth, middlewares.DepartmentCheck("AGENT"), func(c *fiber.Ctx) error {
		response := types.Response{
			Success: true,
			Data:    "Hello, World!",
			Error:   "",
		}
		return c.JSON(response)
	})

	app.Get("/health", func(c *fiber.Ctx) error {
		return c.SendStatus(200)
	})

	app.Get("/swagger/*", fiberSwagger.WrapHandler)

	ticker := time.NewTicker(5000 * time.Millisecond)
	done := make(chan bool)

	go func() {
		for {
			select {
			case <-done:
				return
			case <-ticker.C:
				checkUncompletedOrders()
			}
		}
	}()

	port := os.Getenv("LISTEN_PATH")
	log.Printf("Swagger UI available at http://localhost%s/swagger/index.html", port)
	log.Fatal(app.Listen(port))

	ticker.Stop()
	done <- true
}

func checkUncompletedOrders() {
	var undoneOrders []types.Order

	fmt.Println("Proveravanje neizvršenih naloga...")

	db.DB.Where("status = ? AND is_done = ?", "approved", false).Find(&undoneOrders)
	fmt.Printf("Pronadjeno %v neizvršenih naloga\n", len(undoneOrders))
	previousLength := -1

	for len(undoneOrders) > 0 && previousLength != len(undoneOrders) {
		fmt.Printf("Preostalo još %v neizvršenih naloga\n", len(undoneOrders))
		for _, order := range undoneOrders {
			if !orders.IsSettlementDateValid(&order) {
				fmt.Printf("Order %d automatski odbijen zbog isteka settlement datuma\n", order.ID)
				db.DB.Model(&order).Updates(map[string]interface{}{
					"status":          "declined",
					"is_done":         true,
					"remaining_parts": 0,
				})
				continue
			}

			if orders.CanExecuteAny(order) {
				orders.MatchOrder(order)
				break
			}
		}
		previousLength = len(undoneOrders)
		db.DB.Where("status = ? AND is_done = ?", "approved", false).Find(&undoneOrders)
	}
}
