package db

import (
	"github.com/octokit/go-octokit/octokit"
	"time"
)

const (
	allUsersBatchSize = 5000
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

func AllUserIds(since int) []int {
	ids := []int{}
	rows, err := Query(
		"SELECT id FROM users WHERE id > ? ORDER BY id ASC LIMIT ?",
		since,
		allUsersBatchSize,
	)
	if err != nil {
		return []int{}
	}
	defer rows.Close()

	var id int
	for rows.Next() {
		rows.Scan(&id)
		ids = append(ids, id)
	}
	return ids
}

func UpdateUserLocation(user *octokit.User) {
	if user == nil {
		return
	}

	Exec(
		"UPDATE users SET users.location = ? WHERE id = ?",
		user.Location,
		user.ID,
	)
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
