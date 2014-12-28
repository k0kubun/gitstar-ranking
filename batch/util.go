package main

import (
	"bitbucket.org/tebeka/strftime"
	"os"
	"time"
)

func getEnv(key string, def string) string {
	v := os.Getenv(key)
	if len(v) == 0 {
		return def
	}

	return v
}

func timeNow() string {
	str, err := strftime.Format("%Y/%m/%d %H:%M:%S", time.Now())
	if err != nil {
		panic(err)
	}
	return str
}
