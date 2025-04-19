package broker

import (
	"encoding/json"
	"fmt"
	"log"
	"time"
	"errors"

	"banka1.com/db"
	"banka1.com/dto"
	"banka1.com/saga"
	"banka1.com/types"

	"github.com/go-stomp/stomp/v3"
	"gorm.io/gorm"
)

func handle(_ *stomp.Subscription, message *stomp.Message, makeObject func() any, handler func(any) any) {
	err := conn.Ack(message)
	if err != nil {
		log.Printf("Neuspesno prihvatanje poruke: %v", err)
		return
	}

	replyTo := message.Header.Get("reply-to")
	if replyTo == "" {
		log.Printf("Poruka nema postavljenu ReplyTo adresu: %v", message)
		return
	}

	object := makeObject()

	err = json.Unmarshal(message.Body, object)
	if err != nil {
		log.Printf("Neuspesno parsiranje poruke: %v", err)
		return
	}

	response := handler(object)

	err = send(replyTo, response)
	if err != nil {
		log.Printf("Neuspesno slanje reply-a: %v", err)
		return
	}
}

func handleNoReply(_ *stomp.Subscription, message *stomp.Message, makeObject func() any, handler func(any)) {
	err := conn.Ack(message)
	if err != nil {
		log.Printf("Neuspesno prihvatanje poruke: %v", err)
		return
	}

	object := makeObject()

	err = json.Unmarshal(message.Body, object)
	if err != nil {
		log.Printf("Neuspesno parsiranje poruke: %v", err)
		return
	}

	handler(object)
}

func handleReliable(_ *stomp.Subscription, message *stomp.Message, makeObject func() any, handler func(any) error) {
	object := makeObject()

	err := json.Unmarshal(message.Body, object)
	if err != nil {
		log.Printf("Neuspesno parsiranje poruke: %v", err)
		return
	}

	for true {
		err = handler(object)
		if err == nil {
			break
		}
		log.Printf("Greska u procesiranju poruke %v, sledeci pokusaj za 5 sekundi. Greska: %v", message, err)
		time.Sleep(5 * time.Second)
	}

	err = conn.Ack(message)
	if err != nil {
		log.Printf("Neuspesno prihvatanje poruke: %v", err)
		return
	}
}

func defaultErrHandler(_ *stomp.Subscription, err error) {
	log.Fatalf("Greska u primanju poruke: %v", err)
}

func wrap(makeObject func() any, handler func(any) any) func(*stomp.Subscription, *stomp.Message) {
	return func(subscription *stomp.Subscription, message *stomp.Message) {
		handle(subscription, message, makeObject, handler)
	}
}

func wrapNoReply(makeObject func() any, handler func(any)) func(*stomp.Subscription, *stomp.Message) {
	return func(subscription *stomp.Subscription, message *stomp.Message) {
		handleNoReply(subscription, message, makeObject, handler)
	}
}

func wrapReliable(makeObject func() any, handler func(any) error) func(*stomp.Subscription, *stomp.Message) {
	return func(subscription *stomp.Subscription, message *stomp.Message) {
		handleReliable(subscription, message, makeObject, handler)
	}
}

func getActuary(id any) any {
	var actuary types.Actuary
	if db.DB.First(&actuary, "id = ?", *(id.(*int))).Error != nil {
		return nil
	}
	return &actuary
}

func handleOTCACK(data any) error {
	dto := data.(*types.OTCTransactionACKDTO)

	phase, exists, err := saga.StateManager.GetPhase(db.DB, dto.Uid)
	if err != nil {
		return err
	}
	if !exists {
		log.Printf("[SAGA] Nepoznat UID: %s", dto.Uid)
		return nil
	}

	if dto.Failure {
		log.Println("Saga failed:", dto.Message)
		go func() {
			for true {
				err := rollbackOwnership(dto.Uid)
				if err == nil {
					break
				}
				log.Printf("Greska u rollbackOwnership (uid %v, poruka %v): %v", dto.Uid, dto.Message, err)
				time.Sleep(3 * time.Second)
			}
		}()
		return nil
	}

	log.Println("Saga progressed for", dto.Uid)

	switch phase {

	case types.PhaseInit:
		log.Println("[SAGA] PhaseInit (ACK rezer. para) za", dto.Uid)
		if err := removeOwnership(dto.Uid); err != nil {
			log.Printf("Ownership REMOVE error: %v", err)
			go FailOTC(dto.Uid, "Ownership remove failed")
			return nil
		}
		go successOTC(dto.Uid)
		return nil

	case types.PhaseOwnershipRemoved:
		log.Println("[SAGA] OwnershipRemove faza za", dto.Uid)
		if err := assignOwnership(dto.Uid); err != nil {
			log.Printf("Ownership ASSIGN error: %v", err)
			go FailOTC(dto.Uid, "Ownership assign failed")
			return nil
		}
		go successOTC(dto.Uid)
		return nil

	case types.PhaseOwnershipTransferred:
		log.Println("[SAGA] OwnershipAssign faza za", dto.Uid)
		saga.StateManager.UpdatePhase(db.DB, dto.Uid, types.PhaseVerified)
		go successOTC(dto.Uid)
		return nil

	case types.PhaseVerified:
		log.Println("[SAGA] FinalCheck faza za", dto.Uid)
		if err := verifyFinalState(dto.Uid); err != nil {
			log.Printf("Finalna provera neuspešna: %v", err)
			go FailOTC(dto.Uid, "Finalna provera neuspešna: "+err.Error())
			return nil
		}

		if err := markContractAsExercised(dto.Uid); err != nil {
			log.Printf("Greška prilikom označavanja ugovora kao izvršenog: %v", err)
			go FailOTC(dto.Uid, fmt.Sprintf("Greška prilikom označavanja ugovora kao izvršenog: %v", err))
			return nil
		}

		log.Println("[SAGA] Uspešno završena saga za", dto.Uid)

		go successOTC(dto.Uid)
		return nil

	default:
		log.Printf("Nepoznata faza SAGA-e za uid: %s", dto.Uid)
		return nil
	}
}

func removeOwnership(uid string) error {
	return db.DB.Transaction(func(tx *gorm.DB) error {
		var contract types.OptionContract
		if err := tx.Where("uid = ?", uid).First(&contract).Error; err != nil {
			return fmt.Errorf("Neuspešno pronalaženje ugovora: %w", err)
		}

		var sellerPortfolio types.Portfolio
		if err := tx.First(&sellerPortfolio, contract.PortfolioID).Error; err != nil {
			return fmt.Errorf("Neuspešno pronalaženje portfolija prodavca: %w", err)
		}

		if sellerPortfolio.Quantity < contract.Quantity {
			return fmt.Errorf("Prodavac nema dovoljno akcija")
		}
		if sellerPortfolio.PublicCount < contract.Quantity {
			return fmt.Errorf("Prodavac nema dovoljno javno raspoloživih akcija")
		}

		sellerPortfolio.Quantity -= contract.Quantity
		sellerPortfolio.PublicCount -= contract.Quantity
		if sellerPortfolio.PublicCount < 0 {
			return fmt.Errorf("PublicCount je postao negativan")
		}

		if err := tx.Save(&sellerPortfolio).Error; err != nil {
			return fmt.Errorf("Greška prilikom ažuriranja portfolija prodavca: %w", err)
		}

		return saga.StateManager.UpdatePhase(tx, uid, types.PhaseOwnershipRemoved)
	})
}

func assignOwnership(uid string) error {
	return db.DB.Transaction(func(tx *gorm.DB) error {
		var contract types.OptionContract
		if err := tx.Where("uid = ?", uid).First(&contract).Error; err != nil {
			return fmt.Errorf("Neuspešno pronalaženje ugovora: %w", err)
		}

		/*
		buyerPortfolio := types.Portfolio{
			UserID:        contract.BuyerID,
			SecurityID:    contract.SecurityID,
			Quantity:      contract.Quantity,
			PurchasePrice: contract.StrikePrice,
			PublicCount:   0,
		}
		*/

		var portfolio types.Portfolio
		err := tx.Where("user_id = ? AND security_id = ?", contract.BuyerID, contract.SecurityID).First(&portfolio).Error

		if err != nil {
			if errors.Is(err, gorm.ErrRecordNotFound) {
				portfolio = types.Portfolio{
					UserID:        contract.BuyerID,
					SecurityID:    contract.SecurityID,
					Quantity:      0,
					PurchasePrice: contract.StrikePrice,
					PublicCount:   0,
				}
				if err := tx.Create(&portfolio).Error; err != nil {
					return fmt.Errorf("Greška prilikom kreiranja portfolija kupca: %w", err)
				}
			} else {
				return fmt.Errorf("Greška prilikom ažuriranja portfolija kupca: %w", err)
			}
		}

		portfolio.Quantity += contract.Quantity
		if err := tx.Save(&portfolio).Error; err != nil {
			return fmt.Errorf("Greška prilikom ažuriranja portfolija kupca: %w", err)
		}

		return saga.StateManager.UpdatePhase(tx, uid, types.PhaseOwnershipTransferred)
	})
}

func successOTC(uid string) {
	for true {
		err := SendOTCTransactionSuccess(uid)
		if err == nil {
			break
		}
		log.Printf("Greska u SendOTCTransactionSuccess (uid %v): %v", uid, err)
		time.Sleep(3 * time.Second)
	}
}

func FailAllOTC() {
	var states []types.OTCSagaState

	if err := db.DB.Find(&states).Error; err != nil {
		log.Fatalf("Greska pri pristupanju transakcijama u bazi: %v", err)
	}

	for _, state := range states {
		FailOTC(state.UID, "Ostala nakon pada servisa")
	}
}

func FailOTC(uid, message string) {
	for true {
		err := rollbackOwnership(uid)
		if err == nil {
			break
		}
		log.Printf("Greska u rollbackOwnership (uid %v, poruka %v): %v", uid, message, err)
		time.Sleep(3 * time.Second)
	}
	for true {
		err := SendOTCTransactionFailure(uid, message)
		if err == nil {
			break
		}
		log.Printf("Greska u SendOTCTransactionFailure (uid %v, poruka %v): %v", uid, message, err)
		time.Sleep(3 * time.Second)
	}
}

func rollbackOwnership(uid string) error {
	return db.DB.Transaction(func(tx *gorm.DB) error {
		var contract types.OptionContract
		if err := tx.Where("uid = ?", uid).First(&contract).Error; err != nil {
			return fmt.Errorf("Neuspešno pronalaženje ugovora: %w", err)
		}

		phase, exists, err := saga.StateManager.GetPhase(tx, uid)

		if err != nil {
			return fmt.Errorf("Neuspešno pronalaženje saga stanja: %w", err)
		}

		if !exists {
			return nil
		}

		if phase >= types.PhaseOwnershipRemoved {
			var sellerPortfolio types.Portfolio
			if err := tx.First(&sellerPortfolio, contract.PortfolioID).Error; err == nil {
				sellerPortfolio.Quantity += contract.Quantity
				sellerPortfolio.PublicCount += contract.Quantity
				if err := tx.Save(&sellerPortfolio).Error; err != nil {
					return fmt.Errorf("Greška prilikom vraćanja portfolija prodavcu: %w", err)
				}
			}
		}

		if phase >= types.PhaseOwnershipTransferred {
			// VODITI RACUNA O TOME DA LI JE TO DOBAR PORTFOLIO ZA BRISANJE
			var buyerPortfolio types.Portfolio
			err = tx.Where("user_id = ? AND security_id = ?", contract.BuyerID, contract.SecurityID).First(&buyerPortfolio).Error
			if err == nil {
				if buyerPortfolio.Quantity < contract.Quantity {
					return fmt.Errorf("Kupčev portfolio ima manje hartija nego što je predviđeno za rollback")
				}
				buyerPortfolio.Quantity -= contract.Quantity
				if buyerPortfolio.Quantity == 0 {
					if err := tx.Delete(&buyerPortfolio).Error; err != nil {
						return fmt.Errorf("Greška prilikom brisanja portfolija kupca: %w", err)
					}
				} else {
					if err := tx.Save(&buyerPortfolio).Error; err != nil {
						return fmt.Errorf("Greška prilikom ažuriranja portfolija kupca: %w", err)
					}
				}
			}
		}

		return saga.StateManager.Remove(tx, uid)
	})
}

func verifyFinalState(uid string) error {
	return db.DB.Transaction(func(tx *gorm.DB) error {
		var contract types.OptionContract
		if err := tx.Where("uid = ?", uid).First(&contract).Error; err != nil {
			return fmt.Errorf("Neuspešno pronalaženje ugovora za proveru integriteta: %w", err)
		}

		var sellerPortfolio types.Portfolio
		if err := tx.First(&sellerPortfolio, contract.PortfolioID).Error; err != nil {
			return fmt.Errorf("Neuspešno pronalaženje portfolija prodavca: %w", err)
		}
		expectedSellerQuantity := sellerPortfolio.Quantity
		if expectedSellerQuantity < 0 {
			return fmt.Errorf("Integritet neuspešan: negativna količina kod prodavca")
		}

		var buyerPortfolios []types.Portfolio
		if err := tx.Where("user_id = ? AND security_id = ?", contract.BuyerID, contract.SecurityID).Find(&buyerPortfolios).Error; err != nil {
			return fmt.Errorf("Greška pri traženju portfolija kupca: %w", err)
		}

		totalBuyerQuantity := 0
		for _, p := range buyerPortfolios {
			totalBuyerQuantity += p.Quantity
		}

		if totalBuyerQuantity < contract.Quantity {
			return fmt.Errorf("Integritet neuspešan: kupac nije dobio sve hartije – očekivano %d, ima %d", contract.Quantity, totalBuyerQuantity)
		}

		return nil
	})
}

func markContractAsExercised(uid string) error {
	return db.DB.Transaction(func(tx *gorm.DB) error {
		now := time.Now().Unix()
		err := saga.StateManager.Remove(tx, uid)
		if err != nil {
			return err
		}
		return tx.Model(&types.OptionContract{}).
			Where("uid = ?", uid).
			Updates(map[string]any{
				"is_exercised": true,
				"exercised_at": now,
				"status":       "closed",
			}).Error

	})
}

func GetAccountsForUser(userId int64) ([]dto.Account, error) {
	req := dto.UserRequest{UserId: userId}
	var response dto.UserAccountsResponse

	err := sendAndRecieve("get-accounts-by-user", req, &response)
	if err != nil {
		log.Printf("Greška prilikom dohvatanja računa korisnika %d: %v", userId, err)
		return nil, err
	}

	return response.Accounts, nil
}

func StartListeners() {
	go listen("get-actuary", wrap(func() any { var id int; return &id }, getActuary), defaultErrHandler)
	go listen("otc-ack-trading", wrapReliable(func() any { return &types.OTCTransactionACKDTO{} }, handleOTCACK), defaultErrHandler)
}
