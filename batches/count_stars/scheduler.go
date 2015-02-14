package main

import (
	"fmt"
	"time"
)

const (
	scheduleBatchSize = 2000
)

var (
	maxQueuedAt = time.Date(2015, time.February, 13, 13, 18, 0, 0, time.UTC)
)

func scheduleAll(reqq chan int) {
	total := 0

	for {
		ids := notQueuedIds()
		if len(ids) == 0 {
			break
		}

		markAsQueued(ids)
		ids = filterNoPublicRepos(ids)
		for _, id := range ids {
			reqq <- id
		}

		total += len(ids)
		puts(total)
	}
}

func notQueuedIds() []int {
	sql := "SELECT id, queued_at FROM users ORDER BY queued_at ASC LIMIT ?;"
	rows, err := db.Query(sql, scheduleBatchSize)
	defer rows.Close()
	assertSql(sql, err)

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

func filterNoPublicRepos(userIds []int) []int {
	if len(userIds) == 0 {
		return []int{}
	}

	sql := fmt.Sprintf(
		"SELECT id FROM users WHERE id IN (%s) AND public_repos > 0;",
		commaJoin(userIds),
	)
	rows, err := db.Query(sql)
	defer rows.Close()
	assertSql(sql, err)

	ids := []int{}
	var id int
	for rows.Next() {
		err = rows.Scan(&id)
		assert(err)
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
	assertSql(sql, err)
}
