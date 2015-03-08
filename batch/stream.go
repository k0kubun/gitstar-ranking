package main

import (
	"github.com/k0kubun/githubranking/batch/github"
	"github.com/octokit/go-octokit/octokit"
)

type Stream struct {
	queue chan int
	done  chan bool
	funcs []func(*octokit.User, []octokit.Repository, error)
}

var (
	stFuncs = []func(*octokit.User, []octokit.Repository, error){
		stUpdateUsers,
		stDestroyUsers,
		stUpdateRepos,
		stDestroyRepos,
	}
)

func NewStream(queue chan int, done chan bool) *Stream {
	return &Stream{
		queue: queue,
		done:  done,
		funcs: stFuncs,
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
		case <-s.done:
			return
		}
	}
}

func (s *Stream) requestData(userId int) (*octokit.User, []octokit.Repository, error) {
	user := github.FindUser(userId)
	return user, []octokit.Repository{}, nil
}

func stUpdateUsers(user *octokit.User, repos []octokit.Repository, err error) {
	if github.IsNotFound(err) {
		return
	}
}

func stDestroyUsers(user *octokit.User, repos []octokit.Repository, err error) {
}

func stUpdateRepos(user *octokit.User, repos []octokit.Repository, err error) {
}

func stDestroyRepos(user *octokit.User, repos []octokit.Repository, err error) {
}
