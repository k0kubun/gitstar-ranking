package main

import (
	"database/sql"
	"fmt"
	_ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

const (
	mysqlMaxConn = 100
)

func init() {
	dsn := fmt.Sprintf(
		"%s:%s@/%s?autocommit=true&parseTime=true",
		getEnv("GR_DB_USER", "root"),
		getEnv("GR_DB_PASSWORD", ""),
		getEnv("GR_DB_NAME", "githubranking"),
	)

	var err error
	db, err = sql.Open("mysql", dsn)
	if err != nil {
		panic(err)
	}

	db.SetMaxOpenConns(mysqlMaxConn)
}
