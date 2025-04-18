package orders

import (
	"banka1.com/broker"
	"banka1.com/dto"
	"database/sql"
	"errors"
	"fmt"
	"math/rand"
	"strings"
	"sync"
	"time"

	"banka1.com/db"
	"banka1.com/types"
	"gorm.io/gorm"
)

var (
	securityLocks = make(map[uint]*sync.Mutex)
	locksMu       sync.Mutex
	orderLocks    = make(map[uint]*sync.Mutex)
	orderLocksMu  sync.Mutex
)

func CalculateFee(order types.Order, total float64) float64 {
	switch strings.ToUpper(order.OrderType) {
	case "MARKET":
		fee := total * 0.14
		if fee > 7 {
			return 7
		}
		return fee
	case "LIMIT":
		fee := total * 0.24
		if fee > 12 {
			return 12
		}
		return fee
	default:
		return 0
	}
}

// Funkcija koja vraća uvek isti mutex po securityID
func getLock(securityID uint) *sync.Mutex {
	locksMu.Lock()
	defer locksMu.Unlock()

	if _, exists := securityLocks[securityID]; !exists {
		securityLocks[securityID] = &sync.Mutex{}
	}
	return securityLocks[securityID]
}

func getOrderLock(orderID uint) *sync.Mutex {
	orderLocksMu.Lock()
	defer orderLocksMu.Unlock()

	if _, exists := orderLocks[orderID]; !exists {
		orderLocks[orderID] = &sync.Mutex{}
	}
	return orderLocks[orderID]
}

func MatchOrder(order types.Order) {
	go func() {
		defer func() {
			if r := recover(); r != nil {
				fmt.Printf("Gorutina pukla u MatchOrder! Panic: %v\n", r)
			}
		}()
		// Zaključavanje po ORDER ID – sprečava paralelno izvršavanje istog ordera
		orderLock := getOrderLock(order.ID)
		orderLock.Lock()
		defer orderLock.Unlock()

		if err := db.DB.First(&order, order.ID).Error; err != nil {
			fmt.Printf("Greska pri refetch ordera %d: %v\n", order.ID, err)
			return
		}

		if order.AON {
			if !CanExecuteAll(order) {
				fmt.Println("AON: Nema dovoljno za celokupan order")
				return
			}
		}

		if !canPreExecute(order) {
			fmt.Println("Nije ispunjen uslov za order")
			return
		}

		for order.RemainingParts != nil && *order.RemainingParts > 0 {
			fmt.Printf("Novi krug matchovanja za Order %d | Remaining: %d\n", order.ID, *order.RemainingParts)

			tx := db.DB.Begin()

			// Ponovo proveri order iz baze unutar transakcije
			if err := tx.First(&order, order.ID).Error; err != nil {
				fmt.Printf("Order nije pronađen u transakciji: %v\n", err)
				tx.Rollback()
				break
			}

			price := getOrderPrice(order)

			// izvršavanje koliko može
			matchQuantity := executePartial(order, price, tx)
			if order.RemainingParts != nil {
				fmt.Printf("Nakon executePartial: remaining=%d\n", *order.RemainingParts)
			} else {
				fmt.Printf("order.RemainingParts je NIL nakon executePartial\n")
			}

			if matchQuantity == 0 {
				fmt.Printf("BREAK: Nije pronađen validan match za Order %d, remaining=%d\n", order.ID, *order.RemainingParts)
				tx.Rollback()
				break
			}
			//TO-DO PROVERITI OVO ISPOD
			//executePartial(order, quantityToExecute, price, tx)

			//if order.RemainingParts == nil || *order.RemainingParts == 0 {
			//	order.IsDone = true
			//	order.Status = "done"
			//}

			// SKIDANJE unita ako je kupovina (smanjuje se dostupnost hartija)
			//if order.Direction == "buy" {
			//	var security types.Security
			//	if err := tx.First(&security, order.SecurityID).Error; err == nil {
			//		security.Volume -= int64(quantityToExecute)
			//		if security.Volume < 0 {
			//			security.Volume = 0
			//		}
			//		tx.Save(&security)
			//	}
			//}

			// Ažuriraj order u bazi (unutar transakcije)
			if err := tx.Model(&types.Order{}).Where("id = ?", order.ID).Update("remaining_parts", *order.RemainingParts).Error; err != nil {
				fmt.Printf("Greska pri upisu remaining_parts: %v\n", err)
				tx.Rollback()
				break
			}

			//if err := tx.Commit().Error; err != nil {
			//	fmt.Printf("Nalog %v nije izvršen: %v\n", order.ID, err)
			//	tx.Rollback()
			//	break
			//}

			// Ažuriraj volume preko helper funkcije
			if err := UpdateAvailableVolumeTx(tx, order.SecurityID); err != nil {
				fmt.Printf("Greska pri UpdateAvailableVolume: %v\n", err)
				tx.Rollback()
				break
			}

			//// SKIDANJE unita ako je kupovina (smanjuje se dostupnost hartija)
			//if order.Direction == "buy" {
			//	var security types.Security
			//	if err := tx.First(&security, order.SecurityID).Error; err == nil {
			//		security.Volume -= int64(matchQuantity)
			//		if security.Volume < 0 {
			//			security.Volume = 0
			//		}
			//		tx.Save(&security)
			//	}
			//}

			//// Ažuriraj RemainingParts u bazi (bez is_done/status!)
			//if err := tx.Model(&types.Order{}).Where("id = ?", order.ID).Update("remaining_parts", *order.RemainingParts).Error; err != nil {
			//	fmt.Printf("Greska pri upisu remaining_parts: %v\n", err)
			//	tx.Rollback()
			//	break
			//}

			if err := tx.Commit().Error; err != nil {
				fmt.Printf("Nalog %v nije izvršen: %v\n", order.ID, err)
				tx.Rollback()
				break
			}

			// Refetch ponovo da zna koliko još ima
			if err := db.DB.First(&order, order.ID).Error; err != nil {
				fmt.Printf("Greska pri refetch ordera %d nakon commit-a: %v\n", order.ID, err)
				break
			}

			if order.RemainingParts == nil {
				fmt.Printf("RemainingParts je NIL nakon commita, orderID = %d\n", order.ID)
			} else {
				fmt.Printf("Order %d refetch: remaining = %d\n", order.ID, *order.RemainingParts)
			}

			if order.RemainingParts == nil || *order.RemainingParts <= 0 {
				fmt.Printf("Order %d je već izvršen ili nema više delova za obradu\n", order.ID)
				break
			}

			fmt.Printf("Pauza pre sledećeg pokušaja za Order %d\n", order.ID)
			delay := calculateDelay(order)
			time.Sleep(delay)
		}

		// Konačna provera na kraju svih mečeva
		if order.RemainingParts != nil && *order.RemainingParts == 0 {
			db.DB.Model(&types.Order{}).Where("id = ?", order.ID).Updates(map[string]interface{}{
				"is_done": true,
				"status":  "done",
			})
			fmt.Printf("Order %d označen kao završen nakon svih mečeva\n", order.ID)
		} else {
			fmt.Printf("Order %d ostaje neizvršen | Remaining: %d\n", order.ID, *order.RemainingParts)
		}
	}()
}

func getListingPrice(order types.Order) float64 {
	var security types.Security
	err := db.DB.First(&security, order.SecurityID).Error
	if err != nil {
		fmt.Printf("Security nije pronadjen za ID %d: %v\n", order.SecurityID, err)
		return -1.0
	}

	var listing types.Listing
	err = db.DB.Where("ticker = ?", security.Ticker).First(&listing).Error
	if err != nil {
		fmt.Printf("Listing nije pronadjen za Ticker %s: %v\n", security.Ticker, err)
		return -1.0
	}

	if order.Direction == "sell" {
		return float64(listing.Bid)
	} else {
		return float64(listing.Ask)
	}
}

func getOrderPrice(order types.Order) float64 {
	if strings.ToUpper(order.OrderType) == "MARKET" {
		var security types.Security
		db.DB.First(&security, order.SecurityID)
		return security.LastPrice
	}
	if order.StopPricePerUnit != nil {
		return *order.StopPricePerUnit
	}
	if order.LimitPricePerUnit != nil {
		return *order.LimitPricePerUnit
	}
	return 0.0
}

func executePartial(order types.Order, price float64, tx *gorm.DB) int {
	lock := getLock(order.SecurityID)
	lock.Lock()
	defer lock.Unlock()

	if order.Status == "done" || order.RemainingParts == nil || *order.RemainingParts <= 0 {
		fmt.Printf("Order %d je već završen ili nema remaining parts\n", order.ID)
		return 0
	}

	var match types.Order
	direction := "buy"
	if strings.ToLower(order.Direction) == "buy" {
		direction = "sell"
	} else {
		direction = "buy"
	}

	fmt.Printf("Pokušavam da pronađem match za Order %d...\n", order.ID)

	tx.Debug().Where("security_id = ? AND direction = ? AND status = ? AND is_done = ?", order.SecurityID, direction, "approved", false).
		Order("last_modified").
		First(&match)

	fmt.Printf("Pokušan match — pronađen ID=%d, userID=%d\n", match.ID, match.UserID)

	if match.ID == 0 {
		fmt.Println("Nema dostupnog ordera za matchovanje - match.ID == 0")
		return 0
	}

	// Provere za matchovani order
	if match.UserID == order.UserID {
		fmt.Println("Preskočen self-match")
		return 0
	}

	if match.AccountID == 0 {
		fmt.Println("Matchovani order ima account_id = 0, preskačem ga...")
		return 0
	}

	if match.AON {
		if match.RemainingParts == nil || order.RemainingParts == nil {
			fmt.Println("AON match bez remaining parts")
			return 0
		}
		if *match.RemainingParts < *order.RemainingParts {
			fmt.Println("Matchovani order je AON i ne može da se izvrši u celosti")
			return 0
		}
	}

	// Po specifikaciji, MARKET BUY ne proverava matchov limit
	if !(strings.ToUpper(order.OrderType) == "MARKET" && strings.ToLower(order.Direction) == "buy") {
		if !canPreExecute(match) {
			fmt.Println("Preskočen match sa nedovoljnim uslovima")
			return 0
		}
	}

	marginOrder := order
	if !order.Margin && match.Margin == true {
		marginOrder = match
	}

	if marginOrder.Margin {
		var actuary types.Actuary
		if err := tx.Where("user_id = ?", marginOrder.UserID).First(&actuary).Error; err != nil || actuary.Department != "agent" {
			fmt.Println("Matchovani margin order nema validnog aktuara")
			return 0
		}
		if actuary.Department != "agent" {
			fmt.Println("Korisnik nije agent")
			return 0
		}
		if marginOrder.RemainingParts == nil {
			fmt.Println("RemainingParts je nil u margin logici")
			return 0
		}
		initialMargin := price * float64(*marginOrder.RemainingParts) * 0.3 * 1.1
		if actuary.LimitAmount-actuary.UsedLimit < initialMargin {
			fmt.Println("Matchovani margin order nema dovoljno limita")
			return 0
		}
	}

	matchQty := min(
		ptrSafe(order.RemainingParts),
		ptrSafe(match.RemainingParts),
	)

	if matchQty <= 0 {
		fmt.Printf("Nevalidan matchQty = %d za Order %d\n", matchQty, order.ID)
		return 0
	}

	uid := fmt.Sprintf("ORDER-match-%d-%d", order.ID, time.Now().Unix())
	total := price * float64(matchQty)
	fee := CalculateFee(order, total)
	initiationDto := dto.OrderTransactionInitiationDTO{
		Uid:             uid,
		SellerAccountId: getSellerAccountID(order, match),
		BuyerAccountId:  getBuyerAccountID(order, match),
		Amount:          total,
		Fee:             fee,
		Direction:       order.Direction,
	}

	fmt.Println("Šaljem OrderTransactionInitiationDTO preko brokera...")

	err := broker.SendOrderTransactionInit(&initiationDto)
	if err != nil {
		fmt.Printf("Greska pri slanju OrderTransactionInitiationDTO preko brokera: %v\n", err)
		tx.Rollback()
		return 0
	}

	txn := types.Transaction{
		OrderID:      order.ID,
		BuyerID:      getBuyerID(order, match),
		SellerID:     getSellerID(order, match),
		SecurityID:   order.SecurityID,
		Quantity:     matchQty,
		PricePerUnit: price,
		TotalPrice:   price * float64(matchQty),
	}
	if err := tx.Create(&txn).Error; err != nil {
		fmt.Printf("Greska pri kreiranju transakcije: %v\n", err)
		return 0
	}

	if order.RemainingParts == nil {
		tmp := order.Quantity
		order.RemainingParts = &tmp
	}
	*order.RemainingParts -= matchQty

	if match.RemainingParts == nil {
		tmp := match.Quantity
		match.RemainingParts = &tmp
	}
	*match.RemainingParts -= matchQty

	if *match.RemainingParts == 0 {
		match.IsDone = true
		match.Status = "done"
	}

	if err := tx.Save(&order).Error; err != nil {
		fmt.Printf("Greska pri save za order: %v\n", err)
		return 0
	}
	if err := tx.Save(&match).Error; err != nil {
		fmt.Printf("Greska pri save za match: %v\n", err)
		return 0
	}

	updatePortfolio(getBuyerID(order, match), order.SecurityID, matchQty, tx)
	updatePortfolio(getSellerID(order, match), order.SecurityID, -matchQty, tx)

	if order.Margin {
		var actuary types.Actuary
		if err := tx.Where("user_id = ?", order.UserID).First(&actuary).Error; err == nil {
			initialMargin := price * float64(matchQty) * 0.3 * 1.1
			actuary.UsedLimit += initialMargin
			tx.Save(&actuary)
		}
	}

	fmt.Printf("Match success: Order %d ↔ Order %d za %d @ %.2f\n", order.ID, match.ID, matchQty, price)
	return matchQty
}

func updatePortfolio(userID uint, securityID uint, delta int, tx *gorm.DB) {
	var portfolio types.Portfolio
	err := tx.Where("user_id = ? AND security_id = ?", userID, securityID).First(&portfolio).Error

	if errors.Is(err, gorm.ErrRecordNotFound) {
		if delta > 0 {
			portfolio = types.Portfolio{
				UserID:     userID,
				SecurityID: securityID,
				Quantity:   delta,
			}
			if err := tx.Create(&portfolio).Error; err != nil {
				fmt.Printf("Portfolio greška u create: user=%d, security=%d, delta=%d | %v\n", userID, securityID, delta, err)
			} else {
				fmt.Printf("Portfolio kreiran: user=%d, security=%d, quantity=%d\n", userID, securityID, delta)
			}
		} else {
			fmt.Printf("Nema postojeći portfolio za korisnika %d i security %d, a pokušaj da se oduzme delta=%d\n", userID, securityID, delta)
		}
		return
	}

	if err != nil {
		fmt.Printf("Greska pri dohvatanju portfolia: user=%d, security=%d | %v\n", userID, securityID, err)
		return
	}

	portfolio.Quantity += delta
	if portfolio.Quantity <= 0 {
		err = tx.Delete(&portfolio).Error
		if err != nil {
			fmt.Printf("Portfolio greška pri brisanju: user=%d, security=%d | %v\n", userID, securityID, err)
		} else {
			fmt.Printf("Portfolio obrisan: user=%d, security=%d\n", userID, securityID)
		}
	} else {
		err = tx.Save(&portfolio).Error
		if err != nil {
			fmt.Printf("Portfolio greška pri update: user=%d, security=%d | %v\n", userID, securityID, err)
		} else {
			fmt.Printf("Portfolio ažuriran: user=%d, security=%d, quantity=%d\n", userID, securityID, portfolio.Quantity)
		}
	}
}

func calculateDelay(order types.Order) time.Duration {
	delaySeconds := rand.Intn(10) + 1
	if order.AfterHours {
		return time.Duration(delaySeconds+1800) * time.Second
	}
	return time.Duration(delaySeconds) * time.Second
}

func getExecutableParts(order types.Order) int {
	var matchingOrders []types.Order
	direction := "buy"
	if order.Direction == "buy" {
		direction = "sell"
	} else {
		direction = "buy"
	}

	db.DB.Where("security_id = ? AND direction = ? AND status = ? AND is_done = ?", order.SecurityID, direction, "approved", false).Find(&matchingOrders)
	totalAvailable := 0
	for _, o := range matchingOrders {
		if o.RemainingParts != nil {
			totalAvailable += *o.RemainingParts
		}
	}

	return totalAvailable
}

func CanExecuteAll(order types.Order) bool {
	return getExecutableParts(order) >= *order.RemainingParts
}

func CanExecuteAny(order types.Order) bool {
	return getExecutableParts(order) > 0
}

func canPreExecute(order types.Order) bool {
	if !IsSettlementDateValid(&order) {
		return false
	}

	if strings.ToUpper(order.OrderType) == "LIMIT" {
		if order.LimitPricePerUnit == nil {
			fmt.Println("LIMIT order bez LimitPricePerUnit")
			return false
		}
		price := getListingPrice(order)
		if price < 0 {
			return false
		}
		if order.Direction == "sell" {
			return price >= *order.LimitPricePerUnit
		} else {
			return price <= *order.LimitPricePerUnit
		}
	} else if strings.ToUpper(order.OrderType) == "STOP" {
		if order.StopPricePerUnit == nil {
			fmt.Println("STOP order bez StopPricePerUnit")
			return false
		}
		price := getListingPrice(order)
		if price < 0 {
			return false
		}
		if order.Direction == "sell" {
			return price <= *order.StopPricePerUnit
		} else {
			return price >= *order.StopPricePerUnit
		}
	} else if strings.ToUpper(order.OrderType) == "STOP-LIMIT" {
		if order.LimitPricePerUnit == nil || order.StopPricePerUnit == nil {
			fmt.Println("STOP-LIMIT order bez neophodnih cena")
			return false
		}
		price := getListingPrice(order)
		if price < 0 {
			return false
		}
		if order.Direction == "sell" {
			return price <= *order.StopPricePerUnit && price >= *order.LimitPricePerUnit
		} else {
			return price >= *order.StopPricePerUnit && price <= *order.LimitPricePerUnit
		}
	} else if strings.ToUpper(order.OrderType) == "MARKET" {
		return true
	}
	return true
}

func getBuyerID(a, b types.Order) uint {
	if strings.ToLower(a.Direction) == "buy" {
		return a.UserID
	}
	return b.UserID
}

func getSellerID(a, b types.Order) uint {
	if strings.ToLower(a.Direction) == "sell" {
		return a.UserID
	}
	return b.UserID
}

func ptrSafe(ptr *int) int {
	if ptr == nil {
		return 0
	}
	return *ptr
}

func IsSettlementDateValid(order *types.Order) bool {
	if order.Security.ID == 0 {
		var security types.Security
		if err := db.DB.First(&security, order.SecurityID).Error; err != nil {
			fmt.Printf("Nije pronađena hartija %d za order %d: %v\n", order.SecurityID, order.ID, err)
			return false
		}
		order.Security = security
	}

	if order.Security.SettlementDate != nil {
		parsed, err := time.Parse("2006-01-02", *order.Security.SettlementDate)
		if err != nil {
			fmt.Printf("Nevalidan settlementDate za hartiju %d: %v\n", order.Security.ID, err)
			return false
		}

		// Poredi samo po danima, ne po satu
		now := time.Now().Truncate(24 * time.Hour)
		parsed = parsed.Truncate(24 * time.Hour)

		if parsed.Before(now) {
			fmt.Printf("Hartiji %d je istekao settlementDate: %s\n", order.Security.ID, *order.Security.SettlementDate)
			return false
		}
	}

	return true
}

func UpdateAvailableVolume(securityID uint) error {
	return UpdateAvailableVolumeTx(db.DB, securityID)
}

func UpdateAvailableVolumeTx(tx *gorm.DB, securityID uint) error {
	var total sql.NullInt64

	// Direktno koristi RAW SQL da izbegnemo GORM probleme sa pointerima i imenovanjem
	query := `
		SELECT SUM(remaining_parts)
		FROM "order"
		WHERE security_id = ?
		  AND lower(direction) = 'sell'
		  AND lower(status) = 'approved'
		  AND COALESCE(is_done, false) = false
	`

	err := tx.Raw(query, securityID).Scan(&total).Error
	if err != nil {
		return fmt.Errorf("greska pri izvrsavanju SUM upita: %w", err)
	}

	final := int64(0)
	if total.Valid {
		final = total.Int64
	}

	// Ažuriraj volume u security tabeli
	return tx.Model(&types.Security{}).
		Where("id = ?", securityID).
		Update("volume", final).Error
}

func getBuyerAccountID(a, b types.Order) uint {
	if strings.ToLower(a.Direction) == "buy" {
		return a.AccountID
	}
	return b.AccountID
}

func getSellerAccountID(a, b types.Order) uint {
	if strings.ToLower(a.Direction) == "sell" {
		return a.AccountID
	}
	return b.AccountID
}
