package antichains.multithreaded;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import amfsmall.SmallAntiChain;
import amfsmall.AntiChainInterval;
import amfsmall.SmallBasicSet;
import amfsmall.SyntaxErrorException;

public class M2 {
	
	public final int dedekind;

	public final int cores;

	static private SmallBasicSet[] N;
	static private SmallAntiChain[] fN;
	static private AntiChainInterval[] iS;
	
	public M2(int n, int coresUsed) throws SyntaxErrorException {
		dedekind = n;
		
		N = new SmallBasicSet[n];
		fN = new SmallAntiChain[n];
		iS = new AntiChainInterval[n];

		String basic = "";
		for (int i = 1;i< n;i++) {
			basic += i + ", ";
			N[i] = SmallBasicSet.parser().parse("[" + basic.subSequence(0, basic.length()-1) + "]");
			fN[i] = SmallAntiChain.oneSetAntiChain(N[i]);
			iS[i] = AntiChainInterval.fullSpace(i);
		}
		
		cores = coresUsed;
	}
	
	public void doIt() throws SyntaxErrorException, InterruptedException {
		long startTime = System.currentTimeMillis();
		long cpuTime = getCpuTime();
		
		TestTime timePair = new TestTime(startTime, startTime, startTime);
		TestTime timeCPU = new TestTime(cpuTime, cpuTime, cpuTime);

		timePair = doTime("Starting at ", timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		
		// generate
		int n = dedekind - 3;
		long reportRate = 8;
		
		ArrayList<SmallAntiChain> functions = new ArrayList<>();
		
		SmallAntiChain e = SmallAntiChain.emptyAntiChain();
		SmallAntiChain u = SmallAntiChain.oneSetAntiChain(SmallBasicSet.universe(n));

		Iterator<SmallAntiChain> it = new AntiChainInterval(e,u).fastIterator();
		while(it.hasNext()) {
			functions.add(0, it.next());
		}
		
		timePair = doTime("Generated all functions",timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		
		SortedMap<AntiChainInterval, BigInteger> intervalSizes = new TreeMap<AntiChainInterval, BigInteger>();
		for (SmallAntiChain f : functions) {
			for (SmallAntiChain g : functions) {
				if(f.le(g)) {
					AntiChainInterval interval = new AntiChainInterval(f,g);
					intervalSizes.put(interval, BigInteger.valueOf(interval.latticeSize()));
				}
			}
		}
		
		timePair = doTime("Generated interval sizes",timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		
		// test
		long evaluations = 0;
		long newEvaluations = 0;
				
		Collector collector = new Collector(cores);

		for(SmallAntiChain r2 : functions) {
			new PCThread2(r2, functions, intervalSizes, collector).start();
			newEvaluations += collector.iterations();
			if (newEvaluations > reportRate) {
				evaluations += newEvaluations;
				newEvaluations = 0;
				reportRate <<= 2;
				timePair = doTime(String.format("%d evs\n%s val, %d processes ",evaluations, collector.getResult(), collector.numberOfProcesses()),timePair);
				timeCPU = doCPUTime("CPU ",timeCPU);
				System.out.println("Total thread time " + collector.time());
			}
		}
		

		evaluations += newEvaluations;
		
		timePair = doTime(String.format("%d evs\n%s val, %d processes ",evaluations, collector.getResult(), collector.numberOfProcesses()),timePair);
		timeCPU = doCPUTime("Finishing ",timeCPU);
		if (collector.isReady()) System.out.println(collector.getResult());
		timePair = doTime("Finished",timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		System.out.println(String.format("%30s %15d ns","Total thread time ",collector.time()));

		System.out.println(String.format("%30s %15d ns","Total cpu time used ",collector.time() + getCpuTime()));
		System.out.println(String.format("%30s %15d ms","Total time elapsed ",System.currentTimeMillis() - startTime));
	}
	
	/************************************************************
	 * Timing-utils												*
	 ************************************************************/
	
	private class TestTime {
		public final long previousTime;
		public final long currentTime;
		public final long startTime;
		
		public TestTime(long previous, long current, long start) {
			previousTime = previous;
			currentTime = current;
			startTime = start;
		}
	}

	private TestTime doTime(String msg, TestTime timePair) {
		System.out.println(String.format("%s %d ms %d ms (%d ms)",msg,(timePair.previousTime - timePair.startTime),  
				 (timePair.currentTime - timePair.startTime),(-timePair.previousTime + timePair.currentTime)));
		return new TestTime(timePair.currentTime, System.currentTimeMillis(), timePair.startTime);
	}

	private TestTime doCPUTime(String msg, TestTime timePair) {
		System.out.println(String.format("%s : %d ns (+ %d ns)",msg,  
				 (timePair.currentTime - timePair.startTime),(-timePair.previousTime + timePair.currentTime)));
		return new TestTime(timePair.currentTime, getCpuTime(), timePair.startTime);
	}

	public long getCpuTime( ) {
	    ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
	    return bean.isCurrentThreadCpuTimeSupported( ) ?
	        bean.getCurrentThreadCpuTime( ) : 0L;
	}
	
	/************************************************************
	 * Main														*
	 ************************************************************/

	public static void main(String[] args) throws NumberFormatException, SyntaxErrorException, InterruptedException {
		new M2(Integer.parseInt(args[0]), Integer.parseInt(args[1])).doIt();
	}
	
}
