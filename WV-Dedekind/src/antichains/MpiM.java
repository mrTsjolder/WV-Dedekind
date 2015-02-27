package antichains;

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
public class MpiM {
	
	public static final int DIETAG = 7;
	
	private int dedekind;
	private int nOfProc;
	
	public MpiM(int n, int nOfProc) {
		if(n < 2) {
			System.out.println("For 0, the dedekind number is:\t2\nFor 1, the dedekind number is:\t3\n");
			throw new IllegalArgumentException("Enter a number greater or equal to 2\n");
		}
		this.dedekind = n - 2;
		this.nOfProc = nOfProc;
	}
	
	private void delegate() throws SyntaxErrorException, MPIException {
		BigInteger sum = BigInteger.ZERO;
		SmallAntiChain[] sendbuf = new SmallAntiChain[1];
		BigInteger[] recvbuf1 = new BigInteger[1];
		long[] recvbuf2 = new long[2];
		
		long startTime = System.currentTimeMillis();
		long cpuTime = getCpuTime();
		
		TestTime timePair = new TestTime(startTime, startTime, startTime);
		TestTime timeCPU = new TestTime(cpuTime, cpuTime, cpuTime);

		timePair = doTime("Starting at ", timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		
		SortedMap<BigInteger, Long>[] classes = AntiChainSolver.equivalenceClasses(dedekind);		//different levels in hass-dagramm
		SortedMap<SmallAntiChain, Long> functions = new TreeMap<SmallAntiChain, Long>();			//number of antichains in 1 equivalence-class

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
		SmallAntiChain u = SmallAntiChain.oneSetAntiChain(SmallBasicSet.universe(dedekind));
		SortedMap<SmallAntiChain, BigInteger> leftIntervalSize = new TreeMap<SmallAntiChain, BigInteger>();
		for (SmallAntiChain f : functions.keySet()) {
			leftIntervalSize.put(f, BigInteger.valueOf(new AntiChainInterval(e,f).latticeSize()));
		}
		
		timePair = doTime("Generated interval sizes",timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		
		MPI.COMM_WORLD.Bcast(new Object[]{functions, leftIntervalSize, u}, 0, 3, MPI.OBJECT, 0);

		// test
		int reportRate = 10;
		long evaluations = 0;
		long newEvaluations = 0;
		long time = 0;
		Iterator<SmallAntiChain> it2 = new AntiChainInterval(e,u).fastIterator();
		
		int i;
		for(i = 1; i < nOfProc; i++) {
			if(it2.hasNext()) {
				sendbuf[0] = it2.next();
				MPI.COMM_WORLD.Send(sendbuf, 0, 1, MPI.OBJECT, i, 0);
			}
		}
		
		timePair = doTime("First " + nOfProc + "messages sent",timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		
		while(it2.hasNext()) {
			Status stat = MPI.COMM_WORLD.Recv(recvbuf1, 0, 1, MPI.OBJECT, MPI.ANY_SOURCE, 0);
			MPI.COMM_WORLD.Recv(recvbuf2, 0, 2, MPI.LONG, stat.source, 0);
			sum = sum.add(recvbuf1[0]);
			newEvaluations += recvbuf2[0];
			time += recvbuf2[1];
			if (newEvaluations > reportRate) {
				evaluations += newEvaluations;
				newEvaluations = 0;
				reportRate *= 4;
				timePair = doTime(String.format("%d evs\n%s val",evaluations, sum),timePair);
				timeCPU = doCPUTime("CPU ",timeCPU);
				System.out.println("Total thread time " + time);
			}
			sendbuf[0] = it2.next();
			MPI.COMM_WORLD.Send(sendbuf, 0, 1, MPI.OBJECT, stat.source, 0);
		}
		
		for(int x = 1; x < nOfProc; x++) {
			Status stat = MPI.COMM_WORLD.Recv(recvbuf1, 0, 3, MPI.OBJECT, MPI.ANY_SOURCE, 0);
			sum = sum.add(recvbuf1[0]);
			newEvaluations += recvbuf2[0];
			time += recvbuf2[1];
			if (newEvaluations > reportRate) {
				evaluations += newEvaluations;
				newEvaluations = 0;
				reportRate *= 4;
				timePair = doTime(String.format("%d evs\n%s val",evaluations, sum),timePair);
				timeCPU = doCPUTime("CPU ",timeCPU);
				System.out.println("Total thread time " + time);
			}
			MPI.COMM_WORLD.Send(null, 0, 0, MPI.OBJECT, stat.source, DIETAG);
		}
		

		evaluations += newEvaluations;
		
		timePair = doTime(String.format("%d evs\n%s val",evaluations, sum),timePair);
		timeCPU = doCPUTime("Finishing ",timeCPU);
		System.out.println(sum);
		timePair = doTime("Finished",timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		System.out.println(String.format("%30s %15d ns","Total thread time ",time));

		System.out.println(String.format("%30s %15d ns","Total cpu time used ",time + getCpuTime()));
		System.out.println(String.format("%30s %15d ms","Total time elapsed ",System.currentTimeMillis() - startTime));
	}

	@SuppressWarnings("unchecked")
	private void work() throws MPIException {
		Object[] buf = new Object[3];
		MPI.COMM_WORLD.Bcast(buf, 0, 3, MPI.OBJECT, 0);

		SortedMap<SmallAntiChain, Long> functions = (SortedMap<SmallAntiChain, Long>) buf[0];

		SortedMap<SmallAntiChain, BigInteger> leftIntervalSize = (SortedMap<SmallAntiChain, BigInteger>) buf[1];
		SmallAntiChain u = (SmallAntiChain) buf[2];
		
		SmallAntiChain[] function = new SmallAntiChain[1];
		BigInteger[] subResult = new BigInteger[1];
		long[] testMaterial = new long[2];
		while(true) {
			Status stat = MPI.COMM_WORLD.Recv(function, 0, 1, MPI.OBJECT, 0, MPI.ANY_TAG);
			if(stat.tag == DIETAG) {
				break;
			}
			
			long time = getCpuTime();
			long evaluations = 0;
			BigInteger sumP = BigInteger.ZERO;
			for (SmallAntiChain r1:functions.keySet()) {
				if (r1.le(function[0])) {
					sumP = sumP.add(
							BigInteger.valueOf(functions.get(r1)).multiply(
								leftIntervalSize.get(r1)).multiply(
								AntiChainSolver.PatricksCoefficient(r1, function[0]))
							);
					evaluations++;
				}
			}
			subResult[0] = sumP.multiply(BigInteger.valueOf(new AntiChainInterval(function[0], u).latticeSize()));
			MPI.COMM_WORLD.Send(subResult, 0, 1, MPI.OBJECT, 0, 0);
			testMaterial[0] = evaluations;
			testMaterial[1] = getCpuTime() - time;
			MPI.COMM_WORLD.Send(testMaterial, 0, 2, MPI.LONG, 0, 0);
		}
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
	
	private long getCpuTime( ) {
	    ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
	    return bean.isCurrentThreadCpuTimeSupported( ) ?
	        bean.getCurrentThreadCpuTime( ) : 0L;
	}
	
	/************************************************************
	 * Main														*
	 ************************************************************/
	
	public static void main(String[] args) throws MPIException, SyntaxErrorException {
		MPI.Init(args);

		int myRank = MPI.COMM_WORLD.Rank();
		int nOfProc = MPI.COMM_WORLD.Size();
		
		MpiM node = new MpiM(Integer.parseInt(args[0]), nOfProc);
		
		if(myRank == 0)
			node.delegate();
		else
			node.work();
		
		MPI.Finalize();
	}

}
