package amfsmall;

import java.util.Iterator;
import java.util.NoSuchElementException;

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
	 * Create a closed interval of antimonotonic functions with given limits and closed or open brackets
	 * @param bottom : lower limit
	 * @param top : upper limit
	 */
	public AntiChainInterval(AntiChain bottom,AntiChain top) {
		this(bottom,top,true,true);
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

	/**
	 * returns the interval [{},{N}]
	 * @param n
	 * @return
	 */
	public static AntiChainInterval fullSpace(int n) {
		SmallBasicSet N = SmallBasicSet.universe(n);
		return new AntiChainInterval(AntiChain.emptyFunction(N), AntiChain.universeFunction(n));
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
			@SuppressWarnings("unchecked")
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

	@Override
	public int compareTo(AntiChainInterval o) {
		int c = this.getBottom().compareTo(o.getBottom());
		if (c == 0) c = this.getTop().compareTo(o.getTop());
		return c;
	}


}
