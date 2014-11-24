package antichains;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigInteger;
import java.util.SortedMap;

import amfsmall.AntiChain;
import amfsmall.AntiChainSolver;

public class PCThread extends Thread {

	private AntiChain function;
	private SortedMap<AntiChain, Long> functions;
	private SortedMap<AntiChain, BigInteger> leftIntervalSize;
	private SortedMap<AntiChain, BigInteger> rightIntervalSize;
	private Collector collector;

	public PCThread(AntiChain r2, SortedMap<AntiChain, Long> fs, SortedMap<AntiChain, BigInteger> ls, SortedMap<AntiChain, BigInteger> rs, Collector cr) throws InterruptedException {
		function = new AntiChain(r2);
		functions = fs;
		leftIntervalSize = ls;
		rightIntervalSize = rs;
		collector = cr;
		collector.enter();
	}
	
	@Override
	public void run() {
		long time = getCpuTime();
		BigInteger sumP = BigInteger.ZERO;
		long evaluations = 0;
		for (AntiChain r1:functions.keySet()) {
			if (r1.le(function)) {
				sumP = sumP.add(
						BigInteger.valueOf(functions.get(r1)).multiply(
							leftIntervalSize.get(r1)).multiply(
							AntiChainSolver.PatricksCoefficient(r1, function))
						);
				evaluations++;

			}
		}
		collector.register(sumP.multiply(rightIntervalSize.get(function.standard())), evaluations, getCpuTime() - time);
		collector.leave();
	}
	
	private long getCpuTime( ) {
	    ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
	    return bean.isCurrentThreadCpuTimeSupported( ) ?
	        bean.getCurrentThreadCpuTime( ) : 0L;
	}


}
