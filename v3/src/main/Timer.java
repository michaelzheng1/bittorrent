package main;

public class Timer {

    long startTime;
    long endTime;
    long totalTime;
    public Timer() {
	this.startTime = 0;
	this.endTime = 0;
	this.totalTime = 0;
    }
    public void start() {
	this.startTime = System.nanoTime();
    }
    public void end() {
	this.endTime = System.nanoTime();
    }
    public long getTotalTime() {
	this.totalTime = this.endTime - this.startTime;
	System.out.println("The total execution time is " + totalTime);
	return this.totalTime;
    }   
}
