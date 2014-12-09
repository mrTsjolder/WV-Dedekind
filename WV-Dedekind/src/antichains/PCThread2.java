package antichains;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.SortedMap;

import amfsmall.AntiChain;
import amfsmall.AntiChainInterval;

public class PCThread2 extends Thread {

	private AntiChain function;
	private ArrayList<AntiChain>functions;
	private SortedMap<AntiChainInterval, BigInteger> intervalSizes;
	private Collector collector;
	
	public static int count1 = 0;
	public static int count2 = 0;
	public static int count3 = 0;
	public static String rest = "";

	public PCThread2(AntiChain r2, ArrayList<AntiChain> fs, SortedMap<AntiChainInterval, BigInteger> is, Collector cr) throws InterruptedException {
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
		BigInteger coeff3 = BigInteger.valueOf(3);
		BigInteger coeff6 = BigInteger.valueOf(6);
		long evaluations = 0;
		for (AntiChain r1:functions)
			if(r1.le(function))
				for(AntiChain r2:functions)
					if(r2.le(function) && !r2.gt(r1)) 
						for(AntiChain r3:functions) 
							if (r3.le(function) && !r3.gt(r2) && !r3.gt(r1)) {
								if(r3.lt(r2) && r2.lt(r1)) {
									sumP = sumP.add(
										intervalSizes.get(new AntiChainInterval(AntiChain.emptyFunction(), r1.meet(r2).meet(r3))).multiply(
										intervalSizes.get(new AntiChainInterval(r1.join(r2), function))).multiply(
										intervalSizes.get(new AntiChainInterval(r1.join(r3), function))).multiply(
										intervalSizes.get(new AntiChainInterval(r2.join(r3), function))).multiply(coeff6)
									);
								} else if(!r2.lt(r1) && !r3.lt(r2)){
									sumP = sumP.add(
										intervalSizes.get(new AntiChainInterval(AntiChain.emptyFunction(), r1.meet(r2).meet(r3))).multiply(
										intervalSizes.get(new AntiChainInterval(r1.join(r2), function))).multiply(
										intervalSizes.get(new AntiChainInterval(r1.join(r3), function))).multiply(
										intervalSizes.get(new AntiChainInterval(r2.join(r3), function)))
									);
								} else {
									sumP = sumP.add(
										intervalSizes.get(new AntiChainInterval(AntiChain.emptyFunction(), r1.meet(r2).meet(r3))).multiply(
										intervalSizes.get(new AntiChainInterval(r1.join(r2), function))).multiply(
										intervalSizes.get(new AntiChainInterval(r1.join(r3), function))).multiply(
										intervalSizes.get(new AntiChainInterval(r2.join(r3), function))).multiply(coeff3)
									);
									rest += function.toString() + ":" + r1.toString() + "|" + r2.toString() + "|" + r3.toString() + ", ";
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
