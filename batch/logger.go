package main

import (
	"log"
	"os"
)

var (
	logF *os.File
)

func init() {
	logF, err := os.OpenFile(
		getEnv("GR_BATCH_LOG", "run.log"),
		os.O_RDWR|os.O_CREATE|os.O_APPEND,
		0644,
	)
	if err != nil {
		panic(err)
	}

	log.SetOutput(logF)
}
