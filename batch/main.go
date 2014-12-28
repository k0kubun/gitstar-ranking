package main

import (
	"log"
	"os"
)

const (
	queueLength       = 100
	workerConcurrency = 8
)

var (
	logF *os.File
)

func init() {
	logF, err := os.OpenFile(
		getEnv("GR_BATCH_LOG", "batch.log"),
		os.O_RDWR|os.O_CREATE|os.O_APPEND,
		0644,
	)
	if err != nil {
		panic(err)
	}

	log.SetOutput(logF)
}

func main() {
	defer logF.Close()
	defer db.Close()

	idQueue := make(chan []int, queueLength)

	for i := 0; i < workerConcurrency; i++ {
		workerLoop(i, idQueue)
	}
	schedulerLoop(idQueue)
}
