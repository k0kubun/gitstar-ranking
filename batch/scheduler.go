package main

import (
	"github.com/k0kubun/githubranking/batch/db"
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
		scStarredUsers,
		scPublicReposUsers,
		scEmptyUsers,
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
			for _, userId := range userIds {
				s.queue <- userId
			}
		}
	}
}

func scNewUsers() []int {
	lastId := db.LastUserId()

	return []int{lastId}
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
