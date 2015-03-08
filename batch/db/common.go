package db

import (
	"database/sql"
	"fmt"
	_ "github.com/go-sql-driver/mysql"
	"log"
	"os"
	"strings"
	"time"
)

const (
	bulkSize     = 1000
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

func Exec(query string, a ...interface{}) error {
	var err error

	for i := 0; i < maxRetrial; i++ {
		_, err = db.Exec(query, a...)
		if err == nil {
			return nil
		}
		time.Sleep(5 * time.Second)
	}
	log.Println(query)
	log.Println(err)
	return err
}

func BulkInsert(records []interface{}, table string, columns []string, valueFunc func(interface{}) []interface{}) error {
	pageNum := ((len(records) - 1) / bulkSize) + 1
	for i := 0; i < pageNum; i++ {
		min := bulkSize * i
		max := bulkSize * (i + 1)
		if max > len(records) {
			max = len(records)
		}
		err := paginatedBulkInsert(records[min:max], table, columns, valueFunc)
		if err != nil {
			return err
		}
	}
	return nil
}

func paginatedBulkInsert(records []interface{}, table string, columns []string, valueFunc func(interface{}) []interface{}) error {
	if len(records) == 0 {
		return nil
	}

	sql := fmt.Sprintf(
		"INSERT INTO %s (%s) VALUES ",
		table,
		strings.Join(columns, ","),
	)

	values := []interface{}{}
	for _, record := range records {
		vals := valueFunc(record)
		sql += "("
		for _, val := range vals {
			sql += "?,"
			values = append(values, val)
		}
		sql = sql[0 : len(sql)-1] // trim last ,
		sql += "),"
	}
	sql = sql[0 : len(sql)-1] // trim last ,

	sql += " ON DUPLICATE KEY UPDATE"
	for _, column := range columns {
		if column == "id" {
			continue
		}

		sql += fmt.Sprintf(" %s=VALUES(%s),", column, column)
	}
	sql = sql[0 : len(sql)-1] // trim last ,
	sql += ";"
	return Exec(sql, values...)
}

func getEnv(key string, def string) string {
	v := os.Getenv(key)
	if len(v) == 0 {
		return def
	}

	return v
}
