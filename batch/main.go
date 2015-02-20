package main

import (
	"log"
	"os"
	"runtime"
)

const (
	concurrency = 48
	queueLength = 8000
)

func init() {
	logF, err := os.OpenFile(
		"main.log",
		os.O_RDWR|os.O_CREATE|os.O_APPEND,
		0644,
	)
	if err != nil {
		log.Fatal(err)
	}

	log.SetOutput(logF)
}

func main() {
	runtime.GOMAXPROCS(concurrency)
	queue := make(chan int, queueLength)

	for i := 0; i < concurrency-1; i++ {
		s := NewStream(queue)
		go s.Process()
	}

	NewScheduler(queue).Schedule()
}
