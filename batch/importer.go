package main

import (
	"github.com/octokit/go-octokit/octokit"
)

func importWorker(impq chan *octokit.User) {
	for {
		user := <-impq
		importUser(user)
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
