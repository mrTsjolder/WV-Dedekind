package antichains.hybrid;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

/**
 * A class that allows to calculate the nth Dedekind number over MPI in a hybrid fashion.
 * This is implemented with the smarter implementation as described in the bachelor thesis of Pieter-Jan Hoedt.
 * 
 * @author Pieter-Jan
 *
 */
public class HybridM {
	
	/** Tag to indicate the length of an object is being sent */
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
	 * @param	rank
	 * 			The rank of this node.
	 */
	public HybridM(int n, int nOfProc, int rank) {
		if(n < 2) {
			System.out.println("For 0, the dedekind number is:\t2\nFor 1, the dedekind number is:\t3\n");
			throw new IllegalArgumentException("Enter a number greater or equal to 2\n");
		}
		
		this.dedekind = n - 2;
		this.nOfProc = nOfProc;
		this.myRank = rank;
		//make a thread pool that cannot have more threads than there are processors visible to the JVM
		this.pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}
	
	/**
	 * Receive results from non-parallel part and perform a part of the work.
	 * Send the result of this node back to the collecting node.
	 * 
	 * @throws MPIException if something went wrong with the MPI-routines
	 * @throws InterruptedException if one of the threads of this program was interrupted
	 * @throws ExecutionException if something went wrong with the computation in a certain thread
	 */
	@SuppressWarnings("unchecked")
	private void doIt() throws MPIException, InterruptedException, ExecutionException {
		long startTime = System.currentTimeMillis();
		long cpuTime = getCpuTime();

		TestTime timePair = new TestTime(startTime, startTime, startTime);
		TestTime timeCPU = new TestTime(cpuTime, cpuTime, cpuTime);
		
		//receive results of non-parallel part
		int[] num = new int[1];
		MPI.COMM_WORLD.bcast(num, 1, MPI.INT, 0);
		byte[] bcastbuf = new byte[num[0]];
		MPI.COMM_WORLD.bcast(bcastbuf, bcastbuf.length, MPI.BYTE, 0);
		
		SortedMap<SmallAntiChain, ?>[] obj = (SortedMap[]) deserialize(bcastbuf);

		SortedMap<SmallAntiChain, Long> functions = (SortedMap<SmallAntiChain, Long>) obj[0];
		SortedMap<SmallAntiChain, BigInteger> leftIntervalSize = (SortedMap<SmallAntiChain, BigInteger>) obj[1];
		
		timePair = doTime(String.format("Proces %d started threading at", myRank), timePair);
		timeCPU = doCPUTime("CPU ", timeCPU);

		//do part of work
		BigInteger sum = doThreading(functions, leftIntervalSize);
		
		timePair = doTime(String.format("Proces %d calculated %s", myRank, sum), timePair);
		timeCPU = doCPUTime("CPU ", timeCPU);
		
		//send result to the collecting node
		byte[] bigbuf = sum.toByteArray();
		MPI.COMM_WORLD.send(new int[]{bigbuf.length}, 1, MPI.INT, 0, NUMTAG);
		MPI.COMM_WORLD.send(bigbuf, bigbuf.length, MPI.BYTE, 0, 0);		
	}

	/**
	 * Perform a part of the job given the results of the non-parallel part.
	 * 
	 * @param functions The representatives for the equivalence classes
	 * @param leftIntervalSize The left interval sizes
	 * @return a BigInteger containing the partial result of all work for this node
	 * @throws InterruptedException if one of the threads got interrupted
	 * @throws ExecutionException if something went wrong with the computation in a certain thread
	 */
	private BigInteger doThreading(final SortedMap<SmallAntiChain, Long> functions, final SortedMap<SmallAntiChain, BigInteger> leftIntervalSize)
			throws InterruptedException, ExecutionException {
		int counter = 0;
		final SmallAntiChain u = SmallAntiChain.oneSetAntiChain(SmallBasicSet.universe(dedekind));
		Iterator<SmallAntiChain> it2 = AntiChainInterval.fullSpace(dedekind).fastIterator();
		ArrayList<Future<BigInteger>> results = new ArrayList<>();
		
		//iterate over all antichains
		while(it2.hasNext()) {
			final SmallAntiChain function = it2.next();
			//check whether this node needs to calculate the result for this antichain
			if(counter++ == myRank) {
				//add a new task to the thread pool
				results.add(pool.submit(new Callable<BigInteger>() {
					@Override
					public BigInteger call() throws Exception {
						//calculate partial sum
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
			}
			counter %= nOfProc;
		}
		
		//collect the results from all threads
		BigInteger sum = BigInteger.ZERO;
		for(Future<BigInteger> sumP : results)
			sum = sum.add(sumP.get());
		return sum;
	}
	
	/**
	 * Calculate the non-parallel part and do a part of the job as well.
	 * This method also prints some timing statements and collects all results in the end.
	 * 
	 * @throws InterruptedException if one of the threads got interrupted
	 * @throws ExecutionException if something went wrong with the computation in a certain thread
	 * @throws MPIException if something went wrong with the MPI-routines
	 */
	private void doItThoroughly() throws InterruptedException, ExecutionException, MPIException {
		long startTime = System.currentTimeMillis();
		long cpuTime = getCpuTime();

		TestTime timePair = new TestTime(startTime, startTime, startTime);
		TestTime timeCPU = new TestTime(cpuTime, cpuTime, cpuTime);
		
		timePair = doTime("Starting at ", timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		
		//find the equivalence classes in a multithreaded way using the already existing threadpool
		SortedMap<BigInteger, Long>[] classes = AntiChainSolver.equivalenceClasses(dedekind, pool);		//different levels in hass-dagramm
		SortedMap<SmallAntiChain, Long> functions = new TreeMap<>();			//number of antichains.hybrid in 1 equivalence-class

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
		
		//start computation of the left interval sizes in a multithreaded fashion
		final SmallAntiChain e = SmallAntiChain.emptyAntiChain();
		TreeMap<SmallAntiChain, Future<BigInteger>> temp = new TreeMap<>();
		for (final SmallAntiChain f : functions.keySet()) {
			temp.put(f, pool.submit(new Callable<BigInteger>() {

				@Override
				public BigInteger call() throws Exception {
					return BigInteger.valueOf(new AntiChainInterval(e,f).latticeSize());
				}
				
			}));
		}
		
		//collect the results of all computations
		SortedMap<SmallAntiChain, BigInteger> leftIntervalSize = new TreeMap<>();
		for(SmallAntiChain f : temp.keySet()) {
			leftIntervalSize.put(f, temp.get(f).get());
		}
		
		timePair = doTime("Generated interval sizes",timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		
		//broadcast the results of the non-parallel part
		byte[] bcastbuf = serialize(new SortedMap[]{functions, leftIntervalSize});
		
		MPI.COMM_WORLD.bcast(new int[]{bcastbuf.length}, 1, MPI.INT, 0);
		MPI.COMM_WORLD.bcast(bcastbuf, bcastbuf.length, MPI.BYTE, 0);
		
		timePair = doTime("Broadcast completed", timePair);
		timeCPU = doCPUTime("CPU", timeCPU);
		
		//perform part of the job
		BigInteger sum = doThreading(functions, leftIntervalSize);
		
		timePair = doTime(String.format("Proces %d calculated %s", myRank, sum), timePair);
		timeCPU = doCPUTime("Finishing", timeCPU);
		
		//collect the results of all nodes
		for(int i = 1; i < this.nOfProc; i++) {
			int[] intbuf = new int[1];
			MPI.COMM_WORLD.recv(intbuf, 1, MPI.INT, i, NUMTAG);
			byte[] bigbuf = new byte[intbuf[0]];
			MPI.COMM_WORLD.recv(bigbuf, bigbuf.length, MPI.BYTE, i, MPI.ANY_TAG);
			sum = sum.add(new BigInteger(bigbuf));
		}
		
		//output the result and how much time has been spent
		System.out.println("\n" + sum);
		timePair = doTime("Finished ",timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		System.out.println(String.format("%30s %15d ms","Total time elapsed ",System.currentTimeMillis() - startTime));
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
	
	/**
	 * This class represents a triple of times including a time in history, a current time and a starting time
	 */
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

	/**
	 * Print a set of statistics on how much time has passed since the last TestTime has been made.
	 * 
	 * @param msg A message to display
	 * @param timePair A TestTime representing the previous moment
	 * @return a new TestTime to represent the current moment.
	 */
	private TestTime doTime(String msg, TestTime timePair) {
		TestTime result = new TestTime(timePair.currentTime, System.currentTimeMillis(), timePair.startTime);
		System.out.println(String.format("%s %d ms %d ms (%d ms)",msg,(result.previousTime - result.startTime),  
				 (result.currentTime - result.startTime),(-result.previousTime + result.currentTime)));
		return result;
	}

	/**
	 * Print a set of statistics on how much CPU time has passed since the last TestTime has been made.
	 * 
	 * @param msg A message to display
	 * @param timePair A TestTime representing the previous moment
	 * @return a new TestTime to represent the current moment.
	 */
	private TestTime doCPUTime(String msg, TestTime timePair) {
		TestTime result = new TestTime(timePair.currentTime, getCpuTime(), timePair.startTime);
		System.out.println(String.format("%s : %d ns (+ %d ns)", msg,  
				 (result.currentTime - result.startTime), (result.currentTime - result.previousTime)));
		return result;
	}
	
	/**
	 * @return the total CPU time for the current thread in nanoseconds
	 */
	private long getCpuTime( ) {
	    ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
	    return bean.isCurrentThreadCpuTimeSupported( ) ?
	        bean.getCurrentThreadCpuTime( ) : 0L;
	}
	
	/************************************************************
	 * Main														*
	 ************************************************************/
	
	/**
	 * Initialize MPI and execute the doItThoroughly method if the rank of this process is 0 or
	 * the doIt method otherwise. Finalize MPI on this node after the method returns.
	 * 
	 * @param args0 The Dedekind number to calculate
	 * @throws MPIException if something went wrong with the MPI-routines
	 * @throws InterruptedException if one of the threads was interrupted
	 * @throws ExecutionException if computation in a certain thread went wrong
	 */
	public static void main(String[] args) throws MPIException, InterruptedException, ExecutionException {
		MPI.InitThread(args, MPI.THREAD_MULTIPLE);

		int myRank = MPI.COMM_WORLD.getRank();
		int nOfProc = MPI.COMM_WORLD.getSize();
		
		HybridM node = new HybridM(Integer.parseInt(args[0]), nOfProc, myRank);
		
		if(myRank == 0)
			node.doItThoroughly();
		else
			node.doIt();
		
		//shutdown the thread pool
		node.pool.shutdown();
		
		MPI.Finalize();
	}

}
