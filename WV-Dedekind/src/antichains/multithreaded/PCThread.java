package antichains.multithreaded;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigInteger;
import java.util.SortedMap;

import amfsmall.AntiChainInterval;
import amfsmall.SmallAntiChain;
import amfsmall.AntiChainSolver;

public class PCThread extends Thread {

	private SmallAntiChain function;
	private SmallAntiChain upper;
	private SortedMap<SmallAntiChain, Long> functions;
	private SortedMap<SmallAntiChain, BigInteger> leftIntervalSize;
	private Collector collector;

	public PCThread(SmallAntiChain r2, SortedMap<SmallAntiChain, Long> fs, SortedMap<SmallAntiChain, BigInteger> ls, SmallAntiChain u, Collector cr) throws InterruptedException {
		function = new SmallAntiChain(r2);
		functions = fs;
		leftIntervalSize = ls;
		this.upper = u;
		collector = cr;
		collector.enter();
	}
	
	@Override
	public void run() {
		long time = getCpuTime();
		BigInteger sumP = BigInteger.ZERO;
		long evaluations = 0;
		for (SmallAntiChain r1:functions.keySet()) {
			if (r1.le(function)) {
				sumP = sumP.add(
						BigInteger.valueOf(functions.get(r1)).multiply(
							leftIntervalSize.get(r1)).multiply(
							AntiChainSolver.PatricksCoefficient(r1, function))
						);
				evaluations++;

			}
		}
		collector.register(sumP.multiply(BigInteger.valueOf(new AntiChainInterval(function,upper).latticeSize())), evaluations, getCpuTime() - time);
		collector.leave();
	}
	
	private long getCpuTime( ) {
	    ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
	    return bean.isCurrentThreadCpuTimeSupported( ) ?
	        bean.getCurrentThreadCpuTime( ) : 0L;
	}


}
