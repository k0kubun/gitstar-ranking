package main

import (
	"bufio"
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

func selectToken() string {
	tokenIndex++
	if tokenIndex == tokenCount {
		tokenIndex = 0
	}
	return accessTokens[tokenIndex]
}
