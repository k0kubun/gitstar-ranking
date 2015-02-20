package db

import (
	"database/sql"
	"fmt"
	_ "github.com/go-sql-driver/mysql"
	"log"
	"os"
	"time"
)

const (
	mysqlMaxConn = 100
	maxRetrial   = 5
)

var (
	db *sql.DB
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

	logF, err := os.OpenFile(
		getEnv("GR_BATCH_LOG", "db.log"),
		os.O_RDWR|os.O_CREATE|os.O_APPEND,
		0644,
	)
	if err != nil {
		log.Fatal(err)
	}

	log.SetOutput(logF)
}

func Query(query string, a ...interface{}) (*sql.Rows, error) {
	var rows *sql.Rows
	var err error

	for i := 0; i < maxRetrial; i++ {
		rows, err = db.Query(query, a...)
		if err == nil {
			return rows, err
		}
		time.Sleep(5 * time.Second)
	}
	log.Println(query)
	log.Println(err)
	return rows, err
}

func getEnv(key string, def string) string {
	v := os.Getenv(key)
	if len(v) == 0 {
		return def
	}

	return v
}
