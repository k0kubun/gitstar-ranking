package main

import (
	"log"
	"os"
)

var (
	logF *os.File
)

const (
	divider = "----------------------------------------------------------"
	perPage = 1000
)

func init() {
	logF, err := os.OpenFile(
		getEnv("GR_BATCH_LOG", "run.log"),
		os.O_RDWR|os.O_CREATE|os.O_APPEND,
		0644,
	)
	if err != nil {
		panic(err)
	}

	log.SetOutput(logF)
}

func main() {
	defer logF.Close()
	defer db.Close()

	log.Println(divider)
	if os.Args[1] == "user" {
		calcUserRanks()
	} else if os.Args[1] == "repo" {
		calcRepoRanks()
	} else if os.Args[1] == "org" {
		calcOrgRanks()
	}

	log.Println("Finish")
}
