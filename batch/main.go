package main

import (
	"github.com/octokit/go-octokit/octokit"
	"runtime"
)

const (
	requestConcurrency = 100
	bufferedImports    = 8000
	bufferedRequests   = 8000
	bufferedDestroys   = 1000
)

func main() {
	defer db.Close()
	defer logF.Close()
	runtime.GOMAXPROCS(requestConcurrency + 3)

	updateAllUsers()
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
