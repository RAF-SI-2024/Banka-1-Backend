package orders

import (
	"banka1.com/db"
	"banka1.com/types"
	"log"
)

// Stop Order: proverava sve STOP ordere i aktivira one za koje je ispunjen uslov
func ExecuteStopOrders() {
	var stopOrders []types.Order

	err := db.DB.Preload("Security").Where(
		"order_type = ? AND status = ? AND is_done = ?", "stop", "approved", false,
	).Find(&stopOrders).Error
	if err != nil {
		log.Printf("Greska prilikom dohvatanja STOP ordera: %v", err)
		return
	}

	for _, order := range stopOrders {
		var security types.Security
		err := db.DB.First(&security, order.SecurityID).Error
		if err != nil {
			log.Printf("Security nije pronadjen za ID %d: %v", order.SecurityID, err)
			continue
		}

		var listing types.Listing
		err = db.DB.Where("ticker = ?", security.Ticker).First(&listing).Error
		if err != nil {
			log.Printf("Listing nije pronadjen za Ticker %s: %v", security.Ticker, err)
			continue
		}

		if order.Direction == "buy" && order.StopPricePerUnit != nil && float64(listing.Ask) >= *order.StopPricePerUnit {
			log.Printf("Aktiviran BUY stop order %d\n", order.ID)
			// Pozvati funkciju za market order
			// ExecuteMarketOrder(order)
		} else if order.Direction == "sell" && order.StopPricePerUnit != nil && float64(listing.Bid) <= *order.StopPricePerUnit {
			log.Printf("Aktiviran SELL stop order %d\n", order.ID)
			// Pozvati funkciju za market order
			// ExecuteMarketOrder(order)
		}
	}
}

// Stop-Limit Order: proverava sve STOP LIMIT ordere i aktivira one za koje je ispunjen uslov
func ExecuteStopLimitOrders() {
	var orders []types.Order

	err := db.DB.Preload("Security").Where(
		"order_type = ? AND status = ? AND is_done = false",
		"stop-limit", "approved",
	).Find(&orders).Error
	if err != nil {
		log.Printf("Greska prilikom dohvatanja STOP ordera: %v", err)
		return
	}

	for _, order := range orders {
		var security types.Security
		err := db.DB.First(&security, order.SecurityID).Error
		if err != nil {
			log.Printf("Security nije pronadjen za ID %d: %v", order.SecurityID, err)
			continue
		}

		var listing types.Listing
		err = db.DB.Where("ticker = ?", security.Ticker).First(&listing).Error
		if err != nil {
			log.Printf("Listing nije pronadjen za Ticker %s: %v", security.Ticker, err)
			continue
		}

		if order.Direction == "buy" && order.StopPricePerUnit != nil && order.LimitPricePerUnit != nil {
			if float64(listing.Ask) >= *order.StopPricePerUnit && float64(listing.Ask) <= *order.LimitPricePerUnit {
				log.Printf("[STOP-LIMIT] BUY order %d aktiviran kao Limit Order (Ask: %.2f, Stop: %.2f, Limit: %.2f)\n",
					order.ID, listing.Ask, *order.StopPricePerUnit, *order.LimitPricePerUnit)
				// Pozvati  funkciju za limit order
				// ExecuteLimitOrder(order)
			}
		} else if order.Direction == "sell" && order.StopPricePerUnit != nil && order.LimitPricePerUnit != nil {
			if float64(listing.Bid) <= *order.StopPricePerUnit && float64(listing.Bid) >= *order.LimitPricePerUnit {
				log.Printf("[STOP-LIMIT] SELL order %d aktiviran kao Limit Order (Bid: %.2f, Stop: %.2f, Limit: %.2f)\n",
					order.ID, listing.Bid, *order.StopPricePerUnit, *order.LimitPricePerUnit)
				// Pozvati funkciju za limit SELL order
				// ExecuteLimitOrder(order)
			}
		}
	}
}
