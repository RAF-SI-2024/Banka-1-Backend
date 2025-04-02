package controllers

import (
	"github.com/go-playground/validator/v10"
	"github.com/gofiber/fiber/v2"
	"github.com/gofiber/fiber/v2/log"
	"strconv"
	"strings"

	"banka1.com/db"
	"banka1.com/dto"
	"banka1.com/services"
	"banka1.com/types"
)

type ActuaryController struct {
}

func NewActuaryController() *ActuaryController {
	return &ActuaryController{}
}

var validate = validator.New()

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

	actuary := types.Actuary{
		UserID:       actuaryDTO.UserID,
		Role:         actuaryDTO.Role,
		LimitAmount:  actuaryDTO.LimitAmount,
		UsedLimit:    actuaryDTO.UsedLimit,
		NeedApproval: actuaryDTO.NeedApproval,
	}

	result := db.DB.Create(&actuary)
	if result.Error != nil {
		response := types.Response{
			Success: false,
			Data:    nil,
			Error:   "Greska u bazi.",
		}
		return c.Status(500).JSON(response)
	}

	response := types.Response{
		Success: true,
		Data:    actuary,
		Error:   "",
	}

	return c.Status(201).JSON(response)
}

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

// POPRAVITI FILTER
func (ac *ActuaryController) FilterActuaries(c *fiber.Ctx) error {
	var actuaries []types.Actuary

	name := c.Query("name")
	surname := c.Query("surname")
	email := c.Query("email")
	position := c.Query("position")

	result := db.DB.Find(&actuaries)
	if result.Error != nil {
		return c.Status(500).JSON(types.Response{
			Success: false,
			Error:   "Greska pri preuzimanju aktuara.",
		})
	}

	actuaryMap := make(map[uint]types.Actuary)
	for _, act := range actuaries {
		actuaryMap[act.UserID] = act
	}

	employees, err := services.GetEmployees()
	if err != nil {
		return c.Status(500).JSON(types.Response{
			Success: false,
			Error:   "Neuspesno preuzimanje zaposlenih iz user-service-a",
		})
	}

	var filteredEmployees []dto.FilteredActuaryDTO
	for _, emp := range employees {
		if actuary, exists := actuaryMap[emp.ID]; exists {
			if (name == "" || containsIgnoreCase(emp.FirstName, name)) &&
				(surname == "" || containsIgnoreCase(emp.LastName, surname)) &&
				(email == "" || containsIgnoreCase(emp.Email, email)) &&
				(position == "" || emp.Position == position) {

				filteredEmployees = append(filteredEmployees, dto.FilteredActuaryDTO{
					ID:           emp.ID,
					FirstName:    emp.FirstName,
					LastName:     emp.LastName,
					Email:        emp.Email,
					Role:         actuary.Role,
					LimitAmount:  actuary.LimitAmount,
					UsedLimit:    actuary.UsedLimit,
					NeedApproval: actuary.NeedApproval,
				})
			}
		}
	}

	if len(filteredEmployees) == 0 {
		return c.JSON(types.Response{
			Success: true,
			Data:    []dto.FilteredActuaryDTO{},
			Error:   "Ne postoji ni jedan aktuar.",
		})
	}

	return c.JSON(types.Response{
		Success: true,
		Data:    filteredEmployees,
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

func containsIgnoreCase(source, search string) bool {
	sourceLower := strings.ToLower(source)
	searchLower := strings.ToLower(search)
	return strings.Contains(sourceLower, searchLower)
}
