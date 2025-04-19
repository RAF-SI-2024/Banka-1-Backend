package controllers

import (
	"banka1.com/db"
	"banka1.com/types"
	"errors"
	"github.com/gofiber/fiber/v2"
	"gorm.io/gorm"
	"strconv"
)

type ExchangeController struct {
}

func NewExchangeController() *ExchangeController {
	return &ExchangeController{}
}

// GetAllExchanges godoc
//
//	@Summary		Preuzimanje svih berzi
//	@Description	Vraća listu svih berzi dostupnih u sistemu.
//	@Tags			Exchanges
//	@Produce		json
//	@Success		200	{object}	types.Response{data=[]types.Exchange}	"Lista svih berzi"
//	@Failure		500	{object}	types.Response							"Interna greška servera pri preuzimanju berzi"
//	@Router			/exchanges [get]
func (ec *ExchangeController) GetAllExchanges(c *fiber.Ctx) error {
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
}

// GetExchangeByID godoc
//
//	@Summary		Preuzimanje berze po ID-u
//	@Description	Vraća detalje specifične berze na osnovu njenog internog ID-ja.
//	@Tags			Exchanges
//	@Produce		json
//	@Param			id	path		int									true	"ID berze"
//	@Success		200	{object}	types.Response{data=types.Exchange}	"Detalji tražene berze"
//	@Failure		404	{object}	types.Response						"Berza sa datim ID-jem nije pronađena"
//	@Router			/exchanges/{id} [get]
func (ec *ExchangeController) GetExchangeByID(c *fiber.Ctx) error {
	/*
		Get exchange by ID.
		ID is a unique identifier for the exchange.
		ID is assigned in the code and is not in the exchamges.csv data.
	*/
	idParam := c.Params("id")
	id, err := strconv.Atoi(idParam)
	if err != nil {
		return c.Status(400).JSON(types.Response{
			Success: false,
			Data:    nil,
			Error:   "Neispravan format ID-ja",
		})
	}

	var exchange types.Exchange

	if result := db.DB.First(&exchange, id); result.Error != nil {
		if errors.Is(result.Error, gorm.ErrRecordNotFound) {
			return c.Status(404).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "Berza sa datim ID-jem nije pronađena",
			})
		}
		return c.Status(500).JSON(types.Response{
			Success: false,
			Data:    nil,
			Error:   "Greška pri preuzimanju berze: " + result.Error.Error(),
		})
	}
	return c.JSON(types.Response{
		Success: true,
		Data:    exchange,
		Error:   "",
	})
}

// GetExchangeByMIC godoc
//
//	@Summary		Preuzimanje berze po MIC kodu
//	@Description	Vraća detalje specifične berze na osnovu njenog jedinstvenog MIC koda.
//	@Tags			Exchanges
//	@Produce		json
//	@Param			micCode	path		string								true	"Market Identifier Code (MIC) berze"	example(XNAS)
//	@Success		200		{object}	types.Response{data=types.Exchange}	"Detalji tražene berze"
//	@Failure		404		{object}	types.Response						"Berza sa datim MIC kodom nije pronađena"
//	@Router			/exchanges/mic/{micCode} [get]
func (ec *ExchangeController) GetExchangeByMIC(c *fiber.Ctx) error {
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
}

// GetExchangeByAcronym godoc
//
//	@Summary		Preuzimanje berze po akronimu
//	@Description	Vraća detalje specifične berze na osnovu njenog akronima. Napomena: Akronim ne mora biti jedinstven. Vraća prvu pronađenu.
//	@Tags			Exchanges
//	@Produce		json
//	@Param			acronym	path		string								true	"Akronim berze"	example(NASDAQ)
//	@Success		200		{object}	types.Response{data=types.Exchange}	"Detalji pronađene berze"
//	@Failure		404		{object}	types.Response						"Berza sa datim akronimom nije pronađena"
//	@Router			/exchanges/acronym/{acronym} [get]
func (ec *ExchangeController) GetExchangeByAcronym(c *fiber.Ctx) error {
	/*
		Get exchange by acronym.
		Acronym is a short form of the exchange name.
		Acronym is usually unique BUT DOESN'T HAVE TO BE!
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
}

func InitExchangeRoutes(app *fiber.App) {
	ec := NewExchangeController()

	app.Get("/exchanges", ec.GetAllExchanges)
	app.Get("/exchanges/:id", ec.GetExchangeByID)
	app.Get("/exchanges/mic/:micCode", ec.GetExchangeByMIC)
	app.Get("/exchanges/acronym/:acronym", ec.GetExchangeByAcronym)
}
