package main

import (
	"fmt"
	"github.com/octokit/go-octokit/octokit"
	"log"
	"time"
)

func importWorker(impq chan *ImportJob) {
	for {
		job := <-impq

		star := countStars(job.Repos)
		setStargazersCount(job.UserID, star)

		dropDeletedRepos(job.UserID, job.Repos)
		err := importRepos(job.Repos)
		if err != nil {
			logError(err)
			log.Printf("Skip for %d. Repos count was %d. Sleeping 5 second...", job.UserID, len(job.Repos))
			time.Sleep(5 * time.Second)
		}
	}
}

func countStars(repos []octokit.Repository) int {
	star := 0
	for _, repo := range repos {
		star += repo.StargazersCount
	}
	return star
}

func setStargazersCount(userId, star int) {
	_, err := db.Exec(
		"UPDATE users SET users.stargazers_count = ? WHERE id = ?;",
		star,
		userId,
	)
	if err != nil {
		logError(err)
		puts(userId, ": Fail to update star:", star)
		time.Sleep(5 * time.Second)
	}
}

func dropDeletedRepos(userId int, repos []octokit.Repository) {
	if len(repos) == 0 {
		return
	}

	ids := []int{}
	for _, repo := range repos {
		ids = append(ids, repo.ID)
	}

	sql := fmt.Sprintf(
		"DELETE FROM repositories WHERE owner_id = %d AND id NOT IN (%s);",
		userId,
		commaJoin(ids),
	)
	_, err := db.Exec(sql)
	assertSql(sql, err)
}

func importRepos(repos []octokit.Repository) error {
	if len(repos) == 0 {
		return nil
	}

	sql := `
	  INSERT INTO repositories (
			id,
			name,
			full_name,
			owner_id,
			description,
			fork,
			created_at,
			updated_at,
			homepage,
			stargazers_count,
			language
		) VALUES
	`

	values := []interface{}{}
	for _, repo := range repos {
		sql += "(?,?,?,?,?,?,?,?,?,?,?),"
		values = append(
			values,
			repo.ID,
			repo.Name,
			repo.FullName,
			repo.Owner.ID,
			repo.Description,
			repo.Fork,
			repo.CreatedAt,
			repo.UpdatedAt,
			repo.Homepage,
			repo.StargazersCount,
			repo.Language,
		)
	}
	sql = sql[0 : len(sql)-1] // trim last ,

	sql += `
		ON DUPLICATE KEY UPDATE
		name=VALUES(name),
		full_name=VALUES(full_name),
		owner_id=VALUES(owner_id),
		description=VALUES(description),
		fork=VALUES(fork),
		created_at=VALUES(created_at),
		updated_at=VALUES(updated_at),
		homepage=VALUES(homepage),
		stargazers_count=VALUES(stargazers_count),
		language=VALUES(language);
	`
	_, err := db.Exec(sql, values...)
	return err
}
