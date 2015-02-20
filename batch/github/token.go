package github

import (
	"github.com/k0kubun/githubranking/batch/db"
)

var (
	tokens     []string
	tokenIndex int
)

func init() {
	tokens = db.AllTokens()
}

func selectToken() string {
	tokenIndex++
	if len(tokens) <= tokenIndex {
		tokenIndex = 0
	}
	return tokens[tokenIndex]
}

func removeToken(token string) {
	newTokens := []string{}
	for _, tok := range tokens {
		if tok != token {
			newTokens = append(newTokens, tok)
		}
	}
	tokens = newTokens

	db.DeleteToken(token)
}
