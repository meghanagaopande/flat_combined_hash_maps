package Threads;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import HashMaps.HashMapInterface;

/**
 * 
 * @author Mohamed M. Saad
 */
public class TestThread extends Thread implements ThreadId {
	private static int ID_GEN = 0;
	private static final int MAX_COUNT = 1000;

	public int operations;

	private static double REMOVE;
	private static double ADD;
	private static double CONTAINS;

	public AtomicBoolean running;

	public HashMapInterface<Integer> S;
	private int id;

	public TestThread(HashMapInterface<Integer> S, AtomicBoolean keep_running, double contains) {

		CONTAINS = contains;

		REMOVE = contains + (1- contains)/2;

		ADD = 1;

		this.S = S;
		running = keep_running;
	}
	
	@Override
	public void run(){
		int number;
		double decision;
		while(running.get())
		{
			number = ThreadLocalRandom.current().nextInt(1000);
			decision = ThreadLocalRandom.current().nextDouble();

			if(decision < CONTAINS)
			{
				S.contains(number);
			}
			else if(decision < REMOVE)
			{
				S.remove(number);
			}
			else
			{
				S.add(number);
			}

			operations++;

		}
	}
	
	public int getThreadId()
	{
		return id;
	}
}
