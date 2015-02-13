package main

import (
	"fmt"
	"github.com/octokit/go-octokit/octokit"
	"log"
	"net/url"
)

const (
	maxPerPage = 100
)

func requestWorker(reqq chan int, impq chan *ImportJob, dstq chan int) {
	for {
		userId := <-reqq
		repos, err := getRepos(userId)
		destroyIf404(dstq, userId, err)

		if err == nil {
			impq <- &ImportJob{
				UserID: userId,
				Repos:  repos,
			}
		}
	}
}

func getRepos(userId int) ([]octokit.Repository, error) {
	allRepos := []octokit.Repository{}

	for page := 0; ; page++ {
		repos, err := getPaginatedRepos(userId, page)
		if err != nil {
			return []octokit.Repository{}, err
		}

		if len(repos) == 0 {
			break
		}
		allRepos = append(allRepos, repos...)
	}

	return allRepos, nil
}

func getPaginatedRepos(userId int, page int) ([]octokit.Repository, error) {
	uri := fmt.Sprintf("https://api.github.com/user/%d/repos?page=%d&per_page=%d", userId, page, maxPerPage)
	api, err := url.Parse(uri)
	assert(err)

	client := octokit.NewClient(octokit.TokenAuth{selectToken()})
	repos, result := client.Repositories(api).All()
	if result.HasError() {
		return []octokit.Repository{}, result.Err
	}

	return repos, nil
}

func destroyIf404(dstq chan int, userId int, err error) {
	if err == nil {
		return
	}

	if match(err.Error(), "404 - Not Found") {
		dstq <- userId
	} else {
		log.Println(err)
	}
}
