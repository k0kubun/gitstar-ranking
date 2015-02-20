package main

import (
	"runtime"
)

const (
	concurrency = 48
	queueLength = 8000
)

func main() {
	runtime.GOMAXPROCS(concurrency)
	queue := make(chan int, queueLength)

	for i := 0; i < concurrency-1; i++ {
		s := NewStream(queue)
		go s.Process()
	}

	NewScheduler(queue).Schedule()
}
