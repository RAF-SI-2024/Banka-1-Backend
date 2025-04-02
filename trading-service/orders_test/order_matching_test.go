package orders_test_test

import (
	"banka1.com/db"
	"banka1.com/orders"
	"banka1.com/types"
	"log"
	"os"
	"testing"
	"time"
)

func TestMain(m *testing.M) {
	err := db.InitTestDatabase()
	if err != nil {
		log.Fatalf("Greška prilikom inicijalizacije test baze: %v", err)
	}
	code := m.Run()
	os.Exit(code)
}

func createTestOrder(userID uint, direction string, quantity int, securityID uint, margin bool, aon bool) types.Order {
	remaining := quantity
	order := types.Order{
		UserID:         userID,
		AccountID:      1,
		SecurityID:     securityID,
		Quantity:       quantity,
		RemainingParts: &remaining,
		OrderType:      "MARKET",
		Direction:      direction,
		Status:         "approved",
		IsDone:         false,
		AON:            aon,
		Margin:         margin,
	}
	db.DB.Create(&order)
	return order
}

func waitForOrderCompletion(orderID uint, timeoutSec int) bool {
	timeout := time.After(time.Duration(timeoutSec) * time.Second)
	ticker := time.Tick(500 * time.Millisecond)

	for {
		select {
		case <-timeout:
			return false
		case <-ticker:
			var refreshed types.Order
			db.DB.First(&refreshed, orderID)
			if refreshed.IsDone {
				return true
			}
		}
	}
}

func Test_MatchOrder_SimpleMarket(t *testing.T) {
	security := types.Security{Name: "TestStock", Ticker: "TST", LastPrice: 100.0, Exchange: "NYSE", Type: "stock"}
	db.DB.Create(&security)

	buyer := createTestOrder(101, "buy", 3, security.ID, false, false)
	seller := createTestOrder(102, "sell", 3, security.ID, false, false)
	_ = seller

	orders.MatchOrder(buyer)
	time.Sleep(1 * time.Second)

	if !waitForOrderCompletion(buyer.ID, 30) {
		t.Errorf("Order nije završen u zadatom vremenu")
	}
}

func Test_MatchOrder_AllOrNoneFail(t *testing.T) {
	security := types.Security{Name: "AONStock", Ticker: "AON", LastPrice: 150.0, Exchange: "NYSE", Type: "stock"}
	db.DB.Create(&security)

	buyer := createTestOrder(201, "buy", 5, security.ID, false, true)
	seller := createTestOrder(202, "sell", 3, security.ID, false, false)
	_ = seller

	orders.MatchOrder(buyer)
	time.Sleep(1 * time.Second)
	db.DB.First(&buyer, buyer.ID)

	if buyer.IsDone {
		t.Errorf("AON order je izvrsen iako nije trebalo")
	}
}

func Test_MatchOrder_MarginOrder(t *testing.T) {
	security := types.Security{Name: "MarginStock", Ticker: "MRG", LastPrice: 200.0, Exchange: "NYSE", Type: "stock"}
	db.DB.Create(&security)

	actuary := types.Actuary{UserID: 301, Role: "agent", LimitAmount: 1000, UsedLimit: 0}
	db.DB.Create(&actuary)

	buyer := createTestOrder(301, "buy", 2, security.ID, true, false)
	seller := createTestOrder(302, "sell", 2, security.ID, false, false)
	_ = seller

	orders.MatchOrder(buyer)
	time.Sleep(1 * time.Second)

	if !waitForOrderCompletion(buyer.ID, 30) {
		t.Errorf("Margin order nije izvršen")
	}
	db.DB.Where("user_id = ?", actuary.UserID).First(&actuary)
	if actuary.UsedLimit <= 0 {
		t.Errorf("UsedLimit nije ažuriran za margin order")
	}
}

func Test_MatchOrder_AONMatchFails(t *testing.T) {
	security := types.Security{Name: "FailMatch", Ticker: "FAIL", LastPrice: 120.0, Exchange: "NYSE", Type: "stock"}
	db.DB.Create(&security)

	seller := createTestOrder(401, "sell", 5, security.ID, false, true)
	buyer := createTestOrder(402, "buy", 2, security.ID, false, false)
	_ = seller

	orders.MatchOrder(buyer)
	time.Sleep(2 * time.Second)
	db.DB.First(&seller, seller.ID)

	if seller.IsDone {
		t.Errorf("AON matchovani order je izvršen iako nije trebalo")
	}
}

func Test_MatchOrder_MarginMatchFails(t *testing.T) {
	security := types.Security{Name: "BadMargin", Ticker: "BM", LastPrice: 500.0, Exchange: "NYSE", Type: "stock"}
	db.DB.Create(&security)

	seller := createTestOrder(501, "sell", 2, security.ID, true, false)
	buyer := createTestOrder(502, "buy", 2, security.ID, false, false)
	_ = buyer

	orders.MatchOrder(buyer)
	time.Sleep(2 * time.Second)
	db.DB.First(&seller, seller.ID)

	if seller.IsDone {
		t.Errorf("Margin matchovani order je izvršen iako nije trebalo")
	}
}

func Test_MatchOrder_SelfMatchPrevented(t *testing.T) {
	security := types.Security{Name: "SelfStock", Ticker: "SELF", LastPrice: 100.0, Exchange: "NYSE", Type: "stock"}
	db.DB.Create(&security)

	order1 := createTestOrder(601, "buy", 1, security.ID, false, false)
	order2 := createTestOrder(601, "sell", 1, security.ID, false, false)
	_ = order2

	orders.MatchOrder(order1)
	time.Sleep(2 * time.Second)
	db.DB.First(&order1, order1.ID)

	if order1.IsDone {
		t.Errorf("Self-match je prošao iako ne bi trebalo")
	}
}
