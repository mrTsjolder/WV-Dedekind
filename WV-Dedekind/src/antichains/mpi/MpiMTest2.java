package antichains.mpi;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import mpi.MPI;
import mpi.MPIException;
import mpi.Status;
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
public class MpiMTest2 {
	
	public static final int DIETAG = 7;
	public static final int NUMTAG = 1;
	
	private int dedekind;
	private int nOfProc;

	//buffers
	private int[] num = new int[1];
	private long[] acbuf = new long[2];			//estimated size
	private byte[] bigintbuf = new byte[2];		//estimated size
	private long[] timebuf = new long[2];
	
	/**
	 * Initialise every node with its buffers and set the parameters.
	 * 
	 * @param 	n
	 * 			The dedekind to calculate
	 * @param 	nOfProc
	 * 			The number of processors available.
	 */
	public MpiMTest2(int n, int nOfProc) {
		if(n < 2) {
			System.out.println("For 0, the dedekind number is:\t2\nFor 1, the dedekind number is:\t3\n");
			throw new IllegalArgumentException("Enter a number greater or equal to 2\n");
		}
		
		this.dedekind = n - 2;
		this.nOfProc = nOfProc;
	}
	
	private void delegate() throws SyntaxErrorException, MPIException{
		BigInteger sum = BigInteger.ZERO;
		
		long startTime = System.currentTimeMillis();
		long cpuTime = getCpuTime();
		
		TestTime timePair = new TestTime(startTime, startTime, startTime);
		TestTime timeCPU = new TestTime(cpuTime, cpuTime, cpuTime);

		timePair = doTime("Starting at ", timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		
		SortedMap<BigInteger, Long>[] classes = AntiChainSolver.equivalenceClasses(dedekind);		//different levels in hass-dagramm
		SortedMap<SmallAntiChain, Long> functions = new TreeMap<SmallAntiChain, Long>();			//number of antichains.hybrid in 1 equivalence-class

		timePair = doTime("Generated equivalence classes at ",timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		
		// collect
		for (int i=0;i<classes.length;i++) {
			long coeff = SmallBasicSet.combinations(dedekind, i);
			for (BigInteger b : classes[i].keySet()) {
				Storage.store(functions, SmallAntiChain.decode(b),classes[i].get(b)*coeff);
			}	
		}
		
		timePair = doTime("Collected equivalence classes at ",timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		
		SmallAntiChain e = SmallAntiChain.emptyAntiChain();
		SortedMap<SmallAntiChain, BigInteger> leftIntervalSize = new TreeMap<SmallAntiChain, BigInteger>();
		for (SmallAntiChain f : functions.keySet()) {
			leftIntervalSize.put(f, BigInteger.valueOf(new AntiChainInterval(e,f).latticeSize()));
		}
		
		timePair = doTime("Generated interval sizes",timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);

		// test
		long reportRate = 8;
		long evaluations = 0;
		long newEvaluations = 0;
		long time = 0;
		Iterator<SmallAntiChain> it2 = AntiChainInterval.fullSpace(dedekind).fastIterator();
		
		int x = 1;
		for(int i = 1; i < nOfProc; i++) {
			if(it2.hasNext()) {
				acbuf = it2.next().toLongArray();
				MPI.COMM_WORLD.send(new int[]{acbuf.length}, 1, MPI.INT, i, NUMTAG);
				MPI.COMM_WORLD.send(acbuf, acbuf.length, MPI.LONG, i, 0);
				x = i;
			} else {
				MPI.COMM_WORLD.send(null, 0, MPI.INT, i, NUMTAG);
				MPI.COMM_WORLD.send(null, 0, MPI.INT, i, DIETAG);
			}
		}
		
		timePair = doTime("First " + (nOfProc - 1) + " messages sent", timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		
		int src;
		while(it2.hasNext()) {
			src = retrieveResults();
			acbuf = it2.next().toLongArray();
			MPI.COMM_WORLD.send(new int[]{acbuf.length}, 1, MPI.INT, src, NUMTAG);
			MPI.COMM_WORLD.send(acbuf, acbuf.length, MPI.LONG, src, 0);
			sum = sum.add(new BigInteger(bigintbuf));
			newEvaluations += timebuf[0];
			time += timebuf[1];
			if (newEvaluations > reportRate) {
				evaluations += newEvaluations;
				newEvaluations = 0;
				reportRate <<= 2;
				timePair = doTime(String.format("%d evs\n%s val",evaluations, sum),timePair);
				timeCPU = doCPUTime("CPU ",timeCPU);
				System.out.println("Total thread time " + time);
			}
		}
		
		evaluations += newEvaluations;
		
		timePair = doTime(String.format("%d evs\n%s val",evaluations, sum),timePair);
		timeCPU = doCPUTime("Finishing ",timeCPU);
		
		while(x-- > 0) {
			src = retrieveResults();
			MPI.COMM_WORLD.send(null, 0, MPI.INT, src, NUMTAG);
			MPI.COMM_WORLD.send(null, 0, MPI.LONG, src, DIETAG);
			sum = sum.add(new BigInteger(bigintbuf));
			evaluations += timebuf[0];
			time += timebuf[1];
		}
		
		System.out.println("\n" + sum);
		timeCPU = doCPUTime("Finished ",timeCPU);
		timeCPU = doCPUTime("CPU ",timeCPU);
		System.out.println(String.format("%30s %15d ns","Total thread time ",time));

		System.out.println(String.format("%30s %15d ns","Total cpu time used ",time + getCpuTime()));
		System.out.println(String.format("%30s %15d ms","Total time elapsed ",System.currentTimeMillis() - startTime));
	}

	private void work() throws MPIException, SyntaxErrorException {
		SmallAntiChain u = SmallAntiChain.oneSetAntiChain(SmallBasicSet.universe(dedekind));
		SmallAntiChain function;
		
		SortedMap<BigInteger, Long>[] classes = AntiChainSolver.equivalenceClasses(dedekind);		//different levels in hass-dagramm
		SortedMap<SmallAntiChain, Long> functions = new TreeMap<SmallAntiChain, Long>();			//number of antichains.hybrid in 1 equivalence-class

		// collect
		for (int i=0;i<classes.length;i++) {
			long coeff = SmallBasicSet.combinations(dedekind, i);
			for (BigInteger b : classes[i].keySet()) {
				Storage.store(functions, SmallAntiChain.decode(b),classes[i].get(b)*coeff);
			}	
		}
		
		SmallAntiChain e = SmallAntiChain.emptyAntiChain();
		SortedMap<SmallAntiChain, BigInteger> leftIntervalSize = new TreeMap<SmallAntiChain, BigInteger>();
		for (SmallAntiChain f : functions.keySet()) {
			leftIntervalSize.put(f, BigInteger.valueOf(new AntiChainInterval(e,f).latticeSize()));
		}
		
		long time, evaluations;
		while(true) {
			MPI.COMM_WORLD.recv(num, 1, MPI.INT, 0, NUMTAG);
			if(num[0] != acbuf.length)
				acbuf = new long[num[0]];
			Status stat = MPI.COMM_WORLD.recv(acbuf, acbuf.length, MPI.LONG, 0, MPI.ANY_TAG);
			
			if(stat.getTag() == DIETAG)
				break;
			
			function = new SmallAntiChain(acbuf);
			
			time = getCpuTime();
			evaluations = 0;
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
			bigintbuf = sumP.multiply(BigInteger.valueOf(new AntiChainInterval(function, u).latticeSize())).toByteArray();
			
			MPI.COMM_WORLD.send(new int[]{bigintbuf.length}, 1, MPI.INT, 0, NUMTAG);
			MPI.COMM_WORLD.send(bigintbuf, bigintbuf.length, MPI.BYTE, 0, 0);
			timebuf[0] = evaluations;
			timebuf[1] = getCpuTime() - time;
			MPI.COMM_WORLD.send(timebuf, 2, MPI.LONG, 0, 0);
		}
	}

	/**
	 * Retrieve results sent by the workers.
	 * 
	 * @return	an integer representing the rank of the node that sent the results.
	 * @throws 	MPIException
	 * 			if MPI failed.
	 */
	private int retrieveResults() throws MPIException {
		Status stat = MPI.COMM_WORLD.recv(num, 1, MPI.INT, MPI.ANY_SOURCE, NUMTAG);
		if(num[0] != bigintbuf.length)
			bigintbuf = new byte[num[0]];
		MPI.COMM_WORLD.recv(bigintbuf, bigintbuf.length, MPI.BYTE, stat.getSource(), 0);
		MPI.COMM_WORLD.recv(timebuf, 2, MPI.LONG, stat.getSource(), 0);
		return stat.getSource();
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
		
		MpiMTest2 node = new MpiMTest2(Integer.parseInt(args[0]), nOfProc);
		
		if(myRank == 0)
			node.delegate();
		else
			node.work();
		
		MPI.Finalize();
	}

}
