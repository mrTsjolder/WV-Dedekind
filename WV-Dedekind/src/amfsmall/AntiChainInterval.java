/**
 * 
 */
package amfsmall;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import posets.IntervalPoset;
import posets.IntervalPosetCertificat;
import posets.SetsPoset;

/**
 * An interval of antimonotonic functions
 * @author u0003471
 *
 */
public class AntiChainInterval implements Iterable<AntiChain>,Comparable<AntiChainInterval> {
	
	/**
	 * an interval is described by its lower bound (from) and upper bound (till)
	 * it may be ]..[,[..[,]..],[..] depending on closed[Below,Above]
	 */
	private AntiChain from;
	private AntiChain till;
	private boolean closedBelow;
	private boolean closedAbove;

	/**
	 * Create an interval of antimonotonic functions with given limits and closed or open brackets
	 * @param bottom : lower limit
	 * @param top : upper limit
	 * @param closedBottom : true if closed at lower limit
	 * @param closedTop : true if closed at upper limit
	 */
	public AntiChainInterval(AntiChain bottom,AntiChain top,boolean closedBottom,boolean closedTop) {
		from = bottom;
		till = top;
		closedBelow = closedBottom;
		closedAbove = closedTop;
	}

	/**
	 * Create an interval of antimonotonic functions with given limits and closed or open brackets
	 * and an externally defined method to divide intervals while enumerating
	 * @param bottom : lower limit
	 * @param top : upper limit
	 * @param closedBottom : true if closed at lower limit
	 * @param closedTop : true if closed at upper limit
	 * @param f : the externally defined method
	 */
	public AntiChainInterval(AntiChain bottom,AntiChain top,boolean closedBottom,boolean closedTop,
			SubsetFinder f) {
		this(bottom,top,closedBottom,closedTop);
		setFinder(f);
	}

	/**
	 * Create a closed interval of antimonotonic functions with given limits and closed or open brackets
	 * @param bottom : lower limit
	 * @param top : upper limit
	 */
	public AntiChainInterval(AntiChain bottom,AntiChain top) {
		this(bottom,top,true,true);
	}
	
	public void setFinder(SubsetFinder f) {
		finder = f;
	}

	/**
	 * Create a closed interval of antimonotonic functions with given limits and closed or open brackets
	 * and an externally defined method to divide intervals while enumerating
	 * @param bottom : lower limit
	 * @param top : upper limit
	 * @param f : the externally defined method
	 */
	public AntiChainInterval(AntiChain bottom,AntiChain top,SubsetFinder f) {
		this(bottom,top,true,true);
		setFinder(f);
	}
	
	/**
	 * The Antimonotonic at the bottom of the interval 
	 */
	public AntiChain getBottom() {
		return from;
	}
	/**
	 * The Antimonotonic at the top of the interval 
	 */
	public AntiChain getTop() {
		return till;
	}
	/**
	 * Is the interval closed at the bottom
	 */
	public boolean isClosedAtBottom() {
		return closedBelow;
	}
	/**
	 * Is the interval closed at the top
	 */
	public boolean isClosedAtTop() {
		return closedAbove;
	}
	
	/**
	 * return a string describing the interval
	 * with lower and upper limit and brackets
	 */
	public String toString() { 
		String res = "";
		if (isClosedAtBottom()) res += "[";
		else res += "]";
		res += getBottom() + ", " + getTop();
		if (isClosedAtTop()) res += "]";
		else res += "[";
		return res;
	}
	
	/**
	 * encode an interval into a biginteger
	 * the bottom encoding is pushed first
	 * the top encoding is pushed second
	 * @return
	 */
	public BigInteger encode() {
		BigInteger res = getBottom().encode(); 
		BigInteger top = getTop().encode();
		return res.shiftLeft((int) (4*Math.ceil(top.bitLength()/4.0) + 8)).add(top); // bottom and top separated by two zero half bytes
	}
	
	/**
	 * decode an interval from a biginteger
	 * top is popped first
	 * bottom is popped second
	 * since AntiChainInterval is immutable, a version returning the remaining biginteger is not possible
	 * @param b
	 * @return
	 */
	public static AntiChainInterval decode(BigInteger b) {
		AntiChain top = new AntiChain();
		AntiChain bottom = new AntiChain();
		b = AntiChain.decode(top, b);
		b = AntiChain.decode(bottom, b);
		return new AntiChainInterval(bottom,top);
	}

	/**
	 * iterator delegates to closed iterator

	 */
	public Iterator<AntiChain> iterator() {
		// empty interval?
		if (!getBottom().le(getTop()) ||
				getBottom().equals(getTop()) 
				&& (!isClosedAtBottom() || !isClosedAtTop())) // interval is empty
			return new Iterator<AntiChain>() {
			
			@Override
			public boolean hasNext() {
				return false;
			}
			
			@Override
			public AntiChain next() {
				return null;
			}
			
			@Override
			public void remove() {
			}
					
		};
		if (isClosedAtTop()) {
			Iterator<AntiChain> theIt = closedIterator();
			if (!isClosedAtBottom()) theIt.next();
			return theIt;
		}
		else return new Iterator<AntiChain>() {

			boolean thereIsNext;
			Iterator<AntiChain> theIt = closedIterator();
			AntiChain nxt = null;
			{
				if (!isClosedAtBottom()) theIt.next();
				if (theIt.hasNext()) {
					nxt = theIt.next();
					thereIsNext = theIt.hasNext();					
				}
				else thereIsNext = false;
			}
			
			@Override
			public boolean hasNext() {
				return thereIsNext;
			}

			@Override
			public AntiChain next() {
				AntiChain myNxt = nxt;
				nxt = theIt.next();
				thereIsNext = theIt.hasNext();
				return myNxt;
			}

			@Override
			public void remove() {
				// TODO Auto-generated method stub
				
			}
			
		};
	}
	
	/**
	 *
	 * iterator ignoring the boundaries
	 * @pre getBottom().le(getTop())
	 * @return an iterator ignoring the boundaries
	 */
	private Iterator<AntiChain> closedIterator() {

		/*
		 * case lower limit is empty
		 */
		if (getBottom().size() == 0) {
			return exceptionalClosedIterator();
		}
		final SmallBasicSet span = getTop().sp();
		if (span.size()<=1) return new Iterator<AntiChain>() {
/*
 * Iterator in the case of dimension 1 or 0, explicitly computed
 */
			private AntiChain[] theList;
			private int pos, last;
			{
				if (getBottom().size() == 0) pos = 0;
				else for (SmallBasicSet b : getBottom()) if (b.size() == 0) pos = 1;
				else pos = 2;
				
				if (getTop().size() == 0) last = 0;
				else for (SmallBasicSet b : getTop()) if (b.size() == 0) last = 1;
				else last = 2;
				
				if (pos <= last) {
					theList = new AntiChain[last + 1];
					if (pos == 0) {
						AntiChain amf;
						theList[0] = new AntiChain(getUniverse()); // empty function
						if (last > 0) {
							amf = new AntiChain();
							amf.add(SmallBasicSet.emptySet()); // empty set
							theList[1] = amf;
						}
						if (last > 1) {
							amf = new AntiChain(getUniverse());
							amf.add(span); // set with one element
							theList[2] = amf;
						}
					}
					else if (pos == 1) {
						AntiChain amf;
						if (last > 0) {
							amf = new AntiChain(getUniverse());
							amf.add(SmallBasicSet.emptySet()); // empty set
							theList[1] = amf;
						}
						if (last > 1) {
							amf = new AntiChain(getUniverse());
							amf.add(span); // set with one element
							theList[2] = amf;
						}
						
					}
					else /* pos == 2 */ {
						AntiChain amf;
						if (last > 1) {
							amf = new AntiChain(getUniverse());
							amf.add(span); // set with one element
							theList[2] = amf;
						}
					}	
				}
		}
			
			@Override
			public boolean hasNext() {
				return pos <= last;
			}

			@Override
			public AntiChain next() {
				return theList[pos++];
			}

			@Override
			public void remove() {
				// TODO Auto-generated method stub	
			}
			
		};
		if (getTop().equals(getBottom())) {
			// iterator for one element
			return new Iterator<AntiChain>() {

				boolean given;
				{
					given = false;
				}
				@Override
				public boolean hasNext() {
					return !given;
				}

				@Override
				public AntiChain next() {
					given = true;
					return getBottom();
				}

				@Override
				public void remove() {
					// TODO Auto-generated method stub
					
				}
				
			};
		}
		// iterator general case
		long spanSize = getTop().sp().size();
		long minSizeBottom = spanSize;
		for (SmallBasicSet a : getBottom()) if (a.size() < minSizeBottom) minSizeBottom = a.size();
		if (getTop().size() == 1 
				&& getBottom().size() == spanSize
				&& minSizeBottom + 1 == spanSize) {
			// irreducible interval of two elements
			return new Iterator<AntiChain>() {

				int pos = 0;
				@Override
				public boolean hasNext() {
					return pos < 2;
				}

				@Override
				public AntiChain next() {
					pos++;
					if (pos == 1) return getBottom();
					if (pos == 2) return getTop();
					return null;
				}

				@Override
				public void remove() {
					// TODO Auto-generated method stub
					
				}
				
			};
		}

		/**
		 * compute optimal split of the spaces represented by 'axes'
		 */
		final SmallBasicSet[] axes = bestSplit();
		return new Iterator<AntiChain>() {

/**
 * Iterator general case is reduced to the twice half the dimension
 */
			Iterator<AntiChain> X;
			Iterator<AntiChain> Y;
			AntiChainInterval Xaxis = new AntiChainInterval(getBottom().project(axes[0]),
						getTop().project(axes[0]),true,true);
			AntiChainInterval Yaxis = new AntiChainInterval(getBottom().project(axes[1]),
						getTop().project(axes[1]),true,true);
			Iterator<AntiChain> current;
			AntiChain currentX, currentY;
			{
				X = Xaxis.iterator();
				Y = Yaxis.iterator();
				if (X.hasNext() && Y.hasNext()) {
					currentX = X.next();
					currentY = Y.next();
					current = 
						new AntiChainInterval(currentX.plus(currentY).plus(getBottom()),
								currentX.times(currentY).dot(getTop()),true,true).iterator();
				}
				else current = new AntiChainInterval(new AntiChain(),
						new AntiChain(),false,false).iterator(); // iterator on an empty interval
			}
			@Override
			public boolean hasNext() {
				return  current.hasNext() || X.hasNext() || Y.hasNext();
			}

			@Override
			public AntiChain next() {
				if (current.hasNext()) return current.next();
				if (X.hasNext()) {
					currentX = X.next();
					current = 
						new AntiChainInterval(currentX.plus(currentY).plus(getBottom()),
								currentX.times(currentY).dot(getTop()),true,true).iterator();
					return current.next();
				}
				else if (Y.hasNext()) /* should always be true */ {
					X = Xaxis.iterator();
					if (X.hasNext() && Y.hasNext()) {
						currentX = X.next();
						currentY = Y.next();
						current = 
							new AntiChainInterval(currentX.plus(currentY).plus(getBottom()),
									currentX.times(currentY).dot(getTop()),true,true).iterator();
						return current.next();
					}
				}
				System.out.println("SHOULD NOT HAPPEN");
				return null; // should never happen
			}

			@Override
			public void remove() {
				// TODO Auto-generated method stub
				
			}
			
		};
	}

	private SmallBasicSet getUniverse() {
		return getTop().getUniverse();
	}

	private Iterator<AntiChain> exceptionalClosedIterator() {
		return new Iterator<AntiChain>() {
			AntiChain bottom;
			boolean virgin;
			Iterator<AntiChain> normal ;

			{
				bottom = new AntiChain();
				bottom.add(SmallBasicSet.emptySet());
				virgin = true;
				normal = new AntiChainInterval(bottom,getTop(),true,true).closedIterator();
			}
			@Override
			public boolean hasNext() {
				return virgin || normal.hasNext() ;
			}

			@Override
			public AntiChain next() {
				if (virgin) {
					virgin = false;
					return getBottom();
				}
				else return normal.next();
			}

			@Override
			public void remove() {
			}
		};
	}

	/**
	 * produce a split of the universe that produces two non empty intervals of which at least one
	 * is non singleton
	 * @pre size() >= 3
	 * @return
	 */
	public SmallBasicSet[] bestSplit() {
		SmallBasicSet[] res = new SmallBasicSet[2];
		SmallBasicSet best = SmallBasicSet.emptySet();
		SmallBasicSet span = getTop().sp();
		long spanSize = span.size();
		if (getTop().size() > 1) {
			// easy case. Look for a set in getTop with size about half the span of getTop
			// that is not in getBottom
			AntiChain difference = getTop().minus(getBottom());
			long value = spanSize;
			for (SmallBasicSet a : difference)
				if (Math.abs(a.size() - spanSize/2) < value) {
					value = Math.abs(a.size() - spanSize/2);
					best = a;
				}
		}
		else {
			// only one element in getTop, call it O
			// Look up a subset of O that is not a subset of an element in getBottom 
			// with about half the size of O
			// this problem is np-hard
			// we use an exhaustive procedure that potentially
			// takes exponential time in the size of O
			best = finder.bestSubset(span,spanSize/2,getBottom());
		}
		res[0] = best;
		res[1] = span.minus(best);
		return res;
	}
	
	/**
	 * list all elements of this interval in a string
	 * @return the list of elements as a string
	 */
	public String expand() {
		String res = "";
		for (AntiChain amf : this) res += amf;
		return res;
	}
	
	/**
	 * the lattice of the sources in this interval is isomorphic with the lattice of subsets
	 * @return list of subsets
	 */
	public TreeSet<SmallBasicSet> getSources() {
		TreeSet<SmallBasicSet> res = new TreeSet<SmallBasicSet>();
		for (SmallBasicSet t : getTop()) {
			res.addAll(t.getSubSets());
			}
		for (SmallBasicSet b: getBottom()) {
			res.removeAll(b.getSubSets());
		}
		return res;
	}
	
	/**
	 * the lattice of the sources in this interval is isomorphic with the lattice of subsets
	 * @return list of subsets
	 */
	public TreeMap<Long,TreeSet<SmallBasicSet>> getSourcesRanked() {
		TreeSet<SmallBasicSet> theSet = getSources();
		TreeMap<Long,TreeSet<SmallBasicSet>> res = new TreeMap<Long, TreeSet<SmallBasicSet>>();
		for (SmallBasicSet t : theSet) {
			if (!res.containsKey(t.size())) {
				res.put(t.size(),new TreeSet<SmallBasicSet>());
			}
			res.get(t.size()).add(t);
		}
		return res;
	}
	
	/**
	 * count the number of elements in this interval
	 * @return the number of elements in this interval
	 */
	public long countedSize() {
		long res = 0;
		for (AntiChain amf: this) res ++;
		return res;
	}
	
	/**
	 * Compute the size of the interval as the product of its direct join components
	 * or as the product of its dual direct join components whichever has more components
	 * @return
	 */
	public long size() {
		long res = 1;
		SortedSet<AntiChainInterval> dec = this.decompose();
		SortedSet<AntiChainInterval> decd = this.decomposeDual();
		if (decd.size() > dec.size()) dec = decd;
		for (AntiChainInterval b : decd) res *= b.latticeSize();
		return res;
	}

	/**
	 * interface to compute a suitable subset for splitting
	 * the span of an algorithm such that the algorithm can 
	 * be decomposed
	 *
	 */
	public interface SubsetFinder {
		/**
		 * Look up a subset of span that is not a subset of an element in bottom 
		 * with size about target (indicative)
		 * this problem is in principle np-hard
		 * any approximation is acceptable
		 * @pre bottom not an immediate predecessor of {span}
		 * @param span : the mother set
		 * @param target : the approximate size wanted
		 * @param bottom : the AntiChain in which the answer cannot be contained
		 * @return null if span le bottom
		 * @return the suset of span with the closest size to target not contained in bottom
		 */
		SmallBasicSet bestSubset(SmallBasicSet span, long target, AntiChain bottom);
	};
	
	private SubsetFinder finder = new SubsetFinder() {

		@Override
		/**
		 * Look up a subset of span that is not a subset of an element in bottom 
		 * with about half target
		 * this problem is np-hard
		 * we use an exhaustive procedure that takes exponential time in the size of O
		 * @param span : the mother set
		 * @param target : the approximate size wanted
		 * @param bottom : the AntiChain in which the answer cannot be contained
		 * @return null if span le bottom
		 * @return the suset of span with the closest size to target not contained in bottom
		 */
		public SmallBasicSet bestSubset(SmallBasicSet span, long target, AntiChain bottom) {

			SmallBasicSet best = null;
			long value = 2*target;
			if (!bottom.ge(span)) {
				best = span;
				value = Math.abs(best.size() - target);
			}
			// if best is le bottom or is smaller than target no improvement can be expected
			if (best == null || best.size() <= target) return best;

			for (Integer i: span) {
				SmallBasicSet candidate = bestSubset(span.minus(i),target,bottom);
				if (candidate != null) {
					long newValue = Math.abs(candidate.size() - target);
					if (newValue < value) {
						best = candidate;
						value = newValue;
					}
				}
			}
			return best;
		}
	};
	
	public static Parser<AntiChainInterval> parser() {
		return new Parser<AntiChainInterval>() {

			
			@Override
			public AntiChainInterval parse(String S) throws SyntaxErrorException {

				return AntiChainInterval.parse(S);
			}
			
			@Override
			public boolean makesSense(String input) {
				try {
					AntiChainInterval.parse(input);
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
	private static AntiChainInterval parse(String r) throws SyntaxErrorException{
//		System.out.println("Interval parsing " + r);
		
		r = Parser.removeSpaces(r);

//		System.out.println((t++) + " : " + r);
		
		int opening = r.indexOf('[');
		boolean closedBottom = false;
		if (opening == 0) closedBottom = true;
		else {
			opening = r.indexOf(']');
			if (opening == 0) closedBottom = false;
		}
		if (opening != 0) throw new SyntaxErrorException("AntiChainInterval parsing \"" 
				+ r + "\": No introducing '[' or ']' found");

		r = r.substring(1); // skip bottom bracket
//		System.out.println((t++) + " : " + r);
		
		
		AntiChain bottom;
		try {
			bottom = AntiChain.parser().parse(r);
		} catch (SyntaxErrorException e) {
			throw new SyntaxErrorException("AntiChainInterval parsing \"" 
					+ r + "\": no AntiChain found\n(" + e + ")");
		}

		int bottomEnd = r.indexOf('}');
		if (bottomEnd + 2 > r.length()) throw new SyntaxErrorException("AntiChainInterval parsing \"" 
				+ r + "\": unexpected ending\n");
		
		if (r.charAt(bottomEnd + 1) != ',') throw new SyntaxErrorException("AntiChainInterval parsing \"" 
				+ r + "\": expected ,\n");
		
		r = r.substring(bottomEnd + 2);
//		System.out.println((t++) + " : " + r);
		
		AntiChain top;
		try {
			top = AntiChain.parser().parse(r);
		} catch (SyntaxErrorException e) {
			throw new SyntaxErrorException("AntiChainInterval parsing \"" 
					+ r + "\": no AntiChain found\n(" + e + ")");
		}

		int topEnd = r.indexOf('}');
		r = r.substring(topEnd);
//		System.out.println((t++) + " : " + r);
		
		
		if (r.length() < 2) throw new SyntaxErrorException("AntiChainInterval parsing \"" 
				+ r + "\": unexpected ending\n");
		
		r = r.substring(1);
//		System.out.println((t++) + " : " + r);
		

		boolean closedTop = false;
		int closure = r.indexOf(']');
		if (closure == 0) closedTop = true;
		else {
			closure = r.indexOf('[');
			if (closure == 0) closedTop = false;
		}
		if (closure != 0) throw new SyntaxErrorException("AntiChainInterval parsing \"" 
				+ r  + "\" : No closing '[' or ']' found");

		return new AntiChainInterval(bottom,top,closedBottom,closedTop);
	}
	
	/**
	 * get the poset underlying at this interval
	 * The poset starts from the bottom of the interval (the bottom does not belong to the poset)
	 * and contains all elements with only one predecessor in the interval
	 * @return a mapping of the bottom and all elements in the poset 
	 * 			with their set of immediate successors in the poset
	 */
	public HashMap<AntiChain,HashSet<AntiChain>> getRootElements() {
		HashMap<AntiChain,HashSet<AntiChain>> poSets = new HashMap<AntiChain,HashSet<AntiChain>>();
		AntiChain bottom = getBottom();
		poSets.put(bottom,new HashSet<AntiChain>());
		HashSet<SmallBasicSet> level = new HashSet<SmallBasicSet>();
		for (SmallBasicSet t : getTop()) if (!bottom.ge(t)) level.add(t);
		HashSet<SmallBasicSet> checked = new HashSet<SmallBasicSet>();
		while (!level.isEmpty()) {
			HashSet<SmallBasicSet> nextLevel = new HashSet<SmallBasicSet>();		
			for (SmallBasicSet t : level) {
				if (!checked.contains(t)) {
					checked.add(t);
					AntiChain e = new AntiChain(bottom);
					e.addConditionally(t);
					poSets.put(e, new HashSet<AntiChain>());
					for (SmallBasicSet ti : t.immediateSubSets())
						if (!bottom.ge(ti)) nextLevel.add(ti);
				}
			}
			level = nextLevel;
		}
		// set the immediate succession
		for (AntiChain f : poSets.keySet())
			for (AntiChain g : poSets.keySet()) {
				if (!f.equals(g) && f.le(g)) {
					boolean toBeAdded = true;
					for (Iterator<AntiChain> cit = poSets.get(f).iterator();cit.hasNext();) {
						AntiChain c = cit.next();
						if (g.ge(c)) {toBeAdded = false;break;}
						if (g.le(c)) cit.remove();
					}
					if (toBeAdded) poSets.get(f).add(g);
				}
			}
		return poSets;
	}
	

	/**
	 * get the poset underlying the dual of this interval
	 * The poset starts from the top of the interval (the top does not belong to the poset)
	 * and contains all elements with only one successor in the interval
	 * @return a mapping of the top and all elements in the poset 
	 * 			with their set of immediate predecessors in the poset
	 */
	public HashMap<AntiChain,HashSet<AntiChain>> getDualRootElements() {
		SmallBasicSet universe = getTop().sp();
		
		AntiChain dualTop = getTop().dual(universe);
		AntiChain dualBottom = getBottom().dual(universe);
		
		HashMap<AntiChain,HashSet<AntiChain>> dualPoSet = new AntiChainInterval(dualTop,dualBottom).getRootElements();
		HashMap<AntiChain,HashSet<AntiChain>> poSet = new HashMap<AntiChain,HashSet<AntiChain>>();
		
		for (AntiChain f : dualPoSet.keySet()) {
			AntiChain fd = f.dual(universe);
			poSet.put(fd, new HashSet<AntiChain>());
			for (AntiChain g : dualPoSet.get(f)) {
				poSet.get(fd).add(g.dual(universe));
			}
		}
		return poSet;
	}
	
	/**
	 * mu(tau,alfa) is the largest AMF within the interval for which
	 *        mu.meet(alfa) intersection alfa == tau 
	 * @pre tau is a subset of alfa
	 * @pre alfa is in the interval
	 * @per tau is in the interval
	 * @param tau
	 * @param alfa
	 * @return mu(tau,alfa)
	 */
	public AntiChain mu(AntiChain tau, AntiChain alfa) {
		AntiChain fC = alfa.minus(tau);
		AntiChain res = new AntiChain(getTop());
		AntiChain resR = new AntiChain();
		for (SmallBasicSet s : fC) {
			for (SmallBasicSet t : res) {
				if (t.hasAsSubset(s)) {
					for (int i : s) {
						if (!getBottom().ge(t.minus(i))) {
							resR.addConditionally(t.minus(i));
						}
					}
				}
				else {
					resR.addConditionally(t);
				}
			}
			AntiChain h = res;
			res = resR;
			resR = h;
			resR.clear();
		}
		res = res.join(getBottom());
		return res;
	}
	
	/**
	 * phi(tau, alfa) is the interval of elements chi in this satisfying
	 *        (chi.meet(alfa)).intersection(alfa).equals( tau)
	 * @pre alfa is in the interval
	 * @pre tau is in the interval
	 * @param tau
	 * @param alfa
	 * @return [tau.join(getBottom()), mu(tau,alfa)]
	 */
	public AntiChainInterval phi(AntiChain tau, AntiChain alfa) {
		AntiChain bottom = new AntiChain(tau).join(getBottom());
		return new AntiChainInterval(bottom, mu(tau,alfa));
	}

	/**
	 * iota(tau, alfa) is the interval of elements chi in this satisfying
	 *        chi.join(alfa).equals(tau)
	 * @pre alfa is in the interval
	 * @pre tau is in the interval and tau.le(alfa)
	 * @param tau
	 * @param alfa
	 * @return [getBottom().join(tau.iota(alfa), tau]
	 */
	public AntiChainInterval iota(AntiChain tau, AntiChain alfa) {
		AntiChain b = tau.iota(alfa);
		return new AntiChainInterval(getBottom().join(b), tau);
	}

	/**
	 * omicron(tau, alfa) is the interval of elements chi in this satisfying
	 *        chi.meet(alfa).equals(tau)
	 * @pre alfa is in the interval
	 * @pre tau is in the interval and tau.le(alfa)
	 * @param tau
	 * @param alfa
	 * @return [tau, getTop().meet(tau.omicron(alfa)]
	 */
	public AntiChainInterval omicron(AntiChain tau, AntiChain alfa) {
		return new AntiChainInterval(tau, getTop().meet(tau.omicron(alfa)));
	}

	/*
	 * return the dual of this interval in the smallest lattice it is contained in
	 */
	public AntiChainInterval dual() {
		SmallBasicSet span = getBottom().sp().union(getTop().sp());
		return new AntiChainInterval(getTop().dual(span),getBottom().dual(span));
	}

	/**
	 * Computes a standard version of this or of its dual
	 * whichever has the shortest string representation
	 * @return
	 */
	public AntiChainInterval standard() {
		IntervalPoset fpos = new IntervalPoset(this);
		IntervalPosetCertificat certif = new IntervalPosetCertificat(fpos);
		AntiChainInterval f  = certif.getStandardInterval();
		// I do not know why this is needed, it does produce better standards
		String fDesc = f.toString();
		String oldDesc;
		int cnt = 5;
		do {
			cnt--;
			oldDesc = fDesc;
			f = new IntervalPosetCertificat(new IntervalPoset(f)).getStandardInterval();
			fDesc = f.toString();
		} while (cnt > 0 && !fDesc.equals(oldDesc));

		fpos = new IntervalPoset(this.dual());
		certif = new IntervalPosetCertificat(fpos);
		AntiChainInterval fd  = certif.getStandardInterval();
		String fdDesc = f.toString();
		cnt = 5;
		do {
			cnt--;
			oldDesc = fDesc;
			fd = new IntervalPosetCertificat(new IntervalPoset(fd)).getStandardInterval();
			fdDesc = fd.toString();
		} while (cnt>0 && !fdDesc.equals(oldDesc));

		if (fDesc.compareTo(fdDesc) < 0)
			return f;
		else return fd;
			
	}

	/**
	 * compute the symmetry group of the the interval
	 * this is the set of symmetries that leave both top and bottom invariant
	 * @return getTop().symmetryGroup().intersection(getBottom().symmetryGroup())
	 */
	public Set<int[]> symmetryGroup() {
		Set<int[]> symmetries = getTop().symmetryGroup();
		Set<int[]> toBeRemoved = new HashSet<int[]>();
		for (int[] s : symmetries) {
			if (!getBottom().map(s).equals(getBottom())) toBeRemoved.add(s);
		}
		symmetries.removeAll(toBeRemoved);
		return symmetries;
	}
	/**
	 * compute the symmetry group of the the interval
	 * this is the set of symmetries that leave both top and bottom invariant
	 * and are present in symm
	 * @return getTop().symmetryGroup().intersection(getBottom().symmetryGroup())
	 */
	public Set<int[]> symmetryGroup(Set<int[]> symm) {
		Set<int[]> symmetries = getTop().symmetryGroup(symm);
		Set<int[]> toBeRemoved = new HashSet<int[]>();
		for (int[] s : symmetries) {
			if (!getBottom().map(s).equals(getBottom())) toBeRemoved.add(s);
		}
		symmetries.removeAll(toBeRemoved);
		return symmetries;
	}
	/**
	 * classify all amf in the interval according to the symmetries in top and bottom
	 * @return a sorted map with the encoding of the representative and the number of elements in the equivalence class.
	 */
	public SortedMap<BigInteger,Long> getEquivalenceClasses() {
		Set<int[]> symmetries = this.symmetryGroup();
		TreeMap<BigInteger, Long> res = new TreeMap<BigInteger,Long>();
		for (AntiChain f : this) {
			 Storage.store(res,f.standard(symmetries).encode());
		}
		return res;
	}

	/**
	 * classify all amf in the interval according to the symmetries in top and bottom
	 * that are also given in symm
	 * @return a sorted map with the encoding of the representative and the number of elements in the equivalence class.
	 */
	public SortedMap<BigInteger, Long> getEquivalenceClasses(Set<int[]> symm) {
		Set<int[]> symmetries = this.symmetryGroup(symm);
		TreeMap<BigInteger, Long> res = new TreeMap<BigInteger,Long>();
		for (AntiChain f : this) {
			 Storage.store(res,f.standard(symmetries).encode());
		}
		return res;
	}
	/**
	 * classify all amf in the interval according to the symmetries  in top and bottom
	 * that are also in symm
	 * representatives of the classes are minimal wrt bitrepresentation
	 * @return a sorted map with the bitrepresentation of the representative  (largest element is largest element in top)
	 * and the number of elements in the equivalence class.
	 */
	public SortedMap<BitRepresentation, Long> getEquivalenceClassesBitRepresentation(
			Set<int[]> symm) {
		Set<int[]> symmetries = this.symmetryGroup(symm);
		int l = getTop().sp().maximum();
		TreeMap<BitRepresentation, Long> res = new TreeMap<BitRepresentation,Long>();
		for (AntiChain f : this) {
			 Storage.store(res,f.standardBitRepresentation(symmetries,l));
		}
		return res;
	}

	/**
	 * classify all amf in the interval according to the symmetries in top and bottom
	 * representatives of the classes are minimal wrt bitrepresentation
	 * @return a sorted map with the bitrepresentation of the representative (largest element is largest element in top)
	 * and the number of elements in the equivalence class.
	 */
	public SortedMap<BitRepresentation, Long> getEquivalenceClassesBitRepresentation() {
		Set<int[]> symmetries = this.symmetryGroup();
		int l = getTop().sp().maximum();
		TreeMap<BitRepresentation, Long> res = new TreeMap<BitRepresentation,Long>();
		for (AntiChain f : this) {
			 Storage.store(res,f.standardBitRepresentation(symmetries,l));
		}
		return res;
	}
	/**
	 * compute the equivalenceclasses of elements in this interval as AntiChain. 
	 * The representatives are minimal in the bitrepresentation
	 * @return a sortedset of AntiChain
	 */
	public SortedMap<AntiChain, Long> getEquivalenceClassesAMF() {
		SortedMap<BitRepresentation, Long> bitEquiv = this.getEquivalenceClassesBitRepresentation();
		SortedMap<AntiChain, Long> ret = new TreeMap<AntiChain, Long>();
		for (BitRepresentation b : bitEquiv.keySet()) {
			ret.put(b.toAntiChain(), bitEquiv.get(b));
		}
		return ret;
	}

	/**
	 * compute the equivalenceclasses wrt the symmetry of elements in this interval as AntiChain. 
	 * The representatives are minimal in the bitrepresentation
	 * @return a sortedset of AntiChain
	 */
	public SortedMap<AntiChain, Long> getEquivalenceClassesAMF(Set<int[]> symm) {
		SortedMap<BitRepresentation, Long> bitEquiv = this.getEquivalenceClassesBitRepresentation(symm);
		SortedMap<AntiChain, Long> ret = new TreeMap<AntiChain, Long>();
		for (BitRepresentation b : bitEquiv.keySet()) {
			ret.put(b.toAntiChain(), bitEquiv.get(b));
		}
		return ret;
	}


	/**
	 * Construct the basis of a partition this interval
	 * @return the sorted map of all representatives of equivalence classes according to the symmetry group of fint
	 * such that this == union for all tau in all equivalence classes of fint.omega(tau,alfa)
	 * @param alfa the generator for the partition
	 * @pre alfa is an amf in this
	 */
	public SortedMap<BigInteger, Long> decomposition(AntiChain alfa) {
		// the symmetry group of this
		Set<int[]> symm = symmetryGroup();
		// the amf-equivalence classes for the symmetry group of fint
		return new AntiChainInterval(getBottom(),alfa).getEquivalenceClasses(symm);
	}

	/**
	 * returns the interval [{},{N}]
	 * @param n
	 * @return
	 */
	public static AntiChainInterval fullSpace(int n) {
		SmallBasicSet N = SmallBasicSet.universe(n);
		return new AntiChainInterval(AntiChain.emptyFunction(N), AntiChain.universeFunction(n));
	}

	/**
	 * generate the complete functions in this interval
	 * complete functions are such that each set is involved in at least one immediate successors
	 * | for each a in result.keySet: for each s in a : exists x in N : all subsets of s U {x} are in [{},a]
	 * @return
	 */
	public SortedMap<AntiChain,Long> completeAntiChains() {
		SortedMap<AntiChain,Long> ret = new TreeMap<AntiChain,Long>();
		SetsPoset poset = new SetsPoset(this);
		for (AntiChain a : this) {
			AntiChain c = new AntiChain();
			for (SmallBasicSet s : a) {
				for (SmallBasicSet sub : poset.getPredecessors(s))
					c.addConditionally(sub);
			}
			if (c.ge(getBottom())) Storage.store(ret,c);
		}
		return ret;
	}

	public long latticeSize() {
		if (!getBottom().le(getTop())) return 0;
		else if (getBottom().equals(getTop())) 
			if (this.isClosedAtBottom() && this.isClosedAtTop()) return 1;
			else return 0;
		else return new SetsPoset(this).getLatticeSize();
	}

	/**
	 * Convert this interval to an array of AntiChain
	 * @pre this.size() < Integer.MAXINT
	 * @pre this.size() is less than the maximum size for an array
	 * @return
	 */
	public AntiChain[] toArray() {
		AntiChain[] res = new AntiChain[(int) this.latticeSize()];
		int i=0;
		for (AntiChain r : this) res [i++] = r;
		return res;
	}

	public AntiChainInterval intersect(AntiChainInterval sol1) {
		return new AntiChainInterval(getBottom().join(sol1.getBottom()), getTop().meet(sol1.getTop()));
	}

	/**
	 * compute an intervaliterator working by dynamic programming on the last element of the span
	 * @pre interval is not empty
	 * @param interval
	 * @return
	 */
	private Iterator<AntiChain> fastNonEmptyIterator() {
		final AntiChainInterval interval = this;
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
				iterator[0] = new AntiChainInterval(alfaBottom[0],alfaTop[0]).fastIterator();
				alfa[0] = iterator[0].next();
				iterator[1] = new AntiChainInterval(alfaBottom[1],alfa[0].meet(alfaTop[1])).fastIterator();
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
					iterator[1] = new AntiChainInterval(alfaBottom[1],alfa[0].meet(alfaTop[1])).fastIterator();
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
	 * 
	 * compute an intervaliterator working by dynamic programming on the last element of the span
	 * iterates over the closed interval independently of isClosed...()
	 * @return an iterator that is about 4 times as fast as the normal iterator
	 */
	public Iterator<AntiChain> fastIterator() {
		if (getBottom().le(getTop())) return fastNonEmptyIterator();
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
	 * @return
	 */
	private Iterator<AntiChain> fastNonEmptyIterator(final AntiChain from) {
		final AntiChainInterval interval = this;
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
				iterator[0] = new AntiChainInterval(alfaBottom[0],alfaTop[0]).fastIterator(alfaFrom[0]);
				alfa[0] = iterator[0].next();
				iterator[1] = new AntiChainInterval(alfaBottom[1],alfa[0].meet(alfaTop[1])).fastIterator(alfaFrom[1]);
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
					iterator[1] = new AntiChainInterval(alfaBottom[1],alfa[0].meet(alfaTop[1])).fastIterator();
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
	 * it allows a starting point 'from' which may be the last element where a previous fastIterator stopped
	 * the starting point will be the first element returned
	 * the order is the natural lexicographic order in a = a0 + a1 x {n}
	 * @param from
	 * @return
	 */
	public Iterator<AntiChain> fastIterator(final AntiChain from) {
		if (getBottom().le(getTop()) && from.le(getTop()) && from.ge(getBottom())) 
			return fastNonEmptyIterator(from);
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
	 * if the interval is not empty, find the maximal set of intervals {Ii|i=1..k} such that
	 * I1 dv I2 dv ... dv Ik = this
	 * with dv the direct join
	 * if the interval is empty, return the interval
	 * @param beta
	 * @return the maximal set of intervals allowing a decomposition of this through direct join
	 */
	public SortedSet<AntiChainInterval> decompose() {
		AntiChain bottom = this.getBottom();
		AntiChain top = this.getTop();
		if (!bottom.le(top)) {
			SortedSet<AntiChainInterval> res = new TreeSet<AntiChainInterval>();
			res.add(this);
			return res;
		}
		
		SortedSet<AntiChain> alfac = top.decompose(bottom);
		SortedSet<AntiChainInterval> res = new TreeSet<AntiChainInterval>();
		for (AntiChain x : alfac) {
			res.add(new AntiChainInterval(bottom.meet(x),x));
		}
		return res;
	}
	
	public SortedSet<AntiChainInterval> decomposeDual() {
		AntiChainInterval dual = this.dual();
		return dual.decompose();
	}

	@Override
	public int compareTo(AntiChainInterval o) {
		int c = this.getBottom().compareTo(o.getBottom());
		if (c == 0) c = this.getTop().compareTo(o.getTop());
		return c;
	}


}
