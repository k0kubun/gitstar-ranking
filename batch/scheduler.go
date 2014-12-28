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
		markAsQueued(ids)

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
	defer rows.Close()
	if err != nil {
		log.Fatal("notQueuedIds: ", err.Error())
	}

	var id int
	for rows.Next() {
		err = rows.Scan(&id)
		if err != nil {
			log.Fatal("notQueuedIds rows.Next()", err.Error())
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
	defer rows.Close()
	if err != nil {
		log.Fatal("filterIds: ", err.Error())
	}

	filtered := []int{}
	var id int
	for rows.Next() {
		err = rows.Scan(&id)
		if err != nil {
			log.Fatal("filterIds rows.Next(): ", err.Error())
		}

		filtered = append(filtered, id)
	}

	return filtered
}

func markAsQueued(ids []int) {
	sql := fmt.Sprintf(
		"UPDATE users SET users.queued_at = '%s' WHERE id IN (%s);",
		timeNow(),
		commaJoin(ids),
	)
	_, err := db.Exec(sql)
	if err != nil {
		log.Println("markAsQueued: ", err.Error())
	}
}
