package main

import (
	"github.com/k0kubun/githubranking/batch/db"
	"github.com/k0kubun/githubranking/batch/github"
	prettyprint "github.com/k0kubun/pp"
	"log"
	"os"
	"runtime"
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
	importNewUsers()
}

func scheduleAll() {
	runtime.GOMAXPROCS(concurrency)
	queue := make(chan int, queueLength)

	for i := 0; i < concurrency-1; i++ {
		s := NewStream(queue)
		go s.Process()
	}

	NewScheduler(queue).Schedule()
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
