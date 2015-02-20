package db

import (
	"github.com/octokit/go-octokit/octokit"
)

func LastUserId() (int, error) {
	var id int
	rows, err := Query("SELECT id FROM users ORDER BY id DESC LIMIT 1")
	if err != nil {
		return 0, err
	}
	defer rows.Close()

	for rows.Next() {
		rows.Scan(&id)
	}
	return id, nil
}

func CreateUsers(users []octokit.User) {
	if len(users) == 0 {
		return
	}
}
