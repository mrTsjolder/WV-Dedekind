package antichains;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.SortedMap;

import amfsmall.SmallAntiChain;
import amfsmall.AntiChainInterval;

public class PCThread2 extends Thread {

	private SmallAntiChain function;
	private ArrayList<SmallAntiChain>functions;
	private SortedMap<AntiChainInterval, BigInteger> intervalSizes;
	private Collector collector;

	public static final BigInteger COEFF2 = BigInteger.valueOf(2);
	public static final BigInteger COEFF3 = BigInteger.valueOf(3);
	public static final BigInteger COEFF6 = BigInteger.valueOf(6);

	public PCThread2(SmallAntiChain r2, ArrayList<SmallAntiChain> fs, SortedMap<AntiChainInterval, BigInteger> is, Collector cr) throws InterruptedException {
		function = new SmallAntiChain(r2);
		functions = fs;
		intervalSizes = is;
		collector = cr;
		collector.enter();
	}
	
	@Override
	public void run() {
		long time = getCpuTime();
		BigInteger sumP = BigInteger.ZERO;
		BigInteger term;
		long evaluations = 0;
		for (SmallAntiChain r1:functions)
			if(r1.le(function))
				for(SmallAntiChain r2:functions)
					if(r2.le(function) && !r2.gt(r1)) 
						for(SmallAntiChain r3:functions) 
							if (r3.le(function) && !r3.gt(r2) && !r3.gt(r1)) {
								term = intervalSizes.get(new AntiChainInterval(SmallAntiChain.emptyAntiChain(), (SmallAntiChain) r1.meet(r2).meet(r3))).multiply(
										intervalSizes.get(new AntiChainInterval((SmallAntiChain) r1.join(r2), function))).multiply(
										intervalSizes.get(new AntiChainInterval((SmallAntiChain) r1.join(r3), function))).multiply(
										intervalSizes.get(new AntiChainInterval((SmallAntiChain) r2.join(r3), function)));
								if(r3.lt(r2) && r2.lt(r1)) {
									sumP = sumP.add(term.multiply(COEFF6));
								} else if((r2.lt(r1) && !r3.le(r2) && !r3.le(r1)) || (r3.lt(r1) && !r3.le(r2) && !r2.le(r1)) || 
										(r3.lt(r2) && !r2.le(r1) && !r3.le(r1))) {
									sumP = sumP.add(term.multiply(COEFF2));
								} else if((!r1.lt(r2) && r3.lt(r2)) || (!r2.lt(r3) && r2.lt(r1)) || (!r3.lt(r1) && r3.lt(r2))){
									sumP = sumP.add(term.multiply(COEFF3));
								} else {
									sumP = sumP.add(term);
								}
								evaluations++;
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
