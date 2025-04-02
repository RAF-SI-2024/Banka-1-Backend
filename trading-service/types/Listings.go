package types

import (
	"time"
)

type Listing struct {
	ID           uint      `gorm:"primaryKey" json:"id" `
	Ticker       string    `gorm:"unique;not null" json:"ticker"`
	Name         string    `gorm:"not null" json:"name"`
	ExchangeID   uint      `gorm:"not null" json:"exchange_id"`
	Exchange     Exchange  `gorm:"foreignKey:ExchangeID" json:"exchange"`
	LastRefresh  time.Time `gorm:"not null" json:"last_refresh"`
	Price        float32   `gorm:"not null" json:"lastPrice"`
	Ask          float32   `gorm:"not null" json:"ask"`
	Bid          float32   `gorm:"not null" json:"bid"`
	Type         string    `gorm:"not null" json:"type"` // "Stock", "Forex", "Future", "Option"
	Subtype      string    `gorm:"null" json:"Subtype"`  // "ETF", "Common Stock", "Call Option", "Put Option"
	ContractSize int       `gorm:"not null" json:"contract_size"`
	CreatedAt    time.Time `gorm:"autoCreateTime" json:"created_at"`
	UpdatedAt    time.Time `gorm:"autoUpdateTime" json:"updated_at"`
}

type ListingDailyPriceInfo struct {
	ID        uint      `gorm:"primaryKey" json:"id,omitempty"`
	ListingID uint      `gorm:"not null" json:"listing_id,omitempty"`
	Date      time.Time `gorm:"not null" json:"date"`
	Price     float64   `gorm:"not null" json:"price,omitempty"`
	High      float64   `gorm:"not null" json:"high,omitempty"`
	Low       float64   `gorm:"not null" json:"low,omitempty"`
	Change    float64   `gorm:"not null" json:"change,omitempty"`
	Volume    int64     `gorm:"not null" json:"availableQuantity,omitempty"`
	Listing   Listing   `gorm:"foreignKey:ListingID" json:"listing"`
}

type Stock struct {
	ID                uint    `gorm:"primaryKey" json:"id,omitempty"`
	ListingID         uint    `gorm:"unique;not null" json:"listing_id,omitempty"`
	OutstandingShares int64   `gorm:"not null" json:"outstanding_shares,omitempty"`
	DividendYield     float64 `gorm:"not null" json:"dividend_yield,omitempty"`
	Listing           Listing `gorm:"foreignKey:ListingID" json:"listing"`
}

type ForexPair struct {
	ID            uint    `gorm:"primaryKey" json:"id,omitempty"`
	ListingID     uint    `gorm:"unique;not null" json:"listing_id,omitempty"`
	BaseCurrency  string  `gorm:"not null" json:"base_currency,omitempty"`
	QuoteCurrency string  `gorm:"not null" json:"quote_currency,omitempty"`
	ExchangeRate  float64 `gorm:"not null" json:"exchange_rate,omitempty"`
	Liquidity     string  `gorm:"not null" json:"liquidity,omitempty"` // "High", "Medium", "Low"
	Listing       Listing `gorm:"foreignKey:ListingID" json:"listing"`
}

type FuturesContract struct {
	ID             uint      `gorm:"primaryKey" json:"id,omitempty"`
	ListingID      uint      `gorm:"unique;not null" json:"listing_id,omitempty"`
	ContractSize   int       `gorm:"not null" json:"contract_size,omitempty"`
	ContractUnit   string    `gorm:"not null" json:"contract_unit,omitempty"` // "Barrel", "Kilogram", "Liter"
	SettlementDate time.Time `gorm:"not null" json:"settlement_date"`
	Listing        Listing   `gorm:"foreignKey:ListingID" json:"listing"`
}

type Option struct {
	ID             uint      `gorm:"primaryKey"`
	ListingID      uint      `gorm:"not null"`
	OptionType     string    `gorm:"not null"` // "Call" or "Put"
	StrikePrice    float64   `gorm:"not null"`
	ImpliedVol     float64   `gorm:"not null"`
	OpenInterest   int64     `gorm:"not null"`
	SettlementDate time.Time `gorm:"not null"`
	ContractSize   int       `gorm:"not null;default:100"`
	CreatedAt      time.Time `gorm:"autoCreateTime"`
	UpdatedAt      time.Time `gorm:"autoUpdateTime"`
}
