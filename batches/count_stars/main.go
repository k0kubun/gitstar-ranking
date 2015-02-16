package main

import (
	"github.com/octokit/go-octokit/octokit"
	"os"
	"runtime"
	"time"
)

const (
	requestConcurrency = 48
	bufferedImports    = 1000
	bufferedRequests   = 1000
	bufferedDestroys   = 1000
)

type ImportJob struct {
	UserID int
	Repos  []octokit.Repository
}

func main() {
	defer db.Close()
	defer logF.Close()
	runtime.GOMAXPROCS(requestConcurrency + 3)

	if len(os.Args) > 1 {
		countStarFor(os.Args[1])
	} else {
		countStarOfAllUsers()
	}
}

func countStarFor(login string) {
	userId := idByLogin(login)
	repos, _ := getRepos(userId)
	star := countStars(repos)

	setStargazersCount(userId, star)
	dropDeletedRepos(userId, repos)
	importRepos(repos)
}

func idByLogin(login string) int {
	rows, err := db.Query(
		"SELECT id FROM users where login = ? LIMIT 1;",
		login,
	)
	defer rows.Close()
	assert(err)

	var id int
	for rows.Next() {
		err = rows.Scan(&id)
		assert(err)
	}

	return id
}

func countStarOfAllUsers() {
	importQueue := make(chan *ImportJob, bufferedImports)
	requestQueue := make(chan int, bufferedRequests)
	destroyQueue := make(chan int, bufferedDestroys)

	go importWorker(importQueue)
	go destroyWorker(destroyQueue)
	for i := 0; i < requestConcurrency; i++ {
		go requestWorker(requestQueue, importQueue, destroyQueue)
	}

	defer rescueFailure(requestQueue, importQueue, destroyQueue)
	scheduleAll(requestQueue)
	waitQueues(requestQueue, importQueue, destroyQueue)
}

func rescueFailure(reqq chan int, impq chan *ImportJob, dstq chan int) {
	if r := recover(); r != nil {
		puts("Recoverd:", r)
		go importWorker(impq)
		go destroyWorker(dstq)
		waitQueues(reqq, impq, dstq)
	}
}

func waitQueues(reqq chan int, impq chan *ImportJob, dstq chan int) {
	for {
		if len(reqq) == 0 {
			break
		} else {
			puts("Waiting Reqq: ", len(reqq))
			time.Sleep(5 * time.Second)
		}
	}

	for {
		if len(impq) == 0 {
			break
		} else {
			puts("Waiting Impq: ", len(impq))
			time.Sleep(5 * time.Second)
		}
	}

	for {
		if len(impq) == 0 {
			break
		} else {
			puts("Waiting Dstq: ", len(dstq))
			time.Sleep(5 * time.Second)
		}
	}
}
