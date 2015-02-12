package main

import (
	"bitbucket.org/tebeka/strftime"
	"log"
	"os"
	"regexp"
	"strconv"
	"time"
)

var (
	logF *os.File
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

func assert(err error) {
	if err != nil {
		log.Fatal(err)
	}
}

func logError(err error) {
	if err != nil {
		log.Println(err)
	}
}

func getEnv(key string, def string) string {
	v := os.Getenv(key)
	if len(v) == 0 {
		return def
	}

	return v
}

func match(str string, exp string) bool {
	re := regexp.MustCompile(exp)
	return re.MatchString(str)
}

func puts(a ...interface{}) {
	log.Println(a...)
}

func timeNow() string {
	str, err := strftime.Format("%Y/%m/%d %H:%M:%S", time.Now().UTC())
	if err != nil {
		panic(err)
	}
	return str
}

func commaJoin(nums []int) string {
	if len(nums) == 0 {
		return ""
	}

	// Use buffer for performance
	buf := make([]byte, 0, 10)

	for _, num := range nums {
		numStr := strconv.Itoa(num)
		buf = append(buf, numStr...)
		buf = append(buf, ","...)
	}

	return string(buf[0 : len(buf)-1])
}
