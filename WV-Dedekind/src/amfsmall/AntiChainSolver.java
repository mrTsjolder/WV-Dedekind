package amfsmall;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Solver class for systems of equations in AntiChains
 * 
 * @author patrickdecausmaecker
 *
 */
public class AntiChainSolver {
	
	/**
	 * far more sophisticated version of PatricksCoefficient
	 * @param r1
	 * @param r2
	 * @return
	 */
	public static BigInteger PatricksCoefficient(AntiChain r1, AntiChain r2) {
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
	
	private static SortedMap<SmallBasicSet, SortedSet<SmallBasicSet>> graph(AntiChain r1, AntiChain r2) {
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
	 * assign a number of sets over a number of components such that each set is used at least in one component
	 *
	 * @param numberOfSets
	 * @param numberOfComponents
	 * @return an iterator going over the allowed assignments represented as a bitrepresentation (integers >0 and less thatn 2^numberOfComponents - 1)
	 */
	public static Iterator<int[]> multiSetCover(final int numberOfSets, final int numberOfComponents) {
		return new Iterator<int[]>() {
			
			private int[] current;
			private int limit;
			private boolean notDone;
			
			{
				current = new int[numberOfSets];
				for (int i=0;i<numberOfSets;i++) current[i] = 1; // all sets in component 1
				limit = (1<<numberOfComponents) - 1;
				notDone = true;
			}

			@Override
			public boolean hasNext() {
				return notDone;
			}

			@Override
			public int[] next() {
				increase();
				return current;
			}

			private void increase() {
				{
					int i;
					for (i=0;i<numberOfSets;i++) {
						if (current[i] == limit) current[i] = 1;
						else {
							current[i]++;
							break;
						}
					}
					notDone = (i < numberOfSets);
				}
			}

			@Override
			public void remove() {
				// TODO Auto-generated method stub
				
			}
			
		};
	}
	
	
	/**
	 * compute the equivalences of AMF(n) for n = 0 .. till inclusive
	 * in BigInteger representation
	 * This is based on algorithm 9 in "Ten Beuatiful formula..."
	 * @param till
	 * @return array of maps, mapping each biginteger to the size of the equivalence class it represents
	 * @throws SyntaxErrorException
	 */
	public static SortedMap<BigInteger, Long>[] equivalenceClasses(int till) throws SyntaxErrorException {
		@SuppressWarnings("unchecked")
		SortedMap<BigInteger, Long>[] reS = new TreeMap[till+1];
		reS[0] = new TreeMap<BigInteger,Long>();
		Storage.store(reS[0],AntiChain.emptyFunction().standard().encode());
		Storage.store(reS[0],AntiChain.emptySetFunction().standard().encode());
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
	 * @param n the dimension of the given set of equivalence classes
	 * @param S mapping the equivalence classes of dimension n (in BigInteger representation) to their sizes
	 * @return return maps the equivalence classes of dimensions n+1 (in BigInteger representation) to their sizes
	 */
	private static SortedMap<BigInteger,Long> algorithm7(long n, SortedMap<BigInteger, Long> S) {
		SortedMap<BigInteger,Long> S1 = new TreeMap<BigInteger, Long>();
		AntiChain alfa = AntiChain.universeFunction((int) n);
		AntiChain u = AntiChain.universeFunction((int) (n+1));
		AntiChain l = AntiChain.singletonFunction((int) (n+1));
		for (BigInteger tCode : S.keySet()) {
			AntiChain t = AntiChain.decode(tCode);
			Set<int[]> rtsymm = t.join(l).symmetryGroup();
			SortedMap<BigInteger, Long> St = new TreeMap<BigInteger, Long>();
			for (AntiChain x : new AntiChainInterval(t.join(l),u.omicron(t, alfa))) {
				BigInteger b = x.standard(rtsymm).encode(); 
				Storage.store(St, b);
			}
			for (BigInteger b : St.keySet()) {
				AntiChain x = AntiChain.decode(b);
				BigInteger code = x.standard().encode();
				Storage.store(S1,code,St.get(b)*S.get(tCode));
			}
		}
		return S1;
	}

	/**
	 * compute an intervaliterator working by dynamic programming on the last element of the span
	 * Static version of the equivalent in AntiChainInterval
	 * @pre interval is not empty
	 * @param interval
	 * @return
	 */
	private static Iterator<AntiChain> fastNonEmptyIterator(final AntiChainInterval interval) {
		SmallBasicSet span = interval.getTop().sp();
		if (span.isEmpty()) return new Iterator<AntiChain>() {
			// empty span interval case. At most two elements
			private AntiChain current = interval.getBottom();

			@Override
			public boolean hasNext() {
				return current != null;
			}

			@Override
			public AntiChain next() {
				AntiChain ret = current;
				if (current.equals(interval.getTop())) current = null;
				else current = interval.getTop();
				return ret;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
		return new Iterator<AntiChain>() {

			// non empty span
			private SmallBasicSet span = interval.getTop().sp();
			private AntiChain maxSpan = AntiChain.singletonFunction(span.maximum());
			private AntiChain current = interval.getBottom();
			private AntiChain[] alfaBottom = current.reduce(span);
			private AntiChain[] alfaTop = interval.getTop().reduce(span);

			private AntiChain[] alfa = new AntiChain[2];
			@SuppressWarnings("unchecked")
			private Iterator<AntiChain>[] iterator = new Iterator[2];
			{ 
				iterator[0] = fastIterator(new AntiChainInterval(alfaBottom[0],alfaTop[0]));
				alfa[0] = iterator[0].next();
				iterator[1] = fastIterator(new AntiChainInterval(alfaBottom[1],alfa[0].meet(alfaTop[1])));
				alfa[1] = iterator[1].next();
			};

			private AntiChain nextCurrent() {
				// problem with times
				if (alfa[1].isEmpty()) return alfa[0];
				else return alfa[0].join(alfa[1].times(maxSpan));
			}

			@Override
			public boolean hasNext() {
				return current != null;
			}

			@Override
			public AntiChain next() {
				AntiChain ret = current;
				if (iterator[1].hasNext()) {
					alfa[1] = iterator[1].next();
					current = nextCurrent();
				}
				else if (iterator[0].hasNext()) {
					alfa[0] = iterator[0].next();
					iterator[1] = fastIterator(new AntiChainInterval(alfaBottom[1],alfa[0].meet(alfaTop[1])));
					if (!iterator[1].hasNext()) current = null;
					else { 
						alfa[1] = iterator[1].next();
						current = nextCurrent();		
					}
				}
				else current = null;
				return ret;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
	}
	
	/**
	 * compute an intervaliterator working by dynamic programming on the last element of the span
	 * @param interval
	 * @return
	 */
	public static Iterator<AntiChain> fastIterator(final AntiChainInterval interval) {
		if (interval.getBottom().le(interval.getTop())) return fastNonEmptyIterator(interval);
		else {
			// empty interval
			return new Iterator<AntiChain>() {

				// no next!
				
				@Override
				public boolean hasNext() {
						return false;
				}

				@Override
				public AntiChain next() {
					throw new NoSuchElementException();
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
				
			};
		}
	}
}
