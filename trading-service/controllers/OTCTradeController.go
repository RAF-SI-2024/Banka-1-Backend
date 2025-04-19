package controllers

import (
	"banka1.com/broker"
	"banka1.com/db"
	"banka1.com/dto"
	"banka1.com/middlewares"
	"banka1.com/saga"
	"banka1.com/types"
	"fmt"
	"github.com/go-playground/validator/v10"
	"github.com/gofiber/fiber/v2"
	"time"
)

type UpdateOTCTradeRequest struct {
	Quantity       int     `json:"quantity" validate:"required,gt=0"`
	PricePerUnit   float64 `json:"price_per_unit" validate:"required,gt=0"`
	Premium        float64 `json:"premium" validate:"required,gte=0"`
	SettlementDate string  `json:"settlement_date" validate:"required"`
}

type OTCTradeController struct {
	validator *validator.Validate
}

func NewOTCTradeController() *OTCTradeController {
	return &OTCTradeController{
		validator: validator.New(),
	}
}

type CreateOTCTradeRequest struct {
	PortfolioID    uint    `json:"portfolio_id" validate:"required"`
	Quantity       int     `json:"quantity" validate:"required,gt=0"`
	PricePerUnit   float64 `json:"price_per_unit" validate:"required,gt=0"`
	Premium        float64 `json:"premium" validate:"required,gte=0"`
	SettlementDate string  `json:"settlement_date" validate:"required"`
}

func (c *OTCTradeController) CreateOTCTrade(ctx *fiber.Ctx) error {
	var req CreateOTCTradeRequest

	if err := ctx.BodyParser(&req); err != nil {
		return ctx.Status(fiber.StatusBadRequest).JSON(types.Response{
			Success: false,
			Error:   "Nevalidan JSON format",
		})
	}

	if err := c.validator.Struct(req); err != nil {
		return ctx.Status(fiber.StatusBadRequest).JSON(types.Response{
			Success: false,
			Error:   err.Error(),
		})
	}

	settlementDate, err := time.Parse("2006-01-02", req.SettlementDate)
	if err != nil {
		return ctx.Status(fiber.StatusBadRequest).JSON(types.Response{
			Success: false,
			Error:   "Nevalidan format datuma. Očekivan format: YYYY-MM-DD",
		})
	}

	userID := uint(ctx.Locals("user_id").(float64))

	var portfolio types.Portfolio
	if err := db.DB.Preload("Security").First(&portfolio, req.PortfolioID).Error; err != nil {
		return ctx.Status(fiber.StatusNotFound).JSON(types.Response{
			Success: false,
			Error:   "Portfolio nije pronađen",
		})
	}

	if portfolio.UserID == userID {
		return ctx.Status(fiber.StatusForbidden).JSON(types.Response{
			Success: false,
			Error:   "Ne možete praviti OTC ponudu za svoje akcije",
		})
	}

	if portfolio.PublicCount < req.Quantity {
		return ctx.Status(fiber.StatusBadRequest).JSON(types.Response{
			Success: false,
			Error:   "Nedovoljno javno dostupnih akcija u portfoliju",
		})
	}

	if portfolio.Security.Type != "Stock" {
		return ctx.Status(fiber.StatusBadRequest).JSON(types.Response{
			Success: false,
			Error:   "Hartija od vrednosti mora biti akcija",
		})
	}

	otcTrade := types.OTCTrade{
		PortfolioID:  portfolio.ID,
		SecurityId:   portfolio.SecurityID,
		SellerID:     portfolio.UserID,
		BuyerID:      &userID,
		Quantity:     req.Quantity,
		PricePerUnit: req.PricePerUnit,
		Premium:      req.Premium,
		SettlementAt: settlementDate,
		CreatedAt:    time.Now().Unix(),
		LastModified: time.Now().Unix(),
		ModifiedBy:   &userID,
		Status:       "pending",
	}

	if err := db.DB.Create(&otcTrade).Error; err != nil {
		return ctx.Status(fiber.StatusInternalServerError).JSON(types.Response{
			Success: false,
			Error:   "Greška prilikom čuvanja OTC ponude",
		})
	}

	return ctx.Status(fiber.StatusCreated).JSON(types.Response{
		Success: true,
		Data:    fmt.Sprintf("Ponuda uspešno poslata,kreirana ponuda: %d", otcTrade.ID),
	})
}

func (c *OTCTradeController) CounterOfferOTCTrade(ctx *fiber.Ctx) error {
	id := ctx.Params("id")
	var req UpdateOTCTradeRequest

	if err := ctx.BodyParser(&req); err != nil {
		return ctx.Status(fiber.StatusBadRequest).JSON(types.Response{
			Success: false,
			Error:   "Nevalidan JSON format",
		})
	}

	if err := c.validator.Struct(req); err != nil {
		return ctx.Status(fiber.StatusBadRequest).JSON(types.Response{
			Success: false,
			Error:   err.Error(),
		})
	}

	userID := uint(ctx.Locals("user_id").(float64))

	settlementDate, err := time.Parse("2006-01-02", req.SettlementDate)
	if err != nil {
		return ctx.Status(fiber.StatusBadRequest).JSON(types.Response{
			Success: false,
			Error:   "Nevalidan format datuma. Očekivan format: YYYY-MM-DD",
		})
	}

	var trade types.OTCTrade
	if err := db.DB.Preload("Portfolio").First(&trade, id).Error; err != nil {
		return ctx.Status(fiber.StatusNotFound).JSON(types.Response{
			Success: false,
			Error:   "Ponuda nije pronađena",
		})
	}

	if trade.ModifiedBy != nil && *trade.ModifiedBy == userID {
		return ctx.Status(fiber.StatusForbidden).JSON(types.Response{
			Success: false,
			Error:   "Ne možete uzastopno menjati ponudu",
		})
	}

	var portfolio types.Portfolio
	if err := db.DB.First(&portfolio, trade.PortfolioID).Error; err != nil {
		return ctx.Status(fiber.StatusInternalServerError).JSON(types.Response{
			Success: false,
			Error:   "Greška pri proveri portfolija prodavca",
		})
	}
	if portfolio.PublicCount < req.Quantity {
		return ctx.Status(fiber.StatusBadRequest).JSON(types.Response{
			Success: false,
			Error:   "Prodavac više nema dovoljno raspoloživih akcija",
		})
	}

	trade.Quantity = req.Quantity
	trade.PricePerUnit = req.PricePerUnit
	trade.Premium = req.Premium
	trade.SettlementAt = settlementDate
	trade.LastModified = time.Now().Unix()
	trade.ModifiedBy = &userID
	trade.Status = "pending"

	if err := db.DB.Save(&trade).Error; err != nil {
		return ctx.Status(fiber.StatusInternalServerError).JSON(types.Response{
			Success: false,
			Error:   "Greška prilikom čuvanja kontraponude",
		})
	}

	return ctx.Status(fiber.StatusOK).JSON(types.Response{
		Success: true,
		Data:    fmt.Sprintf("Ponuda uspešno poslata,ažurirana ponuda: %d", trade.ID),
	})
}

func (c *OTCTradeController) AcceptOTCTrade(ctx *fiber.Ctx) error {
	id := ctx.Params("id")
	userID := uint(ctx.Locals("user_id").(float64))

	var trade types.OTCTrade
	if err := db.DB.Preload("Portfolio").First(&trade, id).Error; err != nil {
		return ctx.Status(fiber.StatusNotFound).JSON(types.Response{
			Success: false,
			Error:   "Ponuda nije pronađena",
		})
	}

	if trade.ModifiedBy != nil && *trade.ModifiedBy == userID {
		return ctx.Status(fiber.StatusForbidden).JSON(types.Response{
			Success: false,
			Error:   "Nemate pravo da prihvatite ovu ponudu jer ste je vi poslednji menjali",
		})
	}

	if trade.Status == "accepted" || trade.Status == "executed" {
		return ctx.Status(fiber.StatusBadRequest).JSON(types.Response{
			Success: false,
			Error:   "Ova ponuda je već prihvaćena ili realizovana",
		})
	}

	var portfolio types.Portfolio
	if err := db.DB.First(&portfolio, trade.PortfolioID).Error; err != nil {
		return ctx.Status(fiber.StatusInternalServerError).JSON(types.Response{
			Success: false,
			Error:   "Greška pri dohvatanju portfolija",
		})
	}

	var existingContracts []types.OptionContract
	if err := db.DB.
		Where("seller_id = ? AND portfolio_id = ? AND is_exercised = false AND status = ?", trade.SellerID, portfolio.ID, "active").
		Find(&existingContracts).Error; err != nil {
		return ctx.Status(fiber.StatusInternalServerError).JSON(types.Response{
			Success: false,
			Error:   "Greška pri proveri postojećih ugovora",
		})
	}

	usedQuantity := 0
	for _, contract := range existingContracts {
		usedQuantity += contract.Quantity
	}

	if usedQuantity+trade.Quantity > portfolio.PublicCount {
		return ctx.Status(fiber.StatusBadRequest).JSON(types.Response{
			Success: false,
			Error:   "Nemate dovoljno raspoloživih akcija za prihvatanje ove ponude",
		})
	}

	trade.Status = "accepted"
	trade.LastModified = time.Now().Unix()
	trade.ModifiedBy = &userID
	if err := db.DB.Save(&trade).Error; err != nil {
		return ctx.Status(fiber.StatusInternalServerError).JSON(types.Response{
			Success: false,
			Error:   "Greška pri ažuriranju ponude",
		})
	}

	contract := types.OptionContract{
		OTCTradeID:   trade.ID,
		BuyerID:      *trade.BuyerID,
		SellerID:     trade.SellerID,
		PortfolioID:  trade.PortfolioID,
		Quantity:     trade.Quantity,
		StrikePrice:  trade.PricePerUnit,
		SecurityID:   trade.SecurityId,
		Premium:      trade.Premium,
		Status:       "active",
		SettlementAt: trade.SettlementAt,
		IsExercised:  false,
		CreatedAt:    time.Now().Unix(),
	}

	buyerAccounts, err := broker.GetAccountsForUser(int64(contract.BuyerID))
	if err != nil {
		return ctx.Status(fiber.StatusInternalServerError).JSON(types.Response{
			Success: false,
			Error:   "Neuspešno dohvatanje računa kupca",
		})
	}

	sellerAccounts, err := broker.GetAccountsForUser(int64(contract.SellerID))
	if err != nil {
		return ctx.Status(fiber.StatusInternalServerError).JSON(types.Response{
			Success: false,
			Error:   "Neuspešno dohvatanje računa prodavca",
		})
	}

	var buyerAccountID, sellerAccountID int64 = -1, -1

	for _, acc := range buyerAccounts {
		if acc.CurrencyType == "USD" {
			buyerAccountID = acc.ID
			break
		}
	}

	// Ako nema USD racun,propbati RSD
	// if buyerAccountID == -1 {
	// 	for _, acc := range buyerAccounts {
	// 		if acc.CurrencyType == "RSD" {
	// 			buyerAccountID = acc.ID
	// 			break
	// 		}
	// 	}
	// }

	for _, acc := range sellerAccounts {
		if acc.CurrencyType == "USD" {
			sellerAccountID = acc.ID
			break
		}
	}
	// Ako nema USD racun,propbati RSD
	// if sellerAccountID == -1 {
	// 	for _, acc := range sellerAccounts {
	// 		if acc.CurrencyType == "RSD" {
	// 			sellerAccountID = acc.ID
	// 			break
	// 		}
	// 	}
	// }
	// DODATI OBRADU VALUTI RACUNA NA BANKING STRANI
	if buyerAccountID == -1 || sellerAccountID == -1 {
		return ctx.Status(fiber.StatusBadRequest).JSON(types.Response{
			Success: false,
			Error:   "Kupac ili prodavac nema USD račun",
		})
	}

	var buyerAccount *dto.Account
	for _, acc := range buyerAccounts {
		if acc.ID == buyerAccountID {
			buyerAccount = &acc
			break
		}
	}
	if buyerAccount == nil {
		return ctx.Status(fiber.StatusInternalServerError).JSON(types.Response{
			Success: false,
			Error:   "Greška pri pronalaženju kupčevog računa",
		})
	}

	if buyerAccount.Balance < contract.Premium {
		return ctx.Status(fiber.StatusBadRequest).JSON(types.Response{
			Success: false,
			Error:   "Kupčev račun nema dovoljno sredstava za plaćanje premije",
		})
	}

	if err := db.DB.Create(&contract).Error; err != nil {
		return ctx.Status(fiber.StatusInternalServerError).JSON(types.Response{
			Success: false,
			Error:   "Greška pri kreiranju ugovora " + err.Error(),
		})
	}

	premiumDTO := &dto.OTCPremiumFeeDTO{
		BuyerAccountId:  uint(buyerAccountID),
		SellerAccountId: uint(sellerAccountID),
		Amount:          contract.Premium,
	}

	if err := broker.SendOTCPremium(premiumDTO); err != nil {
		_ = db.DB.Delete(&contract)
		return ctx.Status(fiber.StatusInternalServerError).JSON(types.Response{
			Success: false,
			Error:   "Greška pri plaćanju premije",
		})
	}

	return ctx.Status(fiber.StatusOK).JSON(types.Response{
		Success: true,
		Data:    fmt.Sprintf("Ponuda uspešno prihvaćena.Premija uspešno isplaćena.Kreiran ugovor: %d", contract.ID),
	})
}

func (c *OTCTradeController) ExecuteOptionContract(ctx *fiber.Ctx) error {
	id := ctx.Params("id")
	userID := uint(ctx.Locals("user_id").(float64))

	var contract types.OptionContract
	if err := db.DB.First(&contract, id).Error; err != nil {
		return ctx.Status(fiber.StatusNotFound).JSON(types.Response{
			Success: false,
			Error:   "Ugovor nije pronađen",
		})
	}

	if contract.BuyerID != userID {
		return ctx.Status(fiber.StatusForbidden).JSON(types.Response{
			Success: false,
			Error:   "Nemate pravo da izvršite ovaj ugovor",
		})
	}

	if contract.IsExercised {
		return ctx.Status(fiber.StatusBadRequest).JSON(types.Response{
			Success: false,
			Error:   "Ovaj ugovor je već iskorišćen",
		})
	}

	if contract.SettlementAt.Before(time.Now()) {
		return ctx.Status(fiber.StatusBadRequest).JSON(types.Response{
			Success: false,
			Error:   "Ugovor je istekao",
		})
	}

	buyerAccounts, err := broker.GetAccountsForUser(int64(contract.BuyerID))
	if err != nil {
		return ctx.Status(fiber.StatusInternalServerError).JSON(types.Response{
			Success: false,
			Error:   "Neuspešno dohvatanje računa kupca",
		})
	}

	sellerAccounts, err := broker.GetAccountsForUser(int64(contract.SellerID))
	if err != nil {
		return ctx.Status(fiber.StatusInternalServerError).JSON(types.Response{
			Success: false,
			Error:   "Neuspešno dohvatanje računa prodavca",
		})
	}

	var buyerAccountID, sellerAccountID int64 = -1, -1

	var buyerAccount *dto.Account
	for _, acc := range buyerAccounts {
		if acc.CurrencyType == "USD" {
			buyerAccountID = acc.ID
			buyerAccount = &acc
			break
		}
	}

	// if buyerAccountID == -1 {
	// 	for _, acc := range buyerAccounts {
	// 		if acc.CurrencyType == "RSD" {
	// 			buyerAccountID = acc.ID
	// 			break
	// 		}
	// 	}
	// }

	for _, acc := range sellerAccounts {
		if acc.CurrencyType == "USD" {
			sellerAccountID = acc.ID
			break
		}
	}

	// if sellerAccountID == -1 {
	// 	for _, acc := range sellerAccounts {
	// 		if acc.CurrencyType == "RSD" {
	// 			sellerAccountID = acc.ID
	// 			break
	// 		}
	// 	}
	// }
	if buyerAccountID == -1 || sellerAccountID == -1 {
		return ctx.Status(fiber.StatusBadRequest).JSON(types.Response{
			Success: false,
			Error:   "Kupac ili prodavac nema USD račun",
		})
	}

	if buyerAccount.Balance < (contract.StrikePrice * float64(contract.Quantity)) {
		return ctx.Status(fiber.StatusBadRequest).JSON(types.Response{
			Success: false,
			Error:   "Kupčev račun nema dovoljno sredstava za izvršavanje ugovora",
		})
	}

	uid := fmt.Sprintf("OTC-%d-%d", contract.ID, time.Now().Unix())

	dto := &types.OTCTransactionInitiationDTO{
		Uid:             uid,
		SellerAccountId: uint(sellerAccountID),
		BuyerAccountId:  uint(buyerAccountID),
		Amount:          contract.StrikePrice * float64(contract.Quantity),
	}

	contract.UID = uid

	if err := db.DB.Save(&contract).Error; err != nil {
		go broker.FailOTC(uid, "Greška prilikom čuvanja statusa ugovora")
		return ctx.Status(fiber.StatusInternalServerError).JSON(types.Response{
			Success: false,
			Error:   "Greška prilikom čuvanja statusa ugovora",
		})
	}

	if err := db.DB.Model(&types.OTCTrade{}).Where("id = ?", contract.OTCTradeID).Update("status", "completed").Error; err != nil {
		go broker.FailOTC(uid, "Greška pri ažuriranju OTC ponude")
		return ctx.Status(fiber.StatusInternalServerError).JSON(types.Response{
			Success: false,
			Error:   "Greška pri ažuriranju OTC ponude",
		})
	}

	if err := saga.StateManager.UpdatePhase(db.DB, uid, types.PhaseInit); err != nil {
		return ctx.Status(fiber.StatusInternalServerError).JSON(types.Response{
			Success: false,
			Error:   "Greška prilikom kreiranja OTC transakcije",
		})
	}

	if err := broker.SendOTCTransactionInit(dto); err != nil {
		return ctx.Status(fiber.StatusInternalServerError).JSON(types.Response{
			Success: false,
			Error:   "Greška prilikom slanja OTC transakcije",
		})
	}

	return ctx.Status(fiber.StatusOK).JSON(types.Response{
		Success: true,
		Data:    "Ugovor uspešno realizovan",
	})
}

func (c *OTCTradeController) GetActiveOffers(ctx *fiber.Ctx) error {
	userID := uint(ctx.Locals("user_id").(float64))
	var trades []types.OTCTrade

	if err := db.DB.
		Preload("Portfolio.Security").
		Where("status = ? AND (buyer_id = ? OR seller_id = ?)", "pending", userID, userID).
		Find(&trades).Error; err != nil {
		return ctx.Status(fiber.StatusInternalServerError).JSON(types.Response{
			Success: false,
			Error:   "Greška prilikom dohvatanja aktivnih ponuda",
		})
	}

	return ctx.JSON(types.Response{
		Success: true,
		Data:    trades,
	})
}

func (c *OTCTradeController) GetUserOptionContracts(ctx *fiber.Ctx) error {
	userID := uint(ctx.Locals("user_id").(float64))
	var contracts []types.OptionContract

	if err := db.DB.
		Preload("Portfolio.Security").
		Preload("OTCTrade.Portfolio.Security").
		Where("buyer_id = ? OR seller_id = ?", userID, userID).
		Find(&contracts).Error; err != nil {
		return ctx.Status(fiber.StatusInternalServerError).JSON(types.Response{
			Success: false,
			Error:   "Greška prilikom dohvatanja ugovora",
		})
	}

	return ctx.JSON(types.Response{
		Success: true,
		Data:    contracts,
	})
}

func (c *OTCTradeController) RejectOTCTrade(ctx *fiber.Ctx) error {
	id := ctx.Params("id")
	userID := uint(ctx.Locals("user_id").(float64))

	var trade types.OTCTrade
	if err := db.DB.First(&trade, id).Error; err != nil {
		return ctx.Status(fiber.StatusNotFound).JSON(types.Response{
			Success: false,
			Error:   "Ponuda nije pronađena",
		})
	}

	if trade.ModifiedBy != nil && *trade.ModifiedBy == userID {
		return ctx.Status(fiber.StatusForbidden).JSON(types.Response{
			Success: false,
			Error:   "Ne možete odbiti sopstvenu ponudu",
		})
	}

	if trade.Status != "pending" {
		return ctx.Status(fiber.StatusBadRequest).JSON(types.Response{
			Success: false,
			Error:   "Ponuda više nije aktivna",
		})
	}

	trade.Status = "rejected"
	trade.LastModified = time.Now().Unix()
	trade.ModifiedBy = &userID

	if err := db.DB.Save(&trade).Error; err != nil {
		return ctx.Status(fiber.StatusInternalServerError).JSON(types.Response{
			Success: false,
			Error:   "Greška pri odbijanju ponude",
		})
	}

	return ctx.JSON(types.Response{
		Success: true,
		Data:    "Ponuda je uspešno odbijena",
	})
}

type PortfolioControllerr struct{}

func NewPortfolioControllerr() *PortfolioControllerr {
	return &PortfolioControllerr{}
}

func (c *PortfolioControllerr) GetAllPublicPortfolios(ctx *fiber.Ctx) error {
	var portfolios []types.Portfolio

	userID := uint(ctx.Locals("user_id").(float64))

	if err := db.DB.
		Where("public_count > 0 AND user_id != ?", userID).
		Preload("Security").
		Find(&portfolios).Error; err != nil {
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"success": false,
			"error":   "Greška prilikom dohvatanja javnih portfolija",
		})
	}

	return ctx.JSON(fiber.Map{
		"success": true,
		"data":    portfolios,
	})
}

func InitPortfolioRoutess(app *fiber.App) {
	portfolioController := NewPortfolioControllerr()

	app.Get("/portfolio/public", middlewares.Auth, portfolioController.GetAllPublicPortfolios)
}

func InitOTCTradeRoutes(app *fiber.App) {
	otcController := NewOTCTradeController()
	otc := app.Group("/otctrade", middlewares.Auth)

	otc.Post("/offer", middlewares.RequirePermission("user.customer.otc_trade"), otcController.CreateOTCTrade)
	otc.Put("/offer/:id/counter", middlewares.RequirePermission("user.customer.otc_trade"), otcController.CounterOfferOTCTrade)
	otc.Put("/offer/:id/accept", middlewares.RequirePermission("user.customer.otc_trade"), otcController.AcceptOTCTrade)
	otc.Put("/offer/:id/reject", middlewares.RequirePermission("user.customer.otc_trade"), otcController.RejectOTCTrade)
	otc.Put("/option/:id/execute", middlewares.RequirePermission("user.customer.otc_trade"), otcController.ExecuteOptionContract)
	otc.Get("/offer/active", otcController.GetActiveOffers)
	otc.Get("/option/contracts", otcController.GetUserOptionContracts)
}
