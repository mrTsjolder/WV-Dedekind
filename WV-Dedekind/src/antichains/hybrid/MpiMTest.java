package antichains.hybrid;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import mpi.MPI;
import mpi.MPIException;
import amfsmall.AntiChainInterval;
import amfsmall.AntiChainSolver;
import amfsmall.SmallAntiChain;
import amfsmall.SmallBasicSet;
import amfsmall.Storage;
import amfsmall.SyntaxErrorException;

/**
 * 
 * @author Pieter-Jan
 *
 */
public class MpiMTest {
	
	public static final int DIETAG = 7;
	public static final int NUMTAG = 1;
	
	private ExecutorService pool;
	private int dedekind;
	private int nOfProc;
	private int myRank;
	
	/**
	 * Initialise every node with its buffers and set the parameters.
	 * 
	 * @param 	n
	 * 			The dedekind to calculate
	 * @param 	nOfProc
	 * 			The number of processors available.
	 */
	public MpiMTest(int n, int nOfProc, int rank) {
		if(n < 2) {
			System.out.println("For 0, the dedekind number is:\t2\nFor 1, the dedekind number is:\t3\n");
			throw new IllegalArgumentException("Enter a number greater or equal to 2\n");
		}
		
		this.dedekind = n - 2;
		this.nOfProc = nOfProc;
		this.myRank = rank;
		this.pool = Executors.newCachedThreadPool();
	}
	
	private void doIt() throws SyntaxErrorException, MPIException, InterruptedException, ExecutionException {
		BigInteger sum = BigInteger.ZERO;
		
		long startTime = System.currentTimeMillis();
		long cpuTime = getCpuTime();

		TestTime timePair = new TestTime(startTime, startTime, startTime);
		TestTime timeCPU = new TestTime(cpuTime, cpuTime, cpuTime);
		
		if(myRank == 0) {
			timePair = doTime("Starting at ", timePair);
			timeCPU = doCPUTime("CPU ",timeCPU);
		}
		
		SortedMap<BigInteger, Long>[] classes = AntiChainSolver.equivalenceClasses(dedekind);		//different levels in hass-dagramm
		final SortedMap<SmallAntiChain, Long> functions = new TreeMap<SmallAntiChain, Long>();			//number of antichains.hybrid in 1 equivalence-class

		if(myRank == 0) {
			timePair = doTime("Generated equivalence classes at ",timePair);
			timeCPU = doCPUTime("CPU ",timeCPU);
		}
		
		// collect
		for (int i=0;i<classes.length;i++) {
			long coeff = SmallBasicSet.combinations(dedekind, i);
			for (BigInteger b : classes[i].keySet()) {
				Storage.store(functions, SmallAntiChain.decode(b),classes[i].get(b)*coeff);
			}	
		}
		
		if(myRank == 0) {
			timePair = doTime("Collected equivalence classes at ",timePair);
			timeCPU = doCPUTime("CPU ",timeCPU);
		}
		
		final SmallAntiChain e = SmallAntiChain.emptyAntiChain();
		final SortedMap<SmallAntiChain, BigInteger> leftIntervalSize = new TreeMap<SmallAntiChain, BigInteger>();
		TreeMap<SmallAntiChain, Future<BigInteger>> temp = new TreeMap<>();
		for (final SmallAntiChain f : functions.keySet()) {
			temp.put(f, pool.submit(new Callable<BigInteger>() {

				@Override
				public BigInteger call() throws Exception {
					return BigInteger.valueOf(new AntiChainInterval(e,f).latticeSize());
				}
				
			}));
		}
		
		for(SmallAntiChain f : temp.keySet()) {
			leftIntervalSize.put(f, temp.get(f).get());
		}
		
		if(myRank == 0) {
			timePair = doTime("Generated interval sizes",timePair);
			timeCPU = doCPUTime("CPU ",timeCPU);
		}
		
		int counter = 0;
		long nrThreads = 0;
		long report = 64;

		ArrayList<Future<BigInteger>> results = new ArrayList<>();

		Iterator<SmallAntiChain> it2 = AntiChainInterval.fullSpace(dedekind).fastIterator();
		final SmallAntiChain u = SmallAntiChain.oneSetAntiChain(SmallBasicSet.universe(dedekind));
		
		timePair = doTime(String.format("Proces %d started threading at", myRank), timePair);
		timeCPU = doCPUTime("CPU ", timeCPU);
		
		while(it2.hasNext()) {
			final SmallAntiChain function = it2.next();
			if(counter++ == myRank) {
				results.add(pool.submit(new Callable<BigInteger>() {

					@Override
					public BigInteger call() throws Exception {
						BigInteger sumP = BigInteger.ZERO;
						for (SmallAntiChain r1:functions.keySet()) {
							if (r1.le(function)) {
								sumP = sumP.add(
										BigInteger.valueOf(functions.get(r1)).multiply(
											leftIntervalSize.get(r1)).multiply(
											AntiChainSolver.PatricksCoefficient(r1, function))
										);
							}
						}
						return sumP.multiply(BigInteger.valueOf(new AntiChainInterval(function, u).latticeSize()));
					}
					
				}));
				if(myRank == 0 && nrThreads++ > report) {
					System.out.println(String.format("reached iteration %d", nrThreads));
					report <<= 2;
				}
			}
			counter %= nOfProc;
		}

		for(Future<BigInteger> sumP : results)
			sum = sum.add(sumP.get());
		
		if(myRank != counter) {
			timePair = doTime(String.format("Proces %d calculated %s", myRank, sum), timePair);
			timeCPU = doCPUTime("CPU ", timeCPU);
		}
		
		if(myRank == counter) {
			counter++;
			counter %= nOfProc;
			
			timePair = doTime(String.format("Proces %d calculated %s", myRank, sum),timePair);
			timeCPU = doCPUTime("Finishing ",timeCPU);
			
			int[] intbuf;
			byte[] bigbuf;
			while(counter != myRank) {
				intbuf = new int[1];
				MPI.COMM_WORLD.recv(intbuf, 1, MPI.INT, counter, NUMTAG);
				bigbuf = new byte[intbuf[0]];
				MPI.COMM_WORLD.recv(bigbuf, bigbuf.length, MPI.BYTE, counter++, MPI.ANY_TAG);
				sum = sum.add(new BigInteger(bigbuf));
				counter %= nOfProc;
			}

			System.out.println("\n" + sum);
			timeCPU = doCPUTime("Finished ",timeCPU);
			timeCPU = doCPUTime("CPU ",timeCPU);
			System.out.println(String.format("%30s %15d ms","Total time elapsed ",System.currentTimeMillis() - startTime));
		} else {
			byte[] bigbuf = sum.toByteArray();
			MPI.COMM_WORLD.send(new int[]{bigbuf.length}, 1, MPI.INT, counter, NUMTAG);
			MPI.COMM_WORLD.send(bigbuf, bigbuf.length, MPI.BYTE, counter, 0);
		}
		
	}
	
	/************************************************************
	 * Timing-utils												*
	 ************************************************************/
	
	private static final class TestTime {
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
		TestTime result = new TestTime(timePair.currentTime, System.currentTimeMillis(), timePair.startTime);
		System.out.println(String.format("%s %d ms %d ms (%d ms)",msg,(result.previousTime - result.startTime),  
				 (result.currentTime - result.startTime),(-result.previousTime + result.currentTime)));
		return result;
	}

	private TestTime doCPUTime(String msg, TestTime timePair) {
		TestTime result = new TestTime(timePair.currentTime, getCpuTime(), timePair.startTime);
		System.out.println(String.format("%s : %d ns (+ %d ns)", msg,  
				 (result.currentTime - result.startTime), (result.currentTime - result.previousTime)));
		return result;
	}
	
	private long getCpuTime( ) {
	    ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
	    return bean.isCurrentThreadCpuTimeSupported( ) ?
	        bean.getCurrentThreadCpuTime( ) : 0L;
	}
	
	/************************************************************
	 * Main														*
	 ************************************************************/
	
	public static void main(String[] args) throws MPIException, SyntaxErrorException, InterruptedException, ExecutionException {
		MPI.Init(args);

		int myRank = MPI.COMM_WORLD.getRank();
		int nOfProc = MPI.COMM_WORLD.getSize();
		
		MpiMTest node = new MpiMTest(Integer.parseInt(args[0]), nOfProc, myRank);
		node.doIt();
		node.pool.shutdown();
		
		MPI.Finalize();
	}

}
