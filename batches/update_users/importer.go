package main

import (
	"github.com/octokit/go-octokit/octokit"
)

const (
	importBatchSize = 200
)

func importWorker(impq chan *octokit.User) {
	for {
		users := []*octokit.User{}
		for i := 0; i < importBatchSize; i++ {
			users = append(users, <-impq)
		}
		importUsers(users)
	}
}

func importUser(user *octokit.User) {
	_, err := db.Exec(`
		UPDATE users
		SET users.login = ?, users.avatar_url = ?, users.public_repos = ?
		WHERE id = ?;
		`,
		user.Login,
		user.AvatarURL,
		user.PublicRepos,
		user.ID,
	)
	logError(err)
}

func importUsers(users []*octokit.User) {
	if len(users) == 0 {
		return
	}

	sql := "INSERT INTO users (id, login, avatar_url, public_repos) VALUES "
	values := []interface{}{}
	for _, user := range users {
		sql += "(?,?,?,?),"
		values = append(values, user.ID, user.Login, user.AvatarURL, user.PublicRepos)
	}
	sql = sql[0 : len(sql)-1] // trim last ,
	sql += `
		ON DUPLICATE KEY UPDATE
		id=VALUES(id),
		login=VALUES(login),
		avatar_url=VALUES(avatar_url),
		public_repos=VALUES(public_repos);
	`

	_, err := db.Exec(sql, values...)
	logError(err)
}
