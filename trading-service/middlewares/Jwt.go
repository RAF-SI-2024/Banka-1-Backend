package middlewares

import (
	// "fmt"
	"encoding/base64"
	"os"

	"github.com/dgrijalva/jwt-go"
	// "strings"
)

func keyFunc(token *jwt.Token) (interface{}, error) {
	return getSigningKey()
}

func getSigningKey() ([]byte, error) {
	secret := os.Getenv("JWT_SECRET") // Preuzimanje tajnog ključa iz env
	decodedKey, err := base64.StdEncoding.DecodeString(secret)
	if err != nil {
		return nil, err
	}
	return decodedKey, nil
}

func readToken(tokenString string) (*jwt.Token, jwt.MapClaims, error) {
	claims := jwt.MapClaims{}
	token, err := jwt.ParseWithClaims(tokenString, claims, keyFunc)
	if err != nil {
		return nil, claims, err
	}
	return token, claims, nil
}
