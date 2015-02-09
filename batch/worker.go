package main

import (
	"bufio"
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
		login := loginByUserId(<-queue)
		updateStarCount(login)
	}
}

func updateStarCount(login string) {
	if login == "" {
		return
	}

	userStar := 0
	repos := allRepositories(login)
	bulkInsertRepositories(login, repos)

	for _, repo := range repos {
		userStar += repo.StargazersCount
	}
	_, err := db.Exec(
		"UPDATE users SET stargazers_count = ?, updated_at = ? WHERE login = ? LIMIT 1;",
		userStar,
		timeNow(),
		login,
	)
	// log.Printf("%s: %d\n", login, userStar)
	if err != nil {
		log.Println(err)
	}
}

// TODO: support splitting queries (for max length of one query), proper created_at
func bulkInsertRepositories(login string, repos []octokit.Repository) {
	if len(repos) == 0 {
		return
	}

	sql := "INSERT INTO repositories (id,name,full_name,owner_id,description,fork,created_at,updated_at,homepage,stargazers_count,language) VALUES "

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
	if err != nil {
		log.Println("bulkInsertRepositories: ", login, ": ", err.Error())
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
