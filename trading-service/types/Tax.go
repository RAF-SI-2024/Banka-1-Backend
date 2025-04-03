package types

// swagger:model
type TaxResponse struct {
	UserID        uint    `json:"user_id"`
	TaxableProfit float64 `json:"taxable_profit"`
	TaxAmount     float64 `json:"tax_amount"`
	IsPaid        bool    `json:"is_paid"`
	IsActuary     bool    `json:"is_actuary"`
}

type AggregatedTaxResponse struct {
	UserID          uint    `json:"user_id"`
	PaidThisYear    float64 `json:"paid_this_year"`
	UnpaidThisMonth float64 `json:"unpaid_this_month"`
	IsActuary       bool    `json:"is_actuary"`
}
