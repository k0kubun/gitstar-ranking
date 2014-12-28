package main

func schedulerLoop(queue chan []int) {
	for i := 0; i < 20; i++ {
		queue <- []int{i}
	}

	for {
	}
}
