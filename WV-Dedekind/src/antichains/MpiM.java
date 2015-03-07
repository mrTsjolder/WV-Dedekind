package antichains;

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
	
	private int dedekind;
	private int nOfProc;

	//buffers
	int[] num = new int[1];
	private byte[] bcastbuf;
	long[] acbuf;
	private byte[] bigintbuf = new byte[1];
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
	
	private void delegate() throws SyntaxErrorException, MPIException{
		BigInteger sum = BigInteger.ZERO;
		
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

		bcastbuf = serialize(new Object[]{functions, leftIntervalSize, u});
		
		MPI.COMM_WORLD.bcast(new int[]{bcastbuf.length}, 1, MPI.INT, 0);
		MPI.COMM_WORLD.bcast(bcastbuf, bcastbuf.length, MPI.BYTE, 0);

		// test
		int reportRate = 10;
		long evaluations = 0;
		long newEvaluations = 0;
		long time = 0;
		Iterator<SmallAntiChain> it2 = new AntiChainInterval(e,u).fastIterator();
		
		int i;
		for(i = 1; i < nOfProc; i++) {
			if(it2.hasNext()) {
				acbuf = it2.next().toLongArray();
				MPI.COMM_WORLD.send(new int[]{acbuf.length}, 1, MPI.INT, i, NUMTAG);
				MPI.COMM_WORLD.send(acbuf, acbuf.length, MPI.LONG, i, 0);
			}
		}
		
		timePair = doTime("First " + nOfProc + " messages sent", timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		
		while(it2.hasNext()) {
			int src = retrieveResults();
			sum = sum.add(new BigInteger(bigintbuf));
			newEvaluations += timebuf[0];
			time += timebuf[1];
			if (newEvaluations > reportRate) {
				evaluations += newEvaluations;
				newEvaluations = 0;
				reportRate *= 4;
				timePair = doTime(String.format("%d evs\n%s val",evaluations, sum),timePair);
				timeCPU = doCPUTime("CPU ",timeCPU);
				System.out.println("Total thread time " + time);
			}
			//TODO: get in retrieve...
			acbuf = it2.next().toLongArray();
			MPI.COMM_WORLD.send(new int[]{acbuf.length}, 1, MPI.INT, src, NUMTAG);
			MPI.COMM_WORLD.send(acbuf, acbuf.length, MPI.LONG, src, 0);
		}
		
		for(int x = 1; x < nOfProc; x++) {
			//TODO: duplicated code
			int src = retrieveResults();
			sum = sum.add(new BigInteger(bigintbuf));
			newEvaluations += timebuf[0];
			time += timebuf[1];
			if (newEvaluations > reportRate) {
				evaluations += newEvaluations;
				newEvaluations = 0;
				reportRate *= 4;
				timePair = doTime(String.format("%d evs\n%s val",evaluations, sum),timePair);
				timeCPU = doCPUTime("CPU ",timeCPU);
				System.out.println("Total thread time " + time);
			}
			MPI.COMM_WORLD.send(null, 0, MPI.INT, src, NUMTAG);
			MPI.COMM_WORLD.send(null, 0, MPI.LONG, src, DIETAG);
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
		SmallAntiChain function;
		
		MPI.COMM_WORLD.bcast(num, 1, MPI.INT, 0);
		bcastbuf = new byte[num[0]];
		MPI.COMM_WORLD.bcast(bcastbuf, num[0], MPI.BYTE, 0);
		
		Object[] obj = (Object[]) deserialize(bcastbuf);

		SortedMap<SmallAntiChain, Long> functions = (SortedMap<SmallAntiChain, Long>) obj[0];
		SortedMap<SmallAntiChain, BigInteger> leftIntervalSize = (SortedMap<SmallAntiChain, BigInteger>) obj[1];
		SmallAntiChain u = (SmallAntiChain) obj[2];
		
		while(true) {
			MPI.COMM_WORLD.recv(num, 1, MPI.INT, 0, NUMTAG);
			acbuf = new long[num[0]];
			Status stat = MPI.COMM_WORLD.recv(acbuf, acbuf.length, MPI.LONG, 0, MPI.ANY_TAG);
			
			if(stat.getTag() == DIETAG) {
				break;
			}
			
			function = new SmallAntiChain(acbuf);
			
			long time = getCpuTime();
			long evaluations = 0;
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
	 * @return	An integer representing the rank of the node that sent the results.
	 * @throws 	MPIException
	 * 			If MPI failed.
	 */
	private int retrieveResults() throws MPIException {
		MPI.COMM_WORLD.recv(num, 1, MPI.INT, MPI.ANY_SOURCE, NUMTAG);
		bigintbuf = new byte[num[0]];
		Status stat = MPI.COMM_WORLD.recv(bigintbuf, bigintbuf.length, MPI.BYTE, MPI.ANY_SOURCE, 0);
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
	 * @return	A byte array representing the serialized object.
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
	 * @return	The object that was represented by this byte array.
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
