package main

import (
	"github.com/octokit/go-octokit/octokit"
	"runtime"
)

const (
	requestConcurrency = 100
	bufferedImports    = 1000
	bufferedRequests   = 1000
	bufferedDestroys   = 1000
)

func main() {
	defer db.Close()
	defer logF.Close()
	runtime.GOMAXPROCS(requestConcurrency + 3)

	setAllPublicRepos()
}

func updateAllUsers() {
	importQueue := make(chan *octokit.User, bufferedImports)
	requestQueue := make(chan int, bufferedRequests)
	destroyQueue := make(chan int, bufferedDestroys)

	go importWorker(importQueue)
	go destroyWorker(destroyQueue)
	for i := 0; i < requestConcurrency; i++ {
		go requestWorker(requestQueue, importQueue, destroyQueue)
	}

	scheduleAll(requestQueue)
}

func setAllPublicRepos() {
	rows, err := db.Query(
		"SELECT id FROM users WHERE public_repos IS NULL;",
	)
	defer rows.Close()
	assert(err)

	ids := []int{}
	var id int
	for rows.Next() {
		err = rows.Scan(&id)
		assert(err)
		ids = append(ids, id)
	}

	for _, id := range ids {
		user, err := getUser(id)
		logError(err)
		if user != nil {
			importUser(user)
		}
	}
}
