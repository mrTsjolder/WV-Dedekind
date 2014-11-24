/**
 * 
 */
package amfsmall;

/**
 * General parser interface
 * allows to read an object of type T from a string
 * @author u0003471
 *
 */
public abstract class Parser<T> {
	/**
	 * Reads an object of type T from a String
	 * Returns the object if the string correctly describes a type T object
	 * Throws a SyntaxErrorException if the string does not represent a type T object
	 * @param S
	 * @return object of type T
	 * @throws SyntaxErrorException must be thrown if no object can be created from the string
	 */
	public abstract T parse(String S) throws SyntaxErrorException;
	
	/**
	 * remove the spaces from a string prior to parsing
	 * @param r
	 * @return string r with spaces removed
	 */
	public static String removeSpaces(String r) {
		String res = "";
		for (int i=0;i<r.length();i++) if (r.charAt(i) != ' ') res += r.charAt(i);
		return res;
	}

	abstract public boolean makesSense(String input);
	}
