package types

// swagger:model
type OrderResponse struct {
	ID                uint     `json:"id"`
	AccountID         uint     `json:"account_id"`
	UserID            uint     `json:"user_id"`
	SecurityID        uint     `json:"security_id"`
	Quantity          int      `json:"quantity"`
	ContractSize      int      `json:"contract_size"`
	StopPricePerUnit  *float64 `json:"stop_price_per_unit"`
	LimitPricePerUnit *float64 `json:"limit_price_per_unit"`
	Direction         string   `json:"direction"`
	Status            string   `json:"status"`
	ApprovedBy        *uint    `json:"approved_by"` // Supervizor koji je odobrio order
	IsDone            bool     `json:"is_done"`
	LastModified      int64    `json:"last_modified"`
	RemainingParts    *int     `json:"remaining_parts"`
	AfterHours        bool     `json:"after_hours"`
	AON               bool     `gorm:"default:false"`
	Margin            bool     `gorm:"default:false"`
}

// swagger:model
type CreateOrderRequest struct {
	UserID            uint     `json:"user_id" validate:"required"`
	AccountID         uint     `json:"account_id" validate:"required"`
	SecurityID        uint     `json:"security_id" validate:"required"`
	Quantity          int      `json:"quantity" validate:"required,gt=0"`
	ContractSize      int      `json:"contract_size" validate:"required"`
	StopPricePerUnit  *float64 `json:"stop_price_per_unit"`
	LimitPricePerUnit *float64 `json:"limit_price_per_unit"`
	Direction         string   `json:"direction" validate:"required,oneofci=buy sell"`
	AON               bool     `json:"aon" validate:"required"`
	Margin            bool     `json:"margin" validate:"required"`
}
