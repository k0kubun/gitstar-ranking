package main

import (
	"fmt"
	"github.com/octokit/go-octokit/octokit"
	"log"
	"net/url"
)

func requestWorker(reqq chan int, impq chan *octokit.User, dstq chan int) {
	for {
		userId := <-reqq
		user, err := getUser(userId)

		destroyIf404(dstq, userId, err)
		impq <- user
	}
}

func getUser(userId int) (*octokit.User, error) {
	uri := fmt.Sprintf("https://api.github.com/user/%d", userId)
	api, err := url.Parse(uri)
	assert(err)

	client := octokit.NewClient(octokit.TokenAuth{selectToken()})
	user, result := client.Users(api).One()
	if result.HasError() {
		return nil, result.Err
	}

	return user, nil
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
