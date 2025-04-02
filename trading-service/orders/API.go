package orders

import (
	"banka1.com/db"
	"banka1.com/middlewares"
	"banka1.com/types"
	"fmt"
	"github.com/go-playground/validator/v10"
	"github.com/gofiber/fiber/v2"
	"strings"
)

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

func GetOrderByID(c *fiber.Ctx) error {
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

func GetOrders(c *fiber.Ctx) error {
	filterStatus := strings.ToLower(c.Query("filter_status", "all"))
	var orders []types.Order
	var err error
	if "all" == filterStatus {
		err = db.DB.Find(&orders).Error
	} else {
		err = db.DB.Find(&orders, "status = ?", filterStatus).Error
	}
	if err != nil {
		return c.Status(400).JSON(types.Response{
			Success: false,
			Error:   "Neuspela pretraga: " + err.Error(),
		})
	}
	responses := make([]types.OrderResponse, len(orders))
	for i, order := range orders {
		responses[i] = OrderToOrderResponse(order)
	}
	return c.JSON(types.Response{
		Success: true,
		Data:    responses,
	})
}

func CreateOrder(c *fiber.Ctx) error {
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

	department, ok := c.Locals("department").(string)
	if !ok {
		return c.Status(500).JSON(types.Response{
			Success: false,
			Error:   "[MIDDLEWARE] Greska prilikom dohvatanja department vrednosti",
		})
	}

	status := "pending"
	if department == "SUPERVISOR" {
		status = "approved"
	}

	var orderType string
	switch {
	case orderRequest.StopPricePerUnit == nil && orderRequest.LimitPricePerUnit == nil:
		orderType = "market"
	case orderRequest.StopPricePerUnit == nil && orderRequest.LimitPricePerUnit != nil:
		orderType = "limit"
	case orderRequest.StopPricePerUnit != nil && orderRequest.LimitPricePerUnit == nil:
		orderType = "stop"
	case orderRequest.StopPricePerUnit != nil && orderRequest.LimitPricePerUnit != nil:
		orderType = "stop-limit"
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
		ApprovedBy:        nil,
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

		var actuary types.Actuary
		if err := db.DB.Where("user_id = ?", orderRequest.UserID).First(&actuary).Error; err != nil {
			return c.Status(403).JSON(types.Response{
				Success: false,
				Error:   "Korisnik nema margin nalog (nije agent ili nije registrovan kao aktuar)",
			})
		}

		if actuary.Role != "agent" {
			return c.Status(403).JSON(types.Response{
				Success: false,
				Error:   "Samo agenti mogu da koriste margin",
			})
		}

		if actuary.LimitAmount < initialMarginCost {
			return c.Status(403).JSON(types.Response{
				Success: false,
				Error:   "Nedovoljan limit za margin order",
			})
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
		order.Status = "approved"
		order.ApprovedBy = new(uint)
		*order.ApprovedBy = 0
		db.DB.Save(&order)

		MatchOrder(order)

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

func DeclineOrder(c *fiber.Ctx) error {
	return ApproveDeclineOrder(c, true)
}

func ApproveOrder(c *fiber.Ctx) error {

	return ApproveDeclineOrder(c, false)
}

func InitRoutes(app *fiber.App) {
	// swagger:operation GET /orders/{id} GetOrderByID
	//
	// Pregled naloga po ID-u.
	//
	// ---
	// parameters:
	// - name: id
	//   in: path
	//   description: ID naloga
	//   type: integer
	//   required: true
	// responses:
	//   '200':
	//     schema:
	//       $ref: '#/definitions/OrderResponse'
	app.Get("/orders/:id", GetOrderByID)
	// swagger:operation GET /orders GetOrders
	//
	// Pregled svih naloga, sa filtriranjem po statusu.
	//
	// ---
	// parameters:
	// - name: filter_status
	//   in: query
	//   description: Status za filtriranje, ili "all" (bez filtriranja, to je podrazumevana vrednost)
	//   default: "all"
	//   type: string
	// responses:
	//   '200':
	//     schema:
	//       type: array
	//       items:
	//         $ref: '#/definitions/OrderResponse'
	app.Get("/orders", GetOrders)
	// swagger:operation POST /orders CreateOrder
	//
	// Kreiranje novog naloga
	//
	// ---
	// parameters:
	// - name: body
	//   in: body
	//   description: Podaci o nalogu
	//   required: true
	//   schema:
	//      $ref: '#/definitions/CreateOrderRequest'
	app.Post("/orders", middlewares.Auth, CreateOrder)
	// swagger:operation POST /orders/{id}/decline DeclineOrder
	//
	// Odbijanje naloga.
	//
	// ---
	// parameters:
	// - name: id
	//   in: path
	//   description: ID naloga
	//   type: integer
	//   required: true
	app.Post("/orders/:id/decline", middlewares.Auth, middlewares.DepartmentCheck("SUPERVISOR"), DeclineOrder)
	// swagger:operation POST /orders/{id}/approve ApproveOrder
	//
	// Odobrenje naloga.
	//
	// ---
	// parameters:
	// - name: id
	//   in: path
	//   description: ID naloga
	//   type: integer
	//   required: true
	app.Post("/orders/:id/approve", middlewares.Auth, middlewares.DepartmentCheck("SUPERVISOR"), ApproveOrder)
}
