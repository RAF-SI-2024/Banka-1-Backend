package options

import (
	"encoding/json"
	"errors"
	"io/ioutil"
	"net/http"
	"time"

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
	// get all listings
	var listings []types.Listing
	if err := db.DB.Find(&listings).Error; err != nil {
		return err
	}

	// delete all options
	if err := db.DB.Exec("DELETE FROM option").Error; err != nil {
		return err
	}

	for _, listing := range listings {
		yahooResp, err := FetchYahoo(listing.Ticker)
		if err != nil {
			return err
		}

		SaveOptionsToDB(listing.Ticker, yahooResp)
	}

	return nil
}

func SaveOptionsToDB(ticker string, yahooResp YahooOptionsApiResponse) error {
	var listing types.Listing
	if err := db.DB.Where("ticker = ?", ticker).First(&listing).Error; err != nil {
		return errors.New("listing not found for ticker: " + ticker)
	}

	for _, result := range yahooResp.OptionChain.Result {
		expirationDate := time.Unix(result.ExpirationDate, 0)

		for _, opt := range result.Options {
			// Save CALL options
			for _, call := range opt.Calls {
				option := types.Option{
					ListingID:      listing.ID,
					OptionType:     "Call",
					StrikePrice:    call.Strike,
					ImpliedVol:     call.ImpliedVolatility,
					OpenInterest:   int64(call.OpenInterest),
					SettlementDate: expirationDate,
					ContractSize:   100, // Default contract size
				}

				// Insert into DB
				if err := db.DB.Create(&option).Error; err != nil {
					return err
				}
			}

			// Save PUT options
			for _, put := range opt.Puts {
				option := types.Option{
					ListingID:      listing.ID,
					OptionType:     "Put",
					StrikePrice:    put.Strike,
					ImpliedVol:     put.ImpliedVolatility,
					OpenInterest:   int64(put.OpenInterest),
					SettlementDate: expirationDate,
					ContractSize:   100,
				}

				// Insert into DB
				if err := db.DB.Create(&option).Error; err != nil {
					return err
				}
			}
		}
	}
	return nil
}
