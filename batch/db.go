package main

import (
	"database/sql"
	"fmt"
	_ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func init() {
	dsn := fmt.Sprintf(
		"%s:%s@/%s",
		getEnv("GR_DB_USER", "root"),
		getEnv("GR_DB_PASSWORD", ""),
		getEnv("GR_DB_NAME", "github_ranks_development"),
	)

	var err error
	db, err = sql.Open("mysql", dsn)
	if err != nil {
		panic(err)
	}
}

func sqlTest() {
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
