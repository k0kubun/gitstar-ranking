package main

import (
	"github.com/octokit/go-octokit/octokit"
)

type Stream struct {
	queue chan int
	funcs []func(*octokit.User, []octokit.Repository, error) error
}

var (
	stFuncs = []func(*octokit.User, []octokit.Repository, error) error{
		stUpdateUsers,
	}
)

func NewStream(queue chan int) *Stream {
	return &Stream{
		queue: queue,
		funcs: []func(*octokit.User, []octokit.Repository, error) error{},
	}
}

func (s *Stream) Process() {
	for {
		select {
		case userId := <-s.queue:
			user, repos, err := s.requestData(userId)
			for _, f := range s.funcs {
				f(user, repos, err)
			}
		}
	}
}

func (s *Stream) requestData(userId int) (*octokit.User, []octokit.Repository, error) {
	return nil, []octokit.Repository{}, nil
}

func stUpdateUsers(user *octokit.User, repos []octokit.Repository, err error) error {
	return nil
}
