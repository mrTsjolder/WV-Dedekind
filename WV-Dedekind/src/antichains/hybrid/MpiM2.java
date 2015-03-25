package antichains.hybrid;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import mpi.MPI;
import mpi.MPIException;
import amfsmall.AntiChainInterval;
import amfsmall.SmallAntiChain;
import amfsmall.SmallBasicSet;

public class MpiM2 {
	
	private final int dedekind;
	private final int nOfProc;
	
	public MpiM2(int n, int nrProc) {
		if(n < 3) {
			System.out.println("For 0, the dedekind number is:\t2\nFor 1, the dedekind number is:\t3\nFor 2, the dedekind number is:\t6\n");
			throw new IllegalArgumentException("Enter a number greater or equal to 2\n");
		}
		dedekind = n - 3;
		nOfProc = nrProc;
	}
	
	private void delegate() {
		ArrayList<SmallAntiChain> functions = new ArrayList<>();
		
		SmallAntiChain e = SmallAntiChain.emptyAntiChain();
		SmallAntiChain u = SmallAntiChain.oneSetAntiChain(SmallBasicSet.universe(dedekind));

		Iterator<SmallAntiChain> it = new AntiChainInterval(e,u).fastIterator();
		while(it.hasNext()) {
			functions.add(0, it.next());
		}

		SortedMap<AntiChainInterval, BigInteger> intervalSizes = new TreeMap<AntiChainInterval, BigInteger>();
		for (SmallAntiChain f : functions) {
			for (SmallAntiChain g : functions) {
				if(f.le(g)) {
					AntiChainInterval interval = new AntiChainInterval(f,g);
					intervalSizes.put(interval, BigInteger.valueOf(interval.latticeSize()));
				}
			}
		}
		
		for(SmallAntiChain r2 : functions) {
			//TODO
		}
	}
	
	private void work() {
		
	}
	
	public static void main(String[] args) throws MPIException {
		MPI.Init(args);
		
		int myRank = MPI.COMM_WORLD.getRank();
		int nrProc = MPI.COMM_WORLD.getSize();
		
		MpiM2 node = new MpiM2(Integer.parseInt(args[0]), nrProc);
		
		if(myRank == 0)
			node.delegate();
		else
			node.work();
		
		MPI.Finalize();
	}
}
