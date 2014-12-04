package antichains;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigInteger;
import java.util.SortedMap;

import amfsmall.AntiChain;
import amfsmall.AntiChainInterval;

public class PCThread2 extends Thread {

	private AntiChain function;
	private SortedMap<AntiChain, Long> functions;
	private SortedMap<AntiChainInterval, BigInteger> intervalSizes;
	private Collector collector;

	public PCThread2(AntiChain r2, SortedMap<AntiChain, Long> fs, SortedMap<AntiChainInterval, BigInteger> is, Collector cr) throws InterruptedException {
		function = new AntiChain(r2);
		functions = fs;
		intervalSizes = is;
		collector = cr;
		collector.enter();
	}
	
	@Override
	public void run() {
		long time = getCpuTime();
		BigInteger sumP = BigInteger.ZERO;
		long evaluations = 0;
		for (AntiChain r1:functions.keySet()) {
			for(AntiChain r2:functions.keySet()) {
				for(AntiChain r3:functions.keySet()) {
					if (r1.le(function) && r2.le(function) && r3.le(function)) {
						sumP = sumP.add(
									BigInteger.valueOf(new AntiChainInterval(AntiChain.emptyFunction(), r1.meet(r2).meet(r3)).latticeSize()).multiply(
									BigInteger.valueOf(new AntiChainInterval(r1.join(r2), function).latticeSize())).multiply(
									BigInteger.valueOf(new AntiChainInterval(r1.join(r3), function).latticeSize())).multiply(
									BigInteger.valueOf(new AntiChainInterval(r2.join(r3), function).latticeSize()))
								);
						evaluations++;
		
					}
				}
			}
		}
		collector.register(sumP, evaluations, getCpuTime() - time);
		collector.leave();
	}
	
	private long getCpuTime( ) {
	    ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
	    return bean.isCurrentThreadCpuTimeSupported( ) ?
	        bean.getCurrentThreadCpuTime( ) : 0L;
	}

}
