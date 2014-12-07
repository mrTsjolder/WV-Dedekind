package posets;

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
	
}
