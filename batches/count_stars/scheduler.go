package main

import (
	"fmt"
	"time"
)

var (
	maxQueuedAt = time.Date(2015, time.February, 11, 16, 35, 0, 0, time.UTC)
)

func scheduleAll(reqq chan int) {
	total := 0

	for {
		ids := notQueuedIds()
		if len(ids) == 0 {
			break
		}

		markAsQueued(ids)
		for _, id := range ids {
			reqq <- id
		}

		total += len(ids)
		puts(total)
	}
}

func notQueuedIds() []int {
	rows, err := db.Query(
		"SELECT id, queued_at FROM users ORDER BY queued_at ASC LIMIT ?;",
		1000,
	)
	defer rows.Close()
	assert(err)

	ids := []int{}
	var id int
	var queuedAt time.Time
	for rows.Next() {
		err = rows.Scan(&id, &queuedAt)
		assert(err)
		if queuedAt.After(maxQueuedAt) {
			continue
		}

		ids = append(ids, id)
	}

	return ids
}

func markAsQueued(ids []int) {
	if len(ids) == 0 {
		return
	}

	sql := fmt.Sprintf(
		"UPDATE users SET users.queued_at = ? WHERE id IN (%s);",
		commaJoin(ids),
	)
	_, err := db.Exec(sql, timeNow())
	assert(err)
}
