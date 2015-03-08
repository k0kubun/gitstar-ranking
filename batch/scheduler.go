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
		scAllUsers,
	}
	allUsersLastId = 0
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

func scAllUsers() []int {
	ids := db.AllUserIds(allUsersLastId)
	allUsersLastId = ids[len(ids)-1]
	log.Println(allUsersLastId)
	return ids
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
