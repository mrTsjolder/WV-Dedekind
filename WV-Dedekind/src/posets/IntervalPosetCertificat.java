package posets;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import amfsmall.AntiChainInterval;
import amfsmall.AntiChain;
import amfsmall.SmallBasicSet;

/**
 * Class to produce a certificat for an IntervalPoset
 * dirty version
 * @author patrickdecausmaecker
 *
 */
public class IntervalPosetCertificat {

	private int[] rank;
	private BigInteger[] labels;
	private long[] frequencies;
	private int[] permutation;
	private int[] inversePermutation;
	private boolean[] dontBother;
	private SmallBasicSet span;
	private SortedSet<SmallBasicSet> elements;
	private IntervalPoset poset;
	
	public int getMaximalElement() {return rank.length;}
	
	public SmallBasicSet getSpan() {return span;}
	/**
	 * the constructor builds a certificat from an IntervalPoset by ranking the elements figuring in the sets of the poset
	 * 
	 * @param p
	 */
	public IntervalPosetCertificat(IntervalPoset p) {
		poset = p;
		span = p.getSpan();
		rank = new int[(int) SmallBasicSet.universe().size()]; 
		// rank[i] is the position of i+1 among the elements 
		// in the universe, first position being 1
		for (int i=0;i<rank.length;i++) rank[i] = 1;
		labels = new BigInteger[getMaximalElement()]; // labels[i] is the label of i+1 in the poset
		frequencies = new long[getMaximalElement()]; // frequecies[i] is the number of occurrences of i+1
		permutation = new int[getMaximalElement()]; // permuted partner of i is permutation[i-1]+1
		dontBother = new boolean[getMaximalElement()]; // elements with zero frequency and uniquely determined rank need not be investigated further
		for (int i=0;i<permutation.length;i++) permutation[i] = i;
		elements = p.getPosetElements();
		for (int i=0;i<getMaximalElement();i++) {
			if (getSpan().contains(i+1)) {
				frequencies[i] = getFrequency(i+1,elements);
			}
			else frequencies[i] = 0;
		}
		sortDescendingTableOnTable(permutation, frequencies,rank);
		// elements with zero frequency remain where they are
		for (int i=0;i<getMaximalElement();i++) {
			dontBother[i] = (frequencies[permutation[i]] == 0);
		}
		// elements for which the rank is unique remain where they are
		dontBother[0] = dontBother[0] || rank[permutation[1]] != rank[permutation[0]];
		for (int i=1;i<getMaximalElement()-1;i++) {
			dontBother[i] = dontBother[i] || (rank[permutation[i-1]] != rank[permutation[i]] && rank[permutation[i+1]] != rank[permutation[i]]);
		}
		dontBother[getMaximalElement()-1] = dontBother[getMaximalElement()-1] 
		                                               || rank[permutation[getMaximalElement()-1]] != rank[permutation[getMaximalElement()-2]];
		boolean inComplete;
		do {
			for (int i=0;i<getMaximalElement();i++) 
				labels[permutation[i]] = BigInteger.valueOf(rank[permutation[i]]);
			int radix = getMaximalElement(); // number of bits to shift the previous
			for (int lev = 2;lev <= poset.getMaxLevel();lev++) {
				int[] levelRank = new int[poset.getLevel(lev).size()];
				SmallBasicSet[] level = sortLevel(lev,levelRank);
				for (int i=0;i<getMaximalElement();i++) {
					labels[permutation[i]] = labels[permutation[i]].shiftLeft(radix);
					if (!dontBother[i])
						for (int j=0;j<level.length;j++) 
							if (level[j].contains(permutation[i]+1)) labels[permutation[i]] = labels[permutation[i]].add(BigInteger.valueOf(levelRank[j] + 1));
				}
			}
			inComplete = sortAscendingTableOnTable(permutation, labels, rank, dontBother);
			if (!inComplete && setSome(rank, dontBother)) {
				inComplete = true;
			}
		} while (inComplete);
		inversePermutation = inversePermutation();
	}
	
	public IntervalPoset getStandardPoset() {
		return new IntervalPoset(getStandardInterval());
	}
	
	public String getLabeledDescriptor() {
		if (!setLabeled()) labelSets();
		return getLabelPoset().toString();
	}
	
	private SortedMap<Long,SortedSet<Long>> getLabelPoset() {
		return labelPoset;
	}

	private SortedMap<SmallBasicSet,Long> setLabels;
	private SortedMap<Long,SortedSet<Long>> labelPoset;
	private boolean isSetLabeled;

	private void labelSets() {
		long label = 1;
		SortedSet<SmallBasicSet> permuted = new TreeSet<SmallBasicSet>(getElements());
		permuted.addAll(getElements());
		setLabels = new TreeMap<SmallBasicSet,Long> ();
		for (SmallBasicSet s : permuted) 
			setLabels.put(s, label++);
		labelPoset = new TreeMap<Long,SortedSet<Long>>();
		for (SmallBasicSet s : getElements()) {
			SortedSet<Long> succ = new TreeSet<Long>();
			getLabelPoset().put(setLabels.get(this.permutedSet(s)), succ);
			SortedSet<SmallBasicSet> succs = poset.getSuccessors(s);
			for (SmallBasicSet t : succs) 
				succ.add(setLabels.get(permutedSet(t)));
		}
		isSetLabeled = true;
	}

	public SortedSet<SmallBasicSet> getElements() {
		return elements;
	}

	private boolean setLabeled() {
		return isSetLabeled;
	}

	public AntiChainInterval getStandardInterval() {
		AntiChain bottom = new AntiChain();
		AntiChain top = new AntiChain();
		for (SmallBasicSet s : poset.getInterval().getBottom()) {
			bottom.add(permutedSet(s));
		}
		for (SmallBasicSet s : poset.getInterval().getTop()) {
			top.add(permutedSet(s));
		}	
		return new AntiChainInterval(bottom, top);
	}

	public String toString() {
		SortedMap<SmallBasicSet,String> names = new TreeMap<SmallBasicSet,String>();
		
		IntervalPoset poset = getStandardPoset();
		
		for (int i=1;i<=poset.getMaxLevel();i++) {
			Set<SmallBasicSet> l = poset.getLevel(i);
			String pame = "A";
			for (SmallBasicSet r : l) {
				names.put(r, pame);
				pame = nextName(pame);
				}
			}
		String res = "";
		for (int i=1;i<=poset.getMaxLevel();i++) {
			res += "[";
			Set<SmallBasicSet> l = poset.getLevel(i);
			for (SmallBasicSet r : l) {
				res += "[";
				for (SmallBasicSet s : poset.getSuccessors(r))
					res += names.get(s) + " ";
				res += "]";
			}
			res += "]";
		}
		return res;
	}
	
 	private String nextName(String pame) {
 		int pos = pame.length() - 1;
 		while (pos >= 0 && pame.charAt(pos) == 'Z') {
 			pame = pame.substring(0,pos) + 'A' + pame.substring(pos + 1);
 			pos--;
 		}
 		if (pos < 0)
 			pame = 'A' + pame;
 		else pame = pame.substring(0,pos) + ((char) (pame.charAt(pos) + 1)) + pame.substring(pos + 1);

		return pame;
	}

	private int[] inversePermutation() {
		int[] res = new int[permutation.length];
		for (int i=0;i<res.length;i++) res[permutation[i]] = i;
		return res;
	}
	private SmallBasicSet permutedSet(SmallBasicSet s) {
		SmallBasicSet res = SmallBasicSet.emptySet();
		for (int i:s) {
			res = res.add(inversePermutation[i-1] + 1);
		}
		return res;
	}

	private boolean setSome(int[] rank, boolean[] immutable) {
		int rP = rank[permutation[0]];
		int p = 0;
		int c;
		for (c = 1;c < getMaximalElement();c++) {
			if (rank[permutation[c]] != rP) {
				if (!immutable[c-1] && c - p > 1) {
					rank[permutation[c-1]] = rank[permutation[c-1]] + c-p-1;
					immutable[c-1] = true;
					if (c-p == 2) immutable[c-2] = true;
					return true;
				}
				p = c;
				rP = rank[permutation[p]];
			}
		}
		if (!immutable[c-1] && c - p > 1) {
			rank[permutation[c-1]] = rank[permutation[c-1]] + c-p-1;
			immutable[c-1] = true;
			if (c-p == 2) immutable[c-2] = true;
			return true;
		}
		return false;
	}

	private SmallBasicSet[] sortLevel(int l, int[] levelRank) {
		SmallBasicSet[] res = new SmallBasicSet[poset.getLevel(l).size()];
		res = poset.getLevel(l).toArray(res);
		BigInteger[] labels = new BigInteger[res.length];
		int radix = getMaximalElement();
		for (int i=0;i<labels.length;i++) {
			labels[i] = BigInteger.ZERO;
			int[] desc = new int[(int) res[i].size()];
			int p = 0;
			for (int j:res[i]) {
				desc[p++] = rank[j];
			}
			Arrays.sort(desc);
			for (int j = desc.length-1;j>=0;j--) labels[i] = labels[i].shiftLeft(radix).add(BigInteger.valueOf(desc[j]));
		}
		int[] permutation = new int[res.length];
		for (int i=0;i<permutation.length;i++) permutation[i] = i;
		sortAscendingTableOnTable(permutation,labels,levelRank);
		return permute(res,permutation);
	}

	private   SmallBasicSet[] permute(SmallBasicSet[] res, int[] permutation) {
		SmallBasicSet[] ret =  new SmallBasicSet[res.length];
		for (int i=0;i<permutation.length;i++) ret[permutation[i]] = res[i];
		return ret;                                                     
	}

	/**
	 * sort the toSort table in decreasing order of on
	 * A simple bubble sort, the table will never be longer than 10
	 * @param toSort
	 * @param on
	 * @return l has been changed
	 */
	private boolean sortDescendingTableOnTable(int[] toSort, long[] on, int[] l) {
		
		if (toSort.length == 0) return false;
		
		for (int i=0;i<toSort.length;i++)
			for (int j=0;j<toSort.length-i-1;j++)
				if (on[toSort[j]] < on[toSort[j+1]]) {
					int h = toSort[j];
					toSort[j] = toSort[j+1];
					toSort[j+1] = h;
				}
		int label = 1;
		int identical = 0;
		boolean changed = false;
		for (int j=0;j<toSort.length-1;j++) {
			if (l[toSort[j]] != label) {
				l[toSort[j]] = label;
				changed = true;
			}
			identical++;
			if (on[toSort[j]] != on[toSort[j+1]]) {
				label += identical;
				identical = 0;
			}
		}
		if (l[toSort[toSort.length-1]] != label) {
			l[toSort[toSort.length-1]] = label;
			changed = true;
		}
		return changed;
	}

	/**
	 * sort the toSort table in increasing order of on
	 * A simple bubble sort
	 * @param toSort
	 * @param labels2
	 * @param unique signals unique labels
	 * @return true is l has been changed
	 */
	private boolean sortAscendingTableOnTable(int[] toSort, BigInteger[] labels2, int[] l, boolean[] unique) {
		
		if (toSort.length == 0) return false;
		
		for (int i=0;i<toSort.length;i++)
			for (int j=0;j<toSort.length-i-1;j++)
				if (labels2[toSort[j]].compareTo(labels2[toSort[j+1]]) > 0) {
					int h = toSort[j];
					toSort[j] = toSort[j+1];
					toSort[j+1] = h;
				}
		int label = 1;
		int identical = 0;
		boolean changed = false;
		for (int j=0;j<toSort.length-1;j++) {
			if (l[toSort[j]] != label) {
				l[toSort[j]] = label;
				changed = true;
			}
			identical++;
			if (!labels2[toSort[j]].equals(labels2[toSort[j+1]])) {
				label += identical;
				if (identical == 1) unique[j] = true;
				identical = 0;
			}
		}
		if (l[toSort[toSort.length-1]] != label) {
			l[toSort[toSort.length-1]] = label;
			if (identical == 0) unique[toSort.length-1] = true;
			changed = true;
		}
		return changed;
	}
	/**
	 * sort the toSort table in increasing order of on
	 * A simple bubble sort
	 * @param toSort
	 * @param labels2
	 * @return true is l has been changed
	 */
	private boolean sortAscendingTableOnTable(int[] toSort, BigInteger[] labels2, int[] l) {
		
		if (toSort.length == 0) return false;
		
		for (int i=0;i<toSort.length;i++)
			for (int j=0;j<toSort.length-i-1;j++)
				if (labels2[toSort[j]].compareTo(labels2[toSort[j+1]]) > 0) {
					int h = toSort[j];
					toSort[j] = toSort[j+1];
					toSort[j+1] = h;
				}
		int label = 1;
		int identical = 0;
		boolean changed = false;
		for (int j=0;j<toSort.length-1;j++) {
			if (l[toSort[j]] != label) {
				l[toSort[j]] = label;
				changed = true;
			}
			identical++;
			if (labels2[toSort[j]] != labels2[toSort[j+1]]) {
				label += identical;
				identical = 0;
			}
		}
		if (l[toSort[toSort.length-1]] != label) {
			l[toSort[toSort.length-1]] = label;
			changed = true;
		}
		return changed;
	}
	
	public String Report() {
		return "permutation:" + Arrays.toString(permutation) + "\n" 
			+  "rank       :" + Arrays.toString(rank) + "\n"
			+  "frequencies:" + Arrays.toString(frequencies) + "\n"
			+  "labels     :" + Arrays.toString(labels);
	}

	/**
	 * Compute the number of sets in e in which i occurs
	 * @param i
	 * @param e
	 * @return
	 */
	private long getFrequency(int i, SortedSet<SmallBasicSet> e) {
		long res = 0;
		for (SmallBasicSet s : e) if (s.contains(i)) res++;
		return res;
	}
}
