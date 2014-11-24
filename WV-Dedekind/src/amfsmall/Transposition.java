package amfsmall;

import java.util.Iterator;

/**
 * the transpostion of two integers
 * 
 * @author patrickdecausmaecker
 *
 */
public class Transposition {
	/**
	 * the two integers to be interchanged and
	 * a set containing these two
	 */
	private int p1, p2;
	private SmallBasicSet basicSet;
	public int getP1() {return p1;}
	public int getP2() {return p2;}
	public SmallBasicSet getBasicSet() {return basicSet;}
	public Transposition(int a,int b) {
		p1 = a;
		p2 = b;
		basicSet = SmallBasicSet.emptySet().add(p1).add(p2);
	}
	
	/**
	 * Creates a transposition  from a basic set, exchanging the 
	 * smallest and largest element
	 * @pre s.size() >= 2
	 * @param s
	 */
	public Transposition(SmallBasicSet s) {
		this(s.minimum(),s.maximum());
	}
	
	/**
	 * apply the transposition to a smallbasicset
	 * @param upon the small basic set to apply the transposition
	 * @return the set on which getP1() and getP2() have been interchanged
	 */
	public SmallBasicSet apply(SmallBasicSet upon) {
		SmallBasicSet res = upon.remove(p1).remove(p2);
		if (upon.contains(p1)) res = res.add(p2);
		if (upon.contains(p2)) res = res.add(p1);
		return res;
	}
	
	/**
	 * apply the transposition to an AntiChain
	 * @param upon the input AntiChain
	 * @return the transposed
	 */
	public AntiChain apply(AntiChain upon) {
		AntiChain res = new AntiChain();
		for (SmallBasicSet s : upon) res.add(apply(s));
		return res;
	}
	
	/**
	 * an iterator for the transpositions of m..n
	 * @param m,n the smallest and largest elements
	 * @return the iterator
	 */
	public static Iterator<Transposition> iterator(final int m, final int n) {
		return new Iterator<Transposition>() {
			
			private int end = n;
			private int currenta = m;
			private int currentb = m+1;
			
			@Override
			public boolean hasNext() {
				return currenta < end-1 || currenta == end-1 && currentb <= end;
			}

			@Override
			public Transposition next() {
				Transposition res = new Transposition(currenta,currentb);
				if (currentb < end) currentb++;
				else {
					currenta++;
					currentb = currenta+1;
				}
				return res;
			}

			@Override
			public void remove() {
				
			}
		};
	}
	
	public SmallBasicSet toSmallSet() {
		return SmallBasicSet.emptySet().add(p1).add(p2);
	}	
}
