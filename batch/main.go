package main

import (
	"log"
	"os"
	"runtime"
)

const (
	queueLength       = 50000
	workerConcurrency = 7
	divider           = "--------------------------------------------------------"
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

	runtime.GOMAXPROCS(workerConcurrency + 1)
	idQueue := make(chan []int, queueLength)

	log.Println(divider)
	for i := 0; i < workerConcurrency; i++ {
		go workerLoop(i, idQueue)
	}
	schedulerLoop(idQueue)
}
