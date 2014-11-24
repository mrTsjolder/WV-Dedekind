package posets;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import amfsmall.*;

public class IntervalPoset extends SimplePosetSize<SmallBasicSet> {

	private AntiChainInterval theInterval;
	private SortedSet<SmallBasicSet>[] level;
	private SortedMap<SmallBasicSet,SortedSet<SmallBasicSet>> successors;
	private SortedMap<SmallBasicSet,SortedSet<SmallBasicSet>> predecessors;
	private SortedMap<SmallBasicSet,SortedSet<SmallBasicSet>> before;
	private SortedMap<SmallBasicSet,SortedSet<SmallBasicSet>> after;
	static int cnt = 100;

	
	private SortedMap<Long,SmallBasicSet> labels;
		
	private SortedMap<SmallBasicSet,Long> sets;

	private SmallBasicSet span;

	/**
	 * an empty poset
	 */
/*	protected IntervalPoset(int maxLevel) {
		try {
			theInterval = AntiChainInterval.parser().parse("[{[]},{[]}]");
		} catch (SyntaxErrorException e) {
			e.printStackTrace();
		}
		successors = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> ();
		predecessors = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> ();
		before = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> ();
		after = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> ();	
		level = new SortedSet[maxLevel];
		for (int i = 0;i<maxLevel;i++) level[i] = new TreeSet<SmallBasicSet>();
	}
*/	
	@SuppressWarnings("unchecked")
	public IntervalPoset(AntiChainInterval fint) {
		theInterval = fint;
		SortedMap<Long,SortedSet<SmallBasicSet>> hLevel = new TreeMap<Long,SortedSet<SmallBasicSet>>();
		successors = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> ();
		predecessors = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> ();
		before = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> ();
		after = new TreeMap<SmallBasicSet,SortedSet<SmallBasicSet>> ();
		
		AntiChain h = fint.getTop();
		long minSize = Long.MAX_VALUE;
		long maxSize = Long.MIN_VALUE;
		if (h.equals(fint.getBottom())) {
			level = new SortedSet[1];
			level[0] = new TreeSet<SmallBasicSet>();
		}
		else {
			while (!h.equals(fint.getBottom())) {
				AntiChain hh = new AntiChain();
				for (SmallBasicSet a : h) 
					if (fint.getBottom().contains(a)) hh.addConditionally(a);
					else {
						maxSize = Math.max(maxSize, a.size());
						minSize = Math.min(minSize, a.size());
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
	 * is alfa U {s} antimonotonic
	 * @param alfa
	 * @param s
	 * @return
	 */
	private boolean amf(AntiChain alfa, SmallBasicSet s) {
		for (SmallBasicSet x : alfa) 
			if (x.hasAsSubset(s) || s.hasAsSubset(x)) return false;
		return true;
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

	private long compare(SmallBasicSet s1, SmallBasicSet s2) {
		long ldiv = sets.get(s1) - sets.get(s2);
		if (ldiv != 0) return ldiv;
		ldiv = pred(s1,s2);
		if (ldiv != 0) return ldiv;
		return succ(s1,s2);
	}
	
	/**
	 * decompose this poset recursively
	 * until the width of the components is not greater than split
	 * 
	 * @param split
	 * @param lev for logging purposes only
	 * @return
	 */
	public Set<IntervalPoset> decomposeIntelligently(Set<IntervalPoset> ret,int split,int lev) {
		final long maximumWidth = getWidth();
		if (maximumWidth <= split) {
			ret.add(this);
			return ret;
		}
		ArrayList<Integer> widest;
		// too big find the widest levels
		widest = new ArrayList<Integer>();
		for (int i = 1; i <= IntervalPoset.this.getMaxLevel(); i++) {
			if (IntervalPoset.this.getLevel(i).size() == maximumWidth) {
				widest.add(i);
			}
		}
		// pick a number of non comparative elements from eacch level
		AntiChain alfa = new AntiChain();
		int toPick = (int) Math.min(Math.round(getLevel(widest.get(0)).size()/3),12);
		pick(toPick,alfa,widest);

		// split the poset with respect to this set
		Iterator<AntiChain> tauIt = alfa.subSetsIterator();
		AntiChainInterval interval = this.getInterval();
		while (tauIt.hasNext()) {
			AntiChain tau = tauIt.next();
			AntiChainInterval phi = interval.phi(tau, alfa);
			IntervalPoset cPoset = new IntervalPoset(phi);
			cPoset.decomposeIntelligently(ret,split,lev+1);
		}
		return ret;
	}

	public Set<IntervalPoset> decomposeIntelligently(String[] desc, Set<IntervalPoset> ret,
			int split, int lev) throws SyntaxErrorException {
		final long maximumWidth = getWidth();
		if (maximumWidth <= split) {
			ret.add(this);
			return ret;
		}
		// too big, use one of desc[.]
		AntiChain alfa = AntiChain.parser().parse(desc[0]);
		alfa.retainAll(this.getPosetElements());
		for (String d : desc) {
			AntiChain alfaR = AntiChain.parser().parse(d);
			alfaR.retainAll(getPosetElements());
			if (alfaR.size() > alfa.size()) alfa = alfaR;
		}
		System.out.print(alfa.size() + ", ");

		// split the poset with respect to this set
		Iterator<AntiChain> tauIt = alfa.subSetsIterator();
		AntiChainInterval interval = this.getInterval();
		while (tauIt.hasNext()) {
			AntiChain tau = tauIt.next();
			AntiChainInterval phi = interval.phi(tau, alfa);
			IntervalPoset cPoset = new IntervalPoset(phi);
			cPoset.decomposeIntelligently(ret,split,lev+1);
		}
		return ret;
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

	private SortedMap<Integer, Integer> getFrequencies(SmallBasicSet toBeLabeled) {
		TreeMap<Integer, Integer> res = new TreeMap<Integer, Integer>();
		for (int i: toBeLabeled) {
			for (SmallBasicSet s : getPosetElements())
				if (s.contains(i)) store(res,i);
		}
		return res;
	}

	public long getFrequency(int i) {
		long res = 0;
		for (SmallBasicSet s : getPosetElements())
			if (s.contains(i)) res++;
		return res;
	}

	public AntiChainInterval getInterval() {return theInterval;}
	@Override
	public long getLatticeSize()  {
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
//		System.out.print("Lattice Size doing ");
		for (int i=firstLevel;i<=this.getMaxLevel();i+=2) {
//			System.out.print(getLevel(i).size() + " ");
			for (SmallBasicSet s : getLevel(i)) {
				Set<SmallBasicSet> prepre = new HashSet<SmallBasicSet>();
				prepredec.put(s, prepre);
				for (SmallBasicSet p : this.getPredecessors(s)) {
					prepre.addAll(this.getPredecessors(p));
				}
			}
		}
//		System.out.println(".");
		return getLatticeSize(exp,prepredec,new HashSet<SmallBasicSet>(),firstLevel);
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

	@Override
	public SortedSet<SmallBasicSet> getLevel(int n) {
		if (n > level.length) return new TreeSet<SmallBasicSet>();
		return level[n-1];
	}

	/**
	 * @pre s is in the poset
	 */
	@Override
	public int getLevel(SmallBasicSet s) {
		return (int) s.size();
	}

	public int getLowestLevelOf(int i) {
		for (int l = 1;l <= getMaxLevel();l++) {
			SortedSet<SmallBasicSet> lev = getLevel(l);
			for (SmallBasicSet s : lev) {
				if (s.contains(i)) return l;
			}
		}
		return getMaxLevel() + 1;
	}

	@Override
	public int getMaxLevel() {
		return level.length;
	}

	/**
	 * generate a descriptor of the poset
	 * @return
	 */
	public PosetDescriptor getPosetDescriptor() {
		SortedMap<SmallBasicSet, PosetDescriptor> brr = new TreeMap<SmallBasicSet, PosetDescriptor>();
		PosetDescriptor res = PosetDescriptor.getEmpty();
		for (int l = getMaxLevel();l > 0;l--) {
			for (SmallBasicSet s : getLevel(l)) {
				PosetDescriptor ps = PosetDescriptor.getEmpty();
				for (SmallBasicSet p : getSuccessors(s)) {
					ps = ps.addSuccessor(brr.get(p));
				}
				brr.put(s, ps);
			}
		}
		for (int l = 1;l <= getMaxLevel();l++) {
			for (SmallBasicSet s : getLevel(l)) {
				PosetDescriptor ps = brr.get(s);
				for (SmallBasicSet p : getPredecessors(s)) {
					ps = ps.addPredecessor(brr.get(p));
				}
				brr.put(s, ps);
				if (getPredecessors(s).isEmpty()) res = res.addSuccessor(ps);
			}
		}
		return res;
	}	

	@Override
	public SortedSet<SmallBasicSet> getPosetElements() {
		return (SortedSet<SmallBasicSet>) successors.keySet();
	}

	/**
	 */
	@Override
	public SimplePosetSize<SmallBasicSet> getPosetFrom(SortedSet<SmallBasicSet> bottom) {
/*		if (bottom.isEmpty()) return new IntervalPoset(0);

		SmallBasicSet running = bottom.first();
		long minLevel = running.size();
		for (SmallBasicSet s : bottom) if (getLevel(s) < minLevel) minLevel = getLevel(s);
		IntervalPoset res = new IntervalPoset(getMaxLevel());

		for (SmallBasicSet r : bottom) {
			res.level[(int) (getLevel(r) - minLevel)].add(r);
			res.successors.put(r, new TreeSet<>)
		}
*/		return null;
	}

	@Override
	public SortedSet<SmallBasicSet> getPredecessors(SmallBasicSet v) {
		return predecessors.get(v);
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

	/**
	 * compute the size of the lattice spanned by this poset by recursively
	 * decomposing the poset until the width of the components is not greater than split
	 * 
	 * @param split
	 * @param lev for logging purposes only
	 * @return
	 */
	public BigInteger getSizeIntelligently(int split,int lev) {
//		System.out.println("Entered " + this + "\n + intelligence ( " + split + ", " + lev + ")");
		long maximumWidth = getWidth();
//		System.out.println("maximumWidth " + maximumWidth);
		if (maximumWidth <= split) {
			long lsize = getLatticeSize();
			System.out.print("ev " + String.format("%20d ", lsize));
			return BigInteger.valueOf(lsize);
		}
		// too big find the widest levels
		BigInteger sum = BigInteger.ZERO;
		ArrayList<Integer> widest = new ArrayList<Integer>();
		for (int i=1;i<=this.getMaxLevel();i++) {
			if (this.getLevel(i).size() == maximumWidth) {
				widest.add(i);
			}
		}
		// pick a number of non comparative elements from eacch level
		AntiChain alfa = new AntiChain();
		int toPick = (int) Math.min(Math.round(getLevel(widest.get(0)).size()/3),12);
		pick(toPick,alfa,widest);

		// split the poset with respect to this set
		Iterator<AntiChain> tauIt = alfa.subSetsIterator();
		AntiChainInterval interval = this.getInterval();
		while (tauIt.hasNext()) {
			AntiChain tau = tauIt.next();
			AntiChainInterval phi = interval.phi(tau, alfa);
			IntervalPoset cPoset = new IntervalPoset(phi);
			BigInteger pSum = cPoset.getSizeIntelligently(split,lev+1);
			sum = sum.add(pSum);
			if (lev <= 1) System.out.println(sum);
		}
		return sum;
	}

	/**
	 * Get the union of all poset elements
	 * @return
	 */
	public SmallBasicSet getSpan() {
		if (span == null) {
			span = SmallBasicSet.emptySet();
			for (SmallBasicSet s : this.getPosetElements())
				span = span.union(s);
		}
		return span;
	}

	public String getStringDescriptor() {
		setLabels();
		String res = "";
		TreeSet<Long> succSet = new TreeSet<Long>();
		for (Long label: labels.keySet()) {
			succSet.clear();
			for (SmallBasicSet succ : this.getSuccessors(labels.get(label)))
				succSet.add(sets.get(succ));
			if (!succSet.isEmpty()) res += label + succSet.toString();
		}
		res = res.replace(" ","");
		return res;
	}
	
	@Override
	public SortedSet<SmallBasicSet> getSuccessors(SmallBasicSet v) {
		return successors.get(v);
	}

	private void pick(int toPick, AntiChain alfa, ArrayList<Integer> widest) {
		while (toPick > 0) {
			for (int i=0;toPick > 0 && i<widest.size();i++)
				for (SmallBasicSet s : getLevel(widest.get(i)))
					if (amf(alfa,s)) {
						alfa.add(s);
						toPick--;
						break;
					}
		}
	}

	private long pred(SmallBasicSet s1, SmallBasicSet s2) {
		SortedSet<Long> l1 = new TreeSet<Long>();
		SortedSet<Long> l2 = new TreeSet<Long>();
		for (SmallBasicSet su : this.getPredecessors(s1)) {
			l1.add(sets.get(su));
		}
		for (SmallBasicSet su : this.getPredecessors(s2)) {
			l2.add(sets.get(su));
		}
		Iterator<Long> it1 = l1.iterator();
		Iterator<Long> it2 = l2.iterator();
		while (it1.hasNext() && it2.hasNext()) {
			long diff = it1.next() - it2.next();
			if (diff != 0) return diff;
		}
		if (it1.hasNext()) return 1;
		if (it2.hasNext()) return -1;
		return 0;
	}
	private void setLabels() {
		Long label = 1L;
		sets = new TreeMap<SmallBasicSet,Long>();
		labels = new TreeMap<Long,SmallBasicSet>();
		
		for (int l = 1;l <= this.getMaxLevel();l++) {
			for (SmallBasicSet s : getLevel(l)) {
				sets.put(s, label);
			}
			label += getLevel(l).size();			
		}
		int l = 1;
		while (l <= getMaxLevel() ) {
			if (sortAndRelabel(getLevel(l))) {
				// something changed, if necessary do level below
				if (l > 1) l--;
				else l++;
			}			
			else {
				// nothing changed
				l++;
			}
			if (l > getMaxLevel())
				if (setSomeLabel()) l = 1;
		}
	}
	
	private boolean setSomeLabel() {
		long previous = 0;
		for (long label : labels.keySet()) {
			if (label > previous + 1) {
				sets.put(labels.get(previous),label-1);
				return true;
			}
			previous = label;
		}
		return false;
	}

	private boolean sortAndRelabel(SortedSet<SmallBasicSet> level) {
		SmallBasicSet[] help = new SmallBasicSet[level.size()];
		int filled = 0;
		for (SmallBasicSet s : level) { 
			int i;
			for (i=filled;i > 0 && compare(s,help[i-1]) < 0;i--)
				help[i] = help[i-1];
			help[i] = s;
			filled++;
		}
		boolean change = false;
		long currentLabel = sets.get(help[0]);
		long nextLabel = currentLabel + 1;
		for (int i=1;i<help.length;i++) {
			if (compare(help[i-1],help[i]) == 0) {
				if (sets.get(help[i]) != currentLabel) {
					sets.put(help[i], currentLabel);
					change = true;
				}
				nextLabel++;
			}
			else {
				currentLabel = nextLabel++;
				if (sets.get(help[i]) != currentLabel) {
					sets.put(help[i], currentLabel);
					change = true;
				}
			}
		}
		for (SmallBasicSet s : sets.keySet()) {
			labels.put(sets.get(s), s);
		}
		return change;
	}

	private void sortOrder(SortedMap<Integer, Integer> order,
			SortedMap<Integer, SmallBasicSet> invertedOrder,
			SortedMap<Integer, Integer> onTable) {
		for (int i: invertedOrder.keySet()) {
			SmallBasicSet equal = invertedOrder.get(i);
			SortedMap<Integer,SmallBasicSet> partInvert = new TreeMap<Integer,SmallBasicSet>();
			for (int j:equal) store(partInvert,onTable.get(j),j);
			int increment = 0;
			for (int c:partInvert.keySet()) {
				SmallBasicSet pInv = partInvert.get(c);
				for (int j: pInv)
					order.put(j, i + increment);
				increment += pInv.size();
			}
		}
		invertedOrder.clear();
		for (int o: order.keySet()) 
			store(invertedOrder,order.get(o),o);
	}

	private void sortOrderDecreasing(SortedMap<Integer, Integer> order,
			SortedMap<Integer, SmallBasicSet> invertedOrder,
			SortedMap<Integer, Integer> onTable) {
		for (int i: invertedOrder.keySet()) {
			SmallBasicSet equal = invertedOrder.get(i);
			SortedMap<Integer,SmallBasicSet> partInvert = new TreeMap<Integer,SmallBasicSet>();
			for (int j:equal) store(partInvert,-onTable.get(j),j);
			int increment = 0;
			for (int c:partInvert.keySet()) {
				SmallBasicSet pInv = partInvert.get(c);
				for (int j: pInv)
					order.put(j, i + increment);
				increment += pInv.size();
			}
		}
		invertedOrder.clear();
		for (int o: order.keySet()) 
			store(invertedOrder,order.get(o),o);
	}

	private void store(SortedMap<Integer, SmallBasicSet> rep, int i,
			int i2) {
		if (!rep.containsKey(i)) rep.put(i, SmallBasicSet.emptySet());
		rep.put(i,rep.get(i).add(i2));
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
	
	/**
	 * general counter
	 * @param <V>
	 * @param res
	 * @param i
	 */
	private <V extends Comparable<V>>void store(TreeMap<V, Integer> res, V i) {
		if (!res.containsKey(i)) res.put(i, 0);
		res.put(i, res.get(i) + 1);
	}

	private long succ(SmallBasicSet s1,SmallBasicSet s2) {
		SortedSet<Long> l1 = new TreeSet<Long>();
		SortedSet<Long> l2 = new TreeSet<Long>();
		for (SmallBasicSet su : this.getSuccessors(s1)) {
			l1.add(sets.get(su));
		}
		for (SmallBasicSet su : this.getSuccessors(s2)) {
			l2.add(sets.get(su));
		}
		Iterator<Long> it1 = l1.iterator();
		Iterator<Long> it2 = l2.iterator();
		while (it1.hasNext() && it2.hasNext()) {
			long diff = it1.next() - it2.next();
			if (diff != 0) return -diff;
		}
		if (it1.hasNext()) return -1;
		if (it2.hasNext()) return +1;
		return 0;
	}
	/**
	 * rank the elements figuring in this poset according to their position in the poset topology
	 * @return
	 */
	public SortedMap<Integer,Integer> toStandardOrder() {
		SortedMap<Integer,Integer> order = new TreeMap<Integer,Integer>();
		SortedMap<Integer,SmallBasicSet> 
		invertedOrder = new TreeMap<Integer,SmallBasicSet>() ;

		SmallBasicSet toBeLabeled = getSpan();
		
		// all elements initially obtain the same order
		for (int i : toBeLabeled) {
			order.put(i,1);
			store(invertedOrder,1,i);
		}
		
		// the next criterion is the level at which the elements first occur
		SortedMap<Integer,Integer> levelOf = new TreeMap<Integer,Integer>();
		for (int l = 1;l<=getMaxLevel();l++) {
			SortedSet<SmallBasicSet> lev = getLevel(l);
			SmallBasicSet thisLevel = SmallBasicSet.emptySet();
			for (SmallBasicSet s : lev) {
				thisLevel = thisLevel.union(s);
			}
			for (int i: thisLevel) if (!levelOf.keySet().contains(i)) levelOf.put(i, l);
		}
		sortOrder(order, invertedOrder,levelOf);
		
		// the next criterion is the frequency of occurrence
		SortedMap<Integer,Integer> frequencies = getFrequencies(toBeLabeled);
		sortOrderDecreasing(order, invertedOrder, frequencies);
		
		/*		for (int o : invertedOrder.keySet()) {
			SmallBasicSet so = invertedOrder.get(o);
			if (so.size() > 1) {
				for (int i:so) meeting.put(i, meetingPattern(i));
			}
	}
*/		
		return order;
		
	}
	
	

	public String showPartialOrder() {
		String res = "";
		for (int k = 0;k < level.length;k++) {
			for (SmallBasicSet s: level[k]) {
				res += "||" + s.toString() + "==>" + successors.get(s);
			}
		}
		res += "\n";
		for (int k = 0;k < level.length;k++) {
			for (SmallBasicSet s: level[k]) {
				res += "||" + predecessors.get(s) + "<==" + s.toString();
			}
		}
		res += "\n" + this.getPosetElements();
		return res;
	}
	
	public String toString() {
		SortedSet<SmallBasicSet> set = getPosetElements();
		if (set.isEmpty()) return "[]";
		String res = "[";
		for (SmallBasicSet s : getPosetElements()) {
			res = res + s + ",";
		}
		return res.substring(0,res.length()-1) + "]";
	}
}
