package orders

import (
	"banka1.com/broker"
	"fmt"
	"log"
	"time"

	"banka1.com/db"
	"banka1.com/types"
)

func LoadOrders() {

	var msftSecurity types.Security
	var aaplSecurity types.Security
	var googlSecurity types.Security
	var nvdaSecurity types.Security

	db.DB.Where("ticker = ?", "MSFT").First(&msftSecurity)
	db.DB.Where("ticker = ?", "AAPL").First(&aaplSecurity)
	db.DB.Where("ticker = ?", "GOOGL").First(&googlSecurity)
	db.DB.Where("ticker = ?", "NVDA").First(&nvdaSecurity)

	order1 := types.Order{
		UserID:       3,
		AccountID:    1,
		SecurityID:   msftSecurity.ID,
		OrderType:    "Market",
		Quantity:     10,
		ContractSize: 1,
		Direction:    "Buy",
		Status:       "Approved",
		IsDone:       true,
	}

	order2 := types.Order{
		UserID:       3,
		AccountID:    1,
		SecurityID:   aaplSecurity.ID,
		OrderType:    "Market",
		Quantity:     5,
		ContractSize: 1,
		Direction:    "Buy",
		Status:       "Approved",
		IsDone:       true,
	}

	if err := db.DB.FirstOrCreate(&order1, types.Order{
		UserID:     order1.UserID,
		SecurityID: order1.SecurityID,
		Direction:  order1.Direction,
	}).Error; err != nil {
		log.Println("Error while adding order1:", err)
	} else {
		log.Printf("Order succesfully added to Security ID %d (order1)\n", order1.SecurityID)
	}

	if err := db.DB.FirstOrCreate(&order2, types.Order{
		UserID:     order2.UserID,
		SecurityID: order2.SecurityID,
		Direction:  order2.Direction,
	}).Error; err != nil {
		log.Println("Error while adding order2:", err)
	} else {
		log.Printf("Order succesfully added to Security ID %d (order2)\n", order2.SecurityID)
	}
}

func LoadPortfolios() {

	// get msft Security
	// select from securities where ticker = 'MSFT'

	var msftSecurity types.Security
	var aaplSecurity types.Security
	var googlSecurity types.Security
	var nvdaSecurity types.Security

	db.DB.Where("ticker = ?", "MSFT").First(&msftSecurity)
	db.DB.Where("ticker = ?", "AAPL").First(&aaplSecurity)
	db.DB.Where("ticker = ?", "GOOGL").First(&googlSecurity)
	db.DB.Where("ticker = ?", "NVDA").First(&nvdaSecurity)

	log.Println("MSFT Security preuzeta:", msftSecurity.Ticker)

	portfolio1 := types.Portfolio{
		UserID:        1,
		SecurityID:    aaplSecurity.ID, // AAPL
		Quantity:      20,
		PurchasePrice: 199.99,
		PublicCount:   10,
	}

	portfolio2 := types.Portfolio{
		UserID:        1,
		SecurityID:    googlSecurity.ID, // GOOGL
		Quantity:      20,
		PurchasePrice: 299.99,
		PublicCount:   10,
	}

	portfolio3 := types.Portfolio{
		UserID:        3,
		SecurityID:    msftSecurity.ID, // MSFT
		Quantity:      20,
		PurchasePrice: 299.99,
		PublicCount:   10,
	}

	portfolio4 := types.Portfolio{
		UserID:        3,
		SecurityID:    nvdaSecurity.ID, // NVDA
		Quantity:      20,
		PurchasePrice: 299.99,
		PublicCount:   10,
	}

	if err := db.DB.FirstOrCreate(&portfolio1, types.Portfolio{
		UserID:     portfolio1.UserID,
		SecurityID: portfolio1.SecurityID,
	}).Error; err != nil {
		log.Println("Greška pri dodavanju portfolio3:", err)
	} else {
		log.Println("Portfolio1 uspešno dodat")
	}
	if err := db.DB.FirstOrCreate(&portfolio2, types.Portfolio{
		UserID:     portfolio2.UserID,
		SecurityID: portfolio2.SecurityID,
	}).Error; err != nil {
		log.Println("Greška pri dodavanju portfolio4:", err)
	} else {
		log.Println("Portfolio2 uspešno dodat")
	}
	if err := db.DB.FirstOrCreate(&portfolio3, types.Portfolio{
		UserID:     portfolio3.UserID,
		SecurityID: portfolio3.SecurityID,
	}).Error; err != nil {
		log.Println("Greška pri dodavanju portfolio5:", err)
	} else {
		log.Println("Portfolio3 uspešno dodat")
	}
	if err := db.DB.FirstOrCreate(&portfolio4, types.Portfolio{
		UserID:     portfolio4.UserID,
		SecurityID: portfolio4.SecurityID,
	}).Error; err != nil {
		log.Println("Greška pri dodavanju portfolio5:", err)
	} else {
		log.Println("Portfolio4 uspešno dodat")
	}
}

func CreateInitialSellOrdersFromBank() {
	const InitialQuantity = 50
	const BankUserId = 5
	orderTypes := []string{"LIMIT", "MARKET", "STOP", "STOP-LIMIT"}

	var securities []types.Security
	if err := db.DB.Find(&securities).Error; err != nil {
		log.Printf("[ERROR] Ne mogu da dohvatim hartije: %v\n", err)
		return
	}

	// Dohvatanje računa banke
	sellerAccounts, err := broker.GetAccountsForUser(int64(BankUserId))
	if err != nil {
		log.Printf("[GRESKA] Nije moguće dohvatiti naloge za BankUserId %d: %v", BankUserId, err)
		return
	}

	var bankAccountId uint
	for _, acc := range sellerAccounts {
		if acc.CurrencyType == "RSD" {
			bankAccountId = uint(acc.ID)
			break
		}
	}

	if bankAccountId == 0 {
		log.Printf("[GRESKA] Nije pronađen RSD account za BankUserId %d", BankUserId)
		return
	}

	for _, sec := range securities {
		if sec.Ticker == "MSFT" {
			log.Printf("Preskačem SELL ordere za MSFT zbog testiranja\n")
			continue
		}
		price := sec.LastPrice
		if price <= 0 {
			price = 100.0
		}
		limit := price
		stop := price * 0.95

		for _, ot := range orderTypes {
			// Proveri da li već postoji takav SELL order
			var existing types.Order
			err := db.DB.Where("user_id = ? AND security_id = ? AND direction = ? AND order_type = ?", BankUserId, sec.ID, "sell", ot).First(&existing).Error
			if err == nil {
				continue
			}

			order := types.Order{
				UserID:         BankUserId,
				AccountID:      bankAccountId,
				SecurityID:     sec.ID,
				Direction:      "sell",
				OrderType:      ot,
				Quantity:       InitialQuantity,
				RemainingParts: ptr(InitialQuantity),
				Status:         "approved",
				IsDone:         false,
				LastModified:   time.Now().Unix(),
			}

			switch ot {
			case "LIMIT":
				order.LimitPricePerUnit = &limit
			case "STOP":
				order.StopPricePerUnit = &stop
			case "STOP-LIMIT":
				order.LimitPricePerUnit = &limit
				order.StopPricePerUnit = &stop
			}

			// Kreiraj portfolijo ako ne postoji
			var p types.Portfolio
			err = db.DB.Where("user_id = ? AND security_id = ?", BankUserId, sec.ID).First(&p).Error
			if err != nil {
				p = types.Portfolio{
					UserID:     BankUserId,
					SecurityID: sec.ID,
					Quantity:   InitialQuantity * len(orderTypes),
				}
				_ = db.DB.Create(&p)
			}

			fmt.Printf("Kreiram SELL order za %s (TIP=%s)\n", sec.Ticker, ot)
			if err := db.DB.Create(&order).Error; err != nil {
				log.Printf("Greska pri kreiranju SELL ordera za %s (tip=%s): %v\n", sec.Ticker, ot, err)
			}

			_ = UpdateAvailableVolume(sec.ID)
		}
	}
}

func ptr(i int) *int { return &i }
