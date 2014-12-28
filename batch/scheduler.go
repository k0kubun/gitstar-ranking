package main

import (
	"log"
	"time"
)

const (
	batchSize = 6900
)

func schedulerLoop(queue chan []int) {
	for {
		ids := notQueuedIds()

		for _, id := range ids {
			queue <- []int{id}
		}

		break
		time.Sleep(1 * time.Minute)
	}

	var oldLen int
	var newLen int
	for {
		oldLen = newLen
		newLen = len(queue)

		if oldLen != newLen {
			log.Printf("[master] Queue: %d\n", newLen)
		}
		time.Sleep(1 * time.Second)
	}
}

func notQueuedIds() []int {
	ids := []int{}

	rows, err := db.Query(
		"SELECT id FROM users WHERE queued_at IS NULL LIMIT ?;",
		batchSize,
	)
	if err != nil {
		log.Fatal(err)
	}

	var id int
	for rows.Next() {
		err = rows.Scan(&id)
		if err != nil {
			log.Fatal(err)
		}

		ids = append(ids, id)
	}

	return ids
}
