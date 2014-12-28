package main

import (
	"bufio"
	"fmt"
	"github.com/octokit/go-octokit/octokit"
	"log"
	"net/url"
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
		id := <-queue
		updateStarCount(id)
	}
}

func updateStarCount(userId int) {
	login := loginByUserId(userId)
	if login == "" {
		return
	}

	client := octokit.NewClient(
		octokit.TokenAuth{selectToken()},
	)
	uri := fmt.Sprintf("https://api.github.com/users/%s/repos", login)
	endpoint, _ := url.Parse(uri)

	repos, result := client.Repositories(endpoint).All()
	if result.HasError() {
		log.Println(result)
		return
	}

	for _, repo := range repos {
		break
		fmt.Printf("%#v", repo)
	}
	fmt.Printf("%#v", selectToken())
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
