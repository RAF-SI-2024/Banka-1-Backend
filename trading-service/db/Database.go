package db

import (
	"fmt"
	"log"
	"os"
	"time"

	"banka1.com/types"
	"github.com/glebarez/sqlite"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
	"gorm.io/gorm/schema"
)

var DB *gorm.DB

func Init() {
	var err error
	dsn := getDSN()

	// Kreiramo GORM konfiguraciju
	gormConfig := &gorm.Config{
		NamingStrategy: schema.NamingStrategy{
			SingularTable: true, // Tabele u jednini
		},
		Logger: logger.Default.LogMode(logger.Silent), // Logovanje
	}

	// Inicijalizacija baze
	DB, err = gorm.Open(getDBDialect(dsn), gormConfig)
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}

	// Konfigurisanje connection pool-a
	sqlDB, err := DB.DB()
	if err != nil {
		log.Fatalf("Failed to get SQL DB instance: %v", err)
	}

	sqlDB.SetMaxOpenConns(25)                 // Maksimalan broj istovremenih konekcija
	sqlDB.SetMaxIdleConns(10)                 // Maksimalan broj neaktivnih konekcija
	sqlDB.SetConnMaxLifetime(5 * time.Minute) // Maksimalno vreme trajanja konekcije

	// Migracija šema
	migrate(DB)

	fmt.Println("Database connection established!")
}

// getDSN preuzima DSN string iz okruženja ili koristi default
func getDSN() string {
	dbType := os.Getenv("DB_TYPE") // postgres, mysql, sqlite
	switch dbType {
	case "postgres":
		return os.Getenv("POSTGRES_DSN")
	case "mysql":
		return os.Getenv("MYSQL_DSN")
	default:
		return "test.db" // SQLite fallback
	}
}

// getDBDialect vraća odgovarajući DB dialekt za GORM
func getDBDialect(dsn string) gorm.Dialector {
	dbType := os.Getenv("DB_TYPE") // postgres, mysql, sqlite
	switch dbType {
	// case "postgres":
	// 	return postgres.Open(dsn)
	// case "mysql":
	// 	return mysql.Open(dsn)
	default:
		return sqlite.Open(dsn) // SQLite kao default
	}
}

func migrate(db *gorm.DB) {
	db.AutoMigrate(
		&types.Actuary{},
		&types.Security{},
		&types.Order{},
		&types.OTCTrade{},
		&types.Portfolio{},
		&types.Tax{},
		&types.Exchange{},
		&types.Listing{},
		&types.ListingDailyPriceInfo{},
		&types.Stock{},
		&types.ForexPair{},
		&types.FuturesContract{},
		&types.Option{},
	)
}

func InitTestDatabase() error {
	var err error
	DB, err = gorm.Open(sqlite.Open("file::memory:?cache=shared"), &gorm.Config{})
	if err != nil {
		return err
	}
	return DB.AutoMigrate(&types.Security{}, &types.Order{}, &types.Actuary{}, &types.Transaction{}, &types.Portfolio{})
}
