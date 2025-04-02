package exchanges

import (
    "encoding/csv"
    "fmt"
    "os"
    "path/filepath"

    "banka1.com/db"
    "banka1.com/types"
)

func LoadExchanges(csvPath string) error {
	/*
	Function to load the data from the CSV file into the mysql database.
	Model:
			ID        uint   	UNIQUE
			Name      string 
			Acronym   string 
			MicCode   string 	UNIQUE
			Country   string 
			Currency  string 
			Timezone  string
			OpenTime  string 
			CloseTime string

	This function is designed to be executed when first starting main.go to load the default exchanges into the database.

	Parameters:
		csvPath (string): The path to the CSV file containing the exchange data.
	
	Returns:
		error: An error if the function fails to load the data from the CSV file into the database.
		
		nil: If the function successfully loads the data from the CSV file into the database.
	*/

	// CSV handling code
    file, err := os.Open(csvPath)
    if err != nil {
        return fmt.Errorf("failed to open CSV file: %w", err)
    }
    defer file.Close()

    reader := csv.NewReader(file)
    
    headers, err := reader.Read()
    if err != nil {
        return fmt.Errorf("failed to read CSV header: %w", err)
    }
    
    expectedHeaders := []string{"Exchange Name", "Exchange Acronym", "Exchange Mic Code", "Country", "Currency", "Time Zone", "Open Time", "Close Time"}
    for i, header := range expectedHeaders {
        if i >= len(headers) || headers[i] != header {
            return fmt.Errorf("CSV header mismatch: expected %s at position %d, got %s", header, i, headers[i])
        }
    }

    records, err := reader.ReadAll()
    if err != nil {
        return fmt.Errorf("failed to read CSV data: %w", err)
    }

	// Database handling code
    for _, record := range records {
        if len(record) < 8 {
            return fmt.Errorf("invalid record format, expected 8 fields, got %d", len(record))
        }
        
        // Field Mapping
        exchange := types.Exchange{
            Name:      record[0],
            Acronym:   record[1],
            MicCode:   record[2],
            Country:   record[3],
            Currency:  record[4],
            Timezone:  record[5],
            OpenTime:  record[6],
            CloseTime: record[7],
        }
        
        // Mic code unique
        var existingExchange types.Exchange
        result := db.DB.Where("mic_code = ?", exchange.MicCode).First(&existingExchange)
        
        if result.Error == nil {
            existingExchange.Name = exchange.Name
            existingExchange.Acronym = exchange.Acronym
            existingExchange.Country = exchange.Country
            existingExchange.Currency = exchange.Currency
            existingExchange.Timezone = exchange.Timezone
            existingExchange.OpenTime = exchange.OpenTime
            existingExchange.CloseTime = exchange.CloseTime
            
            if err := db.DB.Save(&existingExchange).Error; err != nil {
                return fmt.Errorf("failed to update exchange %s: %w", exchange.MicCode, err)
            }
        } else {
            if err := db.DB.Create(&exchange).Error; err != nil {
                return fmt.Errorf("failed to create exchange %s: %w", exchange.MicCode, err)
            }
        }
    }
    
    return nil
}

func LoadDefaultExchanges() error {
	/*
	Function to load the default exchanges from the exchanges.csv file into the mysql database.
	This is the default csv file that contains the data for the exchanges.
	*/
    dir, err := os.Getwd()
    if err != nil {
        return fmt.Errorf("failed to get current directory: %w", err)
    }
    
	// If new path is needed, change the path here 
    csvPath := filepath.Join(dir, "exchanges/exchanges.csv")
    
    if _, err := os.Stat(csvPath); err != nil {
        return fmt.Errorf("exchanges.csv file not found: %w", err)
    }
    
    return LoadExchanges(csvPath)
}