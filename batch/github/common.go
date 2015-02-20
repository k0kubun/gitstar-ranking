package github

import (
	"fmt"
	"github.com/octokit/go-octokit/octokit"
	"log"
	"net/url"
	"os"
	"regexp"
	"time"
)

const (
	maxRetrial = 5
)

var (
	reBadCredentials = regexp.MustCompile(`401 - Bad credentials`)
	reNotFound       = regexp.MustCompile(`404 - Not Found`)
)

func init() {
	logF, err := os.OpenFile(
		"github.log",
		os.O_RDWR|os.O_CREATE|os.O_APPEND,
		0644,
	)
	if err != nil {
		log.Fatal(err)
	}

	log.SetOutput(logF)
}

func requestUser() {
}

func requestUsers(path string, a ...interface{}) ([]octokit.User, error) {
	var users []octokit.User
	var result *octokit.Result

	uri := fmt.Sprintf(fmt.Sprintf("https://api.github.com%s", path), a...)
	api, err := url.Parse(uri)
	if err != nil {
		log.Println(err)
		return []octokit.User{}, err
	}

	for i := 0; i < maxRetrial; i++ {
		token := selectToken()
		client := octokit.NewClient(octokit.TokenAuth{token})
		users, result = client.Users(api).All()
		if !result.HasError() {
			return users, nil
		}

		if isNotFound(result.Err) {
			return []octokit.User{}, result.Err
		}
		if isBadCredentials(result.Err) {
			log.Println(result.Err)
			removeToken(token)
		}
		time.Sleep(5 * time.Second)
	}
	log.Println(uri)
	log.Println(result.Err)
	return users, result.Err
}

func requestRepos() {
}

func isNotFound(err error) bool {
	return reNotFound.MatchString(err.Error())
}

func isBadCredentials(err error) bool {
	return reBadCredentials.MatchString(err.Error())
}
