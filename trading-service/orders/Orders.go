package orders

import (
	"banka1.com/middlewares"
	"strings"

	"banka1.com/db"
	"banka1.com/types"
	"github.com/go-playground/validator/v10"
	"github.com/gofiber/fiber/v2"
)

var validate = validator.New(validator.WithRequiredStructEnabled())

func OrderToOrderResponse(order types.Order) types.OrderResponse {
	return types.OrderResponse{
		ID:             order.ID,
		UserID:         order.UserID,
		SecurityID:     order.SecurityID,
		OrderType:      order.OrderType,
		Quantity:       order.Quantity,
		ContractSize:   order.ContractSize,
		PricePerUnit:   order.PricePerUnit,
		Direction:      order.Direction,
		Status:         order.Status,
		ApprovedBy:     order.ApprovedBy,
		IsDone:         order.IsDone,
		LastModified:   order.LastModified,
		RemainingParts: order.RemainingParts,
		AfterHours:     order.AfterHours,
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

	order := types.Order{
		UserID:         orderRequest.UserID,
		SecurityID:     orderRequest.SecurityID,
		OrderType:      orderRequest.OrderType,
		Quantity:       orderRequest.Quantity,
		ContractSize:   orderRequest.ContractSize,
		PricePerUnit:   orderRequest.PricePerUnit,
		Direction:      orderRequest.Direction,
		Status:         "pending", // TODO: pribaviti needs approval vrednost preko token-a?
		ApprovedBy:     nil,
		IsDone:         false,
		RemainingParts: &orderRequest.Quantity,
		AfterHours:     false, // TODO: dodati check za ovo
	}
	tx := db.DB.Create(&order)
	if err := tx.Error; err != nil {
		return c.Status(400).JSON(types.Response{
			Success: false,
			Error:   "Neuspelo kreiranje: " + err.Error(),
		})
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
		// TODO: provera da li je vremenski ogranicen?
		order.Status = "approved"
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
