package posets;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import amfsmall.AntiChainInterval;
import amfsmall.AntiChain;
import amfsmall.SmallBasicSet;

public class SetsPoset extends SimplePosetSize<SmallBasicSet> {

	private SortedSet<SmallBasicSet>[] level;
	private SortedMap<SmallBasicSet,SortedSet<SmallBasicSet>> successors;
	private SortedMap<SmallBasicSet,SortedSet<SmallBasicSet>> predecessors;
	private SortedMap<SmallBasicSet,SortedSet<SmallBasicSet>> before;
	private SortedMap<SmallBasicSet,SortedSet<SmallBasicSet>> after;
	private int minSize;
	private int maxSize;
//	private Set<int[]> symmetries;
	// Symmetries were used in an experiment to compute the latticeSize using hashing.
	// This does not bring any efficiency gain.

	/**
	 * equality of posets: the sets are equal
	 * @param o
	 * @return
	 */
	public boolean equals(SetsPoset o) {
		return getPosetElements().equals(o.getPosetElements());
	}
	
	/**
	 * empty poset: the set is empty
	 * @return
	 */
	public boolean isEmpty() {
		return getPosetElements().isEmpty();
	}
	
	/**
	 * Build a poset of sets from an interval of AMF
	 * this = {A subseteq fint.sp()|fint.getBottom() < {A} V fint.getBottom() <= fint.getTop()}
	 * @param fint
	 */
	@SuppressWarnings("unchecked")
	public SetsPoset(AntiChainInterval fint) {
		SortedMap<Long,SortedSet<SmallBasicSet>> hLevel = new TreeMap<Long,SortedSet<SmallBasicSet>>();
		successors = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> ();
		predecessors = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> ();
		before = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> ();
		after = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> ();
//		symmetries = fint.symmetryGroup();
		
		AntiChain h = fint.getTop();
		if (h.gt(fint.getBottom())) {
			minSize = Integer.MAX_VALUE;
			maxSize = Integer.MIN_VALUE;
			while (!h.equals(fint.getBottom())) {
				AntiChain hh = new AntiChain();
				for (SmallBasicSet a : h) 
					if (fint.getBottom().contains(a)) hh.addConditionally(a);
					else {
						maxSize = (int) Math.max(maxSize, a.size());
						minSize = (int) Math.min(minSize, a.size());
						store(hLevel,a);
						hh.addConditionallyAll(a.immediateSubSets());
					}
				h = hh;
			}
			level = new SortedSet[(int) (maxSize - minSize + 1)];
			for (long k = minSize; k <=maxSize;k++) 
				if (hLevel.containsKey(k)) level[(int) (k-minSize)] = hLevel.get(k);
				else level[(int)(k-minSize)] = new TreeSet<SmallBasicSet>();
		}
		else { // empty interval, or singleton
			minSize = 0;
			maxSize = 0;
			level = new SortedSet[1];
			level[0] = new TreeSet<SmallBasicSet>();
		}

		buildCessors();
	}
	
	/**
	 * Build a poset of sets from a set of sets
	 * @param fint
	 */
	@SuppressWarnings("unchecked")
	public SetsPoset(Set<SmallBasicSet> sets) {
		SortedMap<Long,SortedSet<SmallBasicSet>> hLevel = new TreeMap<Long,SortedSet<SmallBasicSet>>();
		successors = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> ();
		predecessors = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> ();
		before = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> ();
		after = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> ();
		
		minSize = Integer.MAX_VALUE;
		maxSize = Integer.MIN_VALUE;
		if (sets.isEmpty()) {                           //h.equals(fint.getBottom())) {
			level = new SortedSet[1];
			level[0] = new TreeSet<SmallBasicSet>();
		}
		else {
			for (SmallBasicSet a : sets) {
				maxSize = (int) Math.max(maxSize, a.size());
				minSize = (int) Math.min(minSize, a.size());
				store(hLevel,a);
			}

			level = new SortedSet[(int) (maxSize - minSize + 1)];
			for (long k = minSize; k <=maxSize;k++) 
				if (hLevel.containsKey(k)) level[(int) (k-minSize)] = hLevel.get(k);
				else level[(int)(k-minSize)] = new TreeSet<SmallBasicSet>();
		}
		buildCessors();
//		symmetries = getMinimalElements().symmetryGroup(getMaximalElements().symmetryGroup());
	}
	
	/**
	 * the poset consisting of all sets that can be written as the union of
	 * a nonempty subset of an element of a and a nonempty subset of an element of b
	 * @effect this(new AntiChainInterval(a.join(b),a.times(b))
	 * @param a
	 * @param b
	 */
	public SetsPoset(AntiChain a, AntiChain b) {
		this(new AntiChainInterval(a.join(b), a.times(b)));
	}
	
	/**
	 * the poset of all subsets of a set s
	 * @param s
	 */
	public SetsPoset(SmallBasicSet s) {
		this(s.getSubSets());
	}

	public SetsPoset(SetsPoset setsPoset) {
		successors = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> (setsPoset.successors);
		predecessors = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> (setsPoset.predecessors);
		before = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> ();
		after = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> ();
		
		level = Arrays.copyOf(setsPoset.level,setsPoset.level.length);
		minSize =  setsPoset.minSize;
		maxSize = setsPoset.maxSize;
	}

	/**
	 * create an empty SetsPoset to be used in low level constructions
	 */
	private SetsPoset() {
		successors = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> ();
		predecessors = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> ();
		before = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> ();
		after = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> ();
		
		level = null;
		minSize =  0;
		maxSize = 0;
	}
	/**
	 * after the level has been initialised, build the successor and predecessor structures
	 */
	private void buildCessors() {
		for (int k = 0;k< level.length;k++) {
			for (SmallBasicSet t : level[k]) {
				predecessors.put(t,new TreeSet<SmallBasicSet>());
				successors.put(t,new TreeSet<SmallBasicSet>());
			}
		}
		
		for (int k = 0;k<level.length - 1;k++) {
			for (SmallBasicSet s : level[k]) {
				SortedSet<SmallBasicSet> ss = successors.get(s);
				for (SmallBasicSet t : level[k+1]) {
					if (t.hasAsSubset(s)) {
						ss.add(t);
						predecessors.get(t).add(s);
					}
				}
			}
		}

	}

	/**
	 * build a string representation of the poset
	 * The representation comes in two lines
	 * line 1 connects the elements with their immediate successors
	 * lint 2 connects the elements with their immediate predecessors
	 */
	public String toString() {
		String res = "";
		for (int k = 0;k < level.length;k++) {
			for (SmallBasicSet s: level[k]) {
				res += s.toString() + "(" + successors.get(s) + ")\n";
			}
		}
		res += "";
		for (int k = 0;k < level.length;k++) {
			for (SmallBasicSet s: level[k]) {
				res += "(" + predecessors.get(s) + ")" + s.toString() + "\n";
			}
		}
		res += this.getPosetElements();
		return res;
	}

	/**
	 * store a basic set s on its size in hLevel
	 * @param hLevel
	 * @param s
	 */
	private void store(SortedMap<Long,SortedSet<SmallBasicSet>> hLevel,
			SmallBasicSet s) {
		if (!hLevel.containsKey(s.size())) 
			hLevel.put( s.size(), new TreeSet<SmallBasicSet>());
		hLevel.get(s.size()).add(s);
	}

	@Override
	public SortedSet<SmallBasicSet> getSuccessors(SmallBasicSet v) {
		return successors.get(v);
	}

	@Override
	public SortedSet<SmallBasicSet> getPredecessors(SmallBasicSet v) {
		return predecessors.get(v);
	}

	@Override
	public SortedSet<SmallBasicSet> getAfter(SmallBasicSet v) {
		if (!after.containsKey(v) && successors.containsKey(v)) buildAfter(v);
		return after.get(v);
	}
	
	@Override
	public SortedSet<SmallBasicSet> getBefore(SmallBasicSet v) {
		if (!before.containsKey(v) && predecessors.containsKey(v)) buildBefore(v);
		return before.get(v);
	}
	private void buildAfter(SmallBasicSet v) {
		SortedSet<SmallBasicSet> a = new TreeSet<SmallBasicSet>();
		for (SmallBasicSet p : successors.get(v)) {
			a.add(p);
			a.addAll(getAfter(p));
		}
		after.put(v, a);
	}

	private void buildBefore(SmallBasicSet v) {
		SortedSet<SmallBasicSet> b = new TreeSet<SmallBasicSet>();
		for (SmallBasicSet p : predecessors.get(v)) {
			b.add(p);
			b.addAll(getBefore(p));
		}
		before.put(v, b);
	}

	/**
	 * get the amf that is maximal in the generated lattice
	 * 
	 * @return amf containing all elements not having a successor
	 */
	public AntiChain getMaximalElements() {
		AntiChain res = new AntiChain();
		for (SmallBasicSet s : getPosetElements()) {
			if (getSuccessors(s).isEmpty()) res.add(s);
		}
		return res;
	}
	
	/**
	 * get the amf containing the minimal elements in the poset
	 * 
	 * @return amf containing all elements not having a predecessor
	 */
	public AntiChain getMinimalElements() {
		AntiChain res = new AntiChain();
		for (SmallBasicSet s : getPosetElements()) {
			if (getPredecessors(s).isEmpty()) res.add(s);
		}
		return res;
	}
	
	@Override
	public SortedSet<SmallBasicSet> getPosetElements() {
		return (SortedSet<SmallBasicSet>) successors.keySet();
	}

	@Override
	public SortedSet<SmallBasicSet> getLevel(int n) {
		if (n > level.length) return new TreeSet<SmallBasicSet>();
		return level[n-1];
	}

	/**
	 * Get the level at which the set s is to be found
	 * @pre s is in the poset
	 */
	@Override
	public int getLevel(SmallBasicSet s) {
		return (int) s.size() - getMinSize() + 1;
	}

	/**
	 * get the lowest level at which element i is found
	 * @param i
	 * @return
	 */
	public int getLowestLevelOf(int i) {
		for (int l = 1;l <= getMaxLevel();l++) {
			SortedSet<SmallBasicSet> lev = getLevel(l);
			for (SmallBasicSet s : lev) {
				if (s.contains(i)) return l;
			}
		}
		return getMaxLevel() + 1;
	}

	/**
	 * Get the highest level 
	 */
	@Override
	public int getMaxLevel() {
		return level.length;
	}

	/**
	 * get the size of the smallest set
	 * @return
	 */
	public int getMinSize() {
		return minSize;
	}
	/**
	 * get the size of the largest set
	 * @return
	 */
	public int getMaxSize() {
		return maxSize;
	}
	@Override
	public long getLatticeSize()  {
		if (isEmpty()) return 1;
		return getLatticeSize(this.getMaximalLevelNumber() % 2 != 1);
	}

	@Override
	public long getLatticeSize(boolean odd) {
		int firstLevel;
		int exp;
		if (odd) {
			exp = 0;
			firstLevel = 1;
		}
		else {
			exp = (getLevel(1).size());
			firstLevel = 2;
		}
		
		// for all levels firstLevel + 2k, compute the set of predecessors of the predecessors
		Map<SmallBasicSet,Set<SmallBasicSet>> prepredec = new HashMap<SmallBasicSet,Set<SmallBasicSet>>();
		for (int i=firstLevel;i<=this.getMaxLevel();i+=2) {
			for (SmallBasicSet s : getLevel(i)) {
				Set<SmallBasicSet> prepre = new HashSet<SmallBasicSet>();
				prepredec.put(s, prepre);
				for (SmallBasicSet p : this.getPredecessors(s)) {
					prepre.addAll(this.getPredecessors(p));
				}
			}
		}

// not effecive 
//		HashMap<AntiChain, Long> longHash = new HashMap<AntiChain,Long>();
//		HashMap<AntiChain, Integer> intHash = new HashMap<AntiChain,Integer>();
//		Set<int[]> symmetries = getMinimalElements().symmetryGroup(getMaximalElements().symmetryGroup());
//		long res = getLatticeSize(exp,prepredec,new HashSet<SmallBasicSet>(),firstLevel, longHash, intHash, symmetries);
		long res = getLatticeSize(exp,prepredec,new HashSet<SmallBasicSet>(),firstLevel);
		return res;
	}
	
	private long getLatticeSize(int exp,
			Map<SmallBasicSet, Set<SmallBasicSet>> prepredec,
			Set<SmallBasicSet> lowerLevel, int l) {
		if (l > getMaxLevel()) {
			return pow(exp);
		}
		Set<SmallBasicSet> thisLevel = new HashSet<SmallBasicSet>();
		Set<SmallBasicSet> goodSuccessors = new HashSet<SmallBasicSet>();
		Set<SmallBasicSet> allPredecessors = new HashSet<SmallBasicSet>();
		for (SmallBasicSet s : this.getLevel(l)) {
			if (lowerLevel.containsAll(prepredec.get(s))) {
				thisLevel.add(s);
			}
		}
		Iterator<Set<SmallBasicSet>> it = getSetIterator(thisLevel);
		long res = 0L;
		while(it.hasNext()) {
			Set<SmallBasicSet> alfa = it.next();
			goodSuccessors.clear();
			if (l+1 <= this.getMaxLevel()) {
				Set<SmallBasicSet> levelAbove = getLevel(l+1);
				for (SmallBasicSet t : levelAbove) {
					if (alfa.containsAll(this.getPredecessors(t)))
						goodSuccessors.add(t);
				}
			}
			allPredecessors.clear();
			for (SmallBasicSet s:alfa) {
				allPredecessors.addAll(this.getPredecessors(s));
			}
			int myExp = goodSuccessors.size();
			int lowExp = allPredecessors.size();
			res += pow(exp - lowExp)*getLatticeSize(myExp,prepredec,alfa,l+2);
		}
		return res;
	}
	
	
	private Iterator<Set<SmallBasicSet>> getSetIterator(
			final Set<SmallBasicSet> thisLevel) {
		return new Iterator<Set<SmallBasicSet>>() {

			HashSet<SmallBasicSet> currentSet, nextSet;
			SmallBasicSet[] elements;
			boolean finished;
			{
				currentSet = new HashSet<SmallBasicSet>(); // emptySet
				nextSet = new HashSet<SmallBasicSet>();
				nextSet.addAll(thisLevel); // all elements
				finished = false;
				elements = new SmallBasicSet[thisLevel.size()];
				elements = thisLevel.toArray(elements);
			}
			@Override
			public boolean hasNext() {
				return !finished;
			}

			@Override
			public Set<SmallBasicSet> next() {
				// currentSet follows nextSet
				HashSet<SmallBasicSet> res = new HashSet<SmallBasicSet>(currentSet);
				HashSet<SmallBasicSet> h = currentSet;
				// interchange references
				currentSet = nextSet;
				nextSet = h;
				int i;
				// currentSet is increased to become the follower of nextSet
				for (i=0;i<elements.length;i++) {
					if (currentSet.contains(elements[i])) {
						currentSet.remove(elements[i]);
					}
					else {
						currentSet.add(elements[i]);
						break;
					}
				}
				for (i=0;i<elements.length;i++) {
					if (currentSet.contains(elements[i])) {
						currentSet.remove(elements[i]);
					}
					else {
						currentSet.add(elements[i]);
						break;
					}
				}
				// if current becomes empty the iteration is finished
				if (currentSet.isEmpty()) finished = true;
				return res;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
	}

	@Override
	public SimplePosetSize<SmallBasicSet> getPosetFrom(
			SortedSet<SmallBasicSet> bottom) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Build the poset that is the set theoretical difference of 
	 * this and setsPoset
	 * @param setsPoset
	 * @return this\setsPoset
	 */
	public SetsPoset minus(SetsPoset setsPoset) {
//		SetsPoset ret = new SetsPoset();

		SortedSet<SmallBasicSet> newPosetElements = new TreeSet<SmallBasicSet>(getPosetElements());
		newPosetElements.removeAll(setsPoset.getPosetElements());
		
		return new SetsPoset(newPosetElements);
	}

	/**
	 * Build the poset that is the set theoretical intersection of 
	 * this and setsPoset
	 * @param setsPoset
	 * @return this\cap setsPoset
	 */
	@SuppressWarnings("unchecked")
	public SetsPoset intersect(SetsPoset setsPoset) {
		SetsPoset ret = new SetsPoset();

		SortedSet<SmallBasicSet> newPosetElements = new TreeSet<SmallBasicSet>(getPosetElements());
		newPosetElements.retainAll(setsPoset.getPosetElements());
		ret.minSize = Integer.MAX_VALUE;
		ret.maxSize = Integer.MIN_VALUE;

		SortedMap<Long,SortedSet<SmallBasicSet>> hLevel = new TreeMap<Long,SortedSet<SmallBasicSet>>();

		for (SmallBasicSet s : newPosetElements) {
			store(hLevel,s);
			ret.minSize = (int) Math.min(ret.minSize, s.size());
			ret.maxSize = (int) Math.max(ret.maxSize, s.size());
			SortedSet<SmallBasicSet> newSuccessors = new TreeSet<SmallBasicSet>(getSuccessors(s));
			SortedSet<SmallBasicSet> newPredecessors = new TreeSet<SmallBasicSet>(getPredecessors(s));
			newPredecessors.retainAll(setsPoset.getPredecessors(s));
			newSuccessors.retainAll(setsPoset.getSuccessors(s));
			ret.successors.put(s, newSuccessors);
			ret.predecessors.put(s, newPredecessors);
		}
		ret.level = new SortedSet[(int) (ret.maxSize - ret.minSize + 1)];
		for (long k = ret.minSize; k <=ret.maxSize;k++) 
			if (hLevel.containsKey(k)) ret.level[(int) (k-ret.minSize)] = hLevel.get(k);
			else level[(int)(k-ret.minSize)] = new TreeSet<SmallBasicSet>();
		return ret;
	}

	/**
	 * Build the poset that is the set theoretical union of 
	 * this and setsPoset
	 * @param setsPoset
	 * @return this U setsPoset
	 */
	public SetsPoset union(SetsPoset setsPoset) {
		SortedSet<SmallBasicSet> newPosetElements = new TreeSet<SmallBasicSet>(getPosetElements());
		newPosetElements.addAll(setsPoset.getPosetElements());
		return new SetsPoset(newPosetElements);
	}
	
	/**
	 * return the set of amf with this poset as its poset
	 * @return
	 */
	public SortedSet<AntiChain> getLattice() {
		return getLattice(getPosetElements());
	}

	/**
	 * return the set of amf with the set posetElements as its set of posetelements
	 * naive recursive implementation
	 * @return
	 */
	private static SortedSet<AntiChain> getLattice(
			SortedSet<SmallBasicSet> posetElements) {
		if (posetElements.isEmpty()) {
			TreeSet<AntiChain> ret = new TreeSet<AntiChain>();
			ret.add(AntiChain.emptyFunction());
			return ret;
		}
		SortedSet<SmallBasicSet> set = new TreeSet<SmallBasicSet>();
		SmallBasicSet f = posetElements.first();
		set.addAll(posetElements);
		set.remove(f);
		SortedSet<AntiChain> ret = getLattice(set);
		TreeSet<SmallBasicSet> setToRemove = new TreeSet<SmallBasicSet>();
		for (SmallBasicSet s : set) if (s.hasAsSubset(f) || f.hasAsSubset(s)) setToRemove.add(s);
		set.removeAll(setToRemove);
		for (AntiChain x : getLattice(set)) {
			x.add(f);
			ret.add(x);
		}
		return ret;
	}
		
	public Iterator<AntiChain> getLatticeIterator() {
		if (this.isEmpty()) {
			return new Iterator<AntiChain>() {

				private boolean notUsed = true;

				@Override
				public boolean hasNext() {
					return notUsed;
				}

				@Override
				public AntiChain next() {
					notUsed = false;
					return new AntiChain();
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
				
			};
		}
		else return new Iterator<AntiChain>() {

			SmallBasicSet current;
			SortedSet<SmallBasicSet> currentSet ;
			Iterator<AntiChain> currentIterator;

			{
				int maxLevel = getMaxLevel();
				currentSet = new TreeSet<SmallBasicSet>();
				if (!getLevel(maxLevel).isEmpty()) {
					current = getLevel(maxLevel).first();
					currentSet.add(current);
				}
				currentIterator = SetsPoset.this.minus(new SetsPoset(current.getSubSets())).getLatticeIterator();
			}
			
			@Override
			public boolean hasNext() {
				return currentIterator.hasNext() || !currentSet.isEmpty();
			}

			@Override
			public AntiChain next() {
				if (currentIterator.hasNext()) {
					AntiChain nxt = /*new AntiChain*/(currentIterator.next());
					for (SmallBasicSet c : currentSet) nxt.addConditionally(c);
					return nxt;
				}
				else /* (!currentSet.isEmpty()) */ {
					currentIterator = SetsPoset.this.minus(new SetsPoset(currentSet)).getLatticeIterator();
					currentSet.clear();
					return currentIterator.next();
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
	}
}
