package main

import (
	"log"
	"os"
	"runtime"
)

const (
	queueLength       = 8000
	workerConcurrency = 24
	divider           = "--------------------------------------------------------"
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

func main() {
	defer logF.Close()
	defer db.Close()

	if len(os.Args) > 1 {
		updateStarCount(os.Args[1])
		return
	}

	runtime.GOMAXPROCS(workerConcurrency + 1)
	idQueue := make(chan int, queueLength)

	log.Println(divider)
	for i := 0; i < workerConcurrency; i++ {
		go workerLoop(i, idQueue)
	}
	schedulerLoop(idQueue)
}
