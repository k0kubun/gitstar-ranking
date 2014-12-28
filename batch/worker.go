package main

import (
	"bufio"
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
	for _, repo := range repos {
		userStar += repo.StargazersCount
	}
	db.Query(
		"UPDATE users SET stargazers_count = ? WHERE id = ?",
		userStar,
		userId,
	)
	log.Printf("Star %d for %s\n", userStar, login)
}

func loginByUserId(userId int) string {
	var login string

	rows, err := db.Query("SELECT login FROM users WHERE id = ? LIMIT 1;", userId)
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
