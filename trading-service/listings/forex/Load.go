package forex

import (
	"banka1.com/db"
	"banka1.com/types"
	"encoding/json"
	"fmt"
	"github.com/gofiber/fiber/v2"
	"github.com/gofiber/fiber/v2/log"
	"strings"
	"time"
)

var currencies = []string{
	"USD",
	"EUR",
	"BTC",
	"RSD",
	"XAU",
}

func LoadDefaultForexPairs() {
	for _, base := range currencies {
		// Don't respect API rate limits, Cloudflare should handle it
		err := CreatePairsFromAPI(base)
		if err != nil {
			log.Warnf("Failed to load all forex pairs: %v", err)
			return
		}
	}
}

func CreatePairsFromAPI(base string) error {
	url := fmt.Sprintf("https://latest.currency-api.pages.dev/v1/currencies/%s.json", strings.ToLower(base))
	agent := fiber.Get(url)
	statusCode, body, errs := agent.Bytes()
	if len(errs) > 0 {
		log.Warnf("Failed to fetch %s: %v\n", url, errs)
		return nil
	}
	if statusCode != 200 {
		log.Warnf("Failed to fetch %s: %s\n", url, body)
		return nil
	}
	var data fiber.Map
	err := json.Unmarshal(body, &data)
	if err != nil {
		log.Warnf("Failed to unmarshal %s: %v\n", url, err)
		return nil
	}
	rates := data[strings.ToLower(base)].(map[string]interface{})
	for _, quote := range currencies {
		if quote == base {
			continue
		}
		rate := rates[strings.ToLower(quote)].(float64)
		ticker := fmt.Sprintf("%s/%s", base, quote)
		lastRefresh := time.Now()
		tx := db.DB.Begin()
		var listing types.Listing
		if err := tx.Where("ticker = ?", ticker).First(&listing).Error; err != nil {
			listing = types.Listing{
				Ticker:       ticker,
				Name:         fmt.Sprintf("Forex %s", ticker),
				ExchangeID:   1,
				LastRefresh:  lastRefresh,
				Price:        float32(rate),
				Ask:          float32(rate),
				Bid:          float32(rate),
				Type:         "Forex",
				ContractSize: 1000,
			}
			if err := tx.Create(&listing).Error; err != nil {
				tx.Rollback()
				log.Warnf("Failed to create listing: %v\n", err)
			}
		} else {
			listing.LastRefresh = lastRefresh
			listing.Price = float32(rate)
			listing.Ask = float32(rate)
			listing.Bid = float32(rate)
			if err := tx.Save(&listing).Error; err != nil {
				tx.Rollback()
				log.Warnf("Failed to update listing: %v\n", err)
			}
		}

		var forexPair types.ForexPair
		if err := tx.Where("listing_id = ?", listing.ID).First(&forexPair).Error; err != nil {
			forexPair = types.ForexPair{
				ListingID:     listing.ID,
				BaseCurrency:  base,
				QuoteCurrency: quote,
				ExchangeRate:  rate,
				Liquidity:     "High",
			}
			if err := tx.Create(&forexPair).Error; err != nil {
				tx.Rollback()
				log.Warnf("Failed to create forex pair: %v\n", err)
			}
		} else {
			forexPair.ExchangeRate = rate
			if err := tx.Save(&forexPair).Error; err != nil {
				tx.Rollback()
				log.Warnf("Failed to update forex pair: %v\n", err)
			}
		}
		if err := tx.Commit().Error; err != nil {
			return fmt.Errorf("failed to commit transaction: %w", err)
		}
		log.Infof("Successfully loaded forex pair %s\n", ticker)
	}

	return nil
}
