package amfsmall;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import auxiliary.Pair;

/**
 * More efficient representation for AntiChains.
 */
public class SmallAntiChain implements Iterable<SmallBasicSet>, Comparable<SmallAntiChain>, LatticeElement {

	private BitSet theAntiChain = new BitSet();
	private SmallBasicSet universe = SmallBasicSet.universe();
	
	/********************************************************
	 * Constructors											*
	 ********************************************************/
	
	/**
	 * Create an antichain based on set of SmallBasicSets
	 */
	public SmallAntiChain(Collection<SmallBasicSet> C) {
		for(SmallBasicSet s : C) 
			theAntiChain.set(s.toIntRepresentation());
	}
	
	/**
	 * Create an antichain with another antichain.
	 */
	public SmallAntiChain(SmallAntiChain ac) {
		theAntiChain = (BitSet) ac.theAntiChain.clone();
		this.setUniverse(ac.getUniverse());
	}
	
	/**
	 * Create an antichain based on a long.
	 */
	public SmallAntiChain(long[] l) {
		theAntiChain = BitSet.valueOf(l);
	}

	/********************************************************
	 * Basic antichains	& other static methods				*
	 ********************************************************/

	private static SmallAntiChain emptyAntiChain = new SmallAntiChain();
	private static SmallAntiChain emptySetAntiChain = new SmallAntiChain();
		static {emptySetAntiChain.theAntiChain.set(1);}
	
	public static SmallAntiChain emptyAntiChain() {
		return new SmallAntiChain(emptyAntiChain);
	}
	
	public static SmallAntiChain emptySetAntiChain() {
		return new SmallAntiChain(emptySetAntiChain);
	}
	
	public static SmallAntiChain singletonAntiChain(int l) {
		if(l == 0)
			return emptySetAntiChain();
		SmallAntiChain result = new SmallAntiChain();
		result.theAntiChain.set((int) (1L << (l-1)));
		return result;
	}
	
	public static SmallAntiChain universeAntiChain(int n) {
		SmallAntiChain result = new SmallAntiChain();
		result.theAntiChain.set(((int) (1L << n) - 1));
		result.setUniverse(SmallBasicSet.universe(n));
		return result;
	}
	
	public static SmallAntiChain oneSetAntiChain(SmallBasicSet x) {
		SmallAntiChain result = new SmallAntiChain();
		result.theAntiChain.set(x.toIntRepresentation());
		return result;
	}
	
	public static SmallAntiChain emptyAntiChain(SmallBasicSet u) {
		SmallAntiChain result = emptyAntiChain();
		result.setUniverse(u);
		return result;
	}
	
	/**
	 * Decode the BigInteger b into an antichain
	 * @param 	b 
	 * 			The encoding for an antichain
	 * @return	The SmallAntiChain from the given encoding
	 */
	public static SmallAntiChain decode(BigInteger b) {
		SmallAntiChain result = new SmallAntiChain();
		result.theAntiChain = BitSet.valueOf(b.toByteArray());
		return result;
	}
	
	/********************************************************
	 * Utilities											*
	 ********************************************************/
	
	/**
	 * Create an empty antichain
	 */
	public SmallAntiChain() {
		theAntiChain = new BitSet(1);
	}
	
	private void setUniverse(SmallBasicSet s) {
		universe = s;
	}
	
	protected SmallBasicSet getUniverse() {
		return universe;
	}
	
	/**
	 * Add a set to this antichain
	 * 
	 * @param 	s
	 * 			a set that's in the universe of this antichain
	 * @return	true if set was added, false when s was no subset of this.getUniverse()
	 */
	public boolean add(SmallBasicSet s) {
		if(universe.hasAsSubset(s)) {
			theAntiChain.set(s.toIntRepresentation());
			return true;
		}
		return false;
	}
	
	/**
	 * Remove a set from this antichain
	 * 
	 * @param 	s
	 * 			the set to be removed from this antichain
	 * @return 	true if set was succesfully deleted, false when this antichain didn't change
	 */
	public boolean remove(SmallBasicSet s) {
		int temp = s.toIntRepresentation();
		if(!theAntiChain.get(temp))
			return false;
		theAntiChain.set(temp, false);
		return true;
	}
	
	/**
	 * Remove all sets in the collection from this antichain
	 * 
	 * @param 	c
	 * 			the collection of sets to be removed
	 * @return	true if something has been deleted, false otherwise
	 */
	public boolean removeAll(SmallAntiChain c) {
		boolean result = false;
		for(int i = c.theAntiChain.nextSetBit(0); i >= 0; i = c.theAntiChain.nextSetBit(i+1)) {
			if(remove(new SmallBasicSet(i)) && !result)
				result = true;
		}
		return result;
	}
	
	/**
	 * Add a set to this antichain, so that the resulting antichain is 
	 * the supremum of the union of this and {x}.
	 * 
	 * @param 	x
	 * 			The set to be added
	 */
	private void addConditionally(SmallBasicSet x) {
		SmallBasicSet a;
		for(int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1)) {
			a = new SmallBasicSet(i);
			if(a.hasAsSubset(x)) return;
			if(x.hasAsSubset(a)) theAntiChain.set(i, false);
		}
		theAntiChain.set(x.toIntRepresentation());
	}

	/**
	 * Add elements of x that are not contained in this
	 * remove elements of this that are contained in a set in x
	 * 
	 * @param 	x
	 * 			The antichain to be added
	 */
	private void addConditionallyAll(SmallAntiChain x) {
		for(int i = x.theAntiChain.nextSetBit(0); i >= 0; i = x.theAntiChain.nextSetBit(i+1)) {
			addConditionally(new SmallBasicSet(i));
		}
	}
	
	/**
	 * Compute the union of all sets in this SmallAntiChain
	 * 
	 * @return a SmallBasicSet containing the union of all SmallBasicSets in this
	 */
	public SmallBasicSet sp() {
		SmallBasicSet span = SmallBasicSet.emptySet();
		for(int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1)) {
			span = span.union(new SmallBasicSet(i));
		}
		return span;
	}
	
	/**
	 * The projection on a specific dimension described by a SmallBasicSet
	 * 
	 * @param 	b
	 * 			The set to project onto
	 * @return 	the SmallAntiChain containing only intersections of this with b
	 */
	public SmallAntiChain project(SmallBasicSet b) {
		SmallAntiChain res = SmallAntiChain.emptyAntiChain(this.getUniverse());
		for (int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1)) {
			res.addConditionally(new SmallBasicSet(i).intersection(b));
		}
		return res;
	}
	
	//TODO: useful?
/*	public SmallAntiChain sup() {
		SmallAntiChain result = new SmallAntiChain(this);
		for(int i = result.theAntiChain.length(); (i = result.theAntiChain.previousSetBit(i-1)) >= 0; ) {
			if(Integer.bitCount(i) == 1) {
				//for(int j = )
			}
		}
		return result;
	}*/
	
	/**
	 * Check whether this SmallAntiChain is greater or equal to the one with only x as an element
	 * 
	 * @param 	x
	 * 			The one set of the other antichain
	 * @return 	true iff x is contained in at least one set in this
	 */
	public boolean ge(SmallBasicSet x) {
		for(int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1)) {
			if(new SmallBasicSet(i).hasAsSubset(x))
				return true;
		}
		return false;
	}
	
	/**
	 * Check whether this SmallAntiChain is greater than other
	 * 
	 * @param 	other
	 * 			The antichain to comare with
	 * @return 	true iff all sets in other are contained in at least one set in this
	 */
	public boolean gt(SmallAntiChain x) {
		return ge(x) && !equals(x);
	}
	
	/**
	 * Check whether this SmallAntiChain is less than other
	 * 
	 * @param 	other
	 * 			The antichain to compare with
	 * @return 	true iff all sets in this are contained in at least one set in other and other != this
	 */
	public boolean lt(SmallAntiChain x) {
		return le(x) && !equals(x);
	}
	
	/**
	 * Minus for antichains: starting from the sets in this, remove all subsets of sets in f 
	 * 
	 * @param 	f
	 * 			The antichain to substract from this
	 * @return 	a SmallAntiChain containing all the sets in this 
	 * 			which are not subsets of a set in f
	 */
	public SmallAntiChain minus(SmallAntiChain f) {
		SmallAntiChain res = SmallAntiChain.emptyAntiChain(getUniverse());
		for (int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1)) {
			SmallBasicSet x = new SmallBasicSet(i);
			boolean found = false;
			for (int j = theAntiChain.nextSetBit(0); j >= 0; j = theAntiChain.nextSetBit(j+1)) 
				if (new SmallBasicSet(j).hasAsSubset(x)) found = true;
			if (!found) res.add(x);
		}
		return res;
	}
	
	/**
	 * Find the minimal representation of this SmallAntiChain
	 * under permutation of the elements
	 * 
	 * @return the representant with with the smallest encoding
	 */
	public SmallAntiChain standard() {
		SmallBasicSet span = sp();
		int map[] = new int[(int) span.size()];
		int inverseMap[] = new int[span.maximum() + 1];
		int pos = 0;
		for (int i:span) {
			map[pos] = i;
			inverseMap[i] = pos++;
		}
		Iterator<Pair<int[], int[]>> permutations = MappingPermutation.getIterator(map,inverseMap,map.length);
		SmallAntiChain best = this;
		BigInteger bestCode = this.encode();
		while (permutations.hasNext()) {
			SmallAntiChain kand = this.map(permutations.next().snd);
			BigInteger code = kand.encode();
			if (code.compareTo(bestCode) < 0) {
				best = kand;
				bestCode = code;
			}
		}
		return best;	
	}
	
	/**
	 * Find the symmetry group of this SmallAntiChain 
	 * 
	 * @return	the set of permutations of the elements 
	 * 			under which this antichain is invariant
	 */
	public Set<int[]> symmetryGroup() {
		Set<int[]> res = new HashSet<int[]>();
		SmallBasicSet span = sp();
		int map[] = new int[(int) span.size()];
		int inverseMap[] = new int[span.maximum() + 1];
		int pos = 0;
		for (int i:span) {
			map[pos] = i;
			inverseMap[i] = pos++;
		}
		Iterator<Pair<int[], int[]>> permutations = MappingPermutation.getIterator(map,inverseMap,map.length);
		BigInteger theCode = this.encode();
		while (permutations.hasNext()) {
			int[] p = permutations.next().snd;
			SmallAntiChain kand = this.map(p);
			BigInteger code = kand.encode();
			if (code.compareTo(theCode) == 0) {
				res.add(Arrays.copyOf(p, p.length));
			}
		}
		return res;		
	}
	
	/**
	 * ???
	 *
	 * @param 	inverse
	 * 			???
	 * @return	???
	 */
	//TODO: complete comments
	public SmallAntiChain map(int[] inverse) {
		SmallAntiChain res = SmallAntiChain.emptyAntiChain(getUniverse());
		for(int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1)) {
			res.add(new SmallBasicSet(i).map(inverse));
		}
		return res;
	}
	
	/**
	 * Encode this SmallAntiChain to a BigInteger
	 * 
	 * @return the binary encoding of this antichain
	 */
	public BigInteger encode() {
		BigInteger result = BigInteger.ZERO;
		for(int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1))
			result = result.setBit(i);
		return result;
	}
	
	/**
	 * Reduce this with respect to the given span
	 * to the unambiguous this = result[0].join(result[1].times({[m]})
	 * with result[0].ge(result[1]) and m == span.maximum() 
	 * 
	 * @param 	span
	 * 			The span to reduce with
	 * @return	???
	 */
	//TODO: complete comments
	public SmallAntiChain[] reduce(SmallBasicSet span) {
		int m = span.maximum();
		SmallBasicSet p = span.minus(m);
		SmallAntiChain[] result = new SmallAntiChain[2];
		result[0] = this.project(p);
		SmallAntiChain a1 = new SmallAntiChain(this);
		a1.removeAll(result[0]);
		result[1] = a1.project(p);
		return result;
	}
	
	/**
	 * omicron(tau, alfa) is the largest antichain chi le this for which chi.meet(alfa).equals(tau)
	 * 
	 * use omicron(alfa) in stead
	 * 
	 * @param 	tau
	 * 			???
	 * @param 	alfa
	 * 			???
	 * @pre 	tau.le(alfa)
	 * @return	???
	 */
	//TODO: complete comments
	@Deprecated
	public SmallAntiChain omicron(SmallAntiChain tau, SmallAntiChain alfa) {
		if (tau.equals(SmallAntiChain.emptyAntiChain())) {
			if (alfa.equals(SmallAntiChain.emptyAntiChain())) return this;
			else return SmallAntiChain.emptyAntiChain();
		}
		SmallAntiChain res = SmallAntiChain.emptyAntiChain(getUniverse());
		res.add(sp().minus(alfa.sp()));
		for (int i = alfa.theAntiChain.nextSetBit(0); i >= 0; i = alfa.theAntiChain.nextSetBit(i+1)) {
			SmallAntiChain current = SmallAntiChain.emptyAntiChain(getUniverse());
			current.add(new SmallBasicSet(i));
			res = (SmallAntiChain) res.times(current.meet(tau));
		}
		return (SmallAntiChain) res.meet(this);
	}
	
	/**
	 * omicron(alfa) is the largest SmallAntiChain chi for which chi.meet(alfa).equals(this)
	 * 
	 * @param 	alfa
	 * 			???
	 * @pre 	this.le(alfa)
	 * @return	
	 */
	//TODO: complete comments
	public SmallAntiChain omicron(SmallAntiChain alfa) {
		SmallAntiChain tauD = (SmallAntiChain) dual();
		tauD.removeAll((SmallAntiChain) alfa.dual());
		return (SmallAntiChain) tauD.dual();		
	}
	
	/********************************************************
	 * Implementing methods									*
	 ********************************************************/
	
	/**
	 * Sum of two SmallAntiChains is the antichain that contains all sets in the union except those that
	 * are a subset of another set in this union
	 * 
	 * @param 	e
	 * 			The antichain to be joined
	 * @return 	sup(this union e)
	 */
	@Override
	public LatticeElement join(LatticeElement e) {
		SmallAntiChain result = new SmallAntiChain(this);
		//TODO: optie? result.theAntiChain.or(((SmallAntiChain) e).theAntiChain); ...
		result.addConditionallyAll((SmallAntiChain) e);
		return result;
	}

	/**
	 * Dot product of two SmallAntiChains is the function that contains all 
	 * intersections of a set in this and a set in other
	 * 
	 * @param 	e
	 * 			The antichain to be met
	 * @return 	{A^B|A in this, B in e}
	 */
	@Override
	public LatticeElement meet(LatticeElement e) {
		SmallAntiChain result = SmallAntiChain.emptyAntiChain(this.getUniverse());
		for(int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1))
			for(int j = ((SmallAntiChain) e).theAntiChain.nextSetBit(0); j >= 0; j = ((SmallAntiChain) e).theAntiChain.nextSetBit(j+1)) 
				result.addConditionally(new SmallBasicSet(j).intersection(new SmallBasicSet(i)));
		return result;
	}

	/**
	 * Generalised product of two AntiChains is the largest function K 
	 * such that P_sp(this)(K) <= this and P_sp(e)(K) <= e and K <= {sp(this.join(e))}
	 * 
	 * @param 	e
	 * 			The other antichain to apply the binary operation
	 * @effect 	max{K|P_sp(this)(K) <= this and P_sp(other)(K) <= e}
	 */
	@Override
	public LatticeElement times(LatticeElement e) {
		SmallAntiChain result = new SmallAntiChain(this);
		if(theAntiChain.equals(SmallAntiChain.emptyAntiChain) || 
				((SmallAntiChain) e).theAntiChain.equals(SmallAntiChain.emptyAntiChain))
			return SmallAntiChain.emptyAntiChain();
		if(theAntiChain.equals(SmallAntiChain.emptySetAntiChain))
			return new SmallAntiChain((AntiChain) e);
		if(((SmallAntiChain) e).theAntiChain.equals(SmallAntiChain.emptySetAntiChain))
			return new SmallAntiChain(this);
		
		SmallBasicSet x,y;
		SmallBasicSet spthis = this.sp(), spe = ((SmallAntiChain) e).sp();
		for(int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1))
			for(int j = ((SmallAntiChain) e).theAntiChain.nextSetBit(0); j >= 0; j = ((SmallAntiChain) e).theAntiChain.nextSetBit(j+1)) {
				x = new SmallBasicSet(i);
				y = new SmallBasicSet(j);
				result.addConditionally(x.minus(spe).union(y.minus(spthis)).union(x.intersection(y)));
			}
		return result;
	}

	/**
	 * The dual of this SmallAntiChain in the lattice of antichains on its universe
	 * 
	 * @return 	sup{A|forall k in this : this.getUniverse()\k not in A}
	 */
	@Override
	public LatticeElement dual() {
		SmallAntiChain tres, result = SmallAntiChain.emptyAntiChain(this.getUniverse());
		result.add(this.getUniverse());
		SmallBasicSet max, a, k;
		for(int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1)) {
			tres = SmallAntiChain.emptyAntiChain(this.getUniverse());
			k = new SmallBasicSet(i);
			max = this.getUniverse().minus(k);
			for (int j = result.theAntiChain.nextSetBit(0); j >= 0; j = result.theAntiChain.nextSetBit(j+1)) {
				a = new SmallBasicSet(j);
				if (!a.hasAsSubset(max)) tres.addConditionally(a);
				else for (int x:max) tres.addConditionally(a.minus(x));
			}
			result = tres;
		}
		return result;
	}

	/**
	 * Check whether this SmallAntiChain is greater or equal than other
	 * 
	 * @param 	other
	 * 			The antichain to comare with
	 * @return 	true iff all sets in other are contained in at least one set in this
	 */
	@Override
	public boolean ge(LatticeElement e1) {
		for(int i = ((SmallAntiChain) e1).theAntiChain.nextSetBit(0); i >= 0; i = ((SmallAntiChain) e1).theAntiChain.nextSetBit(i+1)) {
			if(!ge(new SmallBasicSet(i)))
				return false;
		}
		return true;
	}

	/**
	 * Check whether this SmallAntiChain is less or equal than other
	 * 
	 * @param 	e1
	 * 			The antichain to compare with
	 * @return 	true iff all sets in this are contained in at least one set in other
	 */
	@Override
	public boolean le(LatticeElement e1) {
		boolean ok;
		for(int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1)) {
			ok = false;
			for(int j = ((SmallAntiChain) e1).theAntiChain.nextSetBit(0); j >= 0; j = ((SmallAntiChain) e1).theAntiChain.nextSetBit(j+1))
				if(new SmallBasicSet(j).hasAsSubset(new SmallBasicSet(i))) {
					ok = true;
					break;
				}
			if(!ok) return false;
		}
		return true;
	}

	/**
	 * Check wehter this SmallAntiChain equals the other
	 * 
	 * @param	e
	 * 			The antichain to comare with
	 * @return	true iff all sets in this are in all sets from the other
	 */
	@Override
	public boolean equals(LatticeElement e) {
		return theAntiChain.equals(((SmallAntiChain) e).theAntiChain);
	}

	/**
	 * A lexicographic comparison of SmallAntiChains
	 * 
	 * @param 	o 
	 * 			The SmallAntiChain to compare with
	 * @return 	< 0 if this is l.g. before o, 
	 * 			> 0 if this is l.g. after o, 
	 * 			= 0 otherwise
	 */
	@Override
	public int compareTo(SmallAntiChain o) {
		Iterator<SmallBasicSet> s1 = this.iterator();
		Iterator<SmallBasicSet> s2 = o.iterator();
		while (s1.hasNext() && s2.hasNext()) {
			int c = s1.next().compareTo(s2.next());
			if (c != 0) return c;
		}
		if (s1.hasNext()) return 1;
		else if (s2.hasNext()) return -1;
		else return 0;
	}

	/**
	 * Get an iterator to iterate over the sets in this antichain
	 * 
	 * @return	an iterator that iterates over the sets in the order
	 * 			that follows from the representation from Carl Salaets
	 */
	@Override
	public Iterator<SmallBasicSet> iterator() {
		return new Iterator<SmallBasicSet>() {
			
			private int current = 0;

			@Override
			public boolean hasNext() {
				return theAntiChain.nextSetBit(current + 1) >= 0;
			}

			@Override
			public SmallBasicSet next() {
				current = theAntiChain.nextSetBit(current + 1);
				return new SmallBasicSet(current);
			}

			@Override
			public void remove() {
				// throw new NotImplementedException();
			}
			
		};
	}
	
	/**
	 * produce a string for display
	 */
	@Override
	public String toString() {
		if (theAntiChain.isEmpty()) return "{}";
		String res = "{";
		for (int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1)) {
			res += new SmallBasicSet(i) + ",";
		}
		return res.substring(0, res.lastIndexOf(',')) + "}";
	}
	
	//TODO: delete
	public static void main(String[] args) {
		
	}
}