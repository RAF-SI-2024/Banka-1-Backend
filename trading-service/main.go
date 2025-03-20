package main

import (
	"os"
	"time"

	"banka1.com/listings/futures"
	"banka1.com/middlewares"
	"banka1.com/routes"

	"banka1.com/controllers"
	"banka1.com/db"
	"banka1.com/exchanges"
	"banka1.com/listings/finhub"
	"banka1.com/listings/forex"
	"banka1.com/listings/options"
	"banka1.com/listings/stocks"
	"banka1.com/orders"
	"banka1.com/types"
	"github.com/gofiber/fiber/v2"
	"github.com/joho/godotenv"

	"log"
)

func main() {

	err := godotenv.Load()
	if err != nil {
		panic("Error loading .env file")
	}

	db.Init()
	db.StartScheduler()

	err = exchanges.LoadDefaultExchanges()
	if err != nil {
		log.Printf("Warning: Failed to load exchanges: %v", err)
	}

	go func() {
		log.Println("Starting to load default stocks...")
		stocks.LoadDefaultStocks()
		log.Println("Finished loading default stocks")
	}()

	func() {
		log.Println("Starting to load default forex pairs...")
		forex.LoadDefaultForexPairs()
		log.Println("Finished loading default forex pairs")
	}()

	func() {
		log.Println("Starting to load default futures...")
		err = futures.LoadDefaultFutures()
		if err != nil {
			log.Printf("Warning: Failed to load futures: %v", err)
		}
		log.Println("Finished loading default futures")
	}()

	func() {
		log.Println("Starting to load default options...")
		err = options.LoadAllOptions()
		if err != nil {
			log.Printf("Warning: Failed to load options: %v", err)
		}
		log.Println("Finished loading default options")
	}()

	app := fiber.New()

	app.Use(func(c *fiber.Ctx) error {
		c.Set("Access-Control-Allow-Origin", "*")
		c.Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE")
		return c.Next()
	})

	routes.Setup(app)

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

	app.Get("/exchanges", func(c *fiber.Ctx) error {
		/*
			Function to get all the exchanges from the database
			Model:
				ID        uint   	UNIQUE
				Name      string
				Acronym   string
				MicCode   string 	UNIQUE
				Country   string
				Currency  string
				Timezone  string
				OpenTime  string
				CloseTime string
		*/
		var exchanges []types.Exchange

		if result := db.DB.Find(&exchanges); result.Error != nil {
			return c.Status(500).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "Failed to fetch exchanges: " + result.Error.Error(),
			})
		}

		return c.JSON(types.Response{
			Success: true,
			Data:    exchanges,
			Error:   "",
		})
	})

	app.Get("/exchanges/:id", func(c *fiber.Ctx) error {
		/*
			Get exchange by ID.
			ID is a unique identifier for the exchange.
			ID is assigned in the code and is not in the exchamges.csv data.
		*/
		id := c.Params("id")

		var exchange types.Exchange

		if result := db.DB.First(&exchange, id); result.Error != nil {
			return c.Status(404).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "ExchangeMic not found with ID: " + id,
			})
		}

		return c.JSON(types.Response{
			Success: true,
			Data:    exchange,
			Error:   "",
		})
	})

	app.Get("/exchanges/mic/:micCode", func(c *fiber.Ctx) error {
		/*
			Get exchange by MIC code.
			Mic code is a Unique identifier for the exchange.
		*/
		micCode := c.Params("micCode")

		var exchange types.Exchange

		if result := db.DB.Where("mic_code = ?", micCode).First(&exchange); result.Error != nil {
			return c.Status(404).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "ExchangeMic not found with MIC code: " + micCode,
			})
		}

		return c.JSON(types.Response{
			Success: true,
			Data:    exchange,
			Error:   "",
		})
	})

	app.Get("/exchanges/acronym/:acronym", func(c *fiber.Ctx) error {
		/*
			Get exchange by acronym.
			Acronym is a short form of the exchange name.
			Acronym is usually unique BUT DOESNT HAVE TO BE!
		*/
		acronym := c.Params("acronym")

		var exchange types.Exchange

		if result := db.DB.Where("acronym = ?", acronym).First(&exchange); result.Error != nil {
			return c.Status(404).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "ExchangeMic not found with acronym: " + acronym,
			})
		}

		return c.JSON(types.Response{
			Success: true,
			Data:    exchange,
			Error:   "",
		})
	})

	app.Get("/stocks", func(c *fiber.Ctx) error {
		var listings []types.Listing

		if result := db.DB.Preload("Exchange").Where("type = ?", "Stock").Find(&listings); result.Error != nil {
			return c.Status(500).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "Failed to fetch stocks: " + result.Error.Error(),
			})
		}

		return c.JSON(types.Response{
			Success: true,
			Data:    listings,
			Error:   "",
		})
	})

	app.Get("/stocks/:ticker", func(c *fiber.Ctx) error {
		ticker := c.Params("ticker")

		var listing types.Listing
		if result := db.DB.Preload("Exchange").Where("ticker = ? AND type = ?", ticker, "Stock").First(&listing); result.Error != nil {
			return c.Status(404).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "Stock not found with ticker: " + ticker,
			})
		}

		var stock types.Stock
		if result := db.DB.Preload("Listing.Exchange").Where("listing_id = ?", listing.ID).First(&stock); result.Error != nil {
			return c.Status(500).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "Failed to fetch stock details: " + result.Error.Error(),
			})
		}

		return c.JSON(types.Response{
			Success: true,
			Data: map[string]interface{}{
				"listing": listing,
				"details": stock,
			},
			Error: "",
		})
	})

	app.Get("/stocks/:ticker/history/first", func(c *fiber.Ctx) error {
		ticker := c.Params("ticker")

		// Find the listing first
		var listing types.Listing
		if result := db.DB.Where("ticker = ? AND type = ?", ticker, "Stock").First(&listing); result.Error != nil {
			return c.Status(404).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "Stock not found with ticker: " + ticker,
			})
		}

		history, err := finhub.GetHistoricalPriceFirst(ticker, listing.Subtype)
		if err != nil {
			return c.Status(500).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "Failed to fetch historical price data: " + err.Error(),
			})
		}

		return c.JSON(types.Response{
			Success: true,
			Data:    history,
			Error:   "",
		})
	})
	app.Get("/stocks/:ticker/history/:date", func(c *fiber.Ctx) error {
		ticker := c.Params("ticker")
		date := c.Params("date")

		// Find the listing first
		var listing types.Listing
		if result := db.DB.Where("ticker = ? AND type = ?", ticker, "Stock").First(&listing); result.Error != nil {
			return c.Status(404).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "Stock not found with ticker: " + ticker,
			})
		}

		// Parse date parameter
		dateTime, err := time.Parse("2006-01-02", date)
		if err != nil {
			return c.Status(400).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "Invalid date format. Use YYYY-MM-DD",
			})
		}

		history, err := finhub.GetHistoricalPriceDate(ticker, listing.Subtype, dateTime)
		if err != nil {
			return c.Status(500).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "Failed to fetch historical price data: " + err.Error(),
			})
		}

		return c.JSON(types.Response{
			Success: true,
			Data:    history,
			Error:   "",
		})
	})

	app.Get("/stocks/:ticker/history", func(c *fiber.Ctx) error {
		ticker := c.Params("ticker")
		// Parse date range parameters
		startDateStr := c.Query("startDate", "")
		endDateStr := c.Query("endDate", "")

		var listing types.Listing
		if result := db.DB.Where("ticker = ? AND type = ?", ticker, "Stock").First(&listing); result.Error != nil {
			return c.Status(404).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "Stock not found with ticker: " + ticker,
			})
		}

		var startDate, endDate time.Time
		var err error

		if startDateStr != "" {
			startDate, err = time.Parse("2006-01-02", startDateStr)
			if err != nil {
				return c.Status(400).JSON(types.Response{
					Success: false,
					Data:    nil,
					Error:   "Invalid startDate format. Use YYYY-MM-DD",
				})
			}
		} else {
			// Default to 30 days ago
			startDate = time.Now().AddDate(0, 0, -30)
		}

		if endDateStr != "" {
			endDate, err = time.Parse("2006-01-02", endDateStr)
			if err != nil {
				return c.Status(400).JSON(types.Response{
					Success: false,
					Data:    nil,
					Error:   "Invalid endDate format. Use YYYY-MM-DD",
				})
			}
		} else {
			// Default to today
			endDate = time.Now()
		}

		// Fetch historical price data
		var history []types.ListingDailyPriceInfo
		if result := db.DB.Where("listing_id = ? AND date BETWEEN ? AND ?",
			listing.ID, startDate, endDate).Order("date").Find(&history); result.Error != nil {
			return c.Status(500).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "Failed to fetch historical price data: " + result.Error.Error(),
			})
		}

		return c.JSON(types.Response{
			Success: true,
			Data:    history,
			Error:   "",
		})
	})

	app.Get("/forex", func(c *fiber.Ctx) error {
		var listings []types.Listing

		if result := db.DB.Preload("Exchange").Where("type = ?", "Forex").Find(&listings); result.Error != nil {
			return c.Status(500).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "Failed to fetch stocks: " + result.Error.Error(),
			})
		}

		return c.JSON(types.Response{
			Success: true,
			Data:    listings,
			Error:   "",
		})
	})

	app.Get("/forex/:base/:quote", func(c *fiber.Ctx) error {

		base := c.Params("base")
		quote := c.Params("quote")
		ticker := base + "/" + quote

		var listing types.Listing
		if result := db.DB.Preload("Exchange").Where("ticker = ? AND type = ?", ticker, "Forex").First(&listing); result.Error != nil {
			return c.Status(404).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "Forex pair not found with ticker: " + ticker,
			})
		}

		var forexPair types.ForexPair
		if result := db.DB.Preload("Listing.Exchange").Where("listing_id = ?", listing.ID).First(&forexPair); result.Error != nil {
			return c.Status(500).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "Failed to fetch stock details: " + result.Error.Error(),
			})
		}

		return c.JSON(types.Response{
			Success: true,
			Data: map[string]interface{}{
				"listing": listing,
				"details": forexPair,
			},
			Error: "",
		})
	})

	app.Get("/forex/:base/:quote/history", func(c *fiber.Ctx) error {
		base := c.Params("base")
		quote := c.Params("quote")
		ticker := base + "/" + quote
		startDateStr := c.Query("startDate", "")
		endDateStr := c.Query("endDate", "")
		var listing types.Listing
		if result := db.DB.Where("ticker = ? AND type = ?", ticker, "Forex").First(&listing); result.Error != nil {
			return c.Status(404).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "Stock not found with ticker: " + ticker,
			})
		}

		var startDate, endDate time.Time
		var err error

		if startDateStr != "" {
			startDate, err = time.Parse("2006-01-02", startDateStr)
			if err != nil {
				return c.Status(400).JSON(types.Response{
					Success: false,
					Data:    nil,
					Error:   "Invalid startDate format. Use YYYY-MM-DD",
				})
			}
		} else {
			// Default to 30 days ago
			startDate = time.Now().AddDate(0, 0, -30)
		}

		if endDateStr != "" {
			endDate, err = time.Parse("2006-01-02", endDateStr)
			if err != nil {
				return c.Status(400).JSON(types.Response{
					Success: false,
					Data:    nil,
					Error:   "Invalid endDate format. Use YYYY-MM-DD",
				})
			}
		} else {
			// Default to today
			endDate = time.Now()
		}

		// Fetch historical price data
		var history []types.ListingDailyPriceInfo
		if result := db.DB.Where("listing_id = ? AND date BETWEEN ? AND ?",
			listing.ID, startDate, endDate).Order("date").Find(&history); result.Error != nil {
			return c.Status(500).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "Failed to fetch historical price data: " + result.Error.Error(),
			})
		}
		return c.JSON(types.Response{
			Success: true,
			Data:    history,
			Error:   "",
		})
	})

	app.Get("/future", func(c *fiber.Ctx) error {
		var listings []types.Listing

		if result := db.DB.Preload("Exchange").Where("type = ?", "Future").Find(&listings); result.Error != nil {
			return c.Status(500).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "Failed to fetch stocks: " + result.Error.Error(),
			})
		}

		return c.JSON(types.Response{
			Success: true,
			Data:    listings,
			Error:   "",
		})
	})

	app.Get("/future/:ticker", func(c *fiber.Ctx) error {
		ticker := c.Params("ticker")

		var listing types.Listing
		if result := db.DB.Preload("Exchange").Where("ticker = ? AND type = ?", ticker, "Stock").First(&listing); result.Error != nil {
			return c.Status(404).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "Stock not found with ticker: " + ticker,
			})
		}

		var future types.FuturesContract
		if result := db.DB.Preload("Listing.Exchange").Where("listing_id = ?", listing.ID).First(&future); result.Error != nil {
			return c.Status(500).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "Failed to fetch stock details: " + result.Error.Error(),
			})
		}

		return c.JSON(types.Response{
			Success: true,
			Data: map[string]interface{}{
				"listing": listing,
				"details": future,
			},
			Error: "",
		})
	})

	app.Get("/securities/available", getSecurities())

	app.Get("/securities", getSecurities())

	app.Post("/actuaries", controllers.NewActuaryController().CreateActuary)
	app.Get("/actuaries", controllers.NewActuaryController().GetAllActuaries)
	app.Put("/actuaries/:ID", controllers.NewActuaryController().ChangeAgentLimits)
	app.Get("/actuaries/filter", controllers.NewActuaryController().FilterActuaries)

	orders.InitRoutes(app)

	port := os.Getenv("PORT")
	log.Fatal(app.Listen(":" + port))
}

func getSecurities() func(c *fiber.Ctx) error {
	return func(c *fiber.Ctx) error {
		var listings []types.Listing
		if result := db.DB.Preload("Exchange").Find(&listings); result.Error != nil {
			return c.Status(500).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "Failed to fetch securities: " + result.Error.Error(),
			})
		}
		var securities []types.Security
		for _, listing := range listings {
			security, err := listingToSecurity(&listing)
			if err != nil || security == nil {
				return c.Status(500).JSON(types.Response{
					Success: false,
					Data:    nil,
					Error:   "Failed to convert listing to security: " + err.Error(),
				})
			}
			securities = append(securities, *security)
		}

		return c.JSON(types.Response{
			Success: true,
			Data:    securities,
			Error:   "",
		})
	}
}

func listingToSecurity(l *types.Listing) (*types.Security, error) {
	var security types.Security
	switch l.Type {
	case "Stock":
		{
			security = types.Security{
				Ticker:    l.Ticker,
				Name:      l.Name,
				Type:      l.Type,
				Exchange:  l.Exchange.Name,
				LastPrice: float64(l.Price),
				AskPrice:  float64(l.Ask),
				BidPrice:  float64(l.Bid),
				Volume:    int64(l.ContractSize * 10),
			}
		}
	case "Forex":
		{
			security = types.Security{
				Ticker:    l.Ticker,
				Name:      l.Name,
				Type:      l.Type,
				Exchange:  l.Exchange.Name,
				LastPrice: float64(l.Price),
				AskPrice:  float64(l.Ask),
				BidPrice:  float64(l.Bid),
				Volume:    int64(l.ContractSize * 10),
			}
		}
	case "Future":
		{
			var future types.FuturesContract
			if result := db.DB.Where("listing_id = ?", l.ID).First(&future); result.Error != nil {
				return nil, result.Error
			}
			settlementDate := future.SettlementDate.Format("2006-01-02")
			security = types.Security{
				Ticker:         l.Ticker,
				Name:           l.Name,
				Type:           l.Type,
				Exchange:       l.Exchange.Name,
				LastPrice:      float64(l.Price),
				AskPrice:       float64(l.Ask),
				BidPrice:       float64(l.Bid),
				Volume:         int64(l.ContractSize * 10),
				SettlementDate: &settlementDate,
			}
		}
	case "Option":
		{
		}
	}
	return &security, nil
}
