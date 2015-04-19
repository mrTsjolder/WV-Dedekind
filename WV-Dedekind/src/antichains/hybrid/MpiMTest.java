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
import amfsmall.SyntaxErrorException;

/**
 * 
 * @author Pieter-Jan
 *
 */
public class MpiMTest {
	
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
		this.pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}
	
	@SuppressWarnings("unchecked")
	private void doIt() throws SyntaxErrorException, MPIException, InterruptedException, ExecutionException {
		long startTime = System.currentTimeMillis();
		long cpuTime = getCpuTime();

		TestTime timePair = new TestTime(startTime, startTime, startTime);
		TestTime timeCPU = new TestTime(cpuTime, cpuTime, cpuTime);
		
		int[] num = new int[1];
		MPI.COMM_WORLD.bcast(num, 1, MPI.INT, 0);
		byte[] bcastbuf = new byte[num[0]];
		MPI.COMM_WORLD.bcast(bcastbuf, bcastbuf.length, MPI.BYTE, 0);
		
		SortedMap<SmallAntiChain, ?>[] obj = (SortedMap[]) deserialize(bcastbuf);

		SortedMap<SmallAntiChain, Long> functions = (SortedMap<SmallAntiChain, Long>) obj[0];
		SortedMap<SmallAntiChain, BigInteger> leftIntervalSize = (SortedMap<SmallAntiChain, BigInteger>) obj[1];
		
		timePair = doTime(String.format("Proces %d started threading at", myRank), timePair);
		timeCPU = doCPUTime("CPU ", timeCPU);

		BigInteger sum = doThreading(functions, leftIntervalSize);
		
		timePair = doTime(String.format("Proces %d calculated %s", myRank, sum), timePair);
		timeCPU = doCPUTime("CPU ", timeCPU);
		
		byte[] bigbuf = sum.toByteArray();
		MPI.COMM_WORLD.send(new int[]{bigbuf.length}, 1, MPI.INT, 0, NUMTAG);
		MPI.COMM_WORLD.send(bigbuf, bigbuf.length, MPI.BYTE, 0, 0);		
	}

	private BigInteger doThreading(final SortedMap<SmallAntiChain, Long> functions, final SortedMap<SmallAntiChain, BigInteger> leftIntervalSize)
			throws InterruptedException, ExecutionException {
		int counter = 0;
		final SmallAntiChain u = SmallAntiChain.oneSetAntiChain(SmallBasicSet.universe(dedekind));
		Iterator<SmallAntiChain> it2 = AntiChainInterval.fullSpace(dedekind).fastIterator();
		ArrayList<Future<BigInteger>> results = new ArrayList<>();
		
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
			}
			counter %= nOfProc;
		}
		
		BigInteger sum = BigInteger.ZERO;
		for(Future<BigInteger> sumP : results)
			sum = sum.add(sumP.get());
		return sum;
	}
	
	private void doItThoroughly() throws SyntaxErrorException, InterruptedException, ExecutionException, MPIException {
		long startTime = System.currentTimeMillis();
		long cpuTime = getCpuTime();

		TestTime timePair = new TestTime(startTime, startTime, startTime);
		TestTime timeCPU = new TestTime(cpuTime, cpuTime, cpuTime);
		
		timePair = doTime("Starting at ", timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		
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
		
		SortedMap<SmallAntiChain, BigInteger> leftIntervalSize = new TreeMap<>();
		for(SmallAntiChain f : temp.keySet()) {
			leftIntervalSize.put(f, temp.get(f).get());
		}
		
		timePair = doTime("Generated interval sizes",timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		
		byte[] bcastbuf = serialize(new SortedMap[]{functions, leftIntervalSize});
		
		MPI.COMM_WORLD.bcast(new int[]{bcastbuf.length}, 1, MPI.INT, 0);
		MPI.COMM_WORLD.bcast(bcastbuf, bcastbuf.length, MPI.BYTE, 0);
		
		timePair = doTime("Broadcast completed", timePair);
		timeCPU = doCPUTime("CPU", timeCPU);
		
		BigInteger sum = doThreading(functions, leftIntervalSize);
		
		timePair = doTime(String.format("Proces %d calculated %s", myRank, sum), timePair);
		timeCPU = doCPUTime("Finishing", timeCPU);
		
		
//		@SuppressWarnings("unchecked")
//		Future<BigInteger>[] results = new Future[this.nOfProc - 1];
		for(int i = 0; i < this.nOfProc - 1; i++) {
			final int par = i + 1;
//			results[i] = pool.submit(new Callable<BigInteger>() {
//
//				@Override
//				public BigInteger call() throws Exception {
					int[] intbuf = new int[1];
					MPI.COMM_WORLD.recv(intbuf, 1, MPI.INT, par, NUMTAG);
					byte[] bigbuf = new byte[intbuf[0]];
					MPI.COMM_WORLD.recv(bigbuf, bigbuf.length, MPI.BYTE, par, MPI.ANY_TAG);
//					return new BigInteger(bigbuf);
//				}
//				
//			});
		}
		
//		for(Future<BigInteger> f : results)
//			sum = sum.add(f.get());

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
		
		MpiMTest node = new MpiMTest(Integer.parseInt(args[0]), nOfProc, myRank);
		
		if(myRank == 0)
			node.doItThoroughly();
		else
			node.doIt();
		node.pool.shutdown();
		
		MPI.Finalize();
	}

}
