package main

import (
	"database/sql"
	"fmt"
	_ "github.com/go-sql-driver/mysql"
	"strconv"
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

func commaJoin(nums []int) string {
	if len(nums) == 0 {
		return ""
	}

	// Use buffer for performance
	buf := make([]byte, 0, 10)

	for _, num := range nums {
		numStr := strconv.Itoa(num)
		buf = append(buf, numStr...)
		buf = append(buf, ","...)
	}

	return string(buf[0 : len(buf)-1])
}
