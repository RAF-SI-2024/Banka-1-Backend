package controllers

import (
	"banka1.com/db"
	"banka1.com/dto"
	"banka1.com/middlewares"
	"banka1.com/services"
	"banka1.com/types"
	"github.com/gofiber/fiber/v2"
	"github.com/gofiber/fiber/v2/log"
	"strconv"
	"strings"
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

type ActuaryFilterResult struct {
	ID           int     `json:"id"`
	FirstName    string  `json:"firstName"`
	LastName     string  `json:"lastName"`
	Email        string  `json:"email"`
	Department   string  `json:"department"`
	LimitAmount  float64 `json:"limitAmount"`
	UsedLimit    float64 `json:"usedLimit"`
	NeedApproval bool    `json:"needApproval"`
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
	if tx.Error != nil {
		return c.Status(500).JSON(types.Response{
			Success: false,
			Error:   "Greška pri započinjanju transakcije.",
			Data:    nil,
		})
	}

	actuary := types.Actuary{
		UserID:       actuaryDTO.UserID,
		Department:   actuaryDTO.Department,
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
	if err := tx.Commit().Error; err != nil {
		return c.Status(500).JSON(types.Response{
			Success: false,
			Error:   "Greška pri potvrđivanju transakcije.",
			Data:    nil,
		})
	}

	response := types.Response{
		Success: true,
		Data:    actuary,
		Error:   "",
	}

	return c.Status(201).JSON(response)
}

// GetAllActuariesAPI godoc
//
//	@Summary		Dobavljanje svih aktuara
//	@Description	Vraća listu svih zapisa aktuara iz baze podataka.
//	@Tags			Actuaries
//	@Produce		json
//	@Success		200	{object}	types.Response{data=[]types.Actuary}	"Uspešno dobavljeni svi aktuari"
//	@Failure		500	{object}	types.Response							"Greška u bazi"
//	@Router			/actuaries [get]
func (ac *ActuaryController) GetAllActuariesAPI(c *fiber.Ctx) error {
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

// GetAllActuariesDB godoc
//
//	@Summary		Dobavljanje svih aktuara
//	@Description	Vraća listu svih zapisa aktuara iz baze podataka.
//	@Tags			Actuaries
//	@Produce		json
//	@Success		200	{object}	types.Response{data=[]types.Actuary}	"Uspešno dobavljeni svi aktuari"
//	@Failure		500	{object}	types.Response							"Greška u bazi"
//	@Router			/actuaries [get]
func (ac *ActuaryController) GetAllActuariesDB(c *fiber.Ctx) error {
	var actuaries []types.Actuary
	result := db.DB.Find(&actuaries)
	if result.Error != nil {
		log.Infof("Database error: %v\n", result.Error)
		return c.Status(500).JSON(types.Response{
			Success: false,
			Error:   "Greška pri pristupu bazi podataka.",
		})
	}

	//// Opcionalno: ako ne postoji ni jedan actuary, možemo vratiti 404 (Not Found)
	//if len(actuaries) == 0 {
	//	return c.Status(404).JSON(types.Response{
	//		Success: false,
	//		Error:   "Aktuari nisu pronadjeni",
	//	})
	//}

	return c.JSON(types.Response{
		Success: true,
		Data:    actuaries,
		Error:   "",
	})
}

// GetAllActuaryAgentsDB godoc
//
//	@Summary		Dobavljanje aktuara koji su agenti
//	@Description	Vraća listu svih aktuara iz baze čiji je departman "AGENT".
//	@Tags			Actuaries
//	@Produce		json
//	@Success		200	{object}	types.Response{data=[]types.Actuary}	"Uspešno dobavljeni agenti"
//	@Failure		500	{object}	types.Response							"Greška u bazi"
//	@Router			/actuaries/agents [get]
func (ac *ActuaryController) GetAllActuaryAgentsDB(c *fiber.Ctx) error {
	var agents []types.Actuary

	result := db.DB.Where("department = ?", "AGENT").Find(&agents)
	if result.Error != nil {
		log.Infof("Database error: %v\n", result.Error)
		return c.Status(500).JSON(types.Response{
			Success: false,
			Error:   "Greška pri pristupu bazi podataka.",
		})
	}

	//if len(agents) == 0 {
	//	return c.Status(404).JSON(types.Response{
	//		Success: false,
	//		Error:   "No actuary agents found",
	//	})
	//}

	return c.JSON(types.Response{
		Success: true,
		Data:    agents,
		Error:   "",
	})
}

// GetActuaryByID godoc
//
//	@Summary		Dobavljanje aktuara po ID-ju
//	@Description	Vraća detalje jednog aktuara na osnovu njegovog ID-ja.
//	@Tags			Actuaries
//	@Produce		json
//	@Param			ID	path		string							true	"ID aktuara"
//	@Success		200	{object}	types.Response{data=types.Actuary}	"Uspešno dobavljen aktuar"
//	@Failure		404	{object}	types.Response					"Aktuar nije pronadjen"
//	@Failure		500	{object}	types.Response					"Greška u bazi"
//	@Router			/actuaries/{ID} [get]
func (ac *ActuaryController) GetActuaryByID(c *fiber.Ctx) error {
	id := c.Params("id") // Uzima ID iz URL-a
	if id == "" {
		log.Error("GetActuaryByID: ID not provided")
		return c.Status(400).JSON(types.Response{
			Success: false,
			Error:   "ID nije prosleđen.",
			Data:    nil,
		})
	}

	var actuary types.Actuary
	result := db.DB.First(&actuary, id)
	if err := result.Error; err != nil {
		return c.Status(404).JSON(types.Response{
			Success: false,
			Data:    nil,
			Error:   "Aktuar nije pronadjen",
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

	tx := db.DB.Begin()
	if tx.Error != nil {
		log.Errorf("ChangeAgentLimits: greška pri započinjanju transakcije: %v", tx.Error)
		return c.Status(500).JSON(types.Response{
			Success: false,
			Data:    nil,
			Error:   "Greška pri započinjanju transakcije.",
		})
	}

	defer func() {
		if r := recover(); r != nil {
			tx.Rollback()
			log.Errorf("ChangeAgentLimits: panic oporavljen: %v", r)
		}
	}()

	result := tx.First(&actuary, id)
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
				Error:   "Neispravan format limita",
			})
		}
		actuary.LimitAmount = float
	}

	if updateData.ResetLimit {
		actuary.UsedLimit = 0
	}

	if err := tx.Save(&actuary).Error; err != nil {
		tx.Rollback()
		log.Errorf("ChangeAgentLimits: greška pri čuvanju aktuara: %v", err)
		return c.Status(500).JSON(types.Response{
			Success: false,
			Data:    nil,
			Error:   "Greška pri ažuriranju aktuara.",
		})
	}

	if err := tx.Commit().Error; err != nil {
		log.Errorf("ChangeAgentLimits: greška pri potvrdi transakcije: %v", err)
		return c.Status(500).JSON(types.Response{
			Success: false,
			Data:    nil,
			Error:   "Greška pri potvrđivanju promena.",
		})
	}

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

	employees, err := services.GetEmployeesFiltered(c, firstName, lastName, email, position)
	if err != nil {
		return c.Status(500).JSON(types.Response{
			Success: false,
			Error:   "Greška pri preuzimanju zaposlenih sa user-service.",
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

// FilterActuariesDB godoc
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
func (ac *ActuaryController) FilterActuariesDB(c *fiber.Ctx) error {
	firstName := c.Query("firstName")
	lastName := c.Query("lastName")
	email := c.Query("email")
	position := c.Query("position")

	var actuaries []types.Actuary
	query := db.DB.Model(&types.Actuary{})

	if firstName != "" && lastName != "" {
		query = query.Where("LOWER(full_name) LIKE ? AND LOWER(full_name) LIKE ?", "%"+strings.ToLower(firstName)+"%", "%"+strings.ToLower(lastName)+"%")
	} else if firstName != "" {
		query = query.Where("LOWER(full_name) LIKE ?", "%"+strings.ToLower(firstName)+"%")
	} else if lastName != "" {
		query = query.Where("LOWER(full_name) LIKE ?", "%"+strings.ToLower(lastName)+"%")
	}

	if email != "" {
		query = query.Where("LOWER(email) LIKE ?", "%"+strings.ToLower(email)+"%")
	}
	if position != "" {
		query = query.Where("LOWER(position) LIKE ?", "%"+strings.ToLower(position)+"%")
	}

	result := query.Find(&actuaries)
	if result.Error != nil {
		log.Infof("Database error: GetFilteredActuaries  %v\n", result.Error)
		return c.Status(500).JSON(types.Response{
			Success: false,
			Error:   "Greška pri pretrazi aktura.",
		})
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

	tx := db.DB.Begin()
	if tx.Error != nil {
		log.Errorf("ResetActuaryLimit: greška pri započinjanju transakcije: %v", tx.Error)
		return c.Status(500).JSON(types.Response{
			Success: false,
			Data:    nil,
			Error:   "Greška pri započinjanju transakcije.",
		})
	}

	defer func() {
		if r := recover(); r != nil {
			tx.Rollback()
			log.Errorf("ResetActuaryLimit: panic oporavljen: %v", r)
		}
	}()

	result := tx.First(&actuary, id)
	if result.Error != nil {
		return c.Status(404).JSON(types.Response{
			Success: false,
			Data:    nil,
			Error:   "Aktuar nije pronadjen",
		})
	}
	actuary.UsedLimit = 0

	if err := tx.Save(&actuary).Error; err != nil {
		tx.Rollback()
		log.Errorf("ResetActuaryLimit: greška pri čuvanju aktuara: %v", err)
		return c.Status(500).JSON(types.Response{
			Success: false,
			Data:    nil,
			Error:   "Greška pri resetovanju limita.",
		})
	}

	if err := tx.Commit().Error; err != nil {
		log.Errorf("ResetActuaryLimit: greška pri potvrdi transakcije: %v", err)
		return c.Status(500).JSON(types.Response{
			Success: false,
			Data:    nil,
			Error:   "Greška pri potvrđivanju promena.",
		})
	}

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

	app.Post("/actuaries", middlewares.Auth, actuaryController.CreateActuary)
	//app.Get("/actuaries/all", middlewares.Auth, actuaryController.GetAllActuariesAPI)
	app.Get("/actuaries/all", middlewares.Auth, actuaryController.GetAllActuariesDB)
	app.Get("/actuaries/agents", middlewares.Auth, actuaryController.GetAllActuaryAgentsDB)
	//	app.Get("/actuaries/filter", middlewares.Auth, actuaryController.FilterActuaries)
	app.Get("/actuaries/filter", middlewares.Auth, actuaryController.FilterActuariesDB)
	app.Get("/actuaries/profits", middlewares.Auth, actuaryController.GetActuaryProfits)
	app.Get("/actuaries/:ID", middlewares.Auth, actuaryController.GetActuaryByID)
	app.Put("/actuaries/:ID/limit", middlewares.Auth, actuaryController.ChangeAgentLimits)
	app.Put("/actuaries/:ID/reset-used-limit", middlewares.Auth, actuaryController.ResetActuaryLimit)
}

func (ac *ActuaryController) GetActuaryProfits(c *fiber.Ctx) error {

	var actuaries []types.Actuary
	if result := db.DB.Find(&actuaries); result.Error != nil {
		return c.Status(500).JSON(types.Response{
			Success: false,
			Error:   "Greška pri preuzimanju aktuara",
		})
	}

	var latestMonth string
	if err := db.DB.Raw(`SELECT MAX(month_year) FROM tax`).Scan(&latestMonth).Error; err != nil {
		return c.Status(500).JSON(types.Response{
			Success: false,
			Error:   "Greška pri preuzimanju najnovijeg meseca",
		})
	}

	type TaxEntry struct {
		UserID        uint
		TaxableProfit float64
	}

	var taxEntries []TaxEntry
	err := db.DB.Raw(`
		SELECT user_id, SUM(taxable_profit) AS taxable_profit
		FROM tax
		WHERE month_year = ? AND is_paid = false
		GROUP BY user_id
	`, latestMonth).Scan(&taxEntries).Error

	if err != nil {
		return c.Status(500).JSON(types.Response{
			Success: false,
			Data:    nil,
			Error:   "Greška pri sabiranju profita: " + err.Error(),
		})
	}

	taxMap := make(map[uint]float64)
	for _, t := range taxEntries {
		taxMap[t.UserID] = t.TaxableProfit
	}

	var results []dto.ActuaryProfitDTO
	for _, a := range actuaries {
		if profit, exists := taxMap[a.UserID]; exists {
			normalized := strings.ToLower(a.Department)
			if normalized == "admin" {
				normalized = "supervisor"
			}

			results = append(results, dto.ActuaryProfitDTO{
				FullName: a.FullName,
				Profit:   profit,
				Role:     normalized,
			})
		}
	}

	return c.JSON(types.Response{
		Success: true,
		Data:    results,
	})
}
