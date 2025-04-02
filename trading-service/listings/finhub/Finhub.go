package finhub

import (
	"context"
	"crypto/tls"
	"encoding/json"
	"fmt"
	"net"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"

	finnhub "github.com/Finnhub-Stock-API/finnhub-go/v2"
)

func GetConfig() *finnhub.Configuration {
	cfg := finnhub.NewConfiguration()
	cfg.AddDefaultHeader("X-Finnhub-Token", os.Getenv("FINNHUB_KEY"))
	return cfg
}

func contains(slice []string, item string) bool {
	for _, v := range slice {
		if v == item {
			return true
		}
	}
	return false
}

func GetAllStockTypes() ([]string, error) {
	finnhubClient := finnhub.NewAPIClient(GetConfig()).DefaultApi

	res, _, err := finnhubClient.StockSymbols(context.Background()).Exchange("US").Execute()
	if err != nil {
		return nil, err
	}

	stocksTypes := []string{}

	for _, stock := range res {
		if !contains(stocksTypes, *stock.Type) {
			stocksTypes = append(stocksTypes, *stock.Type)
		}
	}

	// print all stock types
	fmt.Println("All stock types:")
	fmt.Println("------------------------------")
	for _, stockType := range stocksTypes {
		println(stockType)
	}
	fmt.Println("------------------------------")

	return stocksTypes, nil
}

type Stock struct {
	Symbol string
	Mic    string
	Type   string
}

func GetAllStock() ([]Stock, error) {
	finnhubClient := finnhub.NewAPIClient(GetConfig()).DefaultApi

	res, _, err := finnhubClient.StockSymbols(context.Background()).Exchange("US").Execute()
	if err != nil {
		return nil, err
	}

	stocksData := []Stock{}

	var i = 0
	for _, stock := range res {
		// if not COmmom Stock skip
		if *stock.Type != "Common Stock" && *stock.Type != "Open-End Fund" {
			continue
		}
		// stocksData[*stock.DisplaySymbol] = *stock.Mic
		stocksData = append(stocksData, Stock{Symbol: *stock.DisplaySymbol, Mic: *stock.Mic, Type: *stock.Type})
		i++
		if i >= 10 {
			break
		}
	}

	return stocksData, nil
}

func GetAllStockMock() ([]Stock, error) {

	stocksData := []Stock{}

	stocksData = append(stocksData, Stock{Symbol: "AAPL", Mic: "XNAS", Type: "Common Stock"})
	stocksData = append(stocksData, Stock{Symbol: "MSFT", Mic: "XNAS", Type: "Common Stock"})
	stocksData = append(stocksData, Stock{Symbol: "GOOGL", Mic: "XNAS", Type: "Common Stock"})
	stocksData = append(stocksData, Stock{Symbol: "AMZN", Mic: "XNAS", Type: "Common Stock"})
	stocksData = append(stocksData, Stock{Symbol: "TSLA", Mic: "XNAS", Type: "Common Stock"})
	stocksData = append(stocksData, Stock{Symbol: "NVDA", Mic: "XNAS", Type: "Common Stock"})
	stocksData = append(stocksData, Stock{Symbol: "PYPL", Mic: "XNAS", Type: "Common Stock"})
	stocksData = append(stocksData, Stock{Symbol: "INTC", Mic: "XNAS", Type: "Common Stock"})

	return stocksData, nil
}

func GetStockData(ticker string) (finnhub.Quote, error) {
	finnhubClient := finnhub.NewAPIClient(GetConfig()).DefaultApi
	res, _, err := finnhubClient.Quote(context.Background()).Symbol(ticker).Execute()
	if err != nil {
		return finnhub.Quote{}, err
	}

	return res, nil
}

// https://api.nasdaq.com/api/quote/RGNNF/historical?assetclass=stocks&fromdate=2025-02-19&limit=9999&todate=2025-03-19&random=11
type HistoricalPrice struct {
	Date   time.Time `json:"date"`
	Close  float64   `json:"close"`
	Volume int64     `json:"volume"`
	Open   float64   `json:"open"`
	High   float64   `json:"high"`
	Low    float64   `json:"low"`
}

type NasdaqAPIResponse struct {
	Data struct {
		TradesTable struct {
			Rows []struct {
				Date   string `json:"date"`
				Close  string `json:"close"`
				Volume string `json:"volume"`
				Open   string `json:"open"`
				High   string `json:"high"`
				Low    string `json:"low"`
			} `json:"rows"`
		} `json:"tradesTable"`
	} `json:"data"`
}

func GetNasdaqAPIResponse(stockSymbol, subtype string, start, end time.Time) (NasdaqAPIResponse, error) {
	url := fmt.Sprintf("https://api.nasdaq.com/api/quote/%s/historical?assetclass=%s&fromdate=%s&limit=99999&todate=%s", stockSymbol, "stocks", start.Format("2006-01-02"), end.Format("2006-01-02"))
	fmt.Println("Fetching historical price data for", url)
	req, err := http.NewRequest("GET", url, nil)
	if err != nil {
		return NasdaqAPIResponse{}, err
	}

	// Dodavanje zaglavlja
	req.Header.Set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36")
	req.Header.Set("Accept", "application/json")
	req.Header.Set("Referer", "https://www.nasdaq.com")

	var client = &http.Client{
		Transport: &http.Transport{
			TLSClientConfig: &tls.Config{
				MinVersion: tls.VersionTLS12,
			},
			ForceAttemptHTTP2: false,
			DialContext: (&net.Dialer{
				Timeout:   30 * time.Second,
				KeepAlive: 30 * time.Second,
			}).DialContext,
		},
	}

	resp, err := client.Do(req)
	if err != nil {
		return NasdaqAPIResponse{}, err
	}

	defer resp.Body.Close()

	var apiResp NasdaqAPIResponse
	if err := json.NewDecoder(resp.Body).Decode(&apiResp); err != nil {
		fmt.Println("Failed to decode Nasdaq API response")
		return NasdaqAPIResponse{}, err
	}

	return apiResp, nil
}

func GetHistoricalPriceFirst(stockSymbol, subtype string) (HistoricalPrice, error) {
	var endDate = time.Now()
	var startDate = time.Now().AddDate(-20, 0, 0)
	apiResp, err := GetNasdaqAPIResponse(stockSymbol, subtype, startDate, endDate)
	if err != nil {
		return HistoricalPrice{}, err
	}

	// go through all rows and find the last one
	var closePrice float64
	var volume int64
	var open float64
	var high float64
	var low float64
	// use last row
	if len(apiResp.Data.TradesTable.Rows) == 0 {
		return HistoricalPrice{}, fmt.Errorf("No historical data found")
	}
	row := apiResp.Data.TradesTable.Rows[len(apiResp.Data.TradesTable.Rows)-1]
	closePrice, _ = strconv.ParseFloat(row.Close[1:], 64) // Ignorisanje "$"
	var cleanVolume = strings.ReplaceAll(row.Volume, ",", "")
	volume, _ = strconv.ParseInt(cleanVolume, 10, 64)
	open, _ = strconv.ParseFloat(row.Open[1:], 64)
	high, _ = strconv.ParseFloat(row.High[1:], 64)
	low, _ = strconv.ParseFloat(row.Low[1:], 64)
	date, _ := time.Parse("01/02/2006", row.Date)

	return HistoricalPrice{
		Date:   date,
		Close:  closePrice,
		Volume: volume,
		Open:   open,
		High:   high,
		Low:    low,
	}, nil
}

func GetHistoricalPriceDate(stockSymbol, subtype string, date time.Time) (HistoricalPrice, error) {
	var endDate = time.Now()
	var startDate = time.Now().AddDate(-20, 0, 0)
	apiResp, err := GetNasdaqAPIResponse(stockSymbol, subtype, startDate, endDate)
	if err != nil {
		return HistoricalPrice{}, err
	}

	// go through all rows and find the one with the date, date is in format MM/DD/YYYY
	var date_ = date.Format("01/02/2006")
	var closePrice float64
	var volume int64
	var open float64
	var high float64
	var low float64
	for _, row := range apiResp.Data.TradesTable.Rows {
		if row.Date == date_ {
			closePrice, _ = strconv.ParseFloat(row.Close[1:], 64) // Ignorisanje "$"
			var cleanVolume = strings.ReplaceAll(row.Volume, ",", "")
			volume, _ = strconv.ParseInt(cleanVolume, 10, 64)
			open, _ = strconv.ParseFloat(row.Open[1:], 64)
			high, _ = strconv.ParseFloat(row.High[1:], 64)
			low, _ = strconv.ParseFloat(row.Low[1:], 64)
			break
		}
	}

	return HistoricalPrice{
		Date:   date,
		Close:  closePrice,
		Volume: volume,
		Open:   open,
		High:   high,
		Low:    low,
	}, nil
}

func GetHistoricalPrice(stockSymbol, subtype string) ([]HistoricalPrice, error) {
	// 30 dana unazad
	var startDate = time.Now().AddDate(0, 0, -30)
	var endDate = time.Now()
	// dates should be in format YYYY-MM-DD
	apiResp, err := GetNasdaqAPIResponse(stockSymbol, subtype, startDate, endDate)
	if err != nil {
		return nil, err
	}

	var prices []HistoricalPrice
	for _, row := range apiResp.Data.TradesTable.Rows {
		// Konvertovanje podataka
		date, _ := time.Parse("01/02/2006", row.Date)          // format MM/DD/YYYY
		closePrice, _ := strconv.ParseFloat(row.Close[1:], 64) // Ignorisanje "$"
		var cleanVolume = strings.ReplaceAll(row.Volume, ",", "")
		volume, _ := strconv.ParseInt(cleanVolume, 10, 64)
		open, _ := strconv.ParseFloat(row.Open[1:], 64)
		high, _ := strconv.ParseFloat(row.High[1:], 64)
		low, _ := strconv.ParseFloat(row.Low[1:], 64)

		prices = append(prices, HistoricalPrice{
			Date:   date,
			Close:  closePrice,
			Volume: volume,
			Open:   open,
			High:   high,
			Low:    low,
		})
	}

	fmt.Println("Fetched", len(prices), "historical prices")

	return prices, nil
}
