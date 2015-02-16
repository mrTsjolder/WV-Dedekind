package antichains;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import amfsmall.AntiChainInterval;
import amfsmall.SmallAntiChain;
import amfsmall.AntiChainSolver;
import amfsmall.SmallBasicSet;
import amfsmall.Storage;
import amfsmall.SyntaxErrorException;

import mpi.*;

/**
 * class for the computation of a Dedekind number
 * @author u0003471
 *
 */
public class M {
	
	public final int dedekind;

	public final int cores;

	static private SmallBasicSet[] N;
	static private SmallAntiChain[] fN;
	static private AntiChainInterval[] iS;
	
	public M(int n, int coresUsed) throws SyntaxErrorException {
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
		int n = dedekind - 2;
		int reportRate = 10;
		
		SortedMap<BigInteger, Long>[] classes = AntiChainSolver.equivalenceClasses(n);	//different levels in hass-dagramm
		SortedMap<SmallAntiChain, Long> functions = new TreeMap<SmallAntiChain, Long>();			//number of antichains in 1 equivalence-class

		timePair = doTime("Generated equivalence classes at ",timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);

		PrintStream ps;
		try {
			ps = new PrintStream("EquivalenceClasses" + n);

			// collect
			for (int i=0;i<classes.length;i++) {
				long coeff = SmallBasicSet.combinations(n, i);
				for (BigInteger b : classes[i].keySet()) {
					Storage.store(functions, SmallAntiChain.decode(b),classes[i].get(b)*coeff);
					ps.println(SmallAntiChain.decode(b) + "," + classes[i].get(b)*coeff);
				}	
			}

			ps.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		timePair = doTime("Collected equivalence classes at ",timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		
		SmallAntiChain e = SmallAntiChain.emptyAntiChain();
		SmallAntiChain u = SmallAntiChain.oneSetAntiChain(SmallBasicSet.universe(n));
		SortedMap<SmallAntiChain, BigInteger> leftIntervalSize = new TreeMap<SmallAntiChain, BigInteger>();
		SortedMap<SmallAntiChain, BigInteger> rightIntervalSize = new TreeMap<SmallAntiChain, BigInteger>();
		for (SmallAntiChain f : functions.keySet()) {
			leftIntervalSize.put(f, BigInteger.valueOf(new AntiChainInterval(e,f).latticeSize()));
			//TODO: time gain for calculating rightIntervalSizes in PCThread?
			rightIntervalSize.put(f, BigInteger.valueOf(new AntiChainInterval(f,u).latticeSize()));
		}
		
		timePair = doTime("Generated interval sizes",timePair);
		timeCPU = doCPUTime("CPU ",timeCPU);
		
		/*try {
			MPI.Init(null);
			int myRank = MPI.COMM_WORLD.Rank();
			int nOfProc = MPI.COMM_WORLD.Size();
			String name = MPI.Get_processor_name();
			
			BigInteger sum = BigInteger.ZERO;
			BigInteger buf = null;
			
			System.out.print(String.format("Process %d of %d is on processor %s\n", myRank, nOfProc, name));
			
			if(myRank == 0) {
				Iterator<AntiChain> it2 = new AntiChainInterval(e,u).fastIterator();
				int i;
				for(i = 1; i < nOfProc; i++) {
					if(it2.hasNext()) 
						MPI.COMM_WORLD.Send(it2.next(), 0, 1, MPI.OBJECT, i, 0);
				}
				
				while(it2.hasNext()) {
					
					Status stat = MPI.COMM_WORLD.Recv(buf, 0, 1, MPI.OBJECT, MPI.ANY_SOURCE, 0);
					sum.add(buf);
					MPI.COMM_WORLD.Send(it2.next(), 0, 1, MPI.OBJECT, stat.source, 0);
				}
				
				for(int x = 1; x < nOfProc; x++) {
					Status stat = MPI.COMM_WORLD.Recv(buf, 0, 1, MPI.OBJECT, MPI.ANY_SOURCE, 0);
					sum.add(buf);
					MPI.COMM_WORLD.Send(null, 0, 0, MPI.OBJECT, stat.source, 666);
				}
				
				System.out.println(sum);
				
			} else {
				while(true) {
					AntiChain function = null;
					Status stat = MPI.COMM_WORLD.Recv(function, 0, 1, MPI.OBJECT, 0, MPI.ANY_TAG);
					if(stat.tag == 666) {
						break;
					}
					BigInteger sumP = BigInteger.ZERO;
					for (AntiChain r1:functions.keySet()) {
						if (r1.le(function)) {
							sumP = sumP.add(
									BigInteger.valueOf(functions.get(r1)).multiply(
										leftIntervalSize.get(r1)).multiply(
										AntiChainSolver.PatricksCoefficient(r1, function))
									);

						}
					}
					MPI.COMM_WORLD.Send(sumP.multiply(rightIntervalSize.get(function.standard())), 0, 1, MPI.OBJECT, 0, 0);
				}
			}
			
			MPI.Finalize();
		} catch(MPIException mpie) {
			mpie.printStackTrace();
		}*/
		
		// test
		long evaluations = 0;
		long newEvaluations = 0;
		Iterator<SmallAntiChain> it2 = new AntiChainInterval(e,u).fastIterator();
		
		Collector collector = new Collector(cores);

		while (it2.hasNext()) {
			SmallAntiChain r2 = it2.next();
			new PCThread(r2, functions, leftIntervalSize, rightIntervalSize, collector).start();
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
		new M(Integer.parseInt(args[0]), Integer.parseInt(args[1])).doIt();
	}

}
