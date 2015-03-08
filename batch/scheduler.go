package main

import (
	"github.com/k0kubun/githubranking/batch/db"
	"github.com/k0kubun/githubranking/batch/github"
	"log"
)

type Scheduler struct {
	queue chan int
	funcs []func() []int
}

const (
	batchSize = 1000
)

var (
	scFuncs = []func() []int{
		scNewUsers,
	}
)

func NewScheduler(queue chan int) *Scheduler {
	return &Scheduler{
		queue: queue,
		funcs: scFuncs,
	}
}

func (s *Scheduler) Schedule() {
	for {
		for _, f := range s.funcs {
			userIds := f()
			if len(userIds) == 0 {
				return
			}
			log.Printf("scNewUsers: %d\n", len(userIds))

			for _, userId := range userIds {
				s.queue <- userId
			}
		}
	}
}

func scNewUsers() []int {
	lastId, err := db.LastUserId()
	if err != nil {
		return []int{}
	}

	return createNewUsers(lastId)
}

func scStarredUsers() []int {
	return []int{}
}

func scPublicReposUsers() []int {
	return []int{}
}

func scEmptyUsers() []int {
	return []int{}
}

func createNewUsers(lastId int) []int {
	users := github.AllUsers(lastId)
	db.CreateUsers(users)

	ids := []int{}
	for _, user := range users {
		ids = append(ids, user.ID)
	}
	return ids
}
