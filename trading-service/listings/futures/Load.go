package futures

import (
	"banka1.com/db"
	"banka1.com/types"
	"encoding/csv"
	"fmt"
	"github.com/gofiber/fiber/v2/log"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"
)

var rootSymbols = map[string]string{
	// Agriculture
	"corn":                "ZC",  // CBOT Corn
	"soybean":             "ZS",  // CBOT Soybeans
	"soybean oil":         "ZL",  // CBOT Soybean Oil
	"soybean meal":        "ZM",  // CBOT Soybean Meal
	"chicago wheat":       "ZW",  // CBOT Chicago Wheat
	"wheat":               "KE",  // KC Wheat
	"live cattle":         "LE",  // CME Live Cattle
	"feeder cattle":       "GF",  // CME Feeder Cattle
	"lean hog":            "HE",  // CME Lean Hogs
	"pork cutout":         "PRK", // CME Pork Cutout
	"nonfat dry milk":     "GNF", // CME Nonfat Dry Milk
	"class 3 milk":        "DC",  // CME Class III Milk
	"class 4 milk":        "GDK", // CME Class IV Milk
	"cash-settled butter": "CB",  // CME Cash-Settled Butter
	"cash-settled cheese": "CSC", // CME Cash-Settled Cheese
	"block cheese":        "BLK", // CME Block Cheese
	"oats":                "ZO",  // CBOT Oats
	"rough rice":          "ZR",  // CBOT Rough Rice
	"urea":                "UX",  // CME Urea

	// Energy
	"lumber":                "LBS", // CME Random Length Lumber
	"crude oil":             "CL",  // NYMEX Crude Oil
	"natural gas":           "NG",  // NYMEX Natural Gas
	"gasoline":              "RB",  // NYMEX RBOB Gasoline
	"e-mini crude oil":      "QM",  // NYMEX E-mini Crude Oil
	"NY harbor ULSD":        "HO",  // NYMEX NY Harbor ULSD
	"micro crude oil":       "MCL", // NYMEX Micro Crude Oil
	"Henry Hub natural gas": "NG",  // NYMEX Henry Hub Natural Gas
	"buckeye jet fuel":      "BJF", // NYMEX Buckeye Jet Fuel

	// Metals
	"gold":              "GC",  // COMEX Gold
	"silver":            "SI",  // COMEX Silver
	"platinum":          "PL",  // NYMEX Platinum
	"copper":            "HG",  // COMEX Copper
	"aluminum":          "ALI", // COMEX Aluminum
	"e-mini copper":     "QC",  // E-mini Copper
	"copper mini":       "MHG", // COMEX miNY Copper
	"silver mini":       "QI",  // COMEX miNY Silver
	"platinum mini":     "QPL", // NYMEX miNY Platinum
	"gold options":      "OG",  // COMEX Gold Options
	"silver options":    "SO",  // COMEX Silver Options
	"palladium options": "PAO", // NYMEX Palladium Options

	// Softs
	"cotton":         "CT",  // ICE Cotton
	"coffee":         "KC",  // ICE Coffee
	"sugar":          "SB",  // ICE Sugar
	"cocoa":          "CC",  // ICE Cocoa
	"orange juice":   "OJ",  // ICE Orange Juice
	"lumber options": "LBO", // Random Length Lumber Options

	// Meats
	"lean hog options":      "HE",  // CME Lean Hog Options (same as futures)
	"live cattle options":   "LE",  // CME Live Cattle Options (same as futures)
	"feeder cattle options": "GF",  // CME Feeder Cattle Options (same as futures)
	"butter options":        "CBO", // CME Butter Options
	"cheese options":        "CSO", // CME Cheese Options
	"pork belly options":    "PBO", // CME Pork Belly Options
}

var code = []string{
	"F",
	"G",
	"H",
	"J",
	"K",
	"M",
	"N",
	"Q",
	"U",
	"V",
	"X",
	"Z",
}

func LoadDefaultFutures() error {
	dir, err := os.Getwd()
	if err != nil {
		return fmt.Errorf("failed to get current directory: %w", err)
	}
	csvPath := filepath.Join(dir, "listings/futures/future_data.csv")
	if _, err := os.Stat(csvPath); err != nil {
		return fmt.Errorf("exchanges.csv file not found: %w", err)
	}
	return loadFutures(csvPath)
}

func loadFutures(path string) error {
	file, err := os.Open(path)
	if err != nil {
		return fmt.Errorf("failed to open file: %w", err)
	}
	defer func(file *os.File) {
		err := file.Close()
		if err != nil {

		}
	}(file)
	reader := csv.NewReader(file)
	headers, err := reader.Read()
	if err != nil {
		return fmt.Errorf("failed to read CSV header: %w", err)
	}
	expectedHeaders := []string{"contract_name", "contract_size", "contract_unit", "maintenance_margin", "type"}
	for i, header := range expectedHeaders {
		if i >= len(headers) || headers[i] != header {
			return fmt.Errorf("CSV header mismatch: expected %s at position %d, got %s", header, i, headers[i])
		}
	}
	records, err := reader.ReadAll()
	if err != nil {
		return fmt.Errorf("failed to read CSV records: %w", err)
	}
	for i, record := range records {
		if len(record) != 5 {
			return fmt.Errorf("invalid record format: expected 5 fields, got %d", len(record))
		}
		size, err := strconv.Atoi(record[1])
		if err != nil {
			return fmt.Errorf("failed to convert contract size to integer: %w", err)
		}

		margin, err := strconv.Atoi(record[3])
		if err != nil {
			return fmt.Errorf("failed to convert maintenance margin to integer: %w", err)
		}
		name := record[0]

		rootSymbol := rootSymbols[strings.ToLower(name)]
		if rootSymbol == "" {
			// Fallback for any missing symbols
			rootSymbol = strings.ToUpper(strings.Replace(name[:min(3, len(name))], " ", "", -1))
		}

		monthCode := code[i%12]
		year := 25 + i%2
		ticker := fmt.Sprintf("%s%s%d", rootSymbol, monthCode, year)

		log.Infof("Creating ticker: %v\n", ticker)
		lastRefresh := time.Now()
		tx := db.DB.Begin()
		price := float32(margin*10) / float32(size)
		var listing types.Listing
		if err := tx.Where("ticker = ?", ticker).First(&listing).Error; err != nil {
			listing = types.Listing{
				Ticker:       ticker,
				Name:         name,
				ExchangeID:   1,
				LastRefresh:  lastRefresh,
				Price:        price,
				Ask:          price,
				Bid:          price,
				Type:         "Future",
				ContractSize: size,
			}
			if err := tx.Create(&listing).Error; err != nil {
				tx.Rollback()
				log.Infof("Failed to create listing: %v\n", err)
			}

		} else {
			listing.LastRefresh = lastRefresh
			listing.Price = price
			listing.Ask = price * 1.03
			listing.Bid = price * 0.98
			if err := tx.Save(&listing).Error; err != nil {
				tx.Rollback()
				log.Infof("Failed to update listing: %v\n", err)
			}
		}
		settlementDate, err := ParseFuturesSettlementDate(ticker)
		if err != nil {
			tx.Rollback()
			log.Errorf("Failed to parse settlement date for ticker %s: %v", ticker, err)
			continue
		}
		var future types.FuturesContract
		if err := tx.Where("listing_id = ?", listing.ID).First(&future).Error; err != nil {
			future = types.FuturesContract{
				ListingID:      listing.ID,
				ContractSize:   size,
				ContractUnit:   record[2],
				SettlementDate: settlementDate,
				Listing:        listing,
			}
			if err := tx.Create(&future).Error; err != nil {
				tx.Rollback()
				log.Infof("Failed to create future: %v\n", err)
			}
		} else {
			future.ContractSize = size
			future.ContractUnit = record[2]
			future.SettlementDate = settlementDate
			if err := tx.Save(&future).Error; err != nil {
				tx.Rollback()
				log.Infof("Failed to update future: %v\n", err)
			}
		}

		if err := tx.Commit().Error; err != nil {
			return fmt.Errorf("failed to commit transaction: %w", err)
		}

		log.Infof("Loaded future: %v\n", future)
	}

	return nil
}

var monthCodeMap = map[rune]time.Month{
	'F': time.January,
	'G': time.February,
	'H': time.March,
	'J': time.April,
	'K': time.May,
	'M': time.June,
	'N': time.July,
	'Q': time.August,
	'U': time.September,
	'V': time.October,
	'X': time.November,
	'Z': time.December,
}

func ParseFuturesSettlementDate(ticker string) (time.Time, error) {
	if len(ticker) < 3 {
		return time.Time{}, fmt.Errorf("nevalidan ticker: prekratak")
	}

	suffix := ticker[len(ticker)-3:]
	monthCode := rune(suffix[0])
	yearSuffix := suffix[1:]

	month, ok := monthCodeMap[monthCode]
	if !ok {
		return time.Time{}, fmt.Errorf("nepoznat kod meseca: %c", monthCode)
	}

	year := 2000 + parseYearSuffix(yearSuffix)

	settlement := GetLastWeekdayOfMonth(year, month)
	return settlement, nil
}

func parseYearSuffix(s string) int {
	var year int
	fmt.Sscanf(s, "%02d", &year)
	return year
}

func GetLastWeekdayOfMonth(year int, month time.Month) time.Time {
	firstOfNextMonth := time.Date(year, month+1, 1, 0, 0, 0, 0, time.UTC)
	lastDay := firstOfNextMonth.AddDate(0, 0, -1)
	for lastDay.Weekday() == time.Saturday || lastDay.Weekday() == time.Sunday {
		lastDay = lastDay.AddDate(0, 0, -1)
	}
	return lastDay
}
