package main

import (
	"database/sql"
	"fmt"
	_ "github.com/go-sql-driver/mysql"
)

func main() {
	db, err := sql.Open("mysql", "root:@/github_ranks_development")
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
