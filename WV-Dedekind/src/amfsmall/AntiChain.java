package amfsmall;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import auxiliary.Pair;

// import com.sun.tools.javac.code.Attribute.Array;
// import com.sun.tools.javac.util.Pair;

/**
 * The representation of an antimonotonic function as a set of SmallBasicSet
 * @invar the function does not contain any subset-superset pair |
 * 		isAntiMonotonic(this) 
 * @author u0003471
 *
 */
public class AntiChain extends TreeSet<SmallBasicSet> implements Comparable<AntiChain>, LatticeElement {

	private static final long serialVersionUID = 1L;
	public static final BigInteger EMPTYFUNCTIONENCODING = BigInteger.valueOf((1L << 8) - 1);
	public static final BigInteger FOURBITS = BigInteger.valueOf(15);
	
	/**
	 * the set of elements	
	 */
	private SmallBasicSet universe = SmallBasicSet.universe();

	/**
	 * @pre the Collection C represents an antimonotonic function |
	 * 			isAntiMonotonic(C)	
	 * @param C
	 */
	public AntiChain(Collection<SmallBasicSet> C) {
		super();
		this.addAll(C);
	}

	/**
	 * @pre the Collection C represents an antimonotonic function |
	 * 			isAntiMonotonic(C)	
	 * @param C
	 */
	public AntiChain(AntiChain f) {
		super();
		this.addAll(f);
		this.setUniverse(f.getUniverse());
	}

	/**
	 * AntiChain on the set N
	 * @param N
	 */
	public AntiChain(SmallBasicSet N) {
		super();
		setUniverse(N);
	}

	public void setUniverse(SmallBasicSet n) {
		universe = n;
	}
	
	public SmallBasicSet getUniverse() {
		return universe;
	}

	public AntiChain() {
		super();
	}

	/**
	 * Check whether this AntiChain is less or equal that other
	 * @param other
	 * @return true iff all sets in this are contained in at least one set in other
	 */
	public boolean le(AntiChain other) {
		for (SmallBasicSet A : this)
		{
			boolean Ok = false;
			for (SmallBasicSet B : other)
				if (B.hasAsSubset(A)) {
					Ok = true;
					break;
				}
			if (!Ok) return false;
		}
		return true;
	}
	
	/**
	 * Check whether this AntiChain is less than other
	 * @param other
	 * @return true iff all sets in this are contained in at least one set in other and other != this
	 */
	public boolean lt(AntiChain other) {
		return this.le(other) && !this.equals(other);
	}
	
	/**
	 * Check whether this AntiChain is greater or equal that other
	 * @param other
	 * @return true iff all sets in other are contained in at least one set in this
	 */
	public boolean ge(AntiChain other) {
		for (SmallBasicSet A : other)
		{
			//TODO: if (!ge(A)) return false;
			boolean Ok = false;
			for (SmallBasicSet B : this)
				if (B.hasAsSubset(A)) {
					Ok = true;
					break;
				}
			if (!Ok) return false;
		}
		return true;
	}
	
	/**
	 * Check whether this AntiChain is greater than other
	 * @param other
	 * @return true iff all sets in other are contained in at least one set in this
	 */
	public boolean gt(AntiChain other) {
		return this.ge(other) && !this.equals(other);
	}

	
	/**
	 * Check whether this AntiChain is greater or equal to the one with only x as an element
	 * @param x
	 * @return true iff x is contained in at least one set in this
	 */
	public boolean ge(SmallBasicSet x) {
		for (SmallBasicSet B : this)
			if (B.hasAsSubset(x)) {
				return true;
			}
		return false;
	}

	
	/**
	 * overwrite should not be necessary?
	 * @param amf
	 * @return
	 */
	public boolean equals(AntiChain amf) {
		if (size() != amf.size()) return false;
		for (SmallBasicSet b : this) {
			boolean found = false;
			for (SmallBasicSet a : amf)
				if (b.equals(a)) {
					found = true;
					break;
				}
			if (!found) return false;
		}
		return true;
	}
	
	/**
	 * Add X it is not contained in a set in this
	 * Remove any set in this contained in X
	 * @param X
	 */
	public void addConditionally(SmallBasicSet X) {
		HashSet<SmallBasicSet> badMembers = new HashSet<SmallBasicSet>();
		for (SmallBasicSet A : this) {
			if (A.hasAsSubset(X)) return;
			if (X.hasAsSubset(A)) badMembers.add(A);
		}
		removeAll(badMembers);
		add(X);
	}
	/**
	 * Add elements of B that are not contained in this
	 * remove elements of this that are contained in a set in B
	 * 
	 * @param X
	 */
	public void addConditionallyAll(Collection<SmallBasicSet> B) {
		for (SmallBasicSet A : B) addConditionally(A);
	}
	/**
	 * Sum of two AntiChains is the function that contains all sets in the union except those that
	 * are a subset of another set in this union
	 * @param B
	 * @return sup(this union B)
	 */
	public AntiChain plus(AntiChain B) {
		AntiChain res = new AntiChain(this);
		res.addConditionallyAll(B);
		return res;
	}
	
	/**
	 * Generalised product of two AntiChains is the largest function K 
	 * such that P_sp(this)(K) <= this and P_sp(other)(K) <= other and K <= {sp(this.plus(other))}
	 * @param B
	 * @effect times(sp(this),sp(other),other)
	 */
	public AntiChain times(AntiChain other) {
		return times(this.sp(),other.sp(),other);
	}
	
	/**
	 * Generalised product of two AntiChains is the largest function K 
	 * such that P_A(K) <= this and P_B(K) <= other and K <= {AUB} 
	 * @param A, B, other
	 * @return max{K|P_A(K) <= this and P_B(K) <= other}
	 */
	public AntiChain times(SmallBasicSet A, SmallBasicSet B, AntiChain other) {
		// a x {} = {} x a = a !!!!!!!!!!!!!!!!!
		if (isEmpty()) return new AntiChain(other); // may not have been tested
		else if (other.isEmpty()) return new AntiChain(this); //!!! may not have been tested
		//TODO: isn't a x {} = {} x a = {} ?
		AntiChain res = new AntiChain(getUniverse());
		for (SmallBasicSet X : this)
			for (SmallBasicSet Y : other) {
				res.addConditionally(X.minus(B).union(Y.minus(A)).union(X.intersection(Y)));
			}
		return res;
	}
	/**
	 * Dotproduct of two AntiChains is the function that contains all 
	 * intersections of a set in this and a set in other
	 * @param B
	 * @return {A^B|A in this, B in other}
	 */
	public AntiChain dot(AntiChain other) {
		AntiChain res = new AntiChain(getUniverse());
		for (SmallBasicSet A : this)
			for (SmallBasicSet B : other) {
				res.addConditionally(B.intersection(A));
			}
		return res;
	}
	
	/**
	 * compute the union of all sets in this AntiChain
	 * @return a SmallBasicSet containing the union of all SmallBasicSets
	 */
	public SmallBasicSet sp() {
		SmallBasicSet span = SmallBasicSet.emptySet();
		for (SmallBasicSet b : this) {
			span = span.union(b);
		}
		return span;
	}
	
	/**
	 * The projection on a specific dimension described by a SmallBasicSet
	 * @param b
	 * @return the AntiChain containing only intersections of this with b
	 */
	public AntiChain project(SmallBasicSet b) {
		AntiChain res = new AntiChain(getUniverse());
		for (SmallBasicSet a : this) {
			res.addConditionally(a.intersection(b));
		}
		return res;
	}
	
	/**
	 * find the minimal representation of this antimonotonic function
	 * under permutation of the elements
	 * 
	 * @return the representant with with the smallest encoding
	 */
	public AntiChain standard() {
		SmallBasicSet span = sp();
		int map[] = new int[(int) span.size()];
		int inverseMap[] = new int[span.maximum() + 1];
		int pos = 0;
		for (int i:span) {
			map[pos] = i;
			inverseMap[i] = pos++;
		}
		Iterator<Pair<int[], int[]>> permutations = MappingPermutation.getIterator(map,inverseMap,map.length);
		AntiChain best = this;
		BigInteger bestCode = this.encode();
		while (permutations.hasNext()) {
			AntiChain kand = this.map(permutations.next().snd);
			BigInteger code = kand.encode();
			if (code.compareTo(bestCode) < 0) {
				best = kand;
				bestCode = code;
			}
		}
		return best;	
	}

	/**
	 * find the minimal representation of this antimonotonic function
	 * under the given set of permutation of the elements
	 * 
	 * @return the representant with with the smallest encoding
	 */
	public AntiChain standard(Set<int[]> permutations) {
		AntiChain best = this;
		BigInteger bestCode = this.encode();
		for (int[] p : permutations) {
			AntiChain kand = this.map(p);
			BigInteger code = kand.encode();
			if (code.compareTo(bestCode) < 0) {
				best = kand;
				bestCode = code;
			}
		}
		return best;	
	}
	
	/**
	 * find the symmetry group of this amf 
	 * this is the set of permutations of the elements under which this amf is invariant
	 * @return 
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
			AntiChain kand = this.map(p);
			BigInteger code = kand.encode();
			if (code.compareTo(theCode) == 0) {
				res.add(Arrays.copyOf(p, p.length));
			}
		}
		return res;		
	}
	
	public AntiChain map(int[] inverse) {
		AntiChain res = new AntiChain(getUniverse());
		for (SmallBasicSet s : this) {
			res.add(s.map(inverse));
		}
		return res;
	}

	/**
	 * encode this AMF to a BigInteger
	 * The sets are represented by sequences of fourbit patterns, 
	 * each pattern the binary representation of an element in the set
	 * sets are separated by four bit zero patterns
	 * @pre no element in a set in this is greater than 15
	 * @return the binary encoding of this amf
	 */
	public BigInteger encode() {
		BigInteger res = BigInteger.ZERO;
		if (this.isEmpty()) return EMPTYFUNCTIONENCODING;
		for (SmallBasicSet s : this) {
			res = res.shiftLeft(4);
			for (int i : s) {
				res = res.shiftLeft(4).or(BigInteger.valueOf(i));
			}
		}
		return res;
	}
	
	/**
	 * decode first part of the biginteger b into the set of AMF s
	 * if f is empty before the decoding, f will contain the amf represented by the encoding
	 * inverse of encode
	 * @param b
	 * @return the part of the biginteger b that is left
	 */
	public static BigInteger decode(AntiChain f, BigInteger b) {
		if (b.and(EMPTYFUNCTIONENCODING).equals(EMPTYFUNCTIONENCODING))
			return b.shiftRight(EMPTYFUNCTIONENCODING.bitLength());
		if (f.isEmpty()) {
			// adding unconditionally
			// read first byte of the set
			byte fourBits = b.and(FOURBITS).byteValue();
			b = b.shiftRight(4);
			do {
				SmallBasicSet s = SmallBasicSet.emptySet();
				while (fourBits != 0) {
					s = s.add(fourBits);
					fourBits = b.and(FOURBITS).byteValue();
					b = b.shiftRight(4);
				}
				// started from an empty set
				f.add(s);
				fourBits = b.and(FOURBITS).byteValue();
				b = b.shiftRight(4);			
			} while (fourBits != 0);
			return b;
		} else {
			// have to add conditionally
			// read first byte of the set
			byte fourBits = b.and(FOURBITS).byteValue();
			b = b.shiftLeft(4);
			do {
				SmallBasicSet s = SmallBasicSet.emptySet();
				while (fourBits != 0) {
					s.add(fourBits);
					fourBits = b.and(FOURBITS).byteValue();
					b = b.shiftLeft(4);
				}
				// set was not empty from the start
				f.addConditionally(s);
				fourBits = b.and(FOURBITS).byteValue();
				b = b.shiftLeft(4);			
			} while (fourBits != 0);
			return b;			
		}
			
	}

	/**
	 * decode the BigInteger b into an antimonotonic function
	 * @param b contains the code for the AMF, may contain more but this is not used
	 * @return
	 */
	public static AntiChain decode(BigInteger b) {
		AntiChain ret = new AntiChain();
		AntiChain.decode(ret, b);
		return ret;
	}
	
	/**
	 * minus for antimonotonic functions: starting from the sets in this, remove all subsets of sets in f 
	 * @param f
	 * @return an amf containing all the sets in this which are not subsets of a set in f
	 */
	public AntiChain minus(AntiChain f) {
		AntiChain res = new AntiChain(getUniverse());
		for (SmallBasicSet x : this) {
			boolean found = false;
			for (SmallBasicSet t : f) if (t.hasAsSubset(x)) found = true;
			if (!found) res.add(x);
		}
		return res;
	}

	/**
	 * a lexicographic comparison of AntiChains
	 * @param o the AntiChain to compare with
	 * @return <0 if this is l.g. before o, >0 if this is l.g. after o, 0 otherwise
	 */
	@Override
	public int compareTo(AntiChain o) {
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
	 * The dual of this amf in the lattice of amf's on N
	 * @param N
	 * @return sup{A|forall k in this : N\k not in A}
	 */
	public AntiChain dual(SmallBasicSet N) {
		AntiChain res = new AntiChain(N);
		res.add(N);
		for (SmallBasicSet k : this) {
			AntiChain tres = new AntiChain(N);
			SmallBasicSet max = N.minus(k);
			for (SmallBasicSet A : res) {
				if (!A.hasAsSubset(max)) tres.addConditionally(A);
				else for (int i:max) tres.addConditionally(A.minus(i));
			}
			res = tres;
		}
		return res;
	}

	@Override
	public AntiChain join(LatticeElement e) {
		return this.plus((AntiChain) e);
	}

	@Override
	public AntiChain meet(LatticeElement e) {
		return this.dot((AntiChain) e);
	}

	@Override
	public AntiChain times(LatticeElement e) {
		return this.times((AntiChain) e);
	}

	@Override
	public AntiChain dual() {
		return dual(getUniverse());
	}

	@Override
	public boolean ge(LatticeElement e1) {
		return this.ge((AntiChain) e1);
	}

	@Override
	public boolean le(LatticeElement e1) {
		return this.le((AntiChain) e1);
	}

	@Override
	public boolean equals(LatticeElement e) {
		return equals((AntiChain) e);
	}

	private static AntiChain emptySetFunction;
	static {
		emptySetFunction = new AntiChain();
		emptySetFunction.add(SmallBasicSet.emptySet());
	}
	private static AntiChain emptyFunction = new AntiChain();
	
	public static AntiChain emptySetFunction() {
		return new AntiChain(emptySetFunction);
	}
	
	public static AntiChain emptyFunction() {
		return new AntiChain(emptyFunction);
	}

	/**
	 * the function containing one elemenent which is a singleton
	 * @param l the element of the singleton
	 * @return
	 */
	public static AntiChain singletonFunction(int l) {
		AntiChain ret = new AntiChain();
		ret.add(SmallBasicSet.emptySet().add(l));
		return ret;
	}


	/**
	 * the function representing the universe of size n
	 * @param n
	 * @return
	 */
	public static AntiChain universeFunction(int n) {
		AntiChain ret = new AntiChain();
		ret.add(SmallBasicSet.universe(n));
		ret.setUniverse(SmallBasicSet.universe(n));
		return ret;
	}
	
	/**
	 * omicron(tau, alfa) is the largest AMF chi le this for which chi.meet(alfa).equals(tau)
	 * DEPRECATED
	 * use omicron(alfa) in stead
	 * @pre tau.le(alfa)
	 * @param tau
	 * @param alfa
	 * @return
	 */
	public AntiChain omicron(AntiChain tau, AntiChain alfa) {
		if (tau.isEmpty()) {
			if (alfa.isEmpty()) return this;
			else return AntiChain.emptyFunction();
		}
		AntiChain res = new AntiChain(getUniverse());
		res.add(sp().minus(alfa.sp()));
		for (SmallBasicSet A : alfa) {
			AntiChain current = new AntiChain(getUniverse());
			current.add(A);
			res = res.times(current.meet(tau));
		}
		return res.meet(this);
	}


	/**
	 * return an amf recognising only the set n
	 * @param n
	 * @return
	 */
	public static AntiChain oneSetFunction(SmallBasicSet n) {
		AntiChain res = new AntiChain();
		res.add(n);
		return res;
	}

	public static AntiChain emptyFunction(SmallBasicSet N) {
		AntiChain e = AntiChain.emptyFunction();
		e.setUniverse(N);
		return e;
	}

	/**
	 * reduce this with respect to the given span
	 * to the unambiguous this = result[0].join(result[1].times({[m]})
	 * with result[0].ge(result[1]) and m == span.maximum() 
	 * @param span
	 * @return
	 */
	public AntiChain[] reduce(SmallBasicSet span) {
		int m = span.maximum();
		SmallBasicSet p = span.minus(m);
		AntiChain[] ret = new AntiChain[2];
		ret[0] = this.project(p);
		AntiChain a1 = new AntiChain(this);
		a1.removeAll(ret[0]);
		ret[1] = a1.project(p);
		return ret;
	}

	/**
	 * produce a string for display
	 */
	public String toString() {
		if (size() == 0) return "{}";
		String res = "{";
		for (SmallBasicSet X : this) {
			res += X + ",";
		}
		return res.substring(0, res.lastIndexOf(',')) + "}";
	}

}