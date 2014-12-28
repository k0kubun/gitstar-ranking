package main

import (
	"bufio"
	"fmt"
	"github.com/k0kubun/go-octokit/octokit"
	"log"
	"os"
	"strings"
)

var (
	accessTokens []string
	tokenCount   int
	tokenIndex   int
)

func init() {
	fp, err := os.Open("tokens.txt")
	if err != nil {
		panic(err)
	}
	defer fp.Close()

	reader := bufio.NewReader(fp)
	for {
		line, err := reader.ReadString('\n')
		if err != nil {
			break
		}

		token := strings.TrimSuffix(line, "\n")
		accessTokens = append(accessTokens, token)
	}

	tokenCount = len(accessTokens)
}

func workerLoop(index int, queue chan int) {
	log.Printf("[%d] Invoke worker\n", index)

	for {
		updateStarCount(<-queue)
	}
}

func updateStarCount(userId int) {
	login := loginByUserId(userId)
	if login == "" {
		return
	}

	userStar := 0
	repos := allRepositories(login)
	bulkInsertRepositories(repos)

	for _, repo := range repos {
		userStar += repo.StargazersCount
	}
	_, err := db.Exec(
		"UPDATE users SET stargazers_count = ? WHERE id = ?",
		userStar,
		userId,
	)
	if err != nil {
		log.Println(err)
	}
}

// TODO: support splitting queries (for max length of one query), proper created_at
func bulkInsertRepositories(repos []octokit.Repository) {
	if len(repos) == 0 {
		return
	}

	values := []string{}
	now := timeNow()
	for _, repo := range repos {
		value := fmt.Sprintf("(%d,%d,'%s','%s')", repo.ID, repo.StargazersCount, now, now)
		values = append(values, value)
	}

	sql := fmt.Sprintf(
		`INSERT INTO repositories (id,stargazers_count,created_at,updated_at)
		VALUES %s
		ON DUPLICATE KEY UPDATE stargazers_count=VALUES(stargazers_count);`,
		strings.Join(values, ","),
	)
	_, err := db.Exec(sql)
	if err != nil {
		log.Println("bulkInsertRepositories: ", err.Error())
	}
}

func loginByUserId(userId int) string {
	var login string

	rows, err := db.Query("SELECT login FROM users WHERE id = ? LIMIT 1;", userId)
	defer rows.Close()
	if err != nil {
		log.Fatal(err)
	}

	for rows.Next() {
		rows.Scan(&login)
	}
	return login
}

func selectToken() string {
	tokenIndex++
	if tokenIndex == tokenCount {
		tokenIndex = 0
	}
	return accessTokens[tokenIndex]
}
