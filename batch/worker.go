package main

import (
	"log"
)

func workerLoop(index int, queue chan []int) {
	log.Printf("[%d] Invoke worker\n", index)

	for {
		ids := <-queue
		log.Printf("[%d] %#v\n", index, ids)
	}
}
