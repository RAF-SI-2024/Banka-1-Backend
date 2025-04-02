package stocks

import (
	"fmt"
	"strings"
	"time"

	"banka1.com/db"
	"banka1.com/listings/finhub"
	"banka1.com/types"
)

// API response structures
type TimeSeriesResponse struct {
	MetaData   map[string]string            `json:"Meta Data"`
	TimeSeries map[string]map[string]string `json:"Time Series (Daily)"`
}

// LoadStockFromAPI loads a single stock from Alpha Vantage API
func LoadStockFromAPI(stockRequest finhub.Stock) error {

	stockData, err := finhub.GetStockData(stockRequest.Symbol)
	if err != nil {
		return fmt.Errorf("failed to fetch stock data: %w", err)
	}

	// Debug logging
	fmt.Printf("Trying to find exchange with MIC code: %s\n", stockRequest.Mic)

	// Find exchange

	// Parse values
	price := stockData.Pc
	high := stockData.H
	low := stockData.L
	// volume := strconv.ParseInt(mostRecentData["5. volume"], 10, 64)
	lastRefresh := time.Now()

	// Begin transaction
	tx := db.DB.Begin()
	var type_ string = "Stock"
	// Create or update listing
	var listing types.Listing
	if err := tx.Where("ticker = ?", stockRequest.Symbol).First(&listing).Error; err != nil {
		// Create new listing

		if stockRequest.Type == "Open-End Fund" {
			type_ = "ETF"
		}

		listing = types.Listing{
			Ticker:       stockRequest.Symbol,
			Name:         strings.Split(stockRequest.Symbol, ".")[0] + " Inc.", // Simple name
			ExchangeID:   1,
			LastRefresh:  lastRefresh,
			Price:        *price,
			Ask:          *high,
			Bid:          *low,
			Type:         "Stock",
			Subtype:      type_,
			ContractSize: 1,
		}

		fmt.Printf("Creating new listing for %s\n", stockRequest.Symbol)
		if err := tx.Create(&listing).Error; err != nil {
			tx.Rollback()
			return fmt.Errorf("failed to create listing: %w", err)
		}
	} else {
		// Update existing
		listing.LastRefresh = lastRefresh
		listing.Price = *price
		listing.Ask = *high
		listing.Bid = *low

		fmt.Printf("Updating existing listing for %s (ID: %d)\n", stockRequest.Symbol, listing.ID)
		if err := tx.Save(&listing).Error; err != nil {
			tx.Rollback()
			return fmt.Errorf("failed to update listing: %w", err)
		}
	}

	// Create or update stock info
	var stock types.Stock
	if err := tx.Where("listing_id = ?", listing.ID).First(&stock).Error; err != nil {
		stock = types.Stock{
			ListingID:         listing.ID,
			OutstandingShares: 1000000, // Default value
			DividendYield:     0.01,    // Default value
		}

		fmt.Printf("Creating stock details for listing ID %d\n", listing.ID)
		if err := tx.Create(&stock).Error; err != nil {
			tx.Rollback()
			return fmt.Errorf("failed to create stock details: %w", err)
		}
	}

	history, err := finhub.GetHistoricalPrice(stockRequest.Symbol, "stocks")
	for i, historyDay := range history {
		var existing types.ListingDailyPriceInfo
		if tx.Where("listing_id = ? AND date = ?", listing.ID, historyDay.Date).First(&existing).Error == nil {
			continue
		}

		// Calculate change based on previous day
		change := 0.0
		if i > 0 {
			change = history[i].Open - history[i-1].Open
		}

		fmt.Println("historyDay.Date", historyDay.Volume)

		// Create price record
		priceInfo := types.ListingDailyPriceInfo{
			ListingID: listing.ID,
			Date:      historyDay.Date,
			Price:     historyDay.Open,
			High:      historyDay.High,
			Low:       historyDay.Low,
			Change:    change,
			Volume:    historyDay.Volume,
		}

		fmt.Printf("Adding price history for %s on %s\n", stockRequest.Symbol, historyDay.Date)
		if err := tx.Create(&priceInfo).Error; err != nil {
			tx.Rollback()
			return fmt.Errorf("failed to create price history: %w", err)
		}

		// count++
		// if count >= 30 {
		// 	break
		// }
	}

	if err := tx.Commit().Error; err != nil {
		return fmt.Errorf("failed to commit transaction: %w", err)
	}

	fmt.Printf("Successfully loaded stock data for %s\n", stockRequest.Symbol)
	return nil
}

func LoadDefaultStocks() {
	// Define stocks with their exchanges

	stocksData, err := finhub.GetAllStockMock()
	if err != nil {
		fmt.Printf("Failed to fetch stock data: %v\n", err)
		return
	}

	for _, exchange := range []string{"XNAS"} {
		var count int64
		db.DB.Model(&types.Exchange{}).Where("mic_code = ?", exchange).Count(&count)
		if count == 0 {
			fmt.Printf("WARNING: Exchange %s doesn't exist in the database!\n", exchange)
		} else {
			fmt.Printf("Exchange %s found in database\n", exchange)
		}
	}

	fmt.Println("------------------------------")

	for _, stock := range stocksData {
		fmt.Printf("Loading %s on %s...\n", stock.Symbol, stock.Mic)
		if err := LoadStockFromAPI(stock); err != nil {
			fmt.Printf("Error loading %s: %v\n", stock.Symbol, err)
		} else {
			fmt.Printf("Successfully loaded %s\n", stock.Symbol)
		}
	}
}
