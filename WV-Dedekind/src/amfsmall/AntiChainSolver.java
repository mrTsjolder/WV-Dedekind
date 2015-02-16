package amfsmall;

import java.math.BigInteger;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Solver class for systems of equations in SmallAntiChains
 * 
 * @author patrickdecausmaecker
 *
 */
public class AntiChainSolver {
	
	/**
	 * far more sophisticated version of PatricksCoefficient
	 * 
	 * @param 	r1
	 * @param 	r2
	 * @return
	 */
	public static BigInteger PatricksCoefficient(SmallAntiChain r1, SmallAntiChain r2) {
		// trivial case, no solutions unless r1 <= r2
		if (!r1.le(r2)) return BigInteger.ZERO;
		// trivial case, one solution if r1 == r2
		if (r1.equals(r2)) return BigInteger.ONE;
		// treat the case of empty functions separately (most function in AntiChain and AntiChainInterval do not apply)
		if (r1.isEmpty()) {
			if (r2.isEmpty()) return BigInteger.ONE; // (empty, empty)
			return BigInteger.ONE.add(BigInteger.ONE); // (empty, r2), (r2,empty)
		}
		return BigInteger.valueOf(1<<(CountConnected(graph(r1,r2.minus(r1)))));
	}
	
	private static SortedMap<SmallBasicSet, SortedSet<SmallBasicSet>> graph(SmallAntiChain r1, SmallAntiChain r2) {
		SortedMap<SmallBasicSet, SortedSet<SmallBasicSet>> ret = new TreeMap<SmallBasicSet, SortedSet<SmallBasicSet>>();
		for (SmallBasicSet r : r2) {
			SortedSet<SmallBasicSet> cr = new TreeSet<SmallBasicSet>();
			for (SmallBasicSet s : r2) {
				if (!r1.ge(r.intersection(s))) cr.add(s);
			}
			ret.put(r,cr);
		}
		return ret;
	}

	private static long CountConnected(SortedMap<SmallBasicSet, SortedSet<SmallBasicSet>> g) {
		SortedSet<SmallBasicSet> had = new TreeSet<SmallBasicSet>();
		long ret = 0;
		for (SmallBasicSet n : g.keySet()) {
			if (!had.contains(n)) {
				ret++;
				had.add(n);
				doNode(g,n,had);
			}
		}
		return ret;
	}

	private static void doNode(SortedMap<SmallBasicSet, SortedSet<SmallBasicSet>> g, SmallBasicSet n, SortedSet<SmallBasicSet> had) {
		for (SmallBasicSet m : g.get(n)) {
			if (had.add(m)) doNode(g,m,had);
		}
	}
	
	/**
	 * compute the equivalences of AMF(n) for n = 0 .. till inclusive
	 * in BigInteger representation
	 * This is based on algorithm 9 in "Ten Beuatiful formula..."
	 * 
	 * @param 	till
	 * @return 	array of maps, mapping each biginteger to the size of the equivalence class it represents
	 * @throws 	SyntaxErrorException
	 */
	public static SortedMap<BigInteger, Long>[] equivalenceClasses(int till) throws SyntaxErrorException {
		@SuppressWarnings("unchecked")
		SortedMap<BigInteger, Long>[] reS = new TreeMap[till+1];
		reS[0] = new TreeMap<BigInteger,Long>();
		Storage.store(reS[0],SmallAntiChain.emptyAntiChain().standard().encode());
		Storage.store(reS[0],SmallAntiChain.emptySetAntiChain().standard().encode());
		int n = 0;
		while (n < till) {
			reS[n+1] = algorithm7(n,reS[n]);
			n++;
		}
		return reS;
	}

	/**
	 * (this is algorithm 7 in "Ten Beautiful formula...")
	 * Computing the representatives of AM F (n + 1) with span n + 1 from the representatives of AMF(n) 
	 * with span n and the sizes of the corresponding equivalence classes
	 * 
	 * @param	n 
	 * 			the dimension of the given set of equivalence classes
	 * @param 	S 
	 * 			mapping the equivalence classes of dimension n (in BigInteger representation) to their sizes
	 * @return 	return maps the equivalence classes of dimensions n+1 (in BigInteger representation) to their sizes
	 */
	private static SortedMap<BigInteger,Long> algorithm7(long n, SortedMap<BigInteger, Long> S) {
		SortedMap<BigInteger,Long> S1 = new TreeMap<BigInteger, Long>();
		SmallAntiChain alfa = SmallAntiChain.universeAntiChain((int) n);
		SmallAntiChain u = SmallAntiChain.universeAntiChain((int) (n+1));
		SmallAntiChain l = SmallAntiChain.singletonAntiChain((int) (n+1));
		for (BigInteger tCode : S.keySet()) {
			SmallAntiChain t = SmallAntiChain.decode(tCode);
			Set<int[]> rtsymm = ((SmallAntiChain) t.join(l)).symmetryGroup();
			SortedMap<BigInteger, Long> St = new TreeMap<BigInteger, Long>();
			//TODO: lose deprecated...
			for (SmallAntiChain x : new AntiChainInterval((SmallAntiChain) t.join(l),u.omicron(t, alfa))) {
				BigInteger b = x.standard(rtsymm).encode(); 
				Storage.store(St, b);
			}
			for (BigInteger b : St.keySet()) {
				SmallAntiChain x = SmallAntiChain.decode(b);
				BigInteger code = x.standard().encode();
				Storage.store(S1,code,St.get(b)*S.get(tCode));
			}
		}
		return S1;
	}
}
