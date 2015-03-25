package antichains.multithreaded;
// deprecated, use acmat/Collector<BigInteger,BigInteger>
import java.math.BigInteger;

/**
 * manages a number of threads
 * @author u0003471
 *
 */
public class Collector {

	
	private int numberOfProcesses;
	private int numberOfAllowedProcesses;
	private BigInteger result;
	private long iterations;
	private long time;

	public Collector(int n) {
		numberOfAllowedProcesses = n;
		numberOfProcesses = 0;
		result = BigInteger.ZERO;
		iterations = 0;
		time = 0;
	}
	
	synchronized public void enter() throws InterruptedException {
		if (numberOfProcesses == numberOfAllowedProcesses) wait();
		numberOfProcesses++;
	}
	
	synchronized public void leave() {
		numberOfProcesses--;
		notify();
	}
	
	synchronized public void register(BigInteger v, long its, long sec) {
		result = result.add(v);
		iterations += its;
		time += sec;
	}
	
	synchronized public boolean isReady() throws InterruptedException {
		while (numberOfProcesses > 0) wait();
		return true;
	}
	
	public synchronized BigInteger getResult() {
		return result;
	}
	
	synchronized public long iterations() {
		return iterations;		
	}

	synchronized public long time() {
		return time;		
	}

	synchronized public long numberOfProcesses() {
		return numberOfProcesses;		
	}
	
	synchronized public long numberOfAllowedProcesses() {
		return numberOfAllowedProcesses;		
	}
}
