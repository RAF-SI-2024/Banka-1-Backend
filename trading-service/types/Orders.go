package types

// swagger:model
type OrderResponse struct {
	ID             uint    `json:"id"`
	UserID         uint    `json:"user_id"`
	SecurityID     uint    `json:"security_id"`
	OrderType      string  `json:"order_type"`
	Quantity       int     `json:"quantity"`
	ContractSize   int     `json:"contract_size"`
	PricePerUnit   float64 `json:"price_per_unit"` // Samo za limit i stop order-e
	Direction      string  `json:"direction"`
	Status         string  `json:"status"`
	ApprovedBy     *uint   `json:"approved_by"` // Supervizor koji je odobrio order
	IsDone         bool    `json:"is_done"`
	LastModified   int64   `json:"last_modified"`
	RemainingParts *int    `json:"remaining_parts"`
	AfterHours     bool    `json:"after_hours"`
}

// swagger:model
type CreateOrderRequest struct {
	UserID       uint    `json:"user_id" validate:"required"`
	SecurityID   uint    `json:"security_id" validate:"required"`
	OrderType    string  `json:"order_type" validate:"required,oneofci=market limit stop"`
	Quantity     int     `json:"quantity" validate:"required,gt=0"`
	ContractSize int     `json:"contract_size" validate:"required"`
	PricePerUnit float64 `json:"price_per_unit" validate:"required_unless=OrderType market"`
	Direction    string  `json:"direction" validate:"required,oneofci=buy sell"`
}
