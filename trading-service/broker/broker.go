package broker

import (
	"banka1.com/dto"
	"encoding/json"
	"fmt"
	"log"
	"os"
	"sync/atomic"

	"github.com/go-stomp/stomp/v3"
)

var conn *stomp.Conn

func Connect(network, hostname string) {
	c, err := stomp.Dial(network, hostname,
		stomp.ConnOpt.HeartBeatGracePeriodMultiplier(3.0),
		stomp.ConnOpt.Login("guest", "guest"),
		stomp.ConnOpt.Host("/"))
	if err != nil {
		log.Fatalf("Konekcija sa brokerom nije uspela: %v", err)
	}
	conn = c
}

var tempQueueNumber atomic.Uint64

func sendAndRecieve(address string, object any, response any) error {
	subscription, err := conn.Subscribe(fmt.Sprintf("/temp-queue/%x", tempQueueNumber.Add(1)), stomp.AckClientIndividual)
	if err != nil {
		log.Printf("Neuspelo kreiranje subscription-a: %v", err)
		return err
	}
	errorChan := make(chan error)

	go func() {
		defer subscription.Unsubscribe()
		var err error
		responseMessage, err := subscription.Read()
		if err != nil {
			log.Printf("Neuspesno primanje reply-a: %v", err)
			errorChan <- err
			return
		}

		err = conn.Ack(responseMessage)
		if err != nil {
			log.Printf("Neuspesno prihvatanje reply-a: %v", err)
			errorChan <- err
			return
		}

		err = json.Unmarshal(responseMessage.Body, response)
		if err != nil {
			log.Printf("Neuspesno parsiranje reply-a: %v", err)
			errorChan <- err
			return
		}

		errorChan <- nil
	}()

	body, err := json.Marshal(object)
	if err != nil {
		log.Printf("Neuspelo pretvaranje poruke u JSON: %v", err)
		return err
	}

	err = conn.Send(address, "application/json", body,
		stomp.SendOpt.Header("reply-to", subscription.Destination()),
		stomp.SendOpt.Receipt)

	if err != nil {
		log.Printf("Neuspesan send: %v", err)
		return err
	}

	select {
	case err := <-errorChan:
		return err
	}
}

func send(address string, object any) error {
	body, err := json.Marshal(object)
	if err != nil {
		log.Printf("Neuspelo pretvaranje poruke u JSON: %v", err)
		return err
	}

	err = conn.Send(address, "application/json", body)

	if err != nil {
		log.Printf("Neuspesan send: %v", err)
		return err
	}

	return nil
}

func sendReliable(address string, object any) error {
	body, err := json.Marshal(object)
	if err != nil {
		log.Printf("Neuspelo pretvaranje poruke u JSON: %v", err)
		return err
	}

	err = conn.Send(address, "application/json", body, stomp.SendOpt.Receipt)

	if err != nil {
		log.Printf("Neuspesan send: %v", err)
		return err
	}

	return nil
}

func listen(address string, handler func(*stomp.Subscription, *stomp.Message), handlerErr func(*stomp.Subscription, error)) {
	subscription, err := conn.Subscribe(address, stomp.AckClientIndividual)
	if err != nil {
		log.Fatalf("Neuspelo kreiranje subscription-a za listener %v: %v", address, err)
	}
	defer subscription.Unsubscribe()

	for true {
		message, err := subscription.Read()
		if err != nil {
			handlerErr(subscription, err)
			continue
		}

		go handler(subscription, message)
	}
}

func SendOrderTransactionInit(dto *dto.OrderTransactionInitiationDTO) error {
	brokerHost := os.Getenv("MESSAGE_BROKER_HOST")
	if brokerHost == "" {
		fmt.Println("[WARN] MESSAGE_BROKER_HOST nije postavljen. Preskačem slanje na broker tokom testa.")
		return nil
	}

	conn, err := stomp.Dial("tcp", brokerHost)

	if err != nil {
		fmt.Println("Neuspešno povezivanje na broker:", err)
		return err
	}
	defer conn.Disconnect()

	data, err := json.Marshal(dto)
	if err != nil {
		fmt.Println("Neuspešan marshal DTO-a:", err)
		return err
	}

	err = conn.Send(
		"/queue/order-init",
		"application/json",
		data,
	)
	if err != nil {
		fmt.Println("Neuspešno slanje poruke:", err)
		return err
	}

	fmt.Println("Poslata poruka za OrderTransactionInitiationDTO preko brokera.")
	return nil
}
