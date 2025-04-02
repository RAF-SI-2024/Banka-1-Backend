package dto

type PortfolioSecurityDTO struct {
	Ticker       string  `json:"ticker"`
	Type         string  `json:"type"`
	Amount       int     `json:"amount"`
	Price        float64 `json:"price"`
	Profit       float64 `json:"profit"`
	LastModified int64   `json:"last_modified"`
}