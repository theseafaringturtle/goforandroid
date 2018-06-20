package main

import (
    "fmt"
)

func main() {
	fmt.Println("Enter your name")
    var s string
    fmt.Scanln(&s)
    fmt.Println("Hello",s)
    return
}