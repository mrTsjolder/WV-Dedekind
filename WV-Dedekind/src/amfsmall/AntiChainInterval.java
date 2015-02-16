package amfsmall;

import java.util.Iterator;
import java.util.NoSuchElementException;

import posets.SetsPoset;

/**
 * An interval of antichains
 * @author u0003471
 *
 */
public class AntiChainInterval implements Iterable<SmallAntiChain>,Comparable<AntiChainInterval> {
	
	/*
	 * an interval is described by its lower bound (from) and upper bound (till)
	 * it may be ]..[,[..[,]..],[..] depending on closed[Below,Above]
	 */
	private SmallAntiChain from;
	private SmallAntiChain till;
	private boolean closedBelow;
	private boolean closedAbove;

	/**
	 * Create an interval of antichains with given limits and closed or open brackets
	 * 
	 * @param 	bottom
	 * 			lower limit
	 * @param 	top
	 * 			upper limit
	 * @param 	closedBottom
	 * 			true if closed at lower limit
	 * @param 	closedTop
	 * 			true if closed at upper limit
	 */
	public AntiChainInterval(SmallAntiChain bottom,SmallAntiChain top,boolean closedBottom,boolean closedTop) {
		from = bottom;
		till = top;
		closedBelow = closedBottom;
		closedAbove = closedTop;
	}

	/**
	 * Create a closed interval of antichains with given limits and closed or open brackets
	 * 
	 * @param 	bottom
	 * 			lower limit
	 * @param 	top
	 * 			upper limit
	 */
	public AntiChainInterval(SmallAntiChain bottom,SmallAntiChain top) {
		this(bottom,top,true,true);
	}
	
	/**
	 * The antichain at the bottom of the interval 
	 */
	public SmallAntiChain getBottom() {
		return from;
	}
	/**
	 * The antichain at the top of the interval 
	 */
	public SmallAntiChain getTop() {
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
	 * iterator delegates to closed iterator

	 */
	public Iterator<SmallAntiChain> iterator() {
		// empty interval?
		if (!getBottom().le(getTop()) ||
				getBottom().equals(getTop()) 
				&& (!isClosedAtBottom() || !isClosedAtTop())) // interval is empty
			return new Iterator<SmallAntiChain>() {
			
			@Override
			public boolean hasNext() {
				return false;
			}
			
			@Override
			public SmallAntiChain next() {
				return null;
			}
			
			@Override
			public void remove() {
			}
					
		};
		if (isClosedAtTop()) {
			Iterator<SmallAntiChain> theIt = closedIterator();
			if (!isClosedAtBottom()) theIt.next();
			return theIt;
		}
		else return new Iterator<SmallAntiChain>() {

			boolean thereIsNext;
			Iterator<SmallAntiChain> theIt = closedIterator();
			SmallAntiChain nxt = null;
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
			public SmallAntiChain next() {
				SmallAntiChain myNxt = nxt;
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
	 * iterator ignoring the boundaries
	 * 
	 * @pre 	getBottom().le(getTop())
	 * @return 	an iterator ignoring the boundaries
	 */
	private Iterator<SmallAntiChain> closedIterator() {

		/*
		 * case lower limit is empty
		 */
		if (getBottom().size() == 0) {
			return exceptionalClosedIterator();
		}
		final SmallBasicSet span = getTop().sp();
		if (span.size()<=1) return new Iterator<SmallAntiChain>() {
/*
 * Iterator in the case of dimension 1 or 0, explicitly computed
 */
			private SmallAntiChain[] theList;
			private int pos, last;
			{
				if (getBottom().size() == 0) pos = 0;
				else for (SmallBasicSet b : getBottom()) if (b.size() == 0) pos = 1;
				else pos = 2;
				
				if (getTop().size() == 0) last = 0;
				else for (SmallBasicSet b : getTop()) if (b.size() == 0) last = 1;
				else last = 2;
				
				if (pos <= last) {
					theList = new SmallAntiChain[last + 1];
					if (pos == 0) {
						SmallAntiChain amf;
						theList[0] = SmallAntiChain.emptyAntiChain(getUniverse()); // empty function
						if (last > 0) {
							amf = new SmallAntiChain();
							amf.add(SmallBasicSet.emptySet()); // empty set
							theList[1] = amf;
						}
						if (last > 1) {
							amf = SmallAntiChain.emptyAntiChain(getUniverse());
							amf.add(span); // set with one element
							theList[2] = amf;
						}
					}
					else if (pos == 1) {
						SmallAntiChain amf;
						if (last > 0) {
							amf = SmallAntiChain.emptyAntiChain(getUniverse());
							amf.add(SmallBasicSet.emptySet()); // empty set
							theList[1] = amf;
						}
						if (last > 1) {
							amf = SmallAntiChain.emptyAntiChain(getUniverse());
							amf.add(span); // set with one element
							theList[2] = amf;
						}
						
					}
					else /* pos == 2 */ {
						SmallAntiChain amf;
						if (last > 1) {
							amf = SmallAntiChain.emptyAntiChain(getUniverse());
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
			public SmallAntiChain next() {
				return theList[pos++];
			}

			@Override
			public void remove() {
				// TODO Auto-generated method stub	
			}
			
		};
		if (getTop().equals(getBottom())) {
			// iterator for one element
			return new Iterator<SmallAntiChain>() {

				boolean given;
				{
					given = false;
				}
				@Override
				public boolean hasNext() {
					return !given;
				}

				@Override
				public SmallAntiChain next() {
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
			return new Iterator<SmallAntiChain>() {

				int pos = 0;
				@Override
				public boolean hasNext() {
					return pos < 2;
				}

				@Override
				public SmallAntiChain next() {
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
		return new Iterator<SmallAntiChain>() {

/**
 * Iterator general case is reduced to the twice half the dimension
 */
			Iterator<SmallAntiChain> X;
			Iterator<SmallAntiChain> Y;
			AntiChainInterval Xaxis = new AntiChainInterval(getBottom().project(axes[0]),
						getTop().project(axes[0]),true,true);
			AntiChainInterval Yaxis = new AntiChainInterval(getBottom().project(axes[1]),
						getTop().project(axes[1]),true,true);
			Iterator<SmallAntiChain> current;
			SmallAntiChain currentX, currentY;
			{
				X = Xaxis.iterator();
				Y = Yaxis.iterator();
				if (X.hasNext() && Y.hasNext()) {
					currentX = X.next();
					currentY = Y.next();
					current = 
						new AntiChainInterval((SmallAntiChain) currentX.join(currentY).join(getBottom()),
								(SmallAntiChain) currentX.times(currentY).meet(getTop()),true,true).iterator();
				}
				else current = new AntiChainInterval(new SmallAntiChain(),
						new SmallAntiChain(),false,false).iterator(); // iterator on an empty interval
			}
			@Override
			public boolean hasNext() {
				return  current.hasNext() || X.hasNext() || Y.hasNext();
			}

			@Override
			public SmallAntiChain next() {
				if (current.hasNext()) return current.next();
				if (X.hasNext()) {
					currentX = X.next();
					current = 
						new AntiChainInterval((SmallAntiChain) currentX.join(currentY).join(getBottom()),
								(SmallAntiChain) currentX.times(currentY).meet(getTop()),true,true).iterator();
					return current.next();
				}
				else if (Y.hasNext()) /* should always be true */ {
					X = Xaxis.iterator();
					if (X.hasNext() && Y.hasNext()) {
						currentX = X.next();
						currentY = Y.next();
						current = 
							new AntiChainInterval((SmallAntiChain) currentX.join(currentY).join(getBottom()),
									(SmallAntiChain) currentX.times(currentY).meet(getTop()),true,true).iterator();
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

	private Iterator<SmallAntiChain> exceptionalClosedIterator() {
		return new Iterator<SmallAntiChain>() {
			SmallAntiChain bottom;
			boolean virgin;
			Iterator<SmallAntiChain> normal ;

			{
				bottom = new SmallAntiChain();
				bottom.add(SmallBasicSet.emptySet());
				virgin = true;
				normal = new AntiChainInterval(bottom,getTop(),true,true).closedIterator();
			}
			@Override
			public boolean hasNext() {
				return virgin || normal.hasNext() ;
			}

			@Override
			public SmallAntiChain next() {
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
	 * 
	 * @pre 	size() >= 3
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
			SmallAntiChain difference = getTop().minus(getBottom());
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
	 * interface to compute a suitable subset for splitting
	 * the span of an algorithm such that the algorithm can 
	 * be decomposed
	 */
	public interface SubsetFinder {
		/**
		 * Look up a subset of span that is not a subset of an element in bottom 
		 * with size about target (indicative)
		 * this problem is in principle np-hard
		 * any approximation is acceptable
		 * 
		 * @param 	span
		 * 			the mother set
		 * @param 	target
		 * 			the approximate size wanted
		 * @param 	bottom
		 * 			the AntiChain in which the answer cannot be contained
		 * @pre 	bottom not an immediate predecessor of {span}
		 * @return 	null if span le bottom
		 * @return 	the subset of span with the closest size to target not contained in bottom
		 */
		SmallBasicSet bestSubset(SmallBasicSet span, long target, SmallAntiChain bottom);
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
		public SmallBasicSet bestSubset(SmallBasicSet span, long target, SmallAntiChain bottom) {

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

	/**
	 * returns the interval [{},{N}]
	 * 
	 * @param 	n
	 * @return	
	 */
	public static AntiChainInterval fullSpace(int n) {
		SmallBasicSet N = SmallBasicSet.universe(n);
		return new AntiChainInterval(SmallAntiChain.emptyAntiChain(N), SmallAntiChain.universeAntiChain(n));
	}

	public long latticeSize() {
		if (!getBottom().le(getTop())) return 0;
		else if (getBottom().equals(getTop())) 
			if (this.isClosedAtBottom() && this.isClosedAtTop()) return 1;
			else return 0;
		else return new SetsPoset(this).getLatticeSize();
	}

	/**
	 * compute an intervaliterator working by dynamic programming on the last element of the span
	 * 
	 * @param 	interval
	 * @pre 	interval is not empty
	 * @return	
	 */
	private Iterator<SmallAntiChain> fastNonEmptyIterator() {
		final AntiChainInterval interval = this;
		SmallBasicSet span = interval.getTop().sp();
		if (span.isEmpty()) return new Iterator<SmallAntiChain>() {
			// empty span interval case. At most two elements
			private SmallAntiChain current = interval.getBottom();

			@Override
			public boolean hasNext() {
				return current != null;
			}

			@Override
			public SmallAntiChain next() {
				SmallAntiChain ret = current;
				if (current.equals(interval.getTop())) current = null;
				else current = interval.getTop();
				return ret;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
		return new Iterator<SmallAntiChain>() {

			// non empty span
			private SmallBasicSet span = interval.getTop().sp();
			private SmallAntiChain maxSpan = SmallAntiChain.singletonAntiChain(span.maximum());
			private SmallAntiChain current = interval.getBottom();
			private SmallAntiChain[] alfaBottom = current.reduce(span);
			private SmallAntiChain[] alfaTop = interval.getTop().reduce(span);

			private SmallAntiChain[] alfa = new SmallAntiChain[2];
			@SuppressWarnings("unchecked")
			private Iterator<SmallAntiChain>[] iterator = new Iterator[2];
			{ 
				iterator[0] = new AntiChainInterval(alfaBottom[0],alfaTop[0]).fastIterator();
				alfa[0] = iterator[0].next();
				iterator[1] = new AntiChainInterval(alfaBottom[1],(SmallAntiChain) alfa[0].meet(alfaTop[1])).fastIterator();
				alfa[1] = iterator[1].next();
			};

			private SmallAntiChain nextCurrent() {
				// problem with times
				if (alfa[1].isEmpty()) return alfa[0];
				else return (SmallAntiChain) alfa[0].join(alfa[1].times(maxSpan));
			}

			@Override
			public boolean hasNext() {
				return current != null;
			}

			@Override
			public SmallAntiChain next() {
				SmallAntiChain ret = current;
				if (iterator[1].hasNext()) {
					alfa[1] = iterator[1].next();
					current = nextCurrent();
				}
				else if (iterator[0].hasNext()) {
					alfa[0] = iterator[0].next();
					iterator[1] = new AntiChainInterval(alfaBottom[1],(SmallAntiChain) alfa[0].meet(alfaTop[1])).fastIterator();
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
	public Iterator<SmallAntiChain> fastIterator() {
		if (getBottom().le(getTop())) return fastNonEmptyIterator();
		else {
			// empty interval
			return new Iterator<SmallAntiChain>() {

				// no next!
				
				@Override
				public boolean hasNext() {
						return false;
				}

				@Override
				public SmallAntiChain next() {
					throw new NoSuchElementException();
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
				
			};
		}
	}

	@Override
	public int compareTo(AntiChainInterval o) {
		int c = this.getBottom().compareTo(o.getBottom());
		if (c == 0) c = this.getTop().compareTo(o.getTop());
		return c;
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

}
