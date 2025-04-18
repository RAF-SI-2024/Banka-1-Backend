package cron

import (
	"banka1.com/controllers/orders"
	"banka1.com/exchanges"
	"banka1.com/listings/forex"
	"banka1.com/listings/futures"
	"banka1.com/listings/option"
	"banka1.com/listings/securities"
	"banka1.com/listings/stocks"
	"banka1.com/listings/tax"
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"os"
	"time"

	"banka1.com/db"

	"github.com/gofiber/fiber/v2/log"
	"gorm.io/gorm"

	"banka1.com/types"
	"github.com/robfig/cron/v3"
)

type Employee struct {
	ID          int      `json:"id"`
	FirstName   string   `json:"firstName"`
	LastName    string   `json:"lastName"`
	Email       string   `json:"email"`
	Department  string   `json:"department"`
	Position    string   `json:"position"`
	Active      bool     `json:"active"`
	Permissions []string `json:"permissions"`
}

type APIResponse struct {
	Success bool       `json:"success"`
	Data    []Employee `json:"data"`
}

// cron posao koji resetuje limit agentu svakog dana u 23 59
func StartScheduler() {
	LoadData()
	SnapshotListingsToHistory()

	c := cron.New(cron.WithSeconds())

	_, err := c.AddFunc("0 * * * *", func() {
		LoadData()
	})

	_, err = c.AddFunc("0 59 23 * * *", func() {
		resetDailyLimits()
	})

	_, err = c.AddFunc("0 1 0 * * *", func() {
		expireOldOptionContracts()
	})

	_, err = c.AddFunc("0/15 * * * * * ", func() {
		createNewActuaries()
	})

	_, err = c.AddFunc("0 0 0 * * *", func() {
		SnapshotListingsToHistory()
	})

	if err != nil {
		log.Errorf("Greska pri pokretanju cron job-a:", err)
		return
	}

	c.Start()
}

func LoadData() {
	log.Info("Starting hourly data reload...")

	err := exchanges.LoadDefaultExchanges()
	if err != nil {
		log.Warnf("Warning: Failed to load exchanges: %v", err)
	}

	log.Info("Starting to load default stocks...")
	stocks.LoadDefaultStocks()
	log.Info("Finished loading default stocks")

	log.Info("Starting to load default forex pairs...")
	forex.LoadDefaultForexPairs()
	log.Info("Finished loading default forex pairs")

	log.Info("Starting to load default futures...")
	err = futures.LoadDefaultFutures()
	if err != nil {
		log.Warnf("Warning: Failed to load futures: %v", err)
	}
	log.Info("Finished loading default futures")

	log.Info("Starting to load default options...")
	err = option.LoadAllOptions()
	if err != nil {
		log.Warnf("Warning: Failed to load options: %v", err)
	}
	log.Info("Finished loading default options")

	log.Info("Starting to load default securities...")
	securities.LoadAvailableSecurities()
	log.Info("Finished loading default securities")

	log.Info("Starting to load default taxes...")
	tax.LoadTax()
	log.Info("Finished loading default taxes")

	log.Info("Starting to load default orders...")
	orders.LoadOrders()
	log.Info("Finished loading default orders")

	log.Info("Starting to load default portfolios...")
	orders.LoadPortfolios()
	log.Info("Finished loading default portfolios")

	log.Info("Starting to load default sell orders...")
	orders.CreateInitialSellOrdersFromBank()
	log.Info("Finished loading default sell orders")

	// Ovo je bilo za prvobitno registrovanje volume - ali je zakoentarisano zato sto
	// 	orders.CreateInitialSellOrdersFromBank() ucitava sell ordere i usput azurira Volume

	//log.Println("Starting to calculate volumes for all securities...")
	//var securities []types.Security
	//if err := db.DB.Find(&securities).Error; err != nil {
	//	log.Printf("Warning: Failed to fetch securities for volume update: %v", err)
	//} else {
	//	for _, sec := range securities {
	//		err := orders.UpdateAvailableVolume(sec.ID)
	//		if err != nil {
	//			log.Printf("Warning: Failed to update volume for security %s (ID %d): %v", sec.Ticker, sec.ID, err)
	//		}
	//	}
	//}
	//log.Println("Finished calculating volumes")
	log.Info("Hourly data reload completed")
}

func SnapshotListingsToHistory() error {
	var listings []types.Listing
	if err := db.DB.Find(&listings).Error; err != nil {
		return err
	}

	today := time.Now().Truncate(24 * time.Hour)

	for _, l := range listings {
		// proveri da li već postoji
		var existing types.ListingHistory
		err := db.DB.
			Where("ticker = ? AND snapshot_date = ?", l.Ticker, today).
			First(&existing).Error

		if err == nil {
			continue // već postoji → preskoči
		}

		if !errors.Is(err, gorm.ErrRecordNotFound) {
			return err // neki drugi error
		}

		history := types.ListingHistory{
			Ticker:       l.Ticker,
			Name:         l.Name,
			ExchangeID:   l.ExchangeID,
			LastRefresh:  l.LastRefresh,
			Price:        l.Price,
			Ask:          l.Ask,
			Bid:          l.Bid,
			Type:         l.Type,
			Subtype:      l.Subtype,
			ContractSize: l.ContractSize,
			SnapshotDate: today,
		}
		if err := db.DB.Create(&history).Error; err != nil {
			return err
		}
	}

	return nil
}

func expireOldOptionContracts() {
	now := time.Now()

	var contracts []types.OptionContract
	if err := db.DB.Where("settlement_at < ? AND status = ?", now, "active").Find(&contracts).Error; err != nil {
		log.Errorf("Greška pri pronalaženju ugovora za expirovanje: %v", err)
		return
	}

	for _, contract := range contracts {
		contract.Status = "expired"
		if err := db.DB.Save(&contract).Error; err != nil {
			log.Errorf("Greška pri expirovanju ugovora ID %d: %v", contract.ID, err)
		} else {
			log.Infof("Ugovor ID %d označen kao 'expired'", contract.ID)
		}
	}
}

func resetDailyLimits() {
	db.DB.Model(&types.Actuary{}).Where("role = ?", "agent").Update("usedLimit", 0)
}

func createNewActuaries() {
	data, err := GetActuaries()

	if err != nil {
		return
	}

	if len(data.Data) == 0 {
		return
	}
	for _, actuaryData := range data.Data {
		newActuary := employeeToActuary(actuaryData)

		var existingActuary types.Actuary
		err := db.DB.Where("user_id = ?", newActuary.UserID).First(&existingActuary).Error
		if err != nil {
			if errors.Is(err, gorm.ErrRecordNotFound) {
				if result := db.DB.Create(&newActuary); result.Error != nil {
					log.Errorf("Error creating actuary %v: %v", newActuary, result.Error)
				} else {
					log.Infof("Created new actuary: %v", newActuary)
				}
			} else {
				// Some other error occurred
				log.Errorf("Error checking actuary existence: %v", err)
			}
		} else {
		}
	}
}

func employeeToActuary(employee Employee) types.Actuary {
	actuary := types.Actuary{
		UserID:      uint(employee.ID),
		Department:  employee.Department,
		FullName:    employee.FirstName + " " + employee.LastName,
		Email:       employee.Email,
		LimitAmount: 100000,
	}
	return actuary
}

func GetActuaries() (*APIResponse, error) {
	basePath := os.Getenv("USER_SERVICE")
	url := basePath + "/api/users/employees/actuaries"

	resp, err := http.Get(url)
	if err != nil {
		log.Infof("Failed to fetch %s: %v\n", url, err)
		return nil, err
	}
	defer func(Body io.ReadCloser) {
		err := Body.Close()
		if err != nil {

		}
	}(resp.Body)

	if resp.StatusCode != 200 {
		log.Infof("Error fetching %s: HTTP %d\n", url, resp.StatusCode)
		return nil, err
	}

	var apiResponse *APIResponse
	if err := json.NewDecoder(resp.Body).Decode(&apiResponse); err != nil {
		log.Infof("Failed to parse JSON: %v\n", err)
		return nil, err
	}
	return apiResponse, nil
}
