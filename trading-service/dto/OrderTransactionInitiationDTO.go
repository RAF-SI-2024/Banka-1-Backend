package dto

type OrderTransactionInitiationDTO struct {
	Uid             string  `json:"uid"`
	SellerAccountId uint    `json:"sellerAccountId"`
	BuyerAccountId  uint    `json:"buyerAccountId"`
	Amount          float64 `json:"amount"`
	Fee             float64 `json:"fee"`
	Direction       string  `json:"direction"`
}
