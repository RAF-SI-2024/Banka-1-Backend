package options

import (
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"net/http"
	"time"

	"github.com/gofiber/fiber/v2/log"

	"banka1.com/db"
	"banka1.com/types"
)

type YahooOptionsApiResponse struct {
	OptionChain struct {
		Result []struct {
			ExpirationDate int64 `json:"expirationDate"`
			Options        []struct {
				Calls []struct {
					ContractSymbol    string  `json:"contractSymbol"`
					Strike            float64 `json:"strike"`
					LastPrice         float64 `json:"lastPrice"`
					Change            float64 `json:"change"`
					PercentChange     float64 `json:"percentChange"`
					Volume            int     `json:"volume"`
					OpenInterest      int     `json:"openInterest"`
					Bid               float64 `json:"bid"`
					Ask               float64 `json:"ask"`
					ImpliedVolatility float64 `json:"impliedVolatility"`
				} `json:"calls"`
				Puts []struct {
					ContractSymbol    string  `json:"contractSymbol"`
					Strike            float64 `json:"strike"`
					LastPrice         float64 `json:"lastPrice"`
					Change            float64 `json:"change"`
					PercentChange     float64 `json:"percentChange"`
					Volume            int     `json:"volume"`
					OpenInterest      int     `json:"openInterest"`
					Bid               float64 `json:"bid"`
					Ask               float64 `json:"ask"`
					ImpliedVolatility float64 `json:"impliedVolatility"`
				} `json:"puts"`
			} `json:"options"`
		} `json:"result"`
	} `json:"optionChain"`
}

func FetchYahoo(ticker string) (YahooOptionsApiResponse, error) {
	// https://query1.finance.yahoo.com/v6/finance/options/AAPL

	client := &http.Client{}
	req, err := http.NewRequest("GET", "https://query1.finance.yahoo.com/v6/finance/options/"+ticker, nil)
	if err != nil {
		return YahooOptionsApiResponse{}, err
	}

	req.Header.Set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
	req.Header.Set("Accept", "application/json")
	req.Header.Set("Referer", "https://finance.yahoo.com/quote/"+ticker)

	resp, err := client.Do(req)
	if err != nil {
		return YahooOptionsApiResponse{}, err
	}

	_ = resp

	// Parse response
	yahooResp := YahooOptionsApiResponse{}

	// read body
	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return YahooOptionsApiResponse{}, err
	}

	// unmarshal
	if err := json.Unmarshal(body, &yahooResp); err != nil {
		return YahooOptionsApiResponse{}, err
	}

	return yahooResp, nil
}

func LoadAllOptions() error {
	log.Info("Hello")
	// get all listings
	var listings []types.Listing
	if err := db.DB.Find(&listings).Error; err != nil {
		return err
	}
	log.Infof("Loaded %d listings", len(listings))
	// delete all options
	log.Info("Deleting all options")
	if err := db.DB.Exec("DELETE FROM option").Error; err != nil {
		return err
	}
	for i, listing := range listings {
		if i%10 != 0 {
			continue
		}
		yahooResp, err := FetchYahoo(listing.Ticker)
		if err != nil {
			log.Infof("Error: %v", err)
			continue
		}
		SaveOptionsToDB(listing.Ticker, yahooResp)
	}

	return nil
}

func formatStrikePrice(strike float64) string {
	return fmt.Sprintf("%08d", int(strike*100)) // Convert to cents and pad with zeros
}

// Generate a unique ticker for the option
func GenerateOptionTicker(baseTicker string, expiration time.Time, optionType string, strike float64) string {
	// Extract year (YY), month (MM), and day (DD)
	year := expiration.Year() % 100  // Get last two digits of year (2024 → 24)
	month := int(expiration.Month()) // Month as number (06 for June)
	day := expiration.Day()          // Day of the month (21)

	// Convert strike price to correct format
	strikePrice := formatStrikePrice(strike)

	// Construct the final ticker
	return fmt.Sprintf("%s%02d%02d%02d%s%s", baseTicker, year, month, day, optionType, strikePrice)
}

func SaveOptionsToDB(ticker string, yahooResp YahooOptionsApiResponse) error {
	var baseListing types.Listing
	if err := db.DB.Where("ticker = ?", ticker).First(&baseListing).Error; err != nil {
		return errors.New("listing not found for ticker: " + ticker)
	}

	for _, result := range yahooResp.OptionChain.Result {
		expirationDate := time.Unix(result.ExpirationDate, 0)

		for _, opt := range result.Options {
			// Save CALL options
			for _, call := range opt.Calls {
				optionTicker := GenerateOptionTicker(ticker, expirationDate, "C", call.Strike)

				// Check if Listing already exists
				var optionListing types.Listing
				if err := db.DB.Where("ticker = ?", optionTicker).First(&optionListing).Error; err != nil {
					// If not found, create a new Listing for the option
					optionListing = types.Listing{
						Ticker:       optionTicker,
						Name:         baseListing.Name + " Option",
						ExchangeID:   baseListing.ExchangeID,
						LastRefresh:  time.Now(),
						Price:        float32(call.LastPrice),
						Ask:          float32(call.Ask),
						Bid:          float32(call.Bid),
						Type:         "Option",
						Subtype:      "Call Option",
						ContractSize: 100,
					}
					if err := db.DB.Create(&optionListing).Error; err != nil {
						return err
					}
				}

				// Save Call Option
				option := types.Option{
					ListingID:      optionListing.ID,
					OptionType:     "Call",
					StrikePrice:    call.Strike,
					ImpliedVol:     call.ImpliedVolatility,
					OpenInterest:   int64(call.OpenInterest),
					SettlementDate: expirationDate,
					ContractSize:   100, // Default contract size
				}
				if err := db.DB.Create(&option).Error; err != nil {
					return err
				}
			}

			// Save PUT options
			for _, put := range opt.Puts {
				optionTicker := GenerateOptionTicker(ticker, expirationDate, "P", put.Strike)

				// Check if Listing already exists
				var optionListing types.Listing
				if err := db.DB.Where("ticker = ?", optionTicker).First(&optionListing).Error; err != nil {
					// If not found, create a new Listing for the option
					optionListing = types.Listing{
						Ticker:       optionTicker,
						Name:         baseListing.Name + " Option",
						ExchangeID:   baseListing.ExchangeID,
						LastRefresh:  time.Now(),
						Price:        float32(put.LastPrice),
						Ask:          float32(put.Ask),
						Bid:          float32(put.Bid),
						Type:         "Option",
						Subtype:      "Put Option",
						ContractSize: 100,
					}
					if err := db.DB.Create(&optionListing).Error; err != nil {
						return err
					}
				}

				// Save Put Option
				option := types.Option{
					ListingID:      optionListing.ID,
					OptionType:     "Put",
					StrikePrice:    put.Strike,
					ImpliedVol:     put.ImpliedVolatility,
					OpenInterest:   int64(put.OpenInterest),
					SettlementDate: expirationDate,
					ContractSize:   100,
				}
				if err := db.DB.Create(&option).Error; err != nil {
					return err
				}
			}
		}
	}
	return nil
}
