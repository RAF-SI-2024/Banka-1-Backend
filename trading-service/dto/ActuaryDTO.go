package dto

type ActuaryDTO struct {
	UserID       uint    `json:"userID" validate:"required"`
	Role         string  `json:"role" validate:"required,oneof=supervisor agent"`
	LimitAmount  float64 `json:"limitAmount,omitempty"`
	UsedLimit    float64 `json:"usedLimit,omitempty"`
	NeedApproval bool    `json:"needApproval"`
}

type FilteredActuaryDTO struct {
	ID           uint    `json:"id"`
	FirstName    string  `json:"firstName"`
	LastName     string  `json:"lastName"`
	Email        string  `json:"email"`
	Role         string  `json:"role"`
	LimitAmount  float64 `json:"limitAmount"`
	UsedLimit    float64 `json:"usedLimit"`
	NeedApproval bool    `json:"needApproval"`
}

type EmployeeResponse struct {
	ID          uint     `json:"id"`
	FirstName   string   `json:"firstName"`
	LastName    string   `json:"lastName"`
	Username    string   `json:"username"`
	BirthDate   string   `json:"birthDate"`
	Gender      string   `json:"gender"`
	Email       string   `json:"email"`
	PhoneNumber string   `json:"phoneNumber"`
	Address     string   `json:"address"`
	Position    string   `json:"position"`
	Department  string   `json:"department"`
	Active      bool     `json:"active"`
	IsAdmin     bool     `json:"isAdmin"`
	Permissions []string `json:"permissions"`
}
