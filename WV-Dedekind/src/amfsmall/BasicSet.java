package amfsmall;

import java.util.HashSet;
import java.util.Iterator;

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
	 * construct a basic set with one extra element
	 * @param x the extra element
	 * return this U {x}
	 */
	@SuppressWarnings("unchecked")
	public BasicSet add(int x) {
		BasicSet res = new BasicSet();
		res.theSet = (HashSet<Integer>) theSet.clone();
		res.theSet.add(x);
		return res;
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

}
