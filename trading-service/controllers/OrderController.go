package controllers

import (
	"banka1.com/controllers/orders"
	"banka1.com/db"
	"banka1.com/middlewares"
	"banka1.com/services"
	"banka1.com/types"
	"encoding/json"
	"fmt"
	"github.com/go-playground/validator/v10"
	"github.com/gofiber/fiber/v2"
	"net/http"
	"os"
	"strings"
	"time"
)

type OrderController struct {
}

func NewOrderController() *OrderController {
	return &OrderController{}
}

var validate = validator.New(validator.WithRequiredStructEnabled())

func OrderToOrderResponse(order types.Order) types.OrderResponse {
	return types.OrderResponse{
		ID:                order.ID,
		UserID:            order.UserID,
		AccountID:         order.AccountID,
		SecurityID:        order.SecurityID,
		Quantity:          order.Quantity,
		ContractSize:      order.ContractSize,
		StopPricePerUnit:  order.StopPricePerUnit,
		LimitPricePerUnit: order.LimitPricePerUnit,
		Direction:         order.Direction,
		Status:            order.Status,
		ApprovedBy:        order.ApprovedBy,
		IsDone:            order.IsDone,
		LastModified:      order.LastModified,
		RemainingParts:    order.RemainingParts,
		AfterHours:        order.AfterHours,
		AON:               order.AON,
		Margin:            order.Margin,
	}
}

// GetOrderByID godoc
//
//	@Summary		Preuzimanje naloga po I
//	@Summary		Preuzimanje naloga po ID-u
//	@Description	Vraća detalje specifičnog naloga na osnovu njegovog jedinstvenog identifikatora (ID).
//	@Tags			Orders
//	@Produce		json
//	@Param			id	path		int											true	"ID naloga koji se preuzima"
//	@Success		200	{object}	types.Response{data=types.OrderResponse}	"Uspešno preuzet nalog"
//	@Failure		400	{object}	types.Response								"Nevalidan ID naloga"
//	@Failure		404	{object}	types.Response								"Nalog sa datim ID-jem ne postoji"
//	@Router			/orders/{id} [get]
func (oc *OrderController) GetOrderByID(c *fiber.Ctx) error {
	id, err := c.ParamsInt("id", -1)
	if err != nil || id <= 0 {
		var response types.Response
		response.Success = false
		if err != nil {
			response.Error = "Nevalidan ID: " + err.Error()
		} else {
			response.Error = "Nevalidan ID"
		}
		return c.Status(400).JSON(response)
	}
	var order types.Order
	if err := db.DB.First(&order, id).Error; err != nil {
		return c.Status(404).JSON(types.Response{
			Success: false,
			Error:   "Nije pronadjen: " + err.Error(),
		})
	}
	return c.JSON(types.Response{
		Success: true,
		Data:    OrderToOrderResponse(order),
	})
}

// GetOrders godoc
//
//	@Summary		Preuzimanje liste naloga
//	@Description	Vraća listu naloga, opciono filtriranu po statusu.
//	@Tags			Orders
//	@Produce		json
//	@Param			filter_status	query		string										false	"Status naloga za filtriranje. Podrazumevano 'all' za sve statuse."	default(all)	example(pending)
//	@Success		200				{object}	types.Response{data=[]types.OrderResponse}	"Uspešno preuzeta lista naloga"
//	@Failure		500				{object}	types.Response								"Greška pri preuzimanju naloga iz baze"
//	@Router			/orders [get]
func (oc *OrderController) GetOrders(c *fiber.Ctx) error {
	filterStatus := strings.ToLower(c.Query("filter_status", "all"))
	var ordersList []types.Order
	var err error
	if "all" == filterStatus {
		err = db.DB.Find(&ordersList).Error
	} else {
		err = db.DB.Find(&ordersList, "lower(status) = ?", filterStatus).Error
	}
	if err != nil {
		return c.Status(400).JSON(types.Response{
			Success: false,
			Error:   "Neuspela pretraga: " + err.Error(),
		})
	}
	responses := make([]types.OrderResponse, len(ordersList))
	for i, order := range ordersList {
		responses[i] = OrderToOrderResponse(order)
	}
	return c.JSON(types.Response{
		Success: true,
		Data:    responses,
	})
}

type PaginatedOrders struct {
	Orders     []types.OrderResponse `json:"orders"`
	TotalCount int64                 `json:"totalCount"`
}

// GetOrdersPaged godoc
//
//	@Summary		Preuzimanje paginiranih naloga
//	@Description	Vraća stranicu naloga sa zadatim brojem po stranici i opcionim filtriranjem po statusu.
//	@Tags			Orders
//	@Produce		json
//	@Param			page			query		int											false	"Broj stranice (počinje od 1)"					default(1)
//	@Param			size			query		int											false	"Broj naloga po stranici"						default(20)
//	@Param			filter_status	query		string										false	"Status naloga za filtriranje"					example(pending)
//	@Success		200				{object}	types.Response{data=controllers.PaginatedOrders}	"Uspešno preuzeta stranica naloga"
//	@Failure		500				{object}	types.Response								"Greška pri preuzimanju naloga iz baze"
//	@Router			/orders/paged [get]
func (oc *OrderController) GetOrdersPaged(c *fiber.Ctx) error {
	page := c.QueryInt("page", 1)
	size := c.QueryInt("size", 20)
	filterStatus := strings.ToLower(c.Query("filter_status", "all"))

	if page < 1 {
		page = 1
	}
	if size < 1 || size > 100 {
		size = 20
	}
	offset := (page - 1) * size

	var totalCount int64
	query := db.DB.Model(&types.Order{})
	if filterStatus != "all" {
		query = query.Where("lower(status) = ?", filterStatus)
	}
	if err := query.Count(&totalCount).Error; err != nil {
		return c.Status(500).JSON(types.Response{
			Success: false,
			Error:   "Greška pri brojanju naloga: " + err.Error(),
		})
	}

	var ordersList []types.Order
	if err := query.Offset(offset).Limit(size).Find(&ordersList).Error; err != nil {
		return c.Status(500).JSON(types.Response{
			Success: false,
			Error:   "Greška pri preuzimanju naloga: " + err.Error(),
		})
	}

	responses := make([]types.OrderResponse, len(ordersList))
	for i, order := range ordersList {
		responses[i] = OrderToOrderResponse(order)
	}

	return c.JSON(types.Response{
		Success: true,
		Data: PaginatedOrders{
			Orders:     responses,
			TotalCount: totalCount,
		},
	})
}

// CreateOrder godoc
//
//	@Summary		Kreiranje novog naloga
//	@Description	Kreira novi nalog za hartije od vrednosti.
//	@Tags			Orders
//	@Accept			json
//	@Produce		json
//	@Param			orderRequest	body	types.CreateOrderRequest	true	"Podaci neophodni za kreiranje naloga"
//	@Security		BearerAuth
//	@Success		201	{object}	types.Response{data=uint}	"Uspešno kreiran nalog, vraća ID novog naloga"
//	@Failure		400	{object}	types.Response				"Neispravan format, neuspela validacija ili greška pri upisu u bazu"
//	@Failure		403	{object}	types.Response				"Nije dozvoljeno kreirati nalog za drugog korisnika"
//	@Router			/orders [post]
func (oc *OrderController) CreateOrder(c *fiber.Ctx) error {
	var orderRequest types.CreateOrderRequest
	userId := c.Locals("user_id").(float64)

	if err := c.BodyParser(&orderRequest); err != nil {
		return c.Status(400).JSON(types.Response{
			Success: false,
			Error:   "Neuspelo parsiranje: " + err.Error(),
		})
	}
	if err := validate.Struct(orderRequest); err != nil {
		return c.Status(400).JSON(types.Response{
			Success: false,
			Error:   "Neuspela validacija: " + err.Error(),
		})
	}
	if userId != float64(orderRequest.UserID) {
		return c.Status(403).JSON(types.Response{
			Success: false,
			Error:   "Cannot create order for another user",
		})
	}

	// Učitaj hartiju odmah nakon validacije korisnika
	var security types.Security
	if err := db.DB.First(&security, orderRequest.SecurityID).Error; err != nil {
		return c.Status(404).JSON(types.Response{
			Success: false,
			Error:   "Hartija nije pronađena",
		})
	}

	// Proveri da li je hartija istekla
	if security.SettlementDate != nil {
		parsed, err := time.Parse("2006-01-02", *security.SettlementDate)
		if err != nil {
			return c.Status(400).JSON(types.Response{
				Success: false,
				Error:   "Nevažeći settlement date format",
			})
		}

		// Poredi samo po danima, ne po satu
		now := time.Now().Truncate(24 * time.Hour)
		parsed = parsed.Truncate(24 * time.Hour)

		if parsed.Before(now) {
			return c.Status(400).JSON(types.Response{
				Success: false,
				Error:   "Nije moguće kreirati order za hartiju kojoj je istekao settlement date",
			})
		}
	}

	status := "pending"
	var approvedBy *uint = nil

	if deptRaw := c.Locals("department"); deptRaw != nil {
		if department, ok := deptRaw.(string); ok && department == "SUPERVISOR" {
			status = "approved"
			id := uint(userId)
			approvedBy = &id
		}
	}

	// Provera dostupnosti unita ako se order odobrava odmah
	if status == "approved" {
		if strings.ToLower(orderRequest.Direction) == "sell" {
			var portfolio types.Portfolio
			if err := db.DB.Where("user_id = ? AND security_id = ?", orderRequest.UserID, orderRequest.SecurityID).First(&portfolio).Error; err != nil {
				return c.Status(400).JSON(types.Response{
					Success: false,
					Error:   "Nemate ovu hartiju u portfoliju",
				})
			}
			if portfolio.Quantity < orderRequest.Quantity {
				return c.Status(400).JSON(types.Response{
					Success: false,
					Error:   fmt.Sprintf("Nemate dovoljno hartija za AON prodaju (imate %d, traženo %d)", portfolio.Quantity, orderRequest.Quantity),
				})
			}
		}
	}

	var orderType string
	switch {
	case orderRequest.StopPricePerUnit == nil && orderRequest.LimitPricePerUnit == nil:
		orderType = "MARKET"
	case orderRequest.StopPricePerUnit == nil && orderRequest.LimitPricePerUnit != nil:
		orderType = "LIMIT"
	case orderRequest.StopPricePerUnit != nil && orderRequest.LimitPricePerUnit == nil:
		orderType = "STOP"
	case orderRequest.StopPricePerUnit != nil && orderRequest.LimitPricePerUnit != nil:
		orderType = "STOP-LIMIT"
	}

	order := types.Order{
		UserID:            orderRequest.UserID,
		AccountID:         orderRequest.AccountID,
		SecurityID:        orderRequest.SecurityID,
		Quantity:          orderRequest.Quantity,
		ContractSize:      orderRequest.ContractSize,
		StopPricePerUnit:  orderRequest.StopPricePerUnit,
		LimitPricePerUnit: orderRequest.LimitPricePerUnit,
		OrderType:         orderType,
		Direction:         orderRequest.Direction,
		Status:            status, // TODO: pribaviti needs approval vrednost preko token-a?
		ApprovedBy:        approvedBy,
		LastModified:      time.Now().Unix(),
		IsDone:            false,
		RemainingParts:    &orderRequest.Quantity,
		AfterHours:        false, // TODO: dodati check za ovo
		AON:               orderRequest.AON,
		Margin:            orderRequest.Margin,
	}

	tx := db.DB.Create(&order)
	if err := tx.Error; err != nil {
		return c.Status(400).JSON(types.Response{
			Success: false,
			Error:   "Neuspelo kreiranje: " + err.Error(),
		})
	}

	if order.Status == "approved" {
		go orders.MatchOrder(order)
	}

	if strings.ToLower(order.Direction) == "sell" && order.Status == "approved" {
		_ = orders.UpdateAvailableVolume(order.SecurityID)
	}

	if orderRequest.Margin {
		var security types.Security
		if err := db.DB.First(&security, orderRequest.SecurityID).Error; err != nil {
			return c.Status(404).JSON(types.Response{
				Success: false,
				Error:   "Hartija nije pronađena",
			})
		}

		maintenanceMargin := security.LastPrice * 0.3
		initialMarginCost := maintenanceMargin * 1.1

		department, hasDepartment := c.Locals("department").(string)

		if hasDepartment && (department == "AGENT" || department == "SUPERVISOR") {
			var actuary types.Actuary
			if err := db.DB.Where("user_id = ?", orderRequest.UserID).First(&actuary).Error; err != nil {
				return c.Status(403).JSON(types.Response{
					Success: false,
					Error:   "Korisnik nema margin nalog (nije agent ili nije registrovan kao aktuar)",
				})
			}

			if actuary.LimitAmount < initialMarginCost {
				return c.Status(403).JSON(types.Response{
					Success: false,
					Error:   "Nedovoljan limit za margin order",
				})
			}
		} else {
			client := &http.Client{}
			req, _ := http.NewRequest("GET", fmt.Sprintf("%s/loans/has-approved-loan/%d", os.Getenv("BANKING_SERVICE"), orderRequest.UserID), nil)
			//url := fmt.Sprintf("%s/orders/execute/%s", os.Getenv("BANKING_SERVICE"), token)

			req.Header = http.Header{
				"Authorization": []string{c.Get("Authorization")},
			}
			resp, err := client.Do(req)
			if err != nil || resp.StatusCode != 200 {
				return c.Status(500).JSON(types.Response{
					Success: false,
					Error:   "Greška pri proveri kredita iz banking servisa",
				})
			}

			var body map[string]any
			if err := json.NewDecoder(resp.Body).Decode(&body); err != nil {
				return c.Status(500).JSON(types.Response{
					Success: false,
					Error:   "Neuspešno parsiranje odgovora iz banking servisa",
				})
			}

			approved, ok := body["approvedLoan"].(bool)
			if !ok || !approved {
				return c.Status(403).JSON(types.Response{
					Success: false,
					Error:   "Korisnik nema prava za margin order (nema kredit ni permisiju)",
				})
			}
		}
	}

	return c.JSON(types.Response{
		Success: true,
		Data:    order.ID,
	})
}

func ApproveDeclineOrder(c *fiber.Ctx, decline bool) error {
	id, err := c.ParamsInt("id", -1)
	if err != nil || id <= 0 {
		var response types.Response
		response.Success = false
		if err != nil {
			response.Error = "Nevalidan ID: " + err.Error()
		} else {
			response.Error = "Nevalidan ID"
		}
		return c.Status(400).JSON(response)
	}
	var order types.Order
	if err := db.DB.First(&order, id).Error; err != nil {
		return c.Status(404).JSON(types.Response{
			Success: false,
			Error:   "Nije pronadjen: " + err.Error(),
		})
	}
	if order.Status != "pending" {
		return c.Status(400).JSON(types.Response{
			Success: false,
			Error:   "Nije na cekanju",
		})
	}
	if decline {
		order.Status = "declined"
	} else {
		// Proveri da li je hartiji istekao settlementDate
		var security types.Security
		if err := db.DB.First(&security, order.SecurityID).Error; err != nil {
			return c.Status(404).JSON(types.Response{
				Success: false,
				Error:   "Hartija nije pronađena",
			})
		}
		if security.SettlementDate != nil {
			parsed, err := time.Parse("2006-01-02", *security.SettlementDate)
			if err != nil {
				return c.Status(400).JSON(types.Response{
					Success: false,
					Error:   "Nevažeći settlement date format",
				})
			}

			now := time.Now().Truncate(24 * time.Hour)
			parsed = parsed.Truncate(24 * time.Hour)

			if parsed.Before(now) {
				return c.Status(400).JSON(types.Response{
					Success: false,
					Error:   "Nije moguće odobriti order za hartiju kojoj je istekao settlement date",
				})
			}
		}

		if strings.ToLower(order.Direction) == "sell" {
			// Provera da li korisnik ima dovoljno hartija u portfoliju
			var portfolio types.Portfolio
			if err := db.DB.Where("user_id = ? AND security_id = ?", order.UserID, order.SecurityID).First(&portfolio).Error; err != nil {
				return c.Status(400).JSON(types.Response{
					Success: false,
					Error:   "Nemate ovu hartiju u portfoliju",
				})
			}
			if portfolio.Quantity < order.Quantity {
				return c.Status(400).JSON(types.Response{
					Success: false,
					Error:   fmt.Sprintf("Nemate dovoljno hartija da biste prodali (imate %d, traženo %d)", portfolio.Quantity, order.Quantity),
				})
			}
		}

		order.Status = "approved"

		if uidRaw := c.Locals("user_id"); uidRaw != nil {
			if uid, ok := uidRaw.(float64); ok {
				id := uint(uid)
				order.ApprovedBy = &id
			}
		}
		order.LastModified = time.Now().Unix()
		db.DB.Save(&order)

		if strings.ToLower(order.Direction) == "sell" {
			_ = orders.UpdateAvailableVolume(order.SecurityID)
		}

		orders.MatchOrder(order)

		return c.JSON(types.Response{
			Success: true,
			Data:    fmt.Sprintf("Order %d odobren i pokrenuto izvršavanje", order.ID),
		})
	}

	order.ApprovedBy = new(uint)
	*order.ApprovedBy = 0 // TODO: dobavi iz token-a
	db.DB.Save(&order)

	return c.JSON(types.Response{
		Success: true,
		Data:    order.ID,
	})
}

// DeclineOrder godoc
//
//	@Summary		Odbijanje naloga
//	@Description	Menja status naloga u 'declined'.
//	@Tags			Orders
//	@Produce		json
//	@Param			id	path	int	true	"ID naloga koji se odbija"
//	@Security		BearerAuth
//	@Success		200	{object}	types.Response{data=uint}	"Nalog uspešno odbijen, vraća ID naloga"
//	@Failure		400	{object}	types.Response				"Nevalidan ID ili nalog nije u 'pending' statusu"
//	@Failure		403	{object}	types.Response				"Nedovoljne privilegije"
//	@Failure		404	{object}	types.Response				"Nalog sa datim ID-jem ne postoji"
//	@Failure		500	{object}	types.Response				"Interna Greška Servera"
//	@Router			/orders/{id}/decline [post]
func (oc *OrderController) DeclineOrder(c *fiber.Ctx) error {
	return ApproveDeclineOrder(c, true)
}

// ApproveOrder godoc
//
//	@Summary		Odobravanje naloga
//	@Description	Menja status naloga u 'approved'.
//	@Tags			Orders
//	@Produce		json
//	@Param			id	path	int	true	"ID naloga koji se odobrava"
//	@Security		BearerAuth
//	@Success		200	{object}	types.Response{data=uint}	"Nalog uspešno odobren"
//	@Failure		400	{object}	types.Response				"Nevalidan ID ili nalog nije u 'pending' statusu"
//	@Failure		403	{object}	types.Response				"Nedovoljne privilegije"
//	@Failure		404	{object}	types.Response				"Nalog sa datim ID-jem ne postoji"
//	@Failure		500	{object}	types.Response				"Interna Greška Servera"
//	@Router			/orders/{id}/approve [post]
func (oc *OrderController) ApproveOrder(c *fiber.Ctx) error {
	return ApproveDeclineOrder(c, false)
}

// CancelOrder godoc
//
//	@Summary		Otkazivanje naloga
//	@Description	Menja status naloga u 'cancelled' ukoliko još nije izvršen.
//	@Tags			Orders
//	@Produce		json
//	@Param			id	path	int	true	"ID naloga koji se otkazuje"
//	@Security		BearerAuth
//	@Security		BearerAuth
//	@Success		200	{object}	types.Response{data=string}	"Nalog uspešno otkazan"
//	@Failure		400	{object}	types.Response					"Nevalidan ID ili nalog je već završen"
//	@Failure		403	{object}	types.Response					"Nedovoljne privilegije (ne može se otkazati tuđi nalog)"
//	@Failure		404	{object}	types.Response					"Nalog sa datim ID-jem ne postoji"
//	@Failure		500	{object}	types.Response					"Greška prilikom otkazivanja naloga"
//	@Router			/orders/{id}/cancel [post]
func (oc *OrderController) CancelOrder(c *fiber.Ctx) error {
	id, err := c.ParamsInt("id", -1)
	if err != nil || id <= 0 {
		return c.Status(400).JSON(types.Response{Success: false, Error: "Nevalidan ID"})
	}

	var order types.Order
	if err := db.DB.First(&order, id).Error; err != nil {
		return c.Status(404).JSON(types.Response{Success: false, Error: "Order nije pronađen"})
	}

	userID := uint(c.Locals("user_id").(float64))
	if order.UserID != userID {
		return c.Status(403).JSON(types.Response{Success: false, Error: "Nije dozvoljeno otkazati tuđi order"})
	}

	if order.IsDone || order.Status == "done" || order.Status == "cancelled" {
		return c.Status(400).JSON(types.Response{Success: false, Error: "Order je već izvršen ili otkazan"})
	}

	order.Status = "cancelled"
	order.LastModified = time.Now().Unix()
	if err := db.DB.Save(&order).Error; err != nil {
		return c.Status(500).JSON(types.Response{Success: false, Error: "Greška pri otkazivanju ordera"})
	}

	return c.JSON(types.Response{Success: true, Data: fmt.Sprintf("Order %d je uspešno otkazan", order.ID)})
}

// GetRealizedProfit godoc
//
//	@Summary		Obračun realizovanog profita
//	@Description	Računa ukupni ostvareni profit korisnika na osnovu izvršenih transakcija (FIFO).
//	@Tags			Orders
//	@Produce		json
//	@Param			id	path	int	true	"ID korisnika za kog se računa profit"
//	@Success		200	{object}	types.Response{data=dto.RealizedProfitResponse}	"Uspešno vraćen obračun profita"
//	@Failure		400	{object}	types.Response										"Nevalidan ID korisnika"
//	@Failure		404	{object}	types.Response										"Korisnik nema transakcija, nije moguće izračunati profit"
//	@Failure		500	{object}	types.Response										"Greška prilikom obračuna profita"
//	@Router			/profit/{id} [get]
func (oc *OrderController) GetRealizedProfit(c *fiber.Ctx) error {
	userID, err := c.ParamsInt("id", -1)
	if err != nil || userID <= 0 {
		return c.Status(400).JSON(types.Response{
			Success: false,
			Error:   "Nevalidan ID korisnika",
		})
	}

	profit, err := services.CalculateRealizedProfit(uint(userID))
	if err != nil {
		// Ako korisnik nema transakcija, vrati 404
		if err.Error() == "Korisnik nema transakcija. Ne može se izračunati profit." {
			return c.Status(404).JSON(types.Response{
				Success: false,
				Error:   err.Error(),
			})
		}
		// Sve ostalo tretiraj kao internu grešku
		return c.Status(500).JSON(types.Response{
			Success: false,
			Error:   "Greška prilikom obračuna profita: " + err.Error(),
		})
	}

	return c.JSON(types.Response{
		Success: true,
		Data:    profit,
	})
}

func InitOrderRoutes(app *fiber.App) {
	orderController := NewOrderController()

	app.Get("/orders/:id", orderController.GetOrderByID)
	app.Get("/orders", orderController.GetOrders)
	app.Get("/orders/paged", orderController.GetOrdersPaged)
	app.Post("/orders", middlewares.Auth, orderController.CreateOrder)
	app.Post("/orders/:id/decline", middlewares.Auth, middlewares.DepartmentCheck("SUPERVISOR"), orderController.DeclineOrder)
	app.Post("/orders/:id/approve", middlewares.Auth, middlewares.DepartmentCheck("SUPERVISOR"), orderController.ApproveOrder)
	app.Post("/orders/:id/cancel", middlewares.Auth, orderController.CancelOrder)
	app.Get("/profit/:id", orderController.GetRealizedProfit)
}
