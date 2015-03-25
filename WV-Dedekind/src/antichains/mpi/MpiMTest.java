package antichains.mpi;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

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
	}
	
	private void doIt() throws SyntaxErrorException, MPIException{
		BigInteger sum = BigInteger.ZERO;
		
		long startTime = System.currentTimeMillis();
		long cpuTime = getCpuTime();
		
		TestTime timePair = null;
		TestTime timeCPU = null;

		if(myRank == 0) {
			timePair = new TestTime(startTime, startTime, startTime);
			timeCPU = new TestTime(cpuTime, cpuTime, cpuTime);
			
			timePair = doTime("Starting at ", timePair);
			timeCPU = doCPUTime("CPU ",timeCPU);
		}
		
		SortedMap<BigInteger, Long>[] classes = AntiChainSolver.equivalenceClasses(dedekind);		//different levels in hass-dagramm
		SortedMap<SmallAntiChain, Long> functions = new TreeMap<SmallAntiChain, Long>();			//number of antichains.hybrid in 1 equivalence-class

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
		
		SmallAntiChain e = SmallAntiChain.emptyAntiChain();
		SortedMap<SmallAntiChain, BigInteger> leftIntervalSize = new TreeMap<SmallAntiChain, BigInteger>();
		for (SmallAntiChain f : functions.keySet()) {
			leftIntervalSize.put(f, BigInteger.valueOf(new AntiChainInterval(e,f).latticeSize()));
		}
		
		if(myRank == 0) {
			timePair = doTime("Generated interval sizes",timePair);
			timeCPU = doCPUTime("CPU ",timeCPU);
		}
		
		long evaluations = 0;
		int counter = nOfProc - 1;


		Iterator<SmallAntiChain> it2 = AntiChainInterval.fullSpace(dedekind).fastIterator();
		SmallAntiChain u = SmallAntiChain.oneSetAntiChain(SmallBasicSet.universe(dedekind));
		
		while(it2.hasNext()) {
			SmallAntiChain function = it2.next();
			if(counter++ == myRank) {
				BigInteger sumP = BigInteger.ZERO;
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
				sum = sum.add(sumP.multiply(BigInteger.valueOf(new AntiChainInterval(function, u).latticeSize())));
			}
			counter %= nOfProc;
		}
		
		System.out.println("Proces " + myRank + " calculated " + sum + " in " + evaluations + " evaluations");
		
		if(myRank == 0) {
			
			timePair = doTime(String.format("%d evs\n%s val",evaluations, sum),timePair);
			timeCPU = doCPUTime("Finishing ",timeCPU);
			
			int[] intbuf;
			byte[] bigbuf;
			for(int i = 1; i < nOfProc; i++) {
				intbuf = new int[1];
				MPI.COMM_WORLD.recv(intbuf, 1, MPI.INT, i, NUMTAG);
				bigbuf = new byte[intbuf[0]];
				MPI.COMM_WORLD.recv(bigbuf, bigbuf.length, MPI.BYTE, i, MPI.ANY_TAG);
				sum = sum.add(new BigInteger(bigbuf));
			}

			System.out.println("\n" + sum);
			timeCPU = doCPUTime("Finished ",timeCPU);
			timeCPU = doCPUTime("CPU ",timeCPU);
			System.out.println(String.format("%30s %15d ms","Total time elapsed ",System.currentTimeMillis() - startTime));
		} else {
			byte[] bigbuf = sum.toByteArray();
			MPI.COMM_WORLD.send(new int[]{bigbuf.length}, 1, MPI.INT, 0, NUMTAG);
			MPI.COMM_WORLD.send(bigbuf, bigbuf.length, MPI.BYTE, 0, 0);
		}
		
	}
	
	
	
	/************************************************************
	 * Timing-utils												*
	 ************************************************************/
	
	private final class TestTime {
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
		System.out.println(String.format("%s %d ms %d ms (%d ms)", msg, (timePair.previousTime - timePair.startTime),  
				 (timePair.currentTime - timePair.startTime), (timePair.currentTime - timePair.previousTime)));
		return new TestTime(timePair.currentTime, System.currentTimeMillis(), timePair.startTime);
	}

	private TestTime doCPUTime(String msg, TestTime timePair) {
		System.out.println(String.format("%s : %d ns (+ %d ns)", msg,  
				 (timePair.currentTime - timePair.startTime), (timePair.currentTime - timePair.previousTime)));
		return new TestTime(timePair.currentTime, getCpuTime(), timePair.startTime);
	}
	
	private long getCpuTime( ) {
	    ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
	    return bean.isCurrentThreadCpuTimeSupported( ) ?
	        bean.getCurrentThreadCpuTime( ) : 0L;
	}
	
	/************************************************************
	 * Main														*
	 ************************************************************/
	
	public static void main(String[] args) throws MPIException, SyntaxErrorException, InterruptedException {
		MPI.Init(args);

		int myRank = MPI.COMM_WORLD.getRank();
		int nOfProc = MPI.COMM_WORLD.getSize();
		
		MpiMTest node = new MpiMTest(Integer.parseInt(args[0]), nOfProc, myRank);
		node.doIt();
		
		MPI.Finalize();
	}

}
