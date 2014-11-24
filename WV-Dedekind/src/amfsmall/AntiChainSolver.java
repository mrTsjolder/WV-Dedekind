package amfsmall;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import posets.IntervalPoset;

/**
 * Solver class for systems of equations in AntiChains
 * 
 * @author patrickdecausmaecker
 *
 */
public class AntiChainSolver {

	/**
	 * Compute the number of solutions of the system of simultaneous equations
	 * a1.meet(a2) = r1
	 * a1.join(a2) = r2
	 * all solutions are in the interval [r1,r2]
	 * 
	 * @param r1
	 * @param r2
	 * @return if (r1 <= r2) result = sum for a1 in [r1,r2] of |[dual(omicron(dual(r2),dual(a1),omicron(r1,a1)]|
	 * @return if (!(r1 <= r2) result = 0 
	 */
	public static BigInteger NaivePatrick(AntiChain r1, AntiChain r2) {
		// trivial case, no solutions unless r1 <= r2
		if (!r1.le(r2)) return BigInteger.ZERO;
		// trivial case, one solution if r1 == r2
		if (r1.equals(r2)) return BigInteger.ONE;
		BigInteger res = BigInteger.ZERO;
		// treat the case of empty functions separately (most function in AntiChain and AntiChainInterval do not apply)
		if (r1.isEmpty()) {
			if (r2.isEmpty()) return BigInteger.ONE; // (empty, empty)
			return BigInteger.ONE.add(BigInteger.ONE); // (empty, r2), (r2,empty)
		}
		AntiChainInterval theInterval = new AntiChainInterval(r1,r2); 
		SmallBasicSet N = r2.sp();
		AntiChain f = r2.dual(N);
		AntiChain fN = AntiChain.oneSetFunction(N);
		// treat the case of complete functions separately (most function in AntiChain and AntiChainInterval do not apply)
		if (r2.equals(fN)) {
			if (r1.equals(fN)) return BigInteger.ONE; // (fN, fN)
			return BigInteger.ONE.add(BigInteger.ONE); // (fN, r1), (r1,fN)
		}
		for (AntiChain a1 : theInterval) {
			AntiChain w = fN.omicron(r1, a1);
			AntiChain wD = (fN.omicron(r2.dual(N), a1.dual(N))).dual(N);
			AntiChain bot = wD.join(r1);
			AntiChain top = w.meet(r2);
			long size = new AntiChainInterval(bot, top).latticeSize();
			res = res.add(BigInteger.valueOf(size));
		}
		return res;
	}
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
		BigInteger res = BigInteger.ZERO;
		// treat the case of empty functions separately (most function in AntiChain and AntiChainInterval do not apply)
		if (r1.isEmpty()) {
			if (r2.isEmpty()) return BigInteger.ONE; // (empty, empty)
			return BigInteger.ONE.add(BigInteger.ONE); // (empty, r2), (r2,empty)
		}
		return BigInteger.valueOf(1<<(CountConnected(graph(r1,r2.minus(r1)))));
	}
	
	/**
	 * compute third order PC for the amf r1, r2 in [0,\{N U K\}]
	 * @pre N and K are disjoint
	 * @pre r1 <= r2 in [0,{SP(r2}]
	 * @param r1
	 * @param r2
	 * @return the value of the coefficient
	 */
	public static BigInteger PatricksCoefficient3(AntiChain r1, AntiChain r2) {
		AntiChainInterval baseSpace = new AntiChainInterval(AntiChain.emptyFunction(), AntiChain.oneSetFunction(r2.sp()));
		BigInteger res = BigInteger.ZERO;
		for (AntiChain a12 : new AntiChainInterval(r1,r2))
			for (AntiChain a13 : new AntiChainInterval(r1,r2))
				for (AntiChain a23 : baseSpace.omicron(r1, a12.meet(a13)))
					for (AntiChain a1 : new AntiChainInterval(a12.join(a13),r2))
						for (AntiChain a2 : new AntiChainInterval(a12.join(a23),r2)) {
//							AntiChain r = new AntiChain(r2);
//							r.removeAll(a1.join(a2));
							res  = res.add(BigInteger.valueOf(new AntiChainInterval(r2.iota(a1.join(a2)).join(a13).join(a23),r2).latticeSize()));
						}
		return res;
	}
	/**
	 * compute third order PC for the amf r1, r2 in [0,\{N U K\}]
	 * @pre N and K are disjoint
	 * @pre r1 <= r2 in [0,{N}]
	 * @param r1
	 * @param r2
	 * @param N
	 * @param K
	 * @return the value of the coefficient
	 */
	public static ArrayList<AntiChain[]> PatricksCoefficient3(AntiChain r1, AntiChain r2, SmallBasicSet N, SmallBasicSet K) {
		AntiChainInterval baseSpace = new AntiChainInterval(AntiChain.emptyFunction(), AntiChain.oneSetFunction(N));
		ArrayList<AntiChain[]> res = new ArrayList<AntiChain[]>();
		for (AntiChain a12 : new AntiChainInterval(r1,r2))
			for (AntiChain a13 : new AntiChainInterval(r1,r2))
				for (AntiChain a23 : baseSpace.omicron(r1, a12.meet(a13)))
					for (AntiChain a1 : new AntiChainInterval(a12.join(a13),r2))
						for (AntiChain a2 : new AntiChainInterval(a12.join(a23),r2)) {
							AntiChain r = new AntiChain(r2);
							r.removeAll(a1.join(a2));
							for(AntiChain a3 : new AntiChainInterval(r.join(a13).join(a23),r2)) {
								res.add(new AntiChain[6]);
								AntiChain[] tres = res.get(res.size()-1);
								tres[0] = a12;
								tres[1] = a13;
								tres[2] = a23;
								tres[3] = a1;
								tres[4] = a2;
								tres[5] = a3;	
							}
						}
		return res;
	}
	
	/**
	 * far more sophisticated version of PatricksCoefficient
	 * @param r1
	 * @param r2
	 * @return
	 */
	public static BigInteger PatricksCoefficient(AntiChain s1, AntiChain s2, AntiChain t1, AntiChain t2, AntiChain r1, AntiChain r2) {
		// trivial case, no solutions unless r1 <= r2
		if (!r1.le(r2)) return BigInteger.ZERO;
		// trivial case, one solution if r1 == r2
		if (r1.equals(r2)) return BigInteger.ONE;
		BigInteger res = BigInteger.ZERO;
		// treat the case of empty functions separately (most function in AntiChain and AntiChainInterval do not apply)
		if (r1.isEmpty()) {
			if (r2.isEmpty()) return BigInteger.ONE; // (empty, empty)
			return BigInteger.ONE.add(BigInteger.ONE); // (empty, r2), (r2,empty)
		}
		long count = CountConnected(s1, s2, t1, t2,graph(r1,r2.minus(r1)));
		if (count < 0) return BigInteger.ZERO;
		return BigInteger.valueOf(1<<count);
	}

	public static SortedSet<AntiChain> getConnectedComponents(AntiChain modulo, AntiChain f) {
		SortedMap<SmallBasicSet, SortedSet<SmallBasicSet>> c = transitiveClosure(graph(modulo, f));
		SortedSet<AntiChain> ret = new TreeSet<AntiChain>();
		for (SmallBasicSet b : c.keySet()) {
			ret.add(new AntiChain(c.get(b)));
		}
		return ret;
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
	
	private static long CountConnected(AntiChain s1, AntiChain s2, AntiChain t1, AntiChain t2,
			SortedMap<SmallBasicSet, SortedSet<SmallBasicSet>> g) {
		SortedSet<SmallBasicSet> had = new TreeSet<SmallBasicSet>();
		long ret = 0;
		boolean[] t12 = new boolean[2];
		for (SmallBasicSet n : g.keySet()) {
			if (!had.contains(n)) {
				had.add(n);
				t12[0] = false;
				t12[1] = false;
				doNode(t1,t2,t12,g,n,had);
				if (t12[0] && t12[1]) return -1L;
				else if (!t12[0] && !t12[1]) ret++;	
			}
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

	private static SortedMap<SmallBasicSet, SortedSet<SmallBasicSet>> 
	transitiveClosure(SortedMap<SmallBasicSet, SortedSet<SmallBasicSet>> g) {
		SortedMap<SmallBasicSet, SortedSet<SmallBasicSet>> ret = new TreeMap<SmallBasicSet, SortedSet<SmallBasicSet>>();
		SortedSet<SmallBasicSet> had = new TreeSet<SmallBasicSet>();
		
		for (SmallBasicSet b : g.keySet()) {
			if (!had.contains(b)) {
				SortedSet<SmallBasicSet> bSet = new TreeSet<SmallBasicSet>();
				had.add(b);
				ret.put(b, bSet);
				closeNode(g,b,bSet,had);
			}
		}
		
		return ret;
	}

	private static void doNode(AntiChain t1, AntiChain t2, boolean[] seen, 
			SortedMap<SmallBasicSet, SortedSet<SmallBasicSet>> g, SmallBasicSet n, SortedSet<SmallBasicSet> had) {
		for (SmallBasicSet m : g.get(n)) {
			if (had.add(m)) {
				if (AntiChain.oneSetFunction(m).le(t1) && !AntiChain.oneSetFunction(m).le(t2)) {
					seen[0] = true;
				}
				if (!AntiChain.oneSetFunction(m).le(t1) && AntiChain.oneSetFunction(m).le(t2)) {
					seen[1] = true;
				}
				doNode(t1,t2,seen,g,m,had);
			}
		}		
	}

	private static void doNode(SortedMap<SmallBasicSet, SortedSet<SmallBasicSet>> g, SmallBasicSet n, SortedSet<SmallBasicSet> had) {
		for (SmallBasicSet m : g.get(n)) {
			if (had.add(m)) doNode(g,m,had);
		}
	}
	private static void closeNode(SortedMap<SmallBasicSet, SortedSet<SmallBasicSet>> g, SmallBasicSet b, Set<SmallBasicSet> bSet, Set<SmallBasicSet> had) {
		for (SmallBasicSet m : g.get(b)) {
			bSet.add(m);
			if (had.add(m)) closeNode(g,m,bSet,had);
		}
	}
	
	/**
	 * compute AMF(n+3) by expansion on the 3-dimensional hypercube
	 * @pre n > 3
	 * @param n
	 * @param m
	 * @return
	 */
	public static BigInteger AMFCubeSize(int n) {
		BigInteger result = BigInteger.ZERO;
		ArrayList<AntiChain> function = buildFunctionList(n);
		SortedMap<AntiChain, Integer> inverseFunction = invert(function);
		ArrayList<BigInteger> leftIntervalSize = computeLeftSizes(function);
		ArrayList<BigInteger> rightIntervalSize = computeRightSizes(function, n);
		for (int i1=0;i1<function.size();i1++) {
			AntiChain b1 = function.get(i1);
			for (int i2=0;i2<function.size();i2++) {
				AntiChain b2 = function.get(i2);
				for (int i3=0;i3<function.size();i3++) {
					AntiChain b3 = function.get(i3);
					for (AntiChain a1 : new AntiChainInterval(AntiChain.emptyFunction(),b2.meet(b3)))
						for (AntiChain a2 : new AntiChainInterval(AntiChain.emptyFunction(),b3.meet(b1)))
							for (AntiChain a3 : new AntiChainInterval(AntiChain.emptyFunction(),b1.meet(b2)))
								result = result.add(rightIntervalSize.get(inverseFunction.get(b1.join(b2).join(b3))).multiply(
										leftIntervalSize.get(inverseFunction.get(a1.meet(a2).join(a3)))));
				}
			}
		}				
		return result;
	}
			
	private static	ArrayList<AntiChain> buildFunctionList(int n) {
		ArrayList<AntiChain> result = new ArrayList<AntiChain>();
		for (AntiChain f : AntiChainInterval.fullSpace(n)) result.add(f);
		return result;
	}
	
	private static SortedMap<AntiChain,Integer> invert(ArrayList<AntiChain> l) {
		SortedMap<AntiChain, Integer> result = new TreeMap<AntiChain, Integer>();
		for (int i=0;i<l.size();i++) result.put(l.get(i), i);
		return result;
	}
	
	private static ArrayList<BigInteger> computeLeftSizes(ArrayList<AntiChain> l) {
		ArrayList<BigInteger> result = new ArrayList<BigInteger>();
		for (int i=0;i<l.size();i++) result.add(BigInteger.valueOf(new AntiChainInterval(AntiChain.emptyFunction(),l.get(i)).latticeSize()));
		return result;		
	}

	private static ArrayList<BigInteger> computeRightSizes(ArrayList<AntiChain> l, int n) {
		ArrayList<BigInteger> result = new ArrayList<BigInteger>();
		for (int i=0;i<l.size();i++) result.add(BigInteger.valueOf(new AntiChainInterval(l.get(i),AntiChainInterval.fullSpace(n).getTop()).latticeSize()));
		return result;		
	}
	
	/**
	 * coefficient in the expansion of AMF(n+k) where n is at least the dimension of r1 and r2
	 * and N = {1,..,n}
	 * AMF(n + k) = sum for r1 <= r2 of P(r1,r2,k)*|[0,r1]||[r1,{N}]|
	 * 
	 *  The coefficient is given by (K = {1,..,k}
	 *  P(r1,r2,k) = sum for all k-tuples (si in [r1,r2]|i in K) of |[si x {i}, si x {K\i}]| 
	 * @param r1
	 * @param r2
	 * @param k
	 * @return
	 */
	public static BigInteger PatricksCoefficient(AntiChain r1, AntiChain r2, int k) {
		AntiChain[] beta = new AntiChain[k];
		AntiChain[] interval = new AntiChainInterval(r1,r2).toArray();
		return doCoefficient(interval,r1,r2,beta,0,k);
	}
	private static BigInteger doCoefficient(AntiChain[] interval,AntiChain r1, AntiChain r2,
			AntiChain[] beta, int i, int k) {
		BigInteger res = BigInteger.ZERO;
		if (i == k-1) {
			AntiChain rest = new AntiChain(r2);
//			System.out.print(rest + "->" );
//			System.out.print("||");
//			for (AntiChain sx : beta) System.out.print(", " + sx);
//			System.out.print("||");
			for (int j =0;j<i;j++) rest.removeAll(beta[j]);
//			System.out.println(rest);
//			System.out.println("Result " + r1 + " " + r2 + " ");
			int line = 1;
			for (AntiChain b : new AntiChainInterval(r1.join(rest),r2)) {
				beta[i] = b;
				AntiChain[] alfa = new AntiChain[k];
//				System.out.print((line++) + " ||");
//				for (AntiChain sx : beta) System.out.print(", " + sx);
//				System.out.println("||");				
				res = res.add(doCoefficient(r1,r2,beta,alfa,0,k));
			}
			return res;
		}
		for (AntiChain b : interval) {
			beta[i] = b;
			res = res.add(doCoefficient(interval,r1,r2,beta,i+1,k));
		}
		return res;
	}
	private static BigInteger doCoefficient(AntiChain r1, AntiChain r2,
			AntiChain[] beta, AntiChain[] alfa, int i, int k) {
		if (k != 3) {System.out.println("I cannot do anything but k==3"); System.exit(-1);}
		BigInteger res = BigInteger.ZERO;
		int line =1;
		if (i == k-1) {
//			System.out.print("  a Result " + r1 + " " + r2 + " beta ");
//			System.out.print(" ||");
//			for (AntiChain sx : beta) System.out.print(", " + sx);
//			System.out.println("||");				
			AntiChain rest = new AntiChain(alfa[0]);
			for (int l = 1;l<i;l++) rest = rest.meet(alfa[l]);
			int ibefore = (i==0?k-1:i-1);
			int iafter = (i==k-1?0:i+1);
//			System.out.print("  a " + (line++) + " Rest " + rest + " Omicron " + AntiChain.universeFunction((int) (r2.sp().size()+2)).omicron(r1,rest) + " ||");
//			for (AntiChain sx : alfa) System.out.print(", " + sx);
//			System.out.println("||" + new AntiChainInterval(r1,beta[ibefore].meet(beta[iafter]).omicron(r1, rest)));				
			return BigInteger.valueOf(new AntiChainInterval(r1,beta[ibefore].meet(beta[iafter]).omicron(r1, rest)).latticeSize());
		}
		int ibefore = (i==0?k-1:i-1);
		int iafter = (i==k-1?0:i+1);
		for (AntiChain a : new AntiChainInterval(r1,beta[ibefore].meet(beta[iafter]))) {
			alfa[i] = a;
			res = res.add(doCoefficient(r1,r2,beta,alfa,i+1,k));
		}
		return res;
	}
	
	

	public static Iterator<AntiChain[]> multiSetCoverDual(final AntiChain r, final int numberOfComponents) {
		return new Iterator<AntiChain[]>() {

			SmallBasicSet N = r.sp();
			AntiChain dual = r.dual(N);
			Iterator<AntiChain[]> mscDual = multiSetCover(dual,numberOfComponents);
			
			AntiChain[] current = new AntiChain[numberOfComponents];

			@Override
			public boolean hasNext() {
				// TODO Auto-generated method stub
				return mscDual.hasNext();
			}

			@Override
			public AntiChain[] next() {
				AntiChain[] a = mscDual.next();
				for (int c = 0;c < current.length;c++) {
					current[c] = a[c].dual(N);
				}
				return current;
			}

			@Override
			public void remove() {
				// TODO Auto-generated method stub

			}

		};
	}
	
	/**
	 * assign the sets in AntiChain r to a number of components such that
	 * each set is assigned to at least one component
	 * 
	 * @param r
	 * @param numberOfComponents
	 * @return and array representing the assignment
	 */
	public static Iterator<AntiChain[]> multiSetCover(final AntiChain r, final int numberOfComponents) {
		return new Iterator<AntiChain[]>() {
		
		final Iterator<int[]> msc = multiSetCover(r.size(),numberOfComponents);
		AntiChain[] current = new AntiChain[numberOfComponents];
		SmallBasicSet[] source = new SmallBasicSet[r.size()];
		
		{
			source = r.toArray(source);
		}

		@Override
		public boolean hasNext() {
			// TODO Auto-generated method stub
			return msc.hasNext();
		}

		@Override
		public AntiChain[] next() {
			for (int c =0;c<current.length;c++) current[c] = AntiChain.emptyFunction();
			final int[] assignment = msc.next();
			for (int i=0;i<assignment.length;i++) {
				int a = assignment[i];
				int c = 0;
				while (a > 0) {
					if (a%2 != 0) 
						current[c].
						add(source[i]);
					a = a>>1;
					c++;
				}
			}
			return current;
		}

		@Override
		public void remove() {
			// TODO Auto-generated method stub
			
		}
		};
		
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
	
	
	private static Iterator<AntiChain> threeSets(final AntiChain rp, SmallBasicSet n, final SmallBasicSet c) {

		
		return new Iterator<AntiChain>() {
			
			AntiChain[] sc = new AntiChain[(int) c.size()];
			AntiChain b;
			int p = 0;
			private boolean notDone;
			
			{
				for (int i : c) sc[p++] = AntiChain.singletonFunction(i);
				b = rp.times(sc[0]);
				notDone = rp.isEmpty();
			}
			
			@Override
			public boolean hasNext() {
				return notDone;
			}	

			@Override
			public AntiChain next() {
				return null;
			}

			@Override
			public void remove() {
				// TODO Auto-generated method stub
				
			}
		};
			
		
	}
	
	/**
	 * Solve the equation chi.meet(alfa).equals(tau) 
	 * @param alfa
	 * @param tau
	 * @return
	 */
	static public AntiChainInterval solveMeetEquation(AntiChain alfa, AntiChain tau) {
		if (tau.le(alfa)) {
			return new AntiChainInterval(tau, tau.omicron(alfa));
		}
		else return new AntiChainInterval(AntiChain.emptySetFunction(), AntiChain.emptyFunction()); // empty interval
	}
	/**
	 * Solve the equation chi.join(alfa).equals(tau) 
	 * @param alfa
	 * @param tau
	 * @return
	 */
	static public AntiChainInterval solveJoinEquation(AntiChain alfa, AntiChain tau) {
		if (tau.ge(alfa)) {
			return new AntiChainInterval(tau.iota(alfa),tau);
		}
		else return new AntiChainInterval(AntiChain.emptySetFunction(), AntiChain.emptyFunction()); // empty interval
	}
	/**
	 * Solve the simultaneous equations 
	 * 		chi.join(alfa1).equals(tau1)
	 * 		chi.meet(alfa2).equals(tau2)
	 * @param alfa1
	 * @param alfa2
	 * @param tau1
	 * @param tau2 
	 * @return
	 */
	static public AntiChainInterval solveJoinMeetEquation(
			AntiChain alfa1, AntiChain alfa2, 
			AntiChain tau1, AntiChain tau2) {
		return solveJoinEquation(alfa1, tau1).intersect(solveMeetEquation(alfa2, tau2));
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

	/**
	 * compute an intervaliterator working by dynamic programming on the last element of the span
	 * allowing a starting point: the first element to be returned
	 * @pre interval is not empty
	 * @ore from is in the interval
	 * @param interval
	 * @return
	 */
	private static Iterator<AntiChain> fastNonEmptyIterator(final AntiChainInterval interval, final AntiChain from) {
		SmallBasicSet span = interval.getTop().sp();
		if (span.isEmpty()) return new Iterator<AntiChain>() {
			// empty span interval case. At most two elements
			private AntiChain current = from;

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
			private AntiChain current = from;
			private AntiChain[] alfaBottom = interval.getBottom().reduce(span);
			private AntiChain[] alfaTop = interval.getTop().reduce(span);

			private AntiChain[] alfa = new AntiChain[2];
			private Iterator<AntiChain>[] iterator = new Iterator[2];
			{ 
				AntiChain[] alfaFrom = from.reduce(span);
				iterator[0] = fastIterator(new AntiChainInterval(alfaBottom[0],alfaTop[0]),alfaFrom[0]);
				alfa[0] = iterator[0].next();
				iterator[1] = fastIterator(new AntiChainInterval(alfaBottom[1],alfa[0].meet(alfaTop[1])),alfaFrom[1]);
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
	 * compute an interval iterator working by dynamic programming on the last element of the span
	 * it allows a starting point 'from' which may be the last element where a previous iterator stopped
	 * the starting point will be the first element returned
	 * the order is the natural lexicographic order in a = a0 + a1 x {n}
	 * Static version of the equivalent fastIterator in AntiChainInterval
	 * @param interval, from
	 * @return
	 */
	public static Iterator<AntiChain> fastIterator(final AntiChainInterval interval, final AntiChain from) {
		if (interval.getBottom().le(interval.getTop()) && from.le(interval.getTop()) && from.ge(interval.getBottom())) 
			return fastNonEmptyIterator(interval, from);
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
