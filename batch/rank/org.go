package main

import (
	"log"
)

type Org struct {
	Id              int
	StargazersCount int
	Rank            int
	CreatedAt       string
	UpdatedAt       string
}

func calcOrgRanks() {
	log.Println("Start calcing orgs")

	lastRank := 0
	current := 9999999999
	for i := 0; ; i++ {
		rows, err := db.Query(
			"SELECT id, stargazers_count, created_at, updated_at FROM users WHERE type = 'Organization' ORDER BY stargazers_count DESC LIMIT ? OFFSET ?;",
			perPage,
			i*perPage,
		)
		defer rows.Close()
		if err != nil {
			log.Fatal("calcOrgRanks: ", err.Error())
		}

		offset := i * perPage
		orgs := []Org{}
		for rows.Next() {
			offset += 1
			org := Org{}

			err = rows.Scan(&org.Id, &org.StargazersCount, &org.CreatedAt, &org.UpdatedAt)
			if err != nil {
				log.Fatal("calcOrgRanks rows.Next()", err.Error())
			}

			if current == org.StargazersCount {
				org.Rank = lastRank
			} else {
				org.Rank = offset
				lastRank = offset
				current = org.StargazersCount
			}

			orgs = append(orgs, org)
		}

		if len(orgs) == 0 {
			break
		}
		bulkInsertOrgs(orgs)
		log.Printf("Org Page: %d (%d,%d)\n", i+1, offset, lastRank)
	}
}

func bulkInsertOrgs(orgs []Org) {
	sql := "INSERT INTO users (id,rank,created_at,updated_at) VALUES "

	values := []interface{}{}
	for _, org := range orgs {
		sql += "(?,?,?,?),"
		values = append(
			values,
			org.Id,
			org.Rank,
			org.CreatedAt,
			org.UpdatedAt,
		)
	}
	sql = sql[0 : len(sql)-1] // trim last ,
	sql += " ON DUPLICATE KEY UPDATE rank=VALUES(rank);"
	_, err := db.Exec(sql, values...)
	if err != nil {
		log.Println("bulkInsertOrgs: ", err.Error())
	}
}
