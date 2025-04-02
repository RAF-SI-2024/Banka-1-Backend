package types

// swagger:model
type TaxResponse struct {
	UserID        uint    `json:"user_id"`
	TaxableProfit float64 `json:"taxable_profit"`
	TaxAmount     float64 `json:"tax_amount"`
	IsPaid        bool    `json:"is_paid"`
	IsActuary     bool    `json:"is_actuary"`
}
