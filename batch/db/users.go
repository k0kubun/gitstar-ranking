package db

import (
	"github.com/octokit/go-octokit/octokit"
	"time"
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

	BulkInsert(
		wrapUsers(users),
		"users",
		[]string{
			"id",
			"login",
			"avatar_url",
			"type",
			"created_at",
			"updated_at",
		},
		func(record interface{}) []interface{} {
			user := record.(octokit.User)
			now := time.Now().UTC()
			return []interface{}{
				user.ID,
				user.Login,
				user.AvatarURL,
				user.Type,
				now,
				now,
			}
		},
	)
}

func wrapUsers(users []octokit.User) []interface{} {
	values := []interface{}{}
	for _, user := range users {
		values = append(values, user)
	}
	return values
}
