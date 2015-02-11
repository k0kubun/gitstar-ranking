package main

func destroyWorker(dstq chan int) {
	for {
		userId := <-dstq
		destroyUser(userId)
	}
}

func destroyUser(userId int) {
	_, err := db.Exec(
		"DELETE FROM users WHERE id = ?;",
		userId,
	)
	logError(err)
}
