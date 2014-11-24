package amfsmall;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
/**
 * Immutable representation of antimonotinic functions on sets of elements [1,..,maxElement]
 * Each set is represented by a bitcombination corresponding to the set
 * and each of its subsets
 * The bitindex corresponding to a set S is sum over als elements i in S of 2^i.
 * @author patrickdecausmaecker
 *
 */
public class BitRepresentation implements LatticeElement, Comparable<BitRepresentation	> {

	/** the largest possible element in a set */
	public static final int maxElement = 10; // 1,2,3,4,5,6,7,8,9,10
	/** the number of sets */
	public static final int numberOfSets = 1<<maxElement;
	 /* the table of all sets connecting all the sets with their bitindex */
	public static final BitRepresentation sets[] = new BitRepresentation[numberOfSets];
	/* the table of the bits corresponding to sets of each size */
	public static final BigInteger setsOfSize[] = new BigInteger[maxElement+1];
	static {for (int i=0;i<maxElement+1;i++) setsOfSize[i] = BigInteger.ZERO;}
	public static final BigInteger oddSizedSets;
	public static final BigInteger evenSizedSets;
	
	/** the largest set */
	public static final BigInteger standardUniverse = allOne(numberOfSets);
	/** the sets of a specific size given a largest element as one BigInteger 
	 * phi[s][e] = the sets of a specific size of elements less than or equal to e **/
	private static final BitRepresentation[][] phi;

	// prepare the table of sets, the table of setsOfSize and the phi function
	// this section could be optimised
	static {
		sets[0] = new BitRepresentation(BigInteger.ONE.shiftLeft(numberOfSets-1)); // set the emty set
		for (int i=1;i<=maxElement;i++) // construct the remaining sets
			for (int k=0;k<1<<(i-1);k++){
				// a set containing i is a set without i with i added
				sets[(1<<(i-1))+k] = new BitRepresentation(sets[k].representation.or(sets[k].representation.shiftRight(1<<(i-1))));
			}
		phi = new BitRepresentation[maxElement+1][maxElement+1];
		for (int i=0;i<=maxElement;i++) 	
			for (int j=i;j<=maxElement;j++){
				phi[i][j] = new BitRepresentation(BigInteger.ZERO);
		}
		for (int i=0;i<numberOfSets;i++) {
			int size = BigInteger.valueOf(i).bitCount();
			int maxel = SmallBasicSet.decode(i).maximum();
			for (int j = size;j<= maxel;j++) {
				phi[size][j] = new BitRepresentation(phi[size][j].representation.or(sets[i].representation)); 
			}
		}
		for (int s=0;s < numberOfSets;s++) {
			int pos = BigInteger.valueOf(s).bitCount();
			setsOfSize[pos] = setsOfSize[pos].setBit(numberOfSets - s - 1);
		}
		
		BigInteger oddSets = BigInteger.ZERO;
		for (int pos=1;pos < setsOfSize.length;pos += 2) oddSets = oddSets.or(setsOfSize[pos]);
		oddSizedSets = oddSets;

		BigInteger evenSets = BigInteger.ZERO;
		for (int pos=0;pos < setsOfSize.length;pos += 2) evenSets = evenSets.or(setsOfSize[pos]);
		evenSizedSets = evenSets;
	}
		
	/** the bitrepresentation as a BigInteger
	 * The bitindex corresponding to a set S is sum over all elements i in S of 2^i. */
	public final BigInteger representation;
	/** the actual largest possible set in this representation */
	public final BigInteger universe;
	/** the number of possible sets in this representation, 
	 * also the length of representation */
	public final int length;
	/** the largest element possible in this representation length **/
	public final int largestElement;
	/** the sets represented. This variable is set on demand only (getSets) */
	private Set<Integer> theSets;
	/** the represenation of the corresponding antimonotone set. Set on demand only (getSets) */
	private BigInteger antiMonotone;
	/** the number of sets in this amf, variable kept on demand **/
	private int size;
	/** the number of sets in this amf, variable size kept on demand **/
	private int getSize() {
		if (size < 0) {
			getSets();
		}
		return size;
	}

	/**
	 * Construct a bitrepresentation with the BigInteger r as representation
	 * and the standard universe as actual universe.
	 * @effect BitRepresentation(r, standardUniverse)
	 * @param r the representation
	 */
	public BitRepresentation(BigInteger r) {
		this(r, standardUniverse, maxElement);
	}
	/**
	 * Construct a bitrepresentation with the BigInteger r as representation
	 * and the given universe as actual universe
	 * with l as the largest element in the universe.
	 * @pre r is a valid representation
	 * @pre r is in u
	 * @param r the representation
	 * @param u the given universe
	 * @param l the largest element
	 */
	public BitRepresentation(BigInteger r, BigInteger u, int l) {
		representation = r;
		universe = u;
		length = u.bitLength();
		theSets = null;
		largestElement = l;
		size = -1;
	}

	/**
	 * Construct a bitrepresentation with the BigInteger r as representation
	 * and the  universe with i as the largest element.
	 * @effect BitRepresentation(r, allOne(1<<i))
	 * @param r the representation
	 * @param i largest element in the universe
	 */
	public BitRepresentation(BigInteger bitRep, int i) {
		this(bitRep, allOne(1<<i),i);
	}
	
	/**
	 * construct the bitrepresentation for the amf f
	 * the universe is set to the universe of f
	 * @pre 1<<f.getUniverse().maximum() <= standardUniverse.bitLength()
	 * @param f
	 */
	public BitRepresentation(AntiChain f) {
		this(f,f.getUniverse().maximum());
		size = f.size();
	}
	/**
	 * construct the bitrepresentation for the amf f
	 * the universe is set to the universe size l
	 * @pre f does not contain an element x > l
	 * @param f
	 */
	public BitRepresentation(AntiChain f,int l) {
		this(representation(f,l),allOne(1<<l),l);
		size = f.size();
	}
	
	/**
	 * build a bitrepresentation with rep as representation and 
	 * dimensions as in fB
	 * @param rep
	 * @param fB
	 */
	public BitRepresentation(BigInteger rep, BitRepresentation fB) {
		this(rep,fB.universe,fB.largestElement);
	}

	private static BigInteger representation(AntiChain f, int l) {
		int maxi = 1<<l;
		BigInteger bitRep = BigInteger.ZERO;
		for (SmallBasicSet s : f) {
			bitRep = bitRep.or(getSet(setNumber(s),maxi));
		}
		return bitRep;
	}
	private static int setNumber(SmallBasicSet s) {
		int res = 0;
		for (int i : s) {
			res += 1<<(i-1);
		}
		return res;
	}
	/**
	 * generate a biginteger with bitLength one bits
	 * @param bitLength the number of bits to be set
	 * @return e.g. allOne(8) = 11111111
	 */
	public static BigInteger allOne(int bitLength) {
		BigInteger res = BigInteger.ZERO;
		while (bitLength > 0) {
			res = res.shiftLeft(1).setBit(0);
			bitLength--;
		}
		return res;
	}
	
	/**
	 * computes the representation of the largest amf not containing any of the sets in this amf
	 * at post: size has been set
	 * @return the largest amf not containing any of the sets in this
	 */
	public BitRepresentation getMinus() {
		BigInteger rep = representation;
		BigInteger res = representation;
		size = 0;
		int s = rep.getLowestSetBit();
		while (s >= 0) {
			size++;
			int setnum = length - s - 1;
			BigInteger set = getSet(setnum);
			rep = rep.or(set).xor(set);
			res = res.clearBit(s);
			s = rep.getLowestSetBit();
		}
		return new BitRepresentation(res,universe,largestElement);
	}
	
	public BitRepresentation getPlus() {
		return (BitRepresentation) ((BitRepresentation) dual()).getMinus().dual();
	}
	
	/**
	 * complete the bits in this BigInteger into a bitrepresentations
	 * 
	 * @param p the sets to be completed
	 * @pre the most significant bit is in universe
	 * @param length
	 * @return the bitrepresentation with the given universe and the largest element containing all the sets corresponding to the bits in p
	 */
	public static BitRepresentation completeSet(BigInteger p, BigInteger universe,int largest) {
		BigInteger res = BigInteger.ZERO;
		int theLength = universe.bitLength();
		int s = p.getLowestSetBit();
		while (s >= 0) {
			BigInteger set = getSet(theLength - s - 1,theLength);
			res = res.or(set);
			p = p.andNot(set);
			s = p.getLowestSetBit();
		}
		return new BitRepresentation(res,universe, largest);
	}
	

	/**
	 * complete the bits in this BigInteger into a bitrepresentation
	 * 
	 * @param p the sets to be completed
	 * @param length
	 * @return the bitrepresentation with the same length as this and containing all the sets corresponding to the bits in p
	 * @effect completeSet(p,universe, largestElement)
	 */
	public BitRepresentation completeSet(BigInteger p) {
		return completeSet(p, universe, largestElement);
	}

	@Override
	/**
	 * join two bitrepresentations
	 * Optimisation: join does presently not copy any on demand variables
	 */
	public BitRepresentation join(LatticeElement e) {
		return new BitRepresentation(representation.or (((BitRepresentation) e).representation),universe,largestElement);
	}


	@Override
	/**
	 * meet two bitrepresentations
	 * Optimisation: meet does presently not copy any on demand variables
	 */
	public LatticeElement meet(LatticeElement e) {
		return new BitRepresentation(representation.and (((BitRepresentation) e).representation),universe,largestElement);
	}

	@Override
	/**
	 * the new times operator for two bitrepresentations
	 * max(bitrepresentation K| projection of K on this.sp() <= this, projection of K on e.sp() <= e)
	 * implementation through the list of sets
	 */
	public LatticeElement times(LatticeElement e) {
		BigInteger res = BigInteger.ZERO;
		BitRepresentation conflict = (BitRepresentation) sp().meet((((BitRepresentation) e).sp()));
		int separationSet = complement(setNumber(conflict));
		Set<Integer> mySets = getSets();
		Set<Integer> hisSets = ((BitRepresentation) e).getSets();
		for (int x : mySets)
			for (int y : hisSets) {
				res = res.or(getSet((x & separationSet) + (y & separationSet) + (x & y)));
			}
		return new BitRepresentation(res,universe,largestElement);
	}
	

	/**
	 * compute the complement of the set in s
	 * @pre s is a set
	 * @param s
	 * @return universe \ s
	 */
	private BitRepresentation complement(BitRepresentation s) {
		return new BitRepresentation(getSet(complement(setNumber(s))));
	}
	
	/**
	 * compute the number of the set with number c
	 * @pre 0 <= c < length
	 * @param conflict
	 * @return number of universe \ getSet(c)
	 */
	private int complement(int c) {
		return length - c -1;
	}
	
	/**
	 * get the maximal sets in this
	 * the set of sets is actually a representation of the amf
	 * @post set the ondemand variables theSets and antiMonotone
	 * @return the set of numbers of maximal sets
	 */
	public Set<Integer> getSets() {
		if (theSets != null) return theSets;
		theSets = new TreeSet<Integer>();
		BigInteger rep = representation;
		antiMonotone = BigInteger.ZERO;
		size = 0;
		int s = rep.getLowestSetBit();
		while (s >= 0) {
			size++;
			antiMonotone = antiMonotone.setBit(s);
			int setnum = length - s - 1;
			theSets.add(setnum);
			rep = rep.or(getSet(setnum)).xor(getSet(setnum));
			s = rep.getLowestSetBit();
		}
		return theSets;
	}
	
	/**
	 * get the set with number s in the representations with length bits
	 * @pre s < length
	 * @param s the number of the required set
	 * @param length the largest number in the resulting representation
	 * @return the representation of this set
	 */
	public static BigInteger getSet(int s, int length) {
		return sets[s].representation.shiftRight(numberOfSets - length);
	}

	/**
	 * get the set with number s in the representations  with the current length
	 * @effect getSet(s,this.length)
	 */
	public BigInteger getSet(int s) {
		return getSet(s,length);
	}
	
	/**
	 * get the sets of size s in as bits in a BigInteger, in the representations with length bits
	 * @pre s <= maxElement
	 * @param s the size of the required sets
	 * @param length the largest number in the resulting representation
	 * @return the representation of this set
	 */
	public static BigInteger getSetsOfSize(int s, int length) {
		return setsOfSize[s].shiftRight(numberOfSets - length);		
	}

	/**
	 * get the sets as bits in a BigInteger, of size s in the representation with the current length
	 * @effect getSetsOfSize(s,this.length)
	 */	
	public BigInteger getSetsOfSize(int s) {
		return getSetsOfSize(s,length);		
	}

	/**
	 * get the biginteger representation of the amf with all sets of size s
	 * and maximal element largestElement in the representation of length length
	 * @pre length >= 1<<largestElement
	 * @param s
	 * @param largestElement
	 * @param length
	 * @return
	 */
	public static BigInteger getPhi(int s, int largestElement, int length) {
		return phi[s][largestElement].representation.shiftRight(numberOfSets - length);
	}
	
	/**
	 * get the biginteger representation of the amf with all sets of size s
	 * and maximal element 
	 * @effect getPhi(s,this.largestElement,this.length)
	 */
	public BitRepresentation getPhi(int s) {
		return new BitRepresentation(getPhi(s,this.largestElement, this.length),largestElement);
	}
	
	/**
	 * Compute the number of this set in rep
	 * @pre rep is the representation of a set (and thus not ZERO)
	 * @param rep
	 * @return the number of the set in rep
	 */
	private static int setNumber(BitRepresentation rep) {
		int res = rep.representation.getLowestSetBit();
		return rep.length - res - 1;
	}
	
	/**
	 * compute the span of this representation
	 * This is the set of all elements which are at least in one set
	 * @return
	 */
	public BitRepresentation sp() {
		BigInteger res = getSet(0); // emptyset
		int set = 0;
		for (int p = 1;p < length;p*=2)
			if (representation.testBit(length-p-1)) {
				set += p;
			}
		res = getSet(set);
		return new BitRepresentation(res, universe,largestElement);
	}

	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 * the string representation lists all bits starting with the highest order 1
	 * in groups of 4 bits separated by a space
	 * e.g. 1110 1000 1000 1000
	 */
	public String toString() {
		String res = "";
		for (int p = length-1;p>=0;p--) {
			res += (representation.testBit(p) ? "1" : "0");
			if (p%4 == 0) res += " ";
		}
		return res;
	}
	
	/**
	 * convert the bitrepresentation to an AntiChain representation
	 * the universe is set to largestElement
	 * @return
	 */
	public AntiChain toAntiChain() {
		AntiChain res = new AntiChain(SmallBasicSet.universe(largestElement));
		for (int x : getSets()) {
			res.add(SmallBasicSet.decode(x));
		}
		return res;
	}
	
	/**
	 * convert the biginteger to a set of sets of elements from the universe
	 * 
	 * @return a set of SmallBasicSet
	 */
	public static SortedSet<SmallBasicSet> toSets(BigInteger p, BigInteger universe, int largestElement) {
		SortedSet<SmallBasicSet> res = new TreeSet<SmallBasicSet>();
		int theLength = universe.bitCount();
		int s = p.getLowestSetBit();
		while (s >= 0) {
			res.add(SmallBasicSet.decode(theLength - s - 1));
			p = p.clearBit(s);
			s = p.getLowestSetBit();
		}
		return res;
	}

	/**
	 * convert the biginteger to a set of sets of elements from the universe in this
	 * 
	 * @return a set of SmallBasicSet
	 * @effect toSets(p,universe,largestElement)
	 */
	public  SortedSet<SmallBasicSet> toSets(BigInteger p) {
		return toSets(p,universe,largestElement);
	}
	
	
	
	/**
	 * the sets s of elements x less than p and for which s union {p} is in the amf
	 * for p == 0, the amf is either {} or {[]}
	 * @pre p <= largestElement
	 * @param p
	 * @return
	 */
	public BitRepresentation preamble(int p) {
		if (p == 0) return new BitRepresentation(representation.and(allOne(1).shiftLeft(length-1)),largestElement);
		BigInteger all = allOne(1<<(p-1));
		return new BitRepresentation(
				representation.and(all.shiftLeft(length-(1<<p))).shiftRight(length-(1<<p)),all,p-1);
	}

	/**
	 * the sets s of elements x less than p and for which s union {q} is in the amf
	 * for q == 0, the amf is either {} or {[]}
	 * for p == 0 and q > 0 the amf is allways {}
	 * @pre p <= q
	 * @pre q <= largestElement
	 * @param p
	 * @return
	 */
	public BitRepresentation preamble(int p, int q) {
		if (q == 0) return new BitRepresentation(
				representation.and(allOne(1).shiftLeft(length-1)),largestElement);
		if (p == 0) return new BitRepresentation(BigInteger.ZERO,largestElement);
		BigInteger all = allOne(1<<(p-1)).shiftLeft((1<<(q-1))-(1<<(p-1)));
		return new BitRepresentation(
				representation.and(all.shiftLeft(length-(1<<q))).shiftRight(length-(1<<q)+(1<<(q-1))-(1<<(p-1))),
					p-1);
	}

	@Override
	public BitRepresentation dual() {
		BigInteger res = universe;
		for (int i=0;i<length;i++)
			if (representation.testBit(i)) {
				res = res.clearBit(length - i - 1);
			}
		return new BitRepresentation(res,universe,largestElement);
	}

	@Override
	public boolean ge(LatticeElement e1) {
		return this.join(e1).equals(this);
	}

	@Override
	public boolean le(LatticeElement e1) {
		return this.join(e1).equals(e1);
	}

	@Override
	public boolean equals(LatticeElement e) {
		return representation.equals(((BitRepresentation) e).representation);
	}
	
	/**
	 * Compares all preambles of elts p with the corresponding preambles at elts q>p and returns true
	 * only if all preambles p are greater 
	 * @return greater preambles p come first
	 */
	public boolean comparePreambles() {
		for (int p=1;p<=largestElement;p++) {
			BitRepresentation prep = preamble(p);
			for (int q=p+1;q<=largestElement;q++) {
				if (preamble(p,q).representation.compareTo(prep.representation) > 0) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Compares all preambles of elts p<start with the corresponding preambles at elts q>=start and returns true
	 * only if all preambles p are greater 
	 * @return greater preambles p come first
	 */
	public boolean comparePreambles(int start) {
		for (int p=1;p<start;p++) {
			BitRepresentation prep = preamble(p);
			for (int q=start;q<=largestElement;q++) {
				if (preamble(p,q).representation.compareTo(prep.representation) > 0) return false;
			}
		}
		return true;
	}
	/**
	 * Compares all preambles in this p<start with all corresponding preambles in b and returns true
	 * only if all preambles p are greater 
	 * @return greater preambles p come first in the concatenation of this and b
	 */
	public boolean comparePreambles(BitRepresentation b) {
		return new BitRepresentation(this.representation.shiftLeft(b.length).or(b.representation),
				universe.shiftLeft(b.length).or(b.universe),
				largestElement + b.largestElement).comparePreambles(largestElement+1);
	}

	public static long dedkind(int n) {
		BitRepresentation e = new BitRepresentation(BigInteger.ZERO,n);
		return dedekind(0,e.getPhi(1),1,n);
	}

	private static long dedekind(int exp, BitRepresentation a, int l, int n) {
		BitRepresentation ap = a.getPlus().intersect(a.getPhi(l+1));
		BitRepresentation app = ap.getPlus().intersect(a.getPhi(l+1));
		return 0;
	}

	/**
	 * compute the intersection of two amf in bitrepresentaion
	 * @param b the set to intersect with
	 * @return the sets that are in this as well as in b
	 */
	public BitRepresentation intersect(BitRepresentation b) {
		BigInteger res = BigInteger.ZERO;
		BigInteger rep1 = representation;
		BigInteger rep2 = b.representation;
		int s1 = rep1.getLowestSetBit();
		int s2 = rep2.getLowestSetBit();
		if (s1 >= 0) rep1 = rep1.or(getSet(length-s1-1)).xor(getSet(length-s1-1));
		if (s2 >= 0) rep2 = rep2.or(getSet(length-s2-1)).xor(getSet(length-s2-1));
		while (s1 >= 0 && s2 >= 0) {
			if (s1 == s2) {
				res = res.or(getSet(length-s1-1));
				s1 = rep1.getLowestSetBit(); 
				if (s1 >= 0) rep1 = rep1.or(getSet(length-s1-1)).xor(getSet(length-s1-1));
				s2 = rep2.getLowestSetBit(); 
				if (s2 >= 0) rep2 = rep2.or(getSet(length-s2-1)).xor(getSet(length-s2-1));				
			}
			else if (s1 < s2) {
				s1 = rep1.getLowestSetBit(); 
				if (s1 >= 0) rep1 = rep1.or(getSet(length-s1-1)).xor(getSet(length-s1-1));
			}
			else {
				s2 = rep2.getLowestSetBit(); 
				if (s2 >= 0) rep2 = rep2.or(getSet(length-s2-1)).xor(getSet(length-s2-1));				
			}
		}
		return new BitRepresentation(res,universe,largestElement);
	}
	
	/**
	 * construct the amf consisting of the complements of the sets in this
	 * @return
	 */
	public BitRepresentation tilde() {
		BigInteger res = BigInteger.ZERO;
		BigInteger rep = representation;
		int s = rep.getLowestSetBit();
		while (s >= 0) {
			BigInteger set = getSet(length - s - 1);
			rep = rep.or(set).xor(set);
			res = res.or(getSet(length - (s^(length-1)) - 1));
			s = rep.getLowestSetBit();
		}
		return new BitRepresentation(res,universe,largestElement);
	}

	/**
	 * the empty amf in the representation with i as the largest element
	 * @param i
	 * @return
	 */
	public static BitRepresentation empty(int i) {
		return new BitRepresentation(BigInteger.ZERO,i);
	}

	/**
	 * concatenate the representation of this with n and produce a new bitrepresentation
	 * representing the amf m V (n x {largestElement+1))
	 * @pre length == n.length
	 * @pre n.le(m)
	 * @param n
	 * @return the representation of m V (n x {largestElement+1))
	 */
	public BitRepresentation concatenate(BitRepresentation n) {
		return new BitRepresentation(representation.shiftLeft(length).or(n.representation), universe.shiftLeft(length).or(universe),largestElement + 1);
	}

	@Override
	public int compareTo(BitRepresentation arg0) {
		return representation.compareTo(arg0.representation);
	}

	/**
	 * return the BitRepresentation with getSet(s) added
	 * @param s
	 * @return
	 */
	public BitRepresentation addSet(int s) {
		return new BitRepresentation(this.representation.or(this.getSet(s)),universe,largestElement);
	}

	/**
	 * get the BitRepresentation of the sets of even size in this Bitrepresentation element
	 * @return
	 */
	public BitRepresentation getEvenSets() {
		return new BitRepresentation(representation.and(getEvenSizedSets(length)),universe,largestElement);
	}

	/**
	 * get the BitRepresentation of the sets of odd size in this Bitrepresentation element
	 * @return
	 */
	public BitRepresentation getOddSets() {
		return new BitRepresentation(representation.and(getOddSizedSets(length)),universe,largestElement);
	}

	/**
	 * get the bits representing the sets of even size in the representation of length
	 * @param length
	 * @return
	 */
	public static BigInteger getEvenSizedSets(int length) {
		return evenSizedSets.shiftRight(numberOfSets - length);
	}
	/**
	 * get the bits representing the sets of odd size in the representation of length
	 * @param length
	 * @return
	 */
	public static BigInteger getOddSizedSets(int length) {
		return oddSizedSets.shiftRight(numberOfSets - length);
	}
	/**
	 * get the bits representing the sets of even size in the representation of length
	 * @param length
	 * @return
	 */
	public BigInteger getEvenSizedSets() {
		return evenSizedSets.shiftRight(numberOfSets - length);
	}
	/**
	 * get the bits representing the sets of odd size in the representation of length
	 * @return
	 */
	public BigInteger getOddSizedSets() {
		return oddSizedSets.shiftRight(numberOfSets - length);
	}


	/**
	 * replace each of the sets with their immediate successor
	 * keep only sets represented in limits by one bit
	 * @param limits 
	 * @return
	 */
	public BitRepresentation removeTopSets(BigInteger limits) {
		BigInteger ref = representation;
		BigInteger rep = representation;
		int s = ref.getLowestSetBit();
		while (s >= 0) {
			if (limits.testBit(s)) rep = rep.clearBit(s);
			else rep = rep.andNot(getSet(length - s -1));
			ref = ref.andNot(getSet(length - s -1));
			s = ref.getLowestSetBit();
		}
		return new BitRepresentation(rep,universe,largestElement);

	}
	/**
	 * replace each of the sets with their immediate successor
	 * @param limits 
	 * @return
	 */
	public BitRepresentation removeTopSets() {
		BigInteger ref = representation;
		BigInteger rep = representation;
		int s = ref.getLowestSetBit();
		while (s >= 0) {
			rep = rep.clearBit(s);
			ref = ref.andNot(getSet(length - s -1));
			s = ref.getLowestSetBit();
		}
		return new BitRepresentation(rep,universe,largestElement);

	}
	
	/**
	 * add each set having all its immediate subsets in the amf
	 * keep only sets represented by one bit in limits
	 * @param limits 
	 * @return
	 */
	public BitRepresentation addTopSets(BigInteger limits) {
		return completeSet(dual().removeTopSets().dual().representation.and(limits));
	}
	/**
	 * add each set having all its immediate subsets in the amf
	 * @param limits 
	 * @return
	 */
	public BitRepresentation addTopSets() {
		return dual().removeTopSets().dual();
	}

//	private static Iterable<BitRepresentation> amf(final BitRepresentation p, final int l, final int n) {
//		return new Iterable<BitRepresentation>() {
//
//			@Override
//			public Iterator<BitRepresentation> iterator() {
//				return new Iterator<BitRepresentation>() {
//
//					BitRepresentation current = p;
//					BitRepresentation original = p;
//					
//					long number = (1 << SmallBasicSet.combinations(n, l)) - p.size();
//					long gehad = 0;
//							
//					@Override
//					public boolean hasNext() {
//						return gehad < number;
//					}
//
//					@Override
//					public BitRepresentation next() {
//						BigInteger res = current.representation;
//						int s = ;
//						while (res.testBit(s)) {
//							if (original.representation.testBit(s)) s++;
//							else {
//								res = res.clearBit(s);
//								s++;
//							}
//						}
//						res.setBit(s);
//						gehad++; 
//						return new BitRepresentation(res,current.largestElement);
//					}
//
//					@Override
//					public void remove() {
//						// TODO Auto-generated method stub
//						
//					}
//				};
//			}			
//		};
//	}
}
