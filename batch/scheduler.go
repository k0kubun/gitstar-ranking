package main

func schedulerLoop(queue chan []int) {
	queue <- []int{1, 2, 3}
	queue <- []int{3, 4, 8}
	queue <- []int{3, 8, 9}

	for {
	}
}
