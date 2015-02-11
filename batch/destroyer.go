package main

import (
	"log"
)

func destroyWorker(dstq chan int) {
	for {
		userId := <-dstq
		log.Fatal(userId)
	}
}

func destroyUser(userId int) {
	_, err := db.Exec(
		"DELETE FROM users WHERE id = ?;",
		userId,
	)
	logError(err)
}
