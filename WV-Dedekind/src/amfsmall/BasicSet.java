package amfsmall;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Sets of integers to be used as the base elements in the space of antimonotonic functions
 * immutable
 * @author u0003471
 *
 */
public class BasicSet implements Iterable<Integer> {

	private HashSet<Integer> theSet;
	
	private static BasicSet theEmptySet = new BasicSet();
	
	public static BasicSet emptySet() {
		return theEmptySet;
	}
	
	private BasicSet() {
		theSet = new HashSet<Integer>();
	}
	/**
	 * construct a set from an array
	 * @param is
	 */
	public BasicSet(int[] is) {
		theSet = new HashSet<Integer>();		
		for (Integer i : is) {
				theSet.add(i);
		}
	}
	
	/**
	 * split the basic set in n equally sized parts
	 * @param n
	 * @return an array of n disjoint basic sets, the n parts, the union is this
	 */
	public BasicSet[] split(int n) {
		BasicSet[] res = new BasicSet[n];
		int p = 0;
		for (p=0;p<n;p++) res[p] = new BasicSet();
		p = 0;
		for (int i : theSet) {
			res[p].theSet.add(i);
			p = (p + 1) % n;
		}
		return res;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * the number of subsets of this basic set
	 * @return 2^size()
	 */
	public long numberOFSubsets() {
		long res = 1L;
		return res << theSet.size();
	}

	/**
	 * the number of elements in this set
	 * @param b
	 * @return size
	 */
	public long size() {
		return theSet.size();
	}
	
	/**
	 * subset relationship
	 * @param b
	 * @return is b a subset of this set
	 */
	public boolean hasAsSubset(BasicSet b) {
		return this == b || theSet.containsAll(b.theSet);
	}

	/**
	 * pick one element at random from this set
	 * throws an NoSuchElementException if the set is empty
	 * @param sup
	 */
	public int getOneFrom() throws NoSuchElementException {
		if (isEmpty()) throw new NoSuchElementException();
		Integer[] array = new Integer[theSet.size()];
		theSet.toArray(array);
		double index = (float) Math.random()*theSet.size();
		return array[(int) Math.floor(index)];
	}
	
	/**
	 * checks whether this set has any elements
	 * @return the set has no elements
	 */
	private boolean isEmpty() {
		return theSet.isEmpty();
	}
	/**
	 * Is the set b equal to this set
	 * 
	 * @param b
	 * @return b contains the all elements of this and this contains all elements of b
	 */
	public boolean equals(BasicSet b) {
		return this == b || theSet.equals(b.theSet);	
	}

	/**
	 * compute the difference of two BasicSets
	 * as a new basic set
	 * @param basicSet
	 * @return this \ basicSet
	 */
	public BasicSet minus(BasicSet basicSet) {
		BasicSet res = new BasicSet();
		res.theSet.addAll(theSet);
		res.theSet.removeAll(basicSet.theSet);
		return res;
	}

	/**
	 * compute the difference of this BasicSet and the set containing x
	 * as a new basic set
	 * @param x
	 * @return this \ {x}
	 */
	public BasicSet minus(Integer x) {
		BasicSet res = new BasicSet();
		res.theSet.addAll(theSet);
		res.theSet.remove(x);
		return res;
	}

	/**
	 * compute the union of two BasicSets
	 * as a new basic set
	 * @param basicSet
	 * @return this U basicSet
	 */
	public BasicSet union(BasicSet basicSet) {
		BasicSet res = new BasicSet();
		res.theSet.addAll(theSet);
		res.theSet.addAll(basicSet.theSet);
		return res;
	}

	/**
	 * compute the intersection of two BasicSets
	 * as a new basic set
	 * @param basicSet
	 * @return this intersection basicSet
	 */
	public BasicSet intersection(BasicSet basicSet) {
		BasicSet res = new BasicSet();
		res.theSet.addAll(theSet);
		res.theSet.retainAll(basicSet.theSet);
		return res;
	}

	/**
	 * construct a basic set with one extra element
	 * @param x the extra element
	 * return this U {x}
	 */
	public BasicSet add(int x) {
		BasicSet res = new BasicSet();
		res.theSet = (HashSet<Integer>) theSet.clone();
		res.theSet.add(x);
		return res;
	}
	
	/**
	 * construct a basic set with a number of extra elements
	 * @param x an array of integer
	 * return this U {i|i in x}
	 */
	public BasicSet add(int[] x) {
		BasicSet res = new BasicSet();
		res.theSet = (HashSet<Integer>) theSet.clone();
		for (Integer i : x) res.theSet.add(i);
		return res;
	}
	
	/**
	 * return a string representation for display only
	 * return the set described in a string
	 */
	public String toString() {
		return theSet.toString();
	}

	@Override
	public Iterator<Integer> iterator() {
		// TODO Auto-generated method stub
		return new Iterator<Integer>() {

			Iterator<Integer> it = theSet.iterator();
			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public Integer next() {
				return it.next();
			}

			@Override
			public void remove() throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}
			
		};
	}

	public static Parser<BasicSet> parser() {
		return new Parser<BasicSet>() {

			
			@Override
			public BasicSet parse(String S) throws SyntaxErrorException {

				return BasicSet.parse(S);
			}

			@Override
			public boolean makesSense(String input) {
				try {
					BasicSet.parse(input);
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
	private static BasicSet parse(String r) throws SyntaxErrorException{
		r = Parser.removeSpaces(r);
		int opening = r.indexOf('[');
		int closure = r.indexOf(']');
		int skipped = 0;
		if (opening != 0) {
			// string representation, elements are represented by characters satisfying isAccepatableCharElement
			if (r.charAt(0) == '0') return BasicSet.emptySet();
			else if (isAcceptableCharElement(r.charAt(0))) {
				BasicSet ret = BasicSet.emptySet();
				int p = 0;
				do {
					ret = ret.add(toIntegerElement(r.charAt(p++)));
				}
				while (p < r.length() && isAcceptableCharElement(r.charAt(p)));
				return ret;
			}
			else throw new SyntaxErrorException("BasicSet parsing \"" + r + "\": No introducing '[' found");
		}
		if (closure == -1) throw new SyntaxErrorException("BasicSet parsing \"" + r  + "\" : No closing ']' found");
		BasicSet b = emptySet();
		r = r.substring(opening + 1);
		skipped += opening + 1;
		int comma = r.indexOf(',');
		while ( comma >=0 && skipped + comma < closure) {
			String s = r.substring(0, comma);
			try {
				b = b.add(Integer.parseInt(s));
			} catch (NumberFormatException e) {
				throw new SyntaxErrorException("BasicSet parsing \"" 
						+ r + "\" at \"" + s + "\": no number found\n(" + e + ")");
			}
			r = r.substring(comma + 1);
			skipped += comma + 1;
			comma = r.indexOf(',');
		}
		String s = r.substring(0, closure - skipped);
		if (s.length() > 0)
			try {
				b = b.add(Integer.parseInt(s));
			} catch (NumberFormatException e) {
				throw new SyntaxErrorException("BasicSet parsing \"" 
						+ r + "\" at \"" + s + "\": no number found\n(" + e + ")");
			}
		return b;
	}

	/**
	 * translate an accepatable character to an integer
	 * @param c
	 * @pre isAcceptableCharElement(c)
	 * @return
	 */
	protected static int toIntegerElement(char c) {
		return Character.digit(c,16);
	}

	/**
	 * accept only characters in the range '1'-'9'and 'a' tp 'f'  (hex)
	 * @param charAt
	 * @return
	 */
	protected static boolean isAcceptableCharElement(char c) {
		return '0' < c && c <= '9' || 'a' <= c && c <= 'f';
	}

	/**
	 * elementary combinatorics
	 * @param i
	 * @param j
	 * @return
	 */
	public static long combinations(int i, int j) {
		long res = 1;
		int t = i - j;
		while (i > j) {res *= i;i--;}
		while (t > 1) {res /= t;t--;}
		return res;
	}

	public BasicSet remove(int f) {
		BasicSet res = new BasicSet();
		res.theSet.addAll(theSet);
		res.theSet.remove(f);
		return res;
	}

	public boolean contains(int f) {
		return theSet.contains(f);
	}

}
