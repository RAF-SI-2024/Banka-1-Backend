package dto

type UpdateActuaryDTO struct {
	LimitAmount *string `json:"limit,omitempty"`
	ResetLimit  bool    `json:"reset,omitempty"`
}
