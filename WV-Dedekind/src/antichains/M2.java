package antichains;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import amfsmall.AntiChain;
import amfsmall.AntiChainInterval;
import amfsmall.AntiChainSolver;
import amfsmall.SmallBasicSet;
import amfsmall.Storage;
import amfsmall.SyntaxErrorException;

public class M2 {
	
	public final int dedekind;

	public final int cores;

	static private SmallBasicSet[] N;
	static private AntiChain[] fN;
	static private AntiChainInterval[] iS;
	
	public M2(int n, int coresUsed) throws SyntaxErrorException {
		dedekind = n;
		
		N = new SmallBasicSet[n];
		fN = new AntiChain[n];
		iS = new AntiChainInterval[n];

		String basic = "";
		for (int i = 1;i< n;i++) {
			basic += i + ", ";
			N[i] = SmallBasicSet.parser().parse("[" + basic.subSequence(0, basic.length()-1) + "]");
			fN[i] = AntiChain.oneSetFunction(N[i]);
			iS[i] = AntiChainInterval.fullSpace(i);
		}
		
		cores = coresUsed;

		
	}
	
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
	
	public void doIt() throws SyntaxErrorException, InterruptedException {
		long startTime = System.currentTimeMillis();
		long cpuTime = getCpuTime();
		
		TestTime timePair = new TestTime(startTime, startTime, startTime);
		TestTime timeCPU = new TestTime(cpuTime, cpuTime, cpuTime);

		timePair = doTime("Starting at ", timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		
		// generate
		int n = dedekind - 3;
		int reportRate = 10;
		
		ArrayList<AntiChain> functions = new ArrayList<>();
		
		AntiChain e = AntiChain.emptyFunction();
		AntiChain u = AntiChain.oneSetFunction(SmallBasicSet.universe(n));

		Iterator<AntiChain> it = new AntiChainInterval(e,u).fastIterator();
		while(it.hasNext()) {
			functions.add(it.next());
		}
		
		SortedMap<AntiChainInterval, BigInteger> intervalSizes = new TreeMap<AntiChainInterval, BigInteger>();
		for (AntiChain f : functions) {
			for (AntiChain g : functions) {
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

		for(AntiChain r2 : functions) {
			new PCThread2(r2, functions, intervalSizes, collector).start();
			newEvaluations += collector.iterations();
			if (newEvaluations > reportRate) {
				evaluations += newEvaluations;
				newEvaluations = 0;
				reportRate *= 4;
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
		
		System.out.println("count1: " + PCThread2.count1);
		System.out.println("count2: " + PCThread2.count2);
		System.out.println("count3: " + PCThread2.count3);
		System.out.println("rest: " + PCThread2.rest);
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


	public static void main(String[] args) throws NumberFormatException, SyntaxErrorException, InterruptedException {
		new M2(Integer.parseInt(args[0]), Integer.parseInt(args[1])).doIt();
	}
	
}
