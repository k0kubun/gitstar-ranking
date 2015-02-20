package main

import (
	"github.com/k0kubun/githubranking/batch/db"
	"github.com/k0kubun/githubranking/batch/github"
	prettyprint "github.com/k0kubun/pp"
	"log"
	"os"
	"runtime"
	"time"
)

const (
	concurrency = 48
	queueLength = 8000
)

func init() {
	logF, err := os.OpenFile(
		"main.log",
		os.O_RDWR|os.O_CREATE|os.O_APPEND,
		0644,
	)
	if err != nil {
		log.Fatal(err)
	}

	log.SetOutput(logF)
}

func main() {
	scheduleAll()
}

func scheduleAll() {
	runtime.GOMAXPROCS(concurrency)
	queue := make(chan int, queueLength)
	done := make(chan bool)

	for i := 0; i < concurrency-1; i++ {
		s := NewStream(queue, done)
		go s.Process()
	}

	NewScheduler(queue).Schedule()
	waitQueue(queue)
	joinStreams(done)
}

func waitQueue(queue chan int) {
	for len(queue) > 0 {
		log.Printf("Len: %d, Sleep 5s...\n", len(queue))
		time.Sleep(5 * time.Second)
	}
}

func joinStreams(done chan bool) {
	for i := 0; i < concurrency-1; i++ {
		done <- true
	}
}

func importNewUsers() {
	lastId, _ := db.LastUserId()
	users := github.AllUsers(lastId)
	db.CreateUsers(users)
}

func testApi() {
	lastId, _ := db.LastUserId()
	users := github.AllUsers(lastId)
	pp(users)
}

func pp(a ...interface{}) {
	prettyprint.Println(a...)
}
