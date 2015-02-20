package github

import (
	"github.com/octokit/go-octokit/octokit"
)

func AllUsers(userId int) []octokit.User {
	users, err := requestUsers("/users?since=%d", userId)
	if err != nil {
		return []octokit.User{}
	}
	return users
}
