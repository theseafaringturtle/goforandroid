package main

import (
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
)

func main() {
	resp, err := http.Get("http://example.com")
	if err != nil {
		log.Fatal(err)
	}
	b, _ := ioutil.ReadAll(resp.Body)
	fmt.Println(string(b))
}