package main

import (
	"log"
)

func workerLoop(index int, queue chan int) {
	log.Printf("[%d] Invoke worker\n", index)

	for {
		id := <-queue
		updateStarCount(id)
	}
}

func updateStarCount(userId int) {
	log.Printf("[%d] %d\n", index, id)
}
