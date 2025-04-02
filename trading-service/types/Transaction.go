package types

import "time"

type Transaction struct {
	ID           uint      `gorm:"primaryKey"`
	OrderID      uint      `gorm:"not null"`
	BuyerID      uint      `gorm:"not null"`
	SellerID     uint      `gorm:"not null"`
	SecurityID   uint      `gorm:"not null"`
	Quantity     int       `gorm:"not null"`
	PricePerUnit float64   `gorm:"not null"`
	TotalPrice   float64   `gorm:"not null"`
	CreatedAt    time.Time `gorm:"autoCreateTime"`
}
