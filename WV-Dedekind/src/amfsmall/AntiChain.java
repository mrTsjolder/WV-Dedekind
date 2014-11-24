/**
 * 
 */
package amfsmall;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
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

	/**
	 * 
	 */
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
	 * check whether this AntiChain is symmetric under transposition x
	 * @param x the transposition
	 * @return this AntiChain is symmetric under x
	 */
	public boolean isSymmetric(Transposition x) {
		for (SmallBasicSet s : this) if (!contains(x.apply(s))) return false;
		return true;
	}
	
	public static Parser<AntiChain> parser() {
		return new Parser<AntiChain>() {

			
			@Override
			public AntiChain parse(String S) throws SyntaxErrorException {

				return AntiChain.parse(S);
			}

			@Override
			public boolean makesSense(String input) {
				try {
					AntiChain.parse(input);
				}
				catch (SyntaxErrorException e) {
					return false;
				}
				return true;
			}
			
		};
	}
	/**
	 * parsing function
	 * inverse of toString
	 * @param r
	 * @return the function described by r
	 * throws SyntaxErrorException must be thrown if no object can be returned
	 */
	private static AntiChain parse(String r) throws SyntaxErrorException {
		r = Parser.removeSpaces(r);
		int universeBracket = r.indexOf('[');
		SmallBasicSet universe = SmallBasicSet.universe(); // standard universe
		if (universeBracket == 0) { // syntax "[...]{[.."
			universe = SmallBasicSet.parser().parse(r);
			r = r.substring(r.indexOf(']') + 1);
		}
		int opening = r.indexOf('{');
		int closure = r.indexOf('}');
		if (opening != 0) throw new SyntaxErrorException("AntiChain parsing \"" + r + "\": No introducing '{' found");
		if (closure == -1) throw new SyntaxErrorException("AntiChain parse error \"" + r + "\": No closing '}' found");
		AntiChain amf = new AntiChain(universe);
		r = r.substring(opening + 1, closure).trim();
		if (r.isEmpty()) return amf;
		boolean longNotation = r.indexOf(']') >= 0;
		while (!r.isEmpty()) {
			try {
				amf.addConditionally(SmallBasicSet.parser().parse(r));
			} catch (SyntaxErrorException e) {
				throw new SyntaxErrorException("AntiChain parsing \"" 
						+ r + "\": No SmallBasicSet found\n(" + e + ")");
			}
			if (longNotation) r = r.substring(r.indexOf(']') + 1).trim();
			else {
				int comma = r.indexOf(',');
				if (comma >= 0) r = r.substring(comma);
				else r = "";
			}
			if (!r.isEmpty()) r = r.substring(1).trim();		
		}
		return amf;
	}

	public static boolean isAntiMonotonic(Collection<SmallBasicSet> C) {
		for (SmallBasicSet A : C)
			for (SmallBasicSet B : C) {
				if (A!= B && A.hasAsSubset(B)) return false;
			}
		return true;
	}
	
	public static void makeAM(Collection<SmallBasicSet> C) {
		HashSet<SmallBasicSet> badMembers = new HashSet<SmallBasicSet>();
		for (SmallBasicSet A : C)
			for (SmallBasicSet B : C)
				if (A != B && A.hasAsSubset(B)) badMembers.add(B);
		C.removeAll(badMembers);
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
	 * Check whether this AntiChain is less or equal to the one with only x as an element
	 * @param x
	 * @return true iff x contains all sets in this
	 */
	public boolean le(SmallBasicSet x) {
		for (SmallBasicSet b : this) {
			if (!x.hasAsSubset(b)) return false;
		}
		return true;
	}

	/**
	 * Check whether this AntiChain is less than the one with only x as an element
	 * @param x
	 * @return true iff each set in this is contained in x and this is not the singleton x
	 */
	public boolean lt(SmallBasicSet x) {	
		boolean equal = true;
		for (SmallBasicSet b : this) {
			if (!x.hasAsSubset(b)) return false;
			if (!x.equals(b)) equal = false;
		}
		return !equal;
		
	}
	/**
	 * Check whether this AntiChain is greater or equal that other
	 * @param other
	 * @return true iff all sets in other are contained in at least one set in this
	 */
	public boolean ge(AntiChain other) {
		for (SmallBasicSet A : other)
		{
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
	 * Check whether this AntiChain is greater than the one with only x as an element
	 * @param x
	 * @return true iff x is contained in at least one set in this
	 */
	public boolean gt(SmallBasicSet x) {
		boolean equal = true;
		boolean supersetFound = false;
		for (SmallBasicSet B : this) {
			if (!B.equals(x)) equal = false;
			if (B.hasAsSubset(x)) supersetFound = true;
			if (supersetFound && !equal) return true;
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
	 * Product of two orthogonal AntiChains is the function that contains all unions of a set in this and a set in other
	 * @param B
	 * @pre sp(this) and sp(other) are disjoint
	 * @return {AUB|A in this, B in other}
	 */
	public AntiChain timesOrtho(AntiChain other) {
		AntiChain res = new AntiChain(getUniverse());
		for (SmallBasicSet A : this)
			for (SmallBasicSet B : other) {
				res.addConditionally(B.union(A));
			}
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
	 * Intersections of elements with a non subset-superset relation 
	 * @return {A^B|A in this, B in other, B is not a subset of A}
	 */
/*	private AntiChain intersections(AntiChain other) {
		AntiChain res = new AntiChain(getUniverse());
		for (SmallBasicSet A : this)
			for (SmallBasicSet B : other) {
				if (!A.hasAsSubset(B)) {
					res.addConditionally(A.intersection(B));
				}
			}
		return res;
	}*/

	/**
	 * computes the sup of a collection, that is the smallest antimonotonic function 
	 * that is greater than the collection
	 * @param B
	 * @return The antimonotonic function that contains all sets from B
	 *            that are not a subset and different of another set contained in B
	 */
	public static AntiChain sup(Collection<SmallBasicSet> B) {
		HashSet<SmallBasicSet> res = new HashSet<SmallBasicSet>(B);
		makeAM(res);
		return new AntiChain(res);
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
	 * rank is the number of immediate successors on a chain between this function and the empty function
	 * @return sum(2^|A|) - sum(2^|A section B|) + sum(2^| A section B section C|) - ...
	 */
	public long rank() {
		if (size() == 0) return 0;
		SmallBasicSet b = null;
		for (SmallBasicSet x : this) {b = x;break;}
		AntiChain oneLess = new AntiChain(getUniverse());
		oneLess.addAll(this);
		oneLess.remove(b);
		for (SmallBasicSet s : b.immediateSubSets()) oneLess.addConditionally(s);
		return 1 + oneLess.rank();
	}
	
	/**
	 * distance is the number of immediate successor of a chain of minimal length between this function and x
	 * @param x
	 * @return this.rank() + x.rank() - 2*this.dot(x).rank()
	 */
	public long distance(AntiChain x) {
		AntiChain common = dot(x);
		return rank() + x.rank() - 2*common.rank();
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
	 * find the maximal BitRepresentation with l as largest element
	 * of a representative equivalent to
	 * this antimonotonic function
	 * under permutation of the elements
	 * @param l the largest element in the bitrepresentation
	 * @return the smallest BitRepresentation in the equivalenceclass
	 */
	public BitRepresentation standardBitRepresentation(int l) {
		SmallBasicSet span = sp();
		int map[] = new int[(int) span.size()];
		int inverseMap[] = new int[span.maximum() + 1];
		int pos = 0;
		for (int i:span) {
			map[pos] = i;
			inverseMap[i] = pos++;
		}
		Iterator<Pair<int[], int[]>> permutations = MappingPermutation.getIterator(map,inverseMap,map.length);
		BitRepresentation bestCode = new BitRepresentation(this,l);
		while (permutations.hasNext()) {
			AntiChain kand = this.map(permutations.next().snd);
			BitRepresentation code = new BitRepresentation(kand,l);
			if (code.compareTo(bestCode) > 0) {
				bestCode = code;
			}
		}
		return bestCode;	
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
	 * find the maximal BitRepresentation of a representative with l as largest element
	 * equivalent to
	 * this antimonotonic function
	 * under the given set of permutation of the elements
	 * @param permutations the invariants group
	 * @param l the largest element in the representation
	 * @return the smallest BitRepresentation in the equivalenceclass
	 */
	public BitRepresentation standardBitRepresentation(Set<int[]> permutations, int l) {
		BitRepresentation bestCode = new BitRepresentation(this,l);
		for (int[] p : permutations) {
			AntiChain kand = this.map(p);
			BitRepresentation code = new BitRepresentation(kand,l);
			if (code.compareTo(bestCode) > 0) {
				bestCode = code;
			}
		}
		return bestCode;	
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
	
	public Pair<int[],int[]> compactification() {
		SmallBasicSet span = sp();
		int map[] = new int[span.maximum()+1];
		int imap[] = new int[(int) span.size()+1];
		int i = 0;
		for (int e : span) {
			map[e] = i;
			imap[i+1] = e-1;
			i++;
		}
		return new Pair<int[], int[]>(map,imap);
	}
	
	/**
	 * find the symmetry subgroup of symm of this amf 
	 * this is the set of permutations of the elements under which this amf is invariant
	 * @return the set of permutations in symm that leave this invariant
	 */
	public Set<int[]> symmetryGroup(Set<int[]> symm) {
		Set<int[]> res = new HashSet<int[]>();
		BigInteger theCode = this.encode();
		for (int[] p : symm) { //while (permutations.hasNext()) {
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
	 * The complement of an AMF f with respect to a set N
	 * is the function that accepts 
	 * each smallest superset of the complement of a set in f
	 * @param N : the motherset
	 * @return sup({A in N : there is no B in f: N \ B is a subset of A})
	 */
	public AntiChain complement(SmallBasicSet N) {
		AntiChain res = new AntiChain(getUniverse());
		res.add(N);
		for (SmallBasicSet b : this) {
			SmallBasicSet bb = N.minus(b);
			AntiChain newRes = new AntiChain(getUniverse());
			for (SmallBasicSet x : res) {
				if (x.hasAsSubset(bb)) {
					for (int i : bb) {
						newRes.addConditionally(x.minus(i));
					}
				}
				else newRes.addConditionally(x);
			}
			res = newRes;
		}
		return res;
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
	 * minus for antimonotonic functions: starting from the sets in this, remove all subsets of sets of sa 
	 * @param f
	 * @return an amf containing all the sets in this which are not subsets of a set in f
	 */
	public AntiChain minus(SmallBasicSet sa) {
		AntiChain res = new AntiChain(getUniverse());
		for (SmallBasicSet x : this) {
			if (!sa.hasAsSubset(x)) res.add(x);
		}
		return res;
	}


	
	/**
	 * The measure of this am function m is the maximum size of one of its elements
	 * them measure of the empty am function is zero
	 * @return the measure
	 */
	public long m() {
		long res = 0;
		for (SmallBasicSet x : this) if (x.size() > res) res = x.size();
		return res;
	}
	
	/**
	 * an am function is said to be flat iff all its elements have the same size
	 * @return
	 */
	public boolean isFlat() {
		long m = -1;
		int ct = 0;
		for (SmallBasicSet x : this) 
			if (x.size() != m)
				if (ct++ > 0) return false;
				else m = x.size();
		return true;
	}
	
	/**
	 * @pre this.isFlat()
	 * The p of a flat amf g is the largest amf f >= g for which f.m() <= g.m() + 1 
	 * @return
	 */
	public AntiChain p() {
		TreeMap<SmallBasicSet,Integer> hm = new TreeMap<SmallBasicSet,Integer>();
		SmallBasicSet Universe = this.sp();
		for (SmallBasicSet x : this)
			for (int i: Universe.minus(x)) {
				SmallBasicSet p = x.add(i);
				Integer v = hm.get(p);

				if (v == null) hm.put(p, new Integer(1) );
				else hm.put(p, new Integer(v + 1));
			}
		AntiChain res = new AntiChain(getUniverse());
		long mes = this.flatM();
		for (SmallBasicSet s : hm.keySet()) {
			if (hm.get(s) == mes + 1) res.add(s);
		}
		return res.plus(this);
	}

	/**
	 * @pre this.isFlat()
	 * @return this.p().minus(p) 
	 * 
	 */
	public AntiChain pt() {
		TreeMap<SmallBasicSet,Integer> hm = new TreeMap<SmallBasicSet,Integer>();
		SmallBasicSet Universe = this.sp();
		for (SmallBasicSet x : this)
			for (int i: Universe.minus(x)) {
				SmallBasicSet p = x.add(i);
				Integer v = hm.get(p);
				if (v == null) hm.put(p, new Integer(1) );
				else hm.put(p, new Integer(v + 1));
			}
		AntiChain res = new AntiChain(getUniverse());
		long mes = this.flatM();
		for (SmallBasicSet s : hm.keySet()) {
			if (hm.get(s) == mes + 1) res.add(s);
		}
		return res;
	}

	/**
	 * compute m for a flat amf
	 * @return 0 or the size of one element
	 */
	private long flatM() {
		for (SmallBasicSet x : this) return x.size();
		return 0;
	}
	
	/**
	 * produces the ordered lists of SmallBasicSets
	 * @return
	 */
	public SmallBasicSet[] naturalOrder() {
		SmallBasicSet[] res = new SmallBasicSet[size()];
		toArray(res);
		Arrays.sort(res);
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
	 * Checks whether this is a minimal representative of an
	 * equivalence class under transpositions
	 * @return this is a minimal representative
	 */
	public boolean isMinimal() {
		long mInt = 0;
		for (SmallBasicSet s : this) {
			if (s.maximum() > mInt) mInt = s.maximum();
		}
		return isMinimal(mInt);
	}
	
/*	public AntiChain getSymmetryGroup() {
		int mInt = 0;
		for (SmallBasicSet s : this) if (s.maximum() > mInt) mInt = s.maximum();
		return getSymmetryGroup(mInt);
	}
*/
	/**
	 * the closure of an amf function contains the union of all non-disjoint
	 * sets in the amf function.
	 * @return the closure
	 */
	public AntiChain getClosure() {
		AntiChain res = new AntiChain(getUniverse());
		for (SmallBasicSet s : this)
			for (SmallBasicSet t : this) {
				if (!s.intersection(t).isEmpty()) res.addConditionally(s.union(t));
			}
		if (!res.equals(this)) return res.getClosure();
		else return res;
	}
	
	/**
	 * Computes the number of amf symmetric under the transpositions
	 * present in this amf
	 * @param m the total number of available elements
	 * @pre m >= number of elements in amf
	 * @return
	 */
	public long combinatorial(long m) {
		long tot = 0,d = 1;
		AntiChain t = getClosure();
		for (SmallBasicSet s : t) {
			long siz = s.size();
			d*=factorial(siz);
			tot += siz;
		}
		return d*factorial(m-tot);
	}
	
	private long factorial(long l) {
		long res = 1;
		while (l > 0) {
			res *= l;
			l--;
		}
		return res;
	}
	
/*	*//**
	 * Compute the symmetry group of this amf
	 * @param m the largest element to transpose
	 * @return the set of transpositions accepted by this amf
	 *//*
	public AntiChain getSymmetryGroup(int m) {
		AntiChain res = new AntiChain(getUniverse());
		Iterator<Transposition> it = Transposition.iterator(1, m);
		while (it.hasNext()) {
			Transposition t = it.next();
			if (t.apply(this).compareTo(this) == 0) {
				res.addConditionally(t.toSmallSet());
			}
		}
		return res;
	}
*/	
	/**
	 * Checks whether this is a minimal representatie of an
	 * equivalence class under transpositions of elements in [1..m]
	 * @param m
	 * @return this is a minimal representative
	 */
	public boolean isMinimal(long m) {
		for (int i=1;i<m;i++)
			for (int j=i+1;j<=m;j++)
				if (new Transposition(i,j).apply(this).compareTo(this) < 0) {
					return false;
				}
		return true;
	}
	
	public AntiChain disjoint() {
		TreeMap<Integer,SmallBasicSet> isin = new TreeMap<Integer,SmallBasicSet>();
		for (int i: this.getUniverse()) {
			SmallBasicSet brblei = this.getUniverse();
			boolean found = false;
			for (SmallBasicSet s : this) {
				if (s.contains(i)) {
					brblei = brblei.intersection(s);
					found = true;
				}
				else brblei = brblei.minus(s);
			}
			if (found) isin.put(i, brblei);
		}
		return new AntiChain(isin.values());
	}
	
	public long nrOfSubSets() {
		return 0;
	}
	
	/**
	 * the description of the poset of elements under and above this AMF
	 * @return
	 */
	public String characteristicPoSet() {
		AntiChainInterval under = new AntiChainInterval(new AntiChain(getUniverse()),this);
		AntiChainInterval above = new AntiChainInterval(this,new AntiChain(getUniverse()));
		
		TreeSet<SmallBasicSet> res = under.getSources();
		res.addAll(above.getSources());
		
		return new PoSetOfSets(res).normalisedPoSet();
	}

	/**
	 * Computes the minimal representative of the equivalence class under
	 * transpositions to which this amf belongs
	 * @return the minimal representative
	 */
	public AntiChain getMinimal() {
		long mInt = 0;
		for (SmallBasicSet s : this) if (s.size() > mInt) mInt = s.size();
		AntiChain best = new AntiChain(this);
		boolean changed;
		do {
			AntiChain current = new AntiChain(best);
			changed = false;
			Iterator<Transposition> it = Transposition.iterator(1,(int) mInt);
			while (it.hasNext()) {
				Transposition t = it.next();
				current = t.apply(current);
				if (current.compareTo(best) < 0) {
					best = new AntiChain(current);
					changed = true;
				}
				current = t.apply(current);
			}
		}
		while (changed)	;
		return best;
	}
	
	/**
	 * The orthogonal interval is the set of amf's f for which this.dot(f) is
	 * the singleton empty
	 * @param n
	 * @return forall f in result : this.dot(f) = {[]}
	 */
	public AntiChainInterval ortho(SmallBasicSet n) {
		AntiChain top = new AntiChain(n);
		top.add(n.minus(sp()));
		AntiChain bottom = new AntiChain(n);
		bottom.add(SmallBasicSet.emptySet());
		return new AntiChainInterval(bottom,top);
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
	/**
	 * The dual of this amf in the lattice of amf's on N
	 * Using the times formula for posetelements in this
	 * @param N
	 * @return meet for A in this of {A}.times((N\A).immediateSubsets())
	 */
	public AntiChain dualAlternative(SmallBasicSet N) {
		AntiChain res = AntiChain.oneSetFunction(N);
		for (SmallBasicSet A : this) {
			res = res.meet(AntiChain.oneSetFunction(A).times(N.minus(A).immediateSubSets()));
		}
		return res;
	}
	
	/**
	 * Recursive procedure to compute the part of the hat of this AMF that contains only elements from N 
	 * The hat is the singleton N if N is in this
	 * or the hat is the greatest amf with subsets of N for which the hat contains all immediate subsets 
	 * @pre this.isFlat() at level k
	 * @param N set of allowed elements
	 * @param k the level of this
	 * @return the hat subject to N
	 */
	public AntiChain hat(SmallBasicSet N, int k) {
		AntiChain res = new AntiChain(getUniverse());
		if (N.size() == k) {
			if (this.contains(N)) res.add(N);
			return res;
		}
		boolean complete = true;
		for (SmallBasicSet x : N.immediateSubSets()) {
			AntiChain a = hat(x,k);
			res.addConditionallyAll(a);
			complete = complete && a.contains(x);
		}
		if (complete) res.addConditionally(N);
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

	/**
	 * Interval [t, t V (N k-1)]
	 * @pre this.isFlat()
	 * @param n the size of the universe
	 * @param k the level of this
	 * @return
	 */
	public AntiChainInterval lambda(int n, int k) {
		AntiChainInterval res = new AntiChainInterval(this,this.join(AntiChain.subSets(n, k-1)));
		return res;
	}

	/**
	 * Interval [t, t V (N k-1)]
	 * @pre this is a flat AMF | this.isFlat()
	 * @pre this is not empty | !this.isEmpty()
	 * @param n the size of the universe
	 * @return
	 */
	public AntiChainInterval lambda(int n) {
		long k = 0;
		for (SmallBasicSet s : this) {
			k = s.size();
			break;
		}
		AntiChainInterval res = new AntiChainInterval(this,this.join(AntiChain.subSets(n, (int) (k-1))));
		return res;
	}

	/**
	 * Interval [t, t V (N k-1)]
	 * @pre this is a flat AMF | this.isFlat()
	 * @pre level of this is k | this.maxSize() == k
	 * @pre this is not empty | !this.isEmpty()
	 * @return
	 */
	public AntiChainInterval lambda() {
		long k = 0;
		for (SmallBasicSet s : this) {
			k = s.size();
			break;
		}
		AntiChainInterval res = new AntiChainInterval(this,this.join(AntiChain.subSets((int) this.getUniverse().size(), (int) (k-1))));
		return res;
	}
	
	public AntiChainInterval upsilon() {
		if (this.isEmpty()) {
			return new AntiChainInterval(emptySetFunction, emptySetFunction);
		}
		long level = this.minSize();
		if (level == 0) {
			AntiChain emptySetFunction = new AntiChain(getUniverse());
			emptySetFunction.add(SmallBasicSet.emptySet());
			return new AntiChainInterval(emptySetFunction, emptySetFunction);			
		}
		AntiChain top = new AntiChain(this);
		long maxLevel = SmallBasicSet.universe().size();
		while (level <= maxLevel) top.grow(level++);
		return new AntiChainInterval(this, top);
	}
	
/*	private long maxSize() {
		long res = 0;
		for (SmallBasicSet s : this) if (s.size() > res) res = s.size();
		return res;
	}*/

	private long minSize() {
		long res = SmallBasicSet.universe().size();
		for (SmallBasicSet s : this) if (s.size() < res) res = s.size();
		return res;
	}

	private boolean grow(long level) {
		AntiChain extA = new AntiChain(getUniverse());
		for (SmallBasicSet a : this) {
			if (a.size() == level) {
				for (int x : SmallBasicSet.universe().minus(a)) {
					if (a.add(x).immediateSubSets().le(this)) extA.addConditionally(a.add(x));
				}
			}
		}
		this.addConditionallyAll(extA);
		return !extA.isEmpty();
	}
	
	/**
	 * compute the image of this under the mapping m
	 * @param m
	 * @return
	 */
	public AntiChain map(Mapping m) {
		AntiChain res = new AntiChain(getUniverse());
		for (SmallBasicSet a : this) {
			SmallBasicSet e = SmallBasicSet.emptySet();
			for (int i: a) e = e.add(m.map(i));
			res.add(e);
		}
		return res;
	}
	/**
	 * compute the image of this under the inverse of the mapping m
	 * @param m
	 * @return
	 */
	public AntiChain iMap(Mapping m) {
		AntiChain res = new AntiChain(getUniverse());
		for (SmallBasicSet a : this) {
			SmallBasicSet e = SmallBasicSet.emptySet();
			for (int i: a) e = e.add(m.iMap(i));
			res.add(e);
		}
		return res;
	}
	/**
	 * subsets of size k of the set {1,...,n}
	 * @param n
	 * @param k
	 */
	public static AntiChain subSets(int n,int k) {
		AntiChain res = new AntiChain();
		if (k < 0) return res;
		if (n < k) return res;
		if (k == 0) res.add(SmallBasicSet.emptySet());
		else if (k == n) res.add(SmallBasicSet.universe(n));
		else {
			res = subSets(n-1,k);
			AntiChain res2 = subSets(n-1,k-1);
			for (SmallBasicSet a : res2) res.add(a.add(n));
		}
		return res;
	}
	
	/**
	 * subsets of size k of the set s
	 * @param s
	 * @param k
	 */
	public static AntiChain subSets(SmallBasicSet s, int k) {
		if (k < 0 || s.size() < k) return new AntiChain();
		if (k == 0) {
			AntiChain res = new AntiChain();
			res.add(SmallBasicSet.emptySet());
			return res;
		}
		else {
			int x = s.getOneFrom();
			AntiChain res = subSets(s.minus(x),k);
			AntiChain res2 = subSets(s.minus(x),k-1);
			for (SmallBasicSet a: res2) res.add(a.add(x));
			return res;
		}
	}

	
	/**
	 * iterates over all subsets of the AMF
	 * @return
	 */
	public Iterator<AntiChain> subSetsIterator() {
		return new Iterator<AntiChain>() {

			SmallBasicSet[] wC;
			AntiChain nxt;
			boolean picked[];
			int active;
			boolean ready;
			
			{
				wC = new SmallBasicSet[AntiChain.this.size()];
				wC = AntiChain.this.toArray(wC);
				active = wC.length; // number of sets in nxt, 
				// except in the beginning when it does not make a difference
				nxt = new AntiChain();
				picked = new boolean[wC.length];
				for (int i=0;i<picked.length;i++) picked[i] = true; // no set selected in next nxt
				ready = false;
			}
			
			@Override
			public boolean hasNext() {
				return !ready || active < wC.length;
			}

			@Override
			public AntiChain next() {
				for (int i=0;i<wC.length;i++) {
					if (picked[i]) {
						nxt.remove(wC[i]);
						picked[i] = false;
						active--;
					}
					else {
						nxt.add(wC[i]);
						picked[i] = true;
						active++;
						break;
					}
				}
				ready = true;
				return nxt;
			}

			@Override
			public void remove() {
				// TODO Auto-generated method stub
				
			}
			
		};
	}
	
	/**
	 * compute the dedekind number n by 
	 * expansion of the AMF space in Upsilon and Below intervals
	 * on level k
	 * @param n the rank of the number to be computed
	 * @param k the level
	 * @return
	 */
	public static long countUpsilonWise(int n, int k) {
		long res = 0L;
		AntiChain nK = AntiChain.subSets(n, k);
		Iterator<AntiChain> sNK = nK.subSetsIterator();
		AntiChain nKM1 = AntiChain.subSets(n, k-1);
		while (sNK.hasNext()) {
			AntiChain s = sNK.next();
			long presU = s.upsilon().size();
			long presT = new AntiChainInterval(s,s.join( 
					nKM1.minus(nKM1.meet(s)))).size();
			res += presU*presT;
		}
		return res;
	}
	
	/**
	 * compute the dedekind number n by 
	 * expansion of the AMF space in Below intervals only
	 * on level k
	 * this is equivalent to the expansion based on upsilon and below intervals,
	 * except that the upsilon is replaced by an isomorphic below
	 * @param n the rank of the number to be computed
	 * @param k the level
	 * @return
	 */
	public static long countBelowWise(int n, int k) {
		long res = 0L;
		AntiChain nK = AntiChain.subSets(n, k);
		Iterator<AntiChain> sNK = nK.subSetsIterator();
		AntiChain nKM1 = AntiChain.subSets(n, k-1);
		AntiChain nMKM1 = AntiChain.subSets(n, n-k-1);
		AntiChain nMK = AntiChain.subSets(n, n-k);
		SmallBasicSet N = SmallBasicSet.universe(n);
		while (sNK.hasNext()) {
			AntiChain s = sNK.next();
			AntiChain sC = new AntiChain();
			for (SmallBasicSet A : s) sC.add(N.minus(A));
			AntiChain sP = nMK.minus(sC);

			long presU = new AntiChainInterval(sP,sP.join(nMKM1)).size(); // replaces upsilon
			long presT = new AntiChainInterval(s,s.join( 
					nKM1.minus(nKM1.meet(s)))).size();
			res += presU*presT;
		}
		return res;
		
	}
	
	
	public static void printUpsilonWise(int n, int k) {
		AntiChain nK = AntiChain.subSets(n, k);
		Iterator<AntiChain> sNK = nK.subSetsIterator();
		AntiChain nKM1 = AntiChain.subSets(n, k-1);
		AntiChain nMKM1 = AntiChain.subSets(n, n-k-1);
		AntiChain nMK = AntiChain.subSets(n, n-k);
		SmallBasicSet N = SmallBasicSet.universe(n);
		while (sNK.hasNext()) {
			AntiChain s = sNK.next();
			AntiChain sC = new AntiChain();
			for (SmallBasicSet A : s) sC.add(N.minus(A));
			AntiChain sP = nMK.minus(sC);
			
			for (AntiChain a : new AntiChainInterval(sP, sP.plus(nMKM1)))
				for (AntiChain b : new AntiChainInterval(s, s.plus(nKM1)))
					System.out.println(a.plus(b));
		}
	}

	public static void printUpsilonAndDual(int n, int k) {
		AntiChain nK = AntiChain.subSets(n, k);
		Iterator<AntiChain> sNK = nK.subSetsIterator();
		AntiChain nKM1 = AntiChain.subSets(n, k-1);
		while (sNK.hasNext()) {
			AntiChain s = sNK.next();
			AntiChain sU = nKM1.dot(s);
			AntiChain sC = sU.plus(nKM1.minus(sU));
			AntiChain sCD = sC.dual(SmallBasicSet.universe(n));
			System.out.println(s + "," + sC + ", " + sCD);
			System.out.print(new AntiChainInterval(sU,(sC)).size());
			System.out.println("=+++++=" + sCD.upsilon().size());
		}
	}

	private static AntiChain emptySetFunction;
	static {
		emptySetFunction = new AntiChain();
		emptySetFunction.add(SmallBasicSet.emptySet());
	}
	private static AntiChain emptyFunction = new AntiChain();
//	static {
//		emptyFunction = new AntiChain();
//	}
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
	 * the function containing one elemenent which is a singleton
	 * @param l the elements of the singleton
	 * @return
	 */
	public static AntiChain singletonFunction(int[] l) {
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
	
//	public static long newYearsFormula(int n,int k) {
//		if ()
//	}
	
	public static long mondaysFormula(int n) {
		AntiChain n5 = AntiChain.subSets(n, 5);
		AntiChain n4 = AntiChain.subSets(n, 4);
		AntiChain n2 = AntiChain.subSets(n, 2);
		AntiChain n3 = AntiChain.subSets(n, 3);
		Iterator<AntiChain> itT5 = n5.subSetsIterator();
		long res = 1L;
		while (itT5.hasNext()) {
			AntiChain t5 = itT5.next();
			System.out.println(t5);
			AntiChain n3T5 = n3.minus(n3.meet(t5));
			Iterator<AntiChain> itT3 = n3T5.subSetsIterator();
			long t3Contribution = 0;
//			long t5Span = t5.sp().size();
			long t5n4 = t5.meet(n4).size();
			while (itT3.hasNext()) {
				AntiChain t3T5 = itT3.next();
				System.out.println("    " + t3T5);
				AntiChain trip = t5.join(t3T5);
				long exponent = - t5n4 + trip.countInOrJustBelow(n4) - trip.meet(n2).size();
				System.out.println("--------->" + trip + exponent);
				long combContribution = 0;
				for (int i=(int) trip.sp().size();i<=n;i++) {
					long contribution = SmallBasicSet.combinations(n, i);
					if (i > 2) contribution *= Math.pow(2, exponent + SmallBasicSet.combinations(i, 2));
					combContribution += contribution;
				}
				System.out.println("         " + combContribution);
				t3Contribution += combContribution;
			}
			System.out.println(t5.upsilon().size()*t3Contribution);
			res += t5.upsilon().size()*t3Contribution;
		}
		return res;
	}

	/**
	 * twoLevelFormula
	 * Compute the size of the interval Upsilon(a) in AMF(n) through the twoLevelFormula
	 * @param a flat antimonotonic
	 * @param k of level k
	 * @param n the dimension of the AMF
	 * @param e the already computed exponent
	 * @return the size
	 */

	public static long twoLevelFormula(AntiChain a, int k, int n, long e) {
		AntiChain ap = a.exp(a.sp());
		if (ap.isEmpty()) {
			// nothing to be done
			return AntiChain.pow(2L,e);
		}
		AntiChain app = ap.exp(a.sp());
		if (app.isEmpty()) {
			// special case
			return AntiChain.pow(2L, e + ap.size());
		}
		long res = 0;
		for (Iterator<AntiChain> it = app.subSetsIterator();it.hasNext();) {
			AntiChain t = it.next();
			long tm = t.meet(ap).size();
			res += twoLevelFormula(t,k+2,n,e + ap.size() - tm);
		}
		return res;
	}

	/**
	 * twoLevelFormula
	 * Compute the size of the interval [a, a^{+2l}] through the twoLevelFormula
	 * keepint track of the exponents through the hash variable
	 * @param a a flat antimonotonic
	 * @param k of level k
	 * @param l the number of double levels
	 * @param e the already computed exponent
	 * @param hash maps exponents to coefficients, for documentation of the process.
	 * @return the size
	 */
	public static long twoLevelFormula(AntiChain a,int k, int l,long e,Map<Long,Long> hash) {
		if (l == 0) {
			// maxiumum level reached
			if (!hash.keySet().contains(e)) hash.put(e,0L);
			hash.put(e,hash.get(e) + 1);
			return (long) Math.pow(2, e);
		}
		AntiChain ap = a.exp(a.sp());
		if (ap.isEmpty()) {
			// nothing to be done
			if (!hash.keySet().contains(e)) hash.put(e,0L);
			hash.put(e,hash.get(e) + 1);
			return AntiChain.pow(2L,e);
		}
		AntiChain app = ap.exp(a.sp());
		if (app.isEmpty()) {
			// special case
			long exp = e + ap.size(); 
			if (!hash.keySet().contains(e)) hash.put(exp,0L);
			hash.put(e,hash.get(exp) + 1);
			return AntiChain.pow(2L, exp);
		}
		long res = 0;
		for (Iterator<AntiChain> it = app.subSetsIterator();it.hasNext();) {
			AntiChain t = it.next();
			long tm = t.meet(ap).size();
			res += twoLevelFormula(t,k+2,l-1,e + ap.size() - tm,hash);
		}
		return res;
	}

	/**
	 * upDownFormula
	 * Compute the size of AMF(n) through the upDownFormula at level k
	 * upsilon is computed through the twoLevelFormula
	 * @param k of level k
	 * @param n the dimension of the AMF
	 * @return the size
	 */
	public static long upDownFormula(int k, int n) {
		long res = 0;
		AntiChain phi = AntiChain.subSets(n, k);
		AntiChain phiC = AntiChain.subSets(n, n-k);
		for (Iterator<AntiChain> tauIt = phi.subSetsIterator();tauIt.hasNext();) {
			AntiChain tau = tauIt.next();
			res += twoLevelFormula(tau,k,n,0)*twoLevelFormula(phiC.minus(tau.tilde(n)),n-k,n,0);
		}
		return res;
	}
	

	public AntiChain tilde(int n) {
		AntiChain res = new AntiChain(getUniverse());
		SmallBasicSet u = SmallBasicSet.universe(n);
		for (SmallBasicSet a : this) res.add(u.minus(a));
		return res;
	}

	public AntiChain exp(SmallBasicSet u) {
		AntiChain res = new AntiChain(getUniverse());
		for (SmallBasicSet a : this) {
			for (int i : u.minus(a)) {
				SmallBasicSet candidate = a.add(i);
				boolean notOk = false;
				if (!res.contains(candidate)) {
					for (int j : candidate) 
						if (!this.contains(candidate.minus(j))) {
							notOk = true;
							break;
						}
					if (!notOk) res.add(candidate);
				}
			}
		}
		return res;	
	}

	public static long mondayMorningFormula(int n) {
		AntiChain n2 = AntiChain.subSets(n, 2);
		AntiChain n3 = AntiChain.subSets(n, 3);
		Iterator<AntiChain> itT3 = n3.subSetsIterator();
		long res = 1L;
		while (itT3.hasNext()) {
			AntiChain t3 = itT3.next();
			System.out.println(t3);
			AntiChain n3T2 = t3.meet(n2);
			long exponent = -n3T2.size();
			long combContribution = 0;
			for (int i=(int) t3.sp().maximum();i<=n;i++) {
				long contribution = SmallBasicSet.combinations(n, i);
				if (i > 2) contribution *= Math.pow(2, exponent + SmallBasicSet.combinations(i, 2));
				combContribution += contribution;
			}
			System.out.println("         " + combContribution);
			res += t3.upsilon().size()*combContribution;
		}
		return res;
	}

	public static long mondayMorningFormula1(int n) {
		long res = 1;
		AntiChain n2 = AntiChain.subSets(n, 2);
		for (int i=0;i<=n;i++) {
			AntiChain i3 = AntiChain.subSets(i, 3);
			Iterator<AntiChain> itI3= i3.subSetsIterator();
			long subRes = 0;
			while (itI3.hasNext()) {
				AntiChain t3 = itI3.next();
				long exponent = (i<2? 0:SmallBasicSet.combinations(i, 2) - t3.meet(n2).size());
				long psubRes = (long) Math.pow(2, exponent)*t3.upsilon().size();
				subRes += psubRes;
//				System.out.println(i + " " + t3 + " " + SmallBasicSet.combinations(n, i)*psubRes);
			}
			res = res +  SmallBasicSet.combinations(n, i)*subRes;
		}
		return res;
	}
	
	public static long mondayMorningFormula2(int n) {
		long res = 1;
		AntiChain n2 = AntiChain.subSets(n, 2);
		AntiChain n3 = AntiChain.subSets(n, 3);
		Iterator<AntiChain> itN3= n3.subSetsIterator();
		long coeff[] = new long[n+1]; 
		for (int s=0;s<=n;s++) {
			long coef = 0;
			for (int i=s;i<=n;i++) {
				long exponent = (i<2? 0 : SmallBasicSet.combinations(i, 2));
				coef += SmallBasicSet.combinations(n, i)*SmallBasicSet.combinations(i, s)*Math.pow(2, exponent);
			}
			coeff[s] = coef;
		}
		while (itN3.hasNext()) {
			AntiChain t3 = itN3.next();
//			System.out.print("--" + t3 + "...");
			if (t3.sp().maximum() != t3.sp().size()) continue;
			long exponent = -t3.meet(n2).size();
			res += t3.upsilon().size()*coeff[(int) t3.sp().size()]*Math.pow(2, exponent);	
		}
		return res;
	}

	public static long mondayMorningFormula3(int n) {
		long res = 1;
		AntiChain n2 = AntiChain.subSets(n, 2);
		AntiChain n3 = AntiChain.subSets(n, 3);
		AntiChain n4 = AntiChain.subSets(n, 4);
//		AntiChain n5 = AntiChain.subSets(n, 5);
		Iterator<AntiChain> itN3= n3.subSetsIterator();
		long coeff[] = new long[n+1]; 
		for (int s=0;s<=n;s++) {
			long coef = 0;
			for (int i=s;i<=n;i++) {
				long exponent = (i<2? 0 : SmallBasicSet.combinations(i, 2));
				coef += SmallBasicSet.combinations(n, i)*SmallBasicSet.combinations(i, s)*Math.pow(2, exponent);
			}
			coeff[s] = coef;
		}
		while (itN3.hasNext()) {
			AntiChain t3 = itN3.next();
//			System.out.print("--" + t3 + "...");
			if (!minSpan(t3)) continue;
			AntiChain n5T3 = t3.above(5);
//			System.out.print(t3 + " " + n5T3 + "(");
			Iterator<AntiChain> itN5T3 = n5T3.subSetsIterator();
			long coef = coeff[(int) t3.sp().size()];
			long exponent = -t3.meet(n2).size();
			long factor = 0;
			while (itN5T3.hasNext()) {
				AntiChain t5 = itN5T3.next();
				if (!minSpan(t5)) continue;
				factor += SmallBasicSet.combinations((int) t3.sp().size(), (int) t5.sp().size())*coef*Math.pow(2, exponent - t5.meet(n4).size() + t3.above(4).size())*t5.upsilon().size();
//				System.out.print("???" + t5 + " " + coef + " " + factor + " " + t5.upsilon().size() + " " +  t5.meet(n4).size() + " " + exponent + " " +  t3 + "Above : " + t3.above(4).size() + " (" + t5.upsilon() + "," + t5.upsilon().size() + ")");
			}
//			System.out.println(")");
			res += factor;	
		}
		return res;
	}

	public static long mondayMorningFormula4(int n) throws SyntaxErrorException {
		long res = 1;
		AntiChain n2 = AntiChain.subSets(n, 2);
		AntiChain n3 = AntiChain.subSets(n, 3);
		AntiChain n4 = AntiChain.subSets(n, 4);
		AntiChain n5 = AntiChain.subSets(n, 5);
		Iterator<AntiChain> itN5 = n5.subSetsIterator();
		
		long coeff[][] = getCoefficientsL2(n);
		
		int aantal5=0, aantal5b=0,aantal3=0,aantal3b = 0;
		while (itN5.hasNext()) {
			AntiChain t5 = itN5.next();
			SmallBasicSet t5Sp = t5.sp();
			AntiChain t5cps = t5.compPerSet(t5Sp);
			int s5 = (int) t5Sp.size();
			aantal5++;			
			if (!minSpan(t5) || !minSpan(t5cps)) continue;
			aantal5b++;
			long exponent = -t5.meet(n4).size();
			
			long factor = 0;
			AntiChain n3T5 = n3.minus(t5);
			Iterator<AntiChain> itN3T5 = n3T5.subSetsIterator();
			while (itN3T5.hasNext()) {
				AntiChain t3 = itN3T5.next().join(t5).meet(n3);
				SmallBasicSet t3Sp = t3.sp();
				AntiChain t3cps = t3.compPerSet(t3Sp);
				aantal3++;
				if (!minSpan(t3) || !minSpan(t3cps)) continue;
				int s3 = (int) t3.sp().size();
				aantal3b++;
				long lExponent = exponent + t3.above(4).size() - t3.meet(n2).size();
				factor += SmallBasicSet.combinations((int) t3Sp.size(), (int) t3cps.sp().size())*SmallBasicSet.combinations(s3, s5)*coeff[s3][1]*pow(2, lExponent + coeff[s3][0]);
			}
			res += SmallBasicSet.combinations((int) t5Sp.size(), (int) t5cps.sp().size())*factor*t5.upsilon().size();
			System.out.println("Stats: " + aantal5+ ", " + aantal5b+ ", " + aantal3 + ", " + aantal3b);
		}
		System.out.println("Stats: " + aantal5+ ", " + aantal5b+ ", " + aantal3 + ", " + aantal3b);
		return res;
	}

	/**
	 * The AMF of the complements of this with respect to u
	 * @param u
	 * @return {u\A|A is an element of this}
	 */
	public AntiChain compPerSet(SmallBasicSet u) {
		AntiChain res = new AntiChain(getUniverse());
		for (SmallBasicSet s : this) res.add(u.minus(s));
		return res;
	}

/*	private static long[][] getCoefficientsL1(int n) {
		long[][] coeff = new long[n+1][2]; 
		for (int s=0;s<=n;s++) {
			long coef = 0;
			coeff[s][0] = SmallBasicSet.combinations(s, 2);
			for (int i=s;i<=n;i++) {
				long exponent = SmallBasicSet.combinations(i, 2) - coeff[s][0];
				coef += SmallBasicSet.combinations(n, i)*pow(2, exponent);
			}
			coeff[s][1] = coef;
		}
		return coeff;
	}*/

	private static long[][] getCoefficientsL2(int n) {
		long[][] coeff = new long[n+1][2]; 
		for (int s=0;s<=n;s++) {
			long coef = 0;
			coeff[s][0] = SmallBasicSet.combinations(s, 2);
			for (int i=s;i<=n;i++) {
				long exponent = SmallBasicSet.combinations(i, 2) - coeff[s][0];
				coef += SmallBasicSet.combinations(n, i)*SmallBasicSet.combinations(i, s)*pow(2, exponent);
			}
			coeff[s][1] = coef;
		}
		return coeff;
	}

	public static long pow(long i, long e) {
		long res = 1;
		while (e > 0) {
			if (e % 2 == 1) {
				res *= i;
				e--;
			}
			else {
				i *= i;
				e /= 2;
			}
		}
		return res;
	}

	private static boolean minSpan(AntiChain f) {
		// the function uses all and only numbers between 0 and maximum
		return f.sp().maximum() == f.sp().size();
	}

	private  AntiChain above( int maxLevel) {
		// compute amf at level i in f.upsilon()
		if (this.isEmpty()) {
			return new AntiChain(getUniverse());
		}
		long level = this.minSize();
		if (level == 0) {
			AntiChain emptySetFunction = new AntiChain(getUniverse());
			emptySetFunction.add(SmallBasicSet.emptySet());
			return emptySetFunction;			
		}
		AntiChain top = new AntiChain(this);
		while (level <= maxLevel) top.grow(level++);
		top.keepOnlyLevel(maxLevel);
		return top;
	}

	private void keepOnlyLevel(int maxLevel) {
		// remove all elements of size less than maxLevel
		// replace all elements of size larger than maxLevel by its subsets of size maxLevel
		Iterator<SmallBasicSet> it = this.iterator();
		AntiChain temp = new AntiChain(getUniverse());
		while (it.hasNext()) {
			SmallBasicSet s  = it.next();
			if (s.size() < maxLevel) it.remove();
			if (s.size() > maxLevel) {
				temp.addConditionallyAll(AntiChain.subSets(s,maxLevel));
				it.remove();
			}
		}
		this.addConditionallyAll(temp);
	}


	private long countInOrJustBelow(AntiChain m) {
		long res = 0;
		for (SmallBasicSet s : m) {
			if (s.immediateSubSets().le(this)) res++;
			}
		return res;
	}

	/**
	 * Compute the mirror of this
	 * @pre this is flat at level k: this is subset of (N k)
	 * 
	 * @param n the size of the universe
	 * @param k the level
	 * @return (N n-k) \ this.compPerSet()
	 */
	public AntiChain mirror(int n, int k) {
		AntiChain res = AntiChain.subSets(n, n-k);
		res = res.minus(this.compPerSet(SmallBasicSet.universe(n)));
		return res;
	}

	/**
	 * Compute the mirror of this
	 * @pre this is not empty and flat: this is subset of (N k) with k the size of each element in this
	 * 
	 * @param n the size of the universe
	 * @effect mirror(n,k)
	 */
	public AntiChain mirror(int n) {
		int k = 0;
		for (SmallBasicSet s : this)  {k = (int) s.size();break;}
		return this.mirror(n,k);
	}

	/**
	 * compute the amf at level l of this.upsilon()
	 * == sup(this.upsilon()getTop().meet((N l)) 
	 * @param l : the level
	 * @return sup(this.upsilon()getTop().meet((N l))
	 */
	public AntiChain upsilon(int l) {
		AntiChain nl = AntiChain.subSets((int) sp().size(), l);
		AntiChain res = upsilon().getTop().meet(nl);
		AntiChain rres = new AntiChain(getUniverse());
		for (SmallBasicSet a : res) {
			if (a.size() >= l) rres.add(a);
		}
		return rres;
	}
	
	/**
	 * Compute the size of [{[]},this.hat] ?? 
	 * @return
	 */
	public long generalUpsilonCount() {
		Iterator<AntiChain> itT = this.subSetsIterator();
		long res = 0;
		AntiChain aMinus = new AntiChain(getUniverse());
		for (SmallBasicSet a : this) aMinus.addConditionallyAll(a.immediateSubSets());
		while (itT.hasNext()) {
			AntiChain t = itT.next();
			res += t.upsilon().size()*new AntiChainInterval(t,t.join(aMinus)).size();
		}
		return res;
	}
	
	/**
	 * check whether this is an immediate successor of k
	 * @param k
	 * @return this is an immediate successor of k
	 */
	public boolean isImmediateSuccessorOf(AntiChain k) {
		// this is greater than k
		if (!this.ge(k)) return false;
		// exactly one set in this is not in k
		SmallBasicSet theSet = null;
		for (SmallBasicSet s : this) {
			if (!k.contains(s)) {
				if (theSet != null) return false;
				theSet  = s;
			}
		}
		if (theSet == null) return false;
		// this set has all its immediate subsets included in sets in k
		return theSet.immediateSubSets().le(k);
	}
	/**
	 * check whether this is an immediate predecessor of k
	 * @param k
	 * @return this is an immediate predecessor of k
	 */
	public boolean isImmediatePredecessorOf(AntiChain k) {
		return k.isImmediateSuccessorOf(this);
	}

	/**{
	 * 
	 * compute the AMF containing all immediate subsets of sets in this
	 * @return
	 */
	public AntiChain sub() {
		AntiChain res = new AntiChain(getUniverse());
		for (SmallBasicSet a : this) {
			res.addConditionallyAll(AntiChain.subSets(a, (int) a.size()-1));
		}
		return res;
	}
	
	/**
	 * iota(alfa) is the smallest AMF chi for which chi.join(alfa).equals(this)
	 * @pre this.ge(alfa)
	 * @param alfa
	 * @return
	 */
	public AntiChain iota(AntiChain alfa) {
		AntiChain res = new AntiChain(this);
		res.removeAll(alfa);
		return res;
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
	 * omicron(alfa) is the largest AMF chi for which chi.meet(alfa).equals(this)
	 * @pre this.le(alfa)
	 * @param alfa
	 * @return
	 */
	public AntiChain omicron(AntiChain alfa) {
		AntiChain tauD = dual();
		tauD.removeAll(alfa.dual());
		return tauD.dual();		
	}

	/**
	 * a bitwise representation of this antimonotonic function in a space of size n
	 * @param n
	 * @return a bigInteger representing this amf
	 */
	public BigInteger bitRep(int n) {
		SmallBasicSet s = SmallBasicSet.universe(n);
		if (n == 0) 
			if (this.isEmpty()) return BigInteger.ZERO;
			else return BigInteger.ONE; 
		else {
			BigInteger ms = this.project(s.minus(n)).bitRep(n-1);

			AntiChain xr = new AntiChain(getUniverse());
			for (SmallBasicSet p : this)
				if (p.contains(n)) xr.add(p.minus(n));
			BigInteger ls = xr.bitRep(n-1);

			return ms.shiftLeft(ms.bitLength()).add(ls);
		}
	}

	/**
	 * the inverse of bitRep
	 * @param x
	 * @param n
	 * @return
	 */
	public static AntiChain invBitRep(BigInteger x,int n) {
		AntiChain ret = new AntiChain();
		if (x.equals(BigInteger.ZERO)) return ret;
		ret.add(SmallBasicSet.emptySet());
		if (x.equals(BigInteger.ONE)) return ret;
		BigInteger ms = x.shiftRight(x.bitLength()/2);
		BigInteger ls = x.subtract(ms.shiftLeft(x.bitLength()/2));
		if (ls.equals(BigInteger.ZERO)) return invBitRep(ms,n-1);
		return invBitRep(ms,n-1).join(invBitRep(ls,n-1).times(AntiChain.singletonFunction(n)));
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
	 * For this >= beta, find the connected components in the connectednessgraph of this with respect to beta
	 * the connectedness graph is defined by {A.intersect(B)} <= beta
	 * for !(this >= beta), the only connected component is alpha
	 * @param beta
	 * @return set of maximal amf's xi, pairwise satisfying xi.meet(xj).le beta
	 */
	public SortedSet<AntiChain> decompose(AntiChain beta) {
		if (!ge(beta)) {
			SortedSet<AntiChain> res = new TreeSet<AntiChain>();
			res.add(this);
			return res;
		}
		if (isEmpty()) {
			SortedSet<AntiChain> res = new TreeSet<AntiChain>();
			res.add(this);
			return res;
		}
		SortedSet<SmallBasicSet> alfac = new TreeSet<SmallBasicSet>(this);
		SortedSet<AntiChain> res = new TreeSet<AntiChain>();
		while (!alfac.isEmpty()) {
			SmallBasicSet a = alfac.first();
			AntiChain building = AntiChain.oneSetFunction(a);
			alfac.remove(a);
			boolean done;
			do {
				done = true;
				Iterator<SmallBasicSet> it = alfac.iterator();
				while (it.hasNext()) {
					SmallBasicSet b = it.next();
					if (!building.meet(AntiChain.oneSetFunction(b)).le(beta)) {
						building.addConditionally(b);
						it.remove();
						done = false;
					}
				}
			} while (!done);
			res.add(building);
		}
		return res;
	}

	/**
	 * the largest function le {universe} that does not dominate any element of this
	 * @param alfa
	 * @param universe
	 * @return
	 */
	 public AntiChain lnd(SmallBasicSet universe) {
		if (isEmpty()) return AntiChain.oneSetFunction(universe);
		AntiChain res = AntiChain.oneSetFunction(universe);
		for (SmallBasicSet a : this) {
			AntiChain comp = new AntiChain();
			for (int i: a) comp.add(universe.minus(i));
			res = res.meet(comp);
		}
		return res;
	}

	 /**
	  * Compute the smallest partition (least number of subsets) of sp() such that each of the subsets
	  * is never divided by a set in this.
	  * if (sp() == \emptyset) partition() == \{\emptyset\}
	  * @return
	  */
	public AntiChain partition() {
		SmallBasicSet span = sp();
		AntiChain res = AntiChain.oneSetFunction(span);
		for (SmallBasicSet s : this) {
			res = res.meet(AntiChain.oneSetFunction(s).join(AntiChain.oneSetFunction(span.minus(s))));
		}
		return res;
	}


}
