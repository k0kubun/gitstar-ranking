package main

func destroyWorker(dstq chan int) {
	for {
		userId := <-dstq
		destroyUser(userId)
		destroyRepos(userId)
	}
}

func destroyUser(userId int) {
	_, err := db.Exec(
		"DELETE FROM users WHERE id = ?;",
		userId,
	)
	logError(err)
}

func destroyRepos(userId int) {
	_, err := db.Exec(
		"DELETE FROM repositories WHERE owner_id = ?;",
		userId,
	)
	logError(err)
}
