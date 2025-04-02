package db

func Bootstrap() {
	// Migracija šema
	migrate(DB)
}
