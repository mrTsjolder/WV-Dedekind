package antichains.mpi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigInteger;
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
	public static final int NUMTAG = 1;
	
	private final int dedekind;
	private final int nOfProc;

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
	public MpiM(int n, int nOfProc) {
		if(n < 2) {
			System.out.println("For 0, the dedekind number is:\t2\nFor 1, the dedekind number is:\t3\n");
			throw new IllegalArgumentException("Enter a number greater or equal to 2\n");
		}
		
		this.dedekind = n - 2;
		this.nOfProc = nOfProc;
	}
	
	private void delegate() throws SyntaxErrorException, MPIException, InterruptedException, ExecutionException{
		BigInteger sum = BigInteger.ZERO;
		
		long startTime = System.currentTimeMillis();
		long cpuTime = getCpuTime();
		
		TestTime timePair = new TestTime(startTime, startTime, startTime);
		TestTime timeCPU = new TestTime(cpuTime, cpuTime, cpuTime);

		timePair = doTime("Starting at", timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		
		SortedMap<BigInteger, Long>[] classes = AntiChainSolver.equivalenceClasses(dedekind);		//different levels in hass-dagramm
		SortedMap<SmallAntiChain, Long> functions = new TreeMap<SmallAntiChain, Long>();			//number of antichains.hybrid in 1 equivalence-class

		timePair = doTime("Generated equivalence classes at",timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		
		// collect
		for (int i=0;i<classes.length;i++) {
			long coeff = SmallBasicSet.combinations(dedekind, i);
			for (BigInteger b : classes[i].keySet()) {
				Storage.store(functions, SmallAntiChain.decode(b),classes[i].get(b)*coeff);
			}	
		}
		
		timePair = doTime("Collected equivalence classes at",timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		
		final SmallAntiChain e = SmallAntiChain.emptyAntiChain();
		ExecutorService pool = Executors.newCachedThreadPool();
		SortedMap<SmallAntiChain, BigInteger> leftIntervalSize = new TreeMap<>();
		TreeMap<SmallAntiChain, Future<BigInteger>> temp = new TreeMap<>();
		for (final SmallAntiChain f : functions.keySet()) {
			temp.put(f, pool.submit(new Callable<BigInteger>() {

				@Override
				public BigInteger call() throws Exception {
					return BigInteger.valueOf(new AntiChainInterval(e,f).latticeSize());
				}
				
			}));
		}
		pool.shutdown();
		for(SmallAntiChain f : temp.keySet()) {
			leftIntervalSize.put(f, temp.get(f).get());
		}
		
		timePair = doTime("Generated interval sizes",timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);

		byte[] bcastbuf = serialize(new SortedMap[]{functions, leftIntervalSize});
		
		MPI.COMM_WORLD.bcast(new int[]{bcastbuf.length}, 1, MPI.INT, 0);
		MPI.COMM_WORLD.bcast(bcastbuf, bcastbuf.length, MPI.BYTE, 0);
		
		timePair = doTime("Broadcast complete", timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		
		// test
		long reportRate = 8;
		long evaluations = 0;
		long newEvaluations = 0;
		long time = 0;
		Iterator<SmallAntiChain> it2 = AntiChainInterval.fullSpace(dedekind).fastIterator();
		
		int x = 0;
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
		timePair = doTime("Finished ",timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		System.out.println(String.format("%30s %15d ns","Total thread time ",time));

		System.out.println(String.format("%30s %15d ns","Total cpu time used ",time + getCpuTime()));
		System.out.println(String.format("%30s %15d ms","Total time elapsed ",System.currentTimeMillis() - startTime));
	}

	@SuppressWarnings("unchecked")
	private void work() throws MPIException {
		SmallAntiChain u = SmallAntiChain.oneSetAntiChain(SmallBasicSet.universe(dedekind));
		SmallAntiChain function;
		
		MPI.COMM_WORLD.bcast(num, 1, MPI.INT, 0);
		byte[] bcastbuf = new byte[num[0]];
		MPI.COMM_WORLD.bcast(bcastbuf, bcastbuf.length, MPI.BYTE, 0);
		
		SortedMap<SmallAntiChain, ?>[] obj = (SortedMap[]) deserialize(bcastbuf);

		SortedMap<SmallAntiChain, Long> functions = (SortedMap<SmallAntiChain, Long>) obj[0];
		SortedMap<SmallAntiChain, BigInteger> leftIntervalSize = (SortedMap<SmallAntiChain, BigInteger>) obj[1];
		
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
	 * Retrieve results sent from the workers.
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
	 * Serializing-utils										*
	 ************************************************************/

	/**
	 * Serialize an object in order to send it over MPI.
	 * 
	 * @param 	object
	 * 			The object to be serialized.
	 * @return	a byte array representing the serialized object.
	 * 			This array contains no elements if something went wrong.
	 */
	private byte[] serialize(Object object) {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    ObjectOutputStream oos = null;
		try {
		    oos = new ObjectOutputStream(baos);
		    oos.writeObject(object);
		    oos.flush();
		    oos.close();
		    return baos.toByteArray();
		} 
		catch (IOException ioe) {
		   	ioe.printStackTrace();   
		}
		return new byte[0];
	}
	
	/**
	 * Deserialize a byte array received through MPI.
	 * 
	 * @param	bytes
	 * 			The byte array to be deserialized.
	 * @return	the object that was represented by this byte array.
	 * 			The object will be null if something went wrong.
	 */
	private Object deserialize(byte[] bytes) {
		try { 
		    ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		    ObjectInputStream ois = new ObjectInputStream(bis);
		    return ois.readObject();
		} catch (IOException ioe) {
		    ioe.printStackTrace();
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		}
		return null;
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
		MPI.InitThread(args, MPI.THREAD_MULTIPLE);

		int myRank = MPI.COMM_WORLD.getRank();
		int nOfProc = MPI.COMM_WORLD.getSize();
		
		MpiM node = new MpiM(Integer.parseInt(args[0]), nOfProc);
		
		if(myRank == 0)
			node.delegate();
		else
			node.work();
		
		MPI.Finalize();
	}

}
