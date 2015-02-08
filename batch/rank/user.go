package main

import (
	"log"
)

type User struct {
	Id              int
	StargazersCount int
	Rank            int
	CreatedAt       string
	UpdatedAt       string
}

func calcUserRanks() {
	log.Println("Start calcing users")

	lastRank := 0
	current := 9999999999
	for i := 0; ; i++ {
		rows, err := db.Query(
			"SELECT id, stargazers_count, created_at, updated_at FROM users WHERE type = 'User' ORDER BY stargazers_count DESC LIMIT ? OFFSET ?;",
			perPage,
			i*perPage,
		)
		defer rows.Close()
		if err != nil {
			log.Fatal("calcUserRanks: ", err.Error())
		}

		offset := i * perPage
		users := []User{}
		for rows.Next() {
			offset += 1
			user := User{}

			err = rows.Scan(&user.Id, &user.StargazersCount, &user.CreatedAt, &user.UpdatedAt)
			if err != nil {
				log.Fatal("calcUserRanks rows.Next()", err.Error())
			}

			if current == user.StargazersCount {
				user.Rank = lastRank
			} else {
				user.Rank = offset
				lastRank = offset
				current = user.StargazersCount
			}

			users = append(users, user)
		}

		if len(users) == 0 {
			break
		}
		bulkInsertUsers(users)
		log.Printf("User Page: %d (%d,%d)\n", i+1, offset, lastRank)
	}
}

func bulkInsertUsers(users []User) {
	sql := "INSERT INTO users (id,rank,created_at,updated_at) VALUES "

	values := []interface{}{}
	for _, user := range users {
		sql += "(?,?,?,?),"
		values = append(
			values,
			user.Id,
			user.Rank,
			user.CreatedAt,
			user.UpdatedAt,
		)
	}
	sql = sql[0 : len(sql)-1] // trim last ,
	sql += " ON DUPLICATE KEY UPDATE rank=VALUES(rank);"
	_, err := db.Exec(sql, values...)
	if err != nil {
		log.Println("bulkInsertUsers: ", err.Error())
	}
}
