package main

import (
	"github.com/octokit/go-octokit/octokit"
	"runtime"
	"time"
)

const (
	requestConcurrency = 80
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

	countStarOfAllUsers()
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

	scheduleAll(requestQueue)
	waitQueues(requestQueue, importQueue, destroyQueue)
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
