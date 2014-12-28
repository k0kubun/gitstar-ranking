package main

import (
	"fmt"
	"github.com/k0kubun/go-octokit/octokit"
	"log"
	"net/url"
)

func allRepositories(login string) []octokit.Repository {
	allRepos := []octokit.Repository{}
	page := 1

	for {
		uri := fmt.Sprintf("https://api.github.com/users/%s/repos?per_page=100&page=%d", login, page)
		endpoint, err := url.Parse(uri)
		if err != nil {
			log.Println(err)
			return allRepos
		}

		client := octokit.NewClient(octokit.TokenAuth{selectToken()})
		repos, result := client.Repositories(endpoint).All()
		if result.HasError() {
			log.Println(result)
			return allRepos
		}

		if len(repos) == 0 {
			return allRepos
		}

		allRepos = append(allRepos, repos...)
		page++
	}

	return allRepos
}
