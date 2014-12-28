package main

import (
	"fmt"
	"log"
	"time"
)

const (
	batchSize = 6900
)

func schedulerLoop(queue chan int) {
	for {
		ids := notQueuedIds(batchSize)
		for _, id := range filterIds(ids) {
			queue <- id
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

func notQueuedIds(size int) []int {
	ids := []int{}

	rows, err := db.Query(
		"SELECT id FROM users WHERE queued_at IS NULL LIMIT ?;",
		size,
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

func filterIds(ids []int) []int {
	if len(ids) == 0 {
		return []int{}
	}

	rows, err := db.Query(
		fmt.Sprintf(
			"SELECT distinct(owner_id) FROM repositories WHERE owner_id IN (%s);",
			commaJoin(ids),
		),
	)
	if err != nil {
		log.Fatal(err)
	}

	filtered := []int{}
	var id int
	for rows.Next() {
		err = rows.Scan(&id)
		if err != nil {
			log.Fatal(err)
		}

		filtered = append(filtered, id)
	}

	return filtered
}
