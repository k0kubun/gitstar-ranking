package main

import (
	"database/sql"
	"fmt"
	_ "github.com/go-sql-driver/mysql"
)

func main() {
	dsn := fmt.Sprintf(
		"root:@/%s",
		getEnv("GR_DB_NAME", "github_ranks_development"),
	)

	db, err := sql.Open("mysql", dsn)
	if err != nil {
		panic(err)
	}
	defer db.Close()

	rows, err := db.Query("SELECT id FROM users LIMIT 5;")
	if err != nil {
		panic(err)
	}

	var id int
	for rows.Next() {
		err = rows.Scan(&id)
		if err != nil {
			panic(err)
		}

		fmt.Println(id)
	}
}
