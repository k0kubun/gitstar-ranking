package main

import (
	"os"
)

func getEnv(key string, def string) string {
	v := os.Getenv(key)
	if len(v) == 0 {
		return def
	}

	return v
}
