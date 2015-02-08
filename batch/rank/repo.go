package main

import (
	"log"
)

type Repository struct {
	Id              int
	StargazersCount int
	Rank            int
	CreatedAt       string
	UpdatedAt       string
}

func calcRepoRanks() {
	log.Println("Start calcing repos")

	lastRank := 0
	current := 999999999

	for i := 0; ; i++ {
		rows, err := db.Query(
			"SELECT id, stargazers_count, created_at, updated_at FROM repositories ORDER BY stargazers_count DESC LIMIT ? OFFSET ?;",
			perPage,
			i*perPage,
		)
		defer rows.Close()
		if err != nil {
			log.Fatal("calcRepoRanks: ", err.Error())
		}

		offset := i * perPage
		repos := []Repository{}
		for rows.Next() {
			offset += 1
			repo := Repository{}

			err = rows.Scan(&repo.Id, &repo.StargazersCount, &repo.CreatedAt, &repo.UpdatedAt)
			if err != nil {
				log.Fatal("calcRepoRanks rows.Next()", err.Error())
			}

			if current == repo.StargazersCount {
				repo.Rank = lastRank
			} else {
				repo.Rank = offset
				lastRank = offset
				current = repo.StargazersCount
			}

			repos = append(repos, repo)
		}

		if len(repos) == 0 {
			break
		}
		bulkInsertRepos(repos)
		log.Printf("Repo Page: %d (%d,%d)\n", i+1, offset, lastRank)
	}
}

func bulkInsertRepos(repos []Repository) {
	sql := "INSERT INTO repositories (id,rank,created_at,updated_at) VALUES "

	values := []interface{}{}
	for _, repo := range repos {
		sql += "(?,?,?,?),"
		values = append(
			values,
			repo.Id,
			repo.Rank,
			repo.CreatedAt,
			repo.UpdatedAt,
		)
	}
	sql = sql[0 : len(sql)-1] // trim last ,
	sql += " ON DUPLICATE KEY UPDATE rank=VALUES(rank);"
	_, err := db.Exec(sql, values...)
	if err != nil {
		log.Println("bulkInsertRepos: ", err.Error())
	}
}
