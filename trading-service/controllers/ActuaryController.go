package controllers

import (
	"banka1.com/db"
	"banka1.com/dto"
	"banka1.com/services"
	"banka1.com/types"
	"github.com/gofiber/fiber/v2"
	"github.com/gofiber/fiber/v2/log"
	"strconv"
)

type ActuaryController struct {
}

func NewActuaryController() *ActuaryController {
	return &ActuaryController{}
}

type Employee struct {
	ID          int      `json:"id"`
	FirstName   string   `json:"firstName"`
	LastName    string   `json:"lastName"`
	Email       string   `json:"email"`
	Department  string   `json:"department"`
	Position    string   `json:"position"`
	Active      bool     `json:"active"`
	Permissions []string `json:"permissions"`
}

type APIResponse struct {
	Success bool       `json:"success"`
	Data    []Employee `json:"data"`
}

// CreateActuary godoc
//
//	@Summary		Kreiranje novog aktuara
//	@Description	Kreira novi aktuar na osnovu dostavljenih podataka.
//	@Tags			Actuaries
//	@Accept			json
//	@Produce		json
//	@Param			actuary	body		dto.ActuaryDTO						true	"Podaci za kreiranje aktuara"
//	@Success		201		{object}	types.Response{data=types.Actuary}	"Uspešno kreiran aktuar"
//	@Failure		400		{object}	types.Response						"Neispravan format unosa ili poslati podaci nisu validni."
//	@Failure		500		{object}	types.Response						"Greska u bazi."
//	@Router			/actuaries [post]
func (ac *ActuaryController) CreateActuary(c *fiber.Ctx) error {
	var actuaryDTO dto.ActuaryDTO

	if err := c.BodyParser(&actuaryDTO); err != nil {
		response := types.Response{
			Success: false,
			Data:    nil,
			Error:   "Format zahteva nije ispravan.",
		}
		return c.Status(400).JSON(response)
	}

	if err := validate.Struct(actuaryDTO); err != nil {
		response := types.Response{
			Success: false,
			Data:    nil,
			Error:   "Poslati podaci nisu validni.",
		}
		return c.Status(400).JSON(response)
	}

	// Početak transakcije
	tx := db.DB.Begin()

	actuary := types.Actuary{
		UserID:       actuaryDTO.UserID,
		Role:         actuaryDTO.Role,
		LimitAmount:  actuaryDTO.LimitAmount,
		UsedLimit:    actuaryDTO.UsedLimit,
		NeedApproval: actuaryDTO.NeedApproval,
	}

	if err := tx.Create(&actuary).Error; err != nil {
		tx.Rollback()
		return c.Status(500).JSON(types.Response{
			Success: false,
			Data:    nil,
			Error:   "Greška u bazi pri kreiranju aktuara.",
		})
	}

	// Commit transakcije
	tx.Commit()

	response := types.Response{
		Success: true,
		Data:    actuary,
		Error:   "",
	}

	return c.Status(201).JSON(response)
}

// GetAllActuaries godoc
//
//	@Summary		Dobavljanje svih aktuara
//	@Description	Vraća listu svih zapisa aktuara iz baze podataka.
//	@Tags			Actuaries
//	@Produce		json
//	@Success		200	{object}	types.Response{data=[]types.Actuary}	"Uspešno dobavljeni svi aktuari"
//	@Failure		500	{object}	types.Response							"Greška u bazi"
//	@Router			/actuaries [get]
func (ac *ActuaryController) GetAllActuaries(c *fiber.Ctx) error {
	var actuaries []types.Actuary
	result := db.DB.Find(&actuaries)
	if result.Error != nil {
		log.Infof("Database error: %v\n", result.Error)
		return c.Status(500).JSON(types.Response{
			Success: false,
			Error:   "Database error",
		})
	}

	return c.JSON(types.Response{
		Success: true,
		Data:    actuaries,
		Error:   "",
	})
}

func (ac *ActuaryController) GetActuaryByID(c *fiber.Ctx) error {
	id := c.Params("id") // Uzima ID iz URL-a

	var actuary types.Actuary
	result := db.DB.First(&actuary, id)
	if result.Error != nil {
		//if errors.Is(result.Error, gorm.ErrRecordNotFound) {
		//	return c.Status(404).JSON(types.Response{
		//		Success: false,
		//		Error:   "Actuary not found",
		//	})
		//}
		log.Infof("Database error: GetActuaryByID  %v\n", result.Error)
		return c.Status(500).JSON(types.Response{
			Success: false,
			Error:   "Database error",
		})
	}

	return c.JSON(types.Response{
		Success: true,
		Data:    actuary,
		Error:   "",
	})
}

// ChangeAgentLimits godoc
//
//	@Summary		Izmena limita aktuara
//	@Description	Ažurira iznos limita za određeni aktuar prema ID-ju.
//	@Tags			Actuaries
//	@Accept			json
//	@Produce		json
//	@Param			ID			path		string								true	"ID aktuara"
//	@Param			updateData	body		dto.UpdateActuaryDTO				true	"Podaci za ažuriranje limita"
//	@Success		200			{object}	types.Response{data=types.Actuary}	"Uspešno ažurirani limiti aktuara"
//	@Failure		400			{object}	types.Response						"Neispravan format podataka"
//	@Failure		404			{object}	types.Response						"Aktuar nije pronadjen"
//	@Router			/actuaries/{id} [put]
func (ac *ActuaryController) ChangeAgentLimits(c *fiber.Ctx) error {
	id := c.Params("ID")
	var actuary types.Actuary

	result := db.DB.First(&actuary, id)
	if result.Error != nil {
		return c.Status(404).JSON(types.Response{
			Success: false,
			Data:    nil,
			Error:   "Aktuar nije pronadjen",
		})
	}

	var updateData dto.UpdateActuaryDTO
	if err := c.BodyParser(&updateData); err != nil {
		return c.Status(400).JSON(types.Response{
			Success: false,
			Data:    nil,
			Error:   "Neispravan format podataka",
		})
	}

	if updateData.LimitAmount != nil {
		float, err := strconv.ParseFloat(*updateData.LimitAmount, 64)
		if err != nil {
			return c.Status(400).JSON(types.Response{
				Success: false,
				Data:    nil,
				Error:   "Neispravan format podataka",
			})
		}
		actuary.LimitAmount = float
	}

	if updateData.ResetLimit {
		actuary.UsedLimit = 0
	}

	db.DB.Save(&actuary)

	return c.JSON(types.Response{
		Success: true,
		Data:    actuary,
		Error:   "",
	})
}

// FilterActuaries godoc
//
//	@Summary		Filtriranje aktuara
//	@Description	Vraća listu aktuara filtriranu po imenu, prezimenu, email-u i/ili poziciji..
//	@Tags			Actuaries
//	@Produce		json
//	@Param			name		query		string											false	"Filter po imenu (case-insensitive, partial match)"
//	@Param			surname		query		string											false	"Filter po prezimenu (case-insensitive, partial match)"
//	@Param			email		query		string											false	"Filter po email-u (case-insensitive, partial match)"
//	@Param			position	query		string											false	"Filter po poziciji (exact match from user-service)"
//	@Success		200			{object}	types.Response{data=[]dto.FilteredActuaryDTO}	"Uspešno filtrirani aktuari"
//	@Failure		500			{object}	types.Response									"Greska pri preuzimanju aktuara."
//	@Router			/actuaries/filter [get]
//

func (ac *ActuaryController) FilterActuaries(c *fiber.Ctx) error {
	firstName := c.Query("firstName")
	lastName := c.Query("lastName")
	email := c.Query("email")
	position := c.Query("position")

	employees, err := services.GetEmployeesFiltered(firstName, lastName, email, position)
	if err != nil {
		return c.Status(500).JSON(types.Response{
			Success: false,
			Error:   "Greška pri preuzimanju zaposlenih sa user-service.  heeej",
		})
	}

	var actuaries []dto.FilteredActuaryDTO

	for _, emp := range employees {
		var actuary dto.FilteredActuaryDTO
		//	result := db.DB.Table("actuaries").Where("user_id = ?", emp.ID).Scan(&actuary)
		//	if result.Error != nil {
		//		return c.Status(500).JSON(types.Response{
		//			Success: false,
		//			Error:   "Greška pri preuzimanju aktuara.",
		//		})
		//	}

		actuary.ID = emp.ID
		actuary.FirstName = emp.FirstName
		actuary.LastName = emp.LastName
		actuary.Email = emp.Email
		actuary.Position = emp.Position
		actuaries = append(actuaries, actuary)
	}

	return c.JSON(types.Response{
		Success: true,
		Data:    actuaries,
		Error:   "",
	})
}

func (ac *ActuaryController) ResetActuaryLimit(c *fiber.Ctx) error {
	id := c.Params("ID")
	var actuary types.Actuary

	result := db.DB.First(&actuary, id)
	if result.Error != nil {
		return c.Status(404).JSON(types.Response{
			Success: false,
			Data:    nil,
			Error:   "Aktuar nije pronadjen",
		})
	}
	actuary.UsedLimit = 0

	db.DB.Save(&actuary)

	return c.JSON(types.Response{
		Success: true,
		Data:    actuary,
		Error:   "",
	})
}

//func containsIgnoreCase(source, search string) bool {
//	sourceLower := strings.ToLower(source)
//	searchLower := strings.ToLower(search)
//	return strings.Contains(sourceLower, searchLower)
//}

func InitActuaryRoutes(app *fiber.App) {
	actuaryController := NewActuaryController()

	app.Post("/actuaries", actuaryController.CreateActuary)
	app.Get("/actuaries/all", actuaryController.GetAllActuaries)
	app.Get("/actuaries/:ID", actuaryController.GetActuaryByID)
	app.Put("/actuaries/:ID/limit", actuaryController.ChangeAgentLimits)
	app.Get("/actuaries/filter", actuaryController.FilterActuaries)
	app.Put("/actuaries/:ID/reset-used-limit", actuaryController.ResetActuaryLimit)
}
