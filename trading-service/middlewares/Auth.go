package middlewares

import (
	"fmt"

	"github.com/gofiber/fiber/v2"
)

func Auth(c *fiber.Ctx) error {
	auth := c.Get("Authorization")
	if auth == "" {
		return c.Status(401).JSON(fiber.Map{
			"error":   "Unauthorized - No token provided",
			"success": false,
		})
	}

	// remove Bearer from token
	token := auth[7:]
	if token == "" {
		return c.Status(401).JSON(fiber.Map{
			"error":   "Unauthorized - No token provided",
			"success": false,
		})
	}

	_, claims, err := readToken(token)
	if err != nil {
		fmt.Println(err)
		return c.Status(401).JSON(fiber.Map{
			"error":   "Unauthorized - Invalid token",
			"success": false,
		})
	}

	fmt.Println(claims)

	c.Locals("claims", claims)
	c.Locals("user_id", claims["id"])
	c.Locals("position", claims["position"])
	c.Locals("department", claims["department"])
	return c.Next()
}

func DepartmentCheck(requiredDept string) fiber.Handler {
	return func(c *fiber.Ctx) error {
		// Dohvatanje department vrednosti iz Locals
		department, ok := c.Locals("department").(string)
		if !ok || department != requiredDept {
			return c.Status(fiber.StatusForbidden).JSON(fiber.Map{
				"success": false,
				"error":   "Unauthorized: Invalid department",
			})
		}

		// Nastavi sa sledećim middleware-om ili handler-om
		return c.Next()
	}
}
