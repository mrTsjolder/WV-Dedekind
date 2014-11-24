package amfsmall;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * a poset of sets without gaps 
 * @invar (a, b in set and a < c < b then c in set)
 * 
 * @author patrickdecausmaecker
 *
 */
public class PoSetOfSets {

	/**
	 * the level of each set in the poSet
	 */
	private TreeMap<SmallBasicSet,Integer> level;
	private TreeMap<Integer, TreeSet<SmallBasicSet>> inverseLevel;
	private TreeMap<SmallBasicSet,TreeSet<SmallBasicSet>> immediateS; // list of immediate successors
	private TreeMap<SmallBasicSet,TreeSet<SmallBasicSet>> immediateP; // list of immediate predecessors

	/**
	 * @pre no gaps in s
	 * @param c
	 */
	public PoSetOfSets(Collection<SmallBasicSet> c) {
		level = new TreeMap<SmallBasicSet,Integer>();
		inverseLevel = new TreeMap<Integer, TreeSet<SmallBasicSet>>();
		immediateS = new TreeMap<SmallBasicSet, TreeSet<SmallBasicSet>>();
		immediateP = new TreeMap<SmallBasicSet, TreeSet<SmallBasicSet>>();
		
		for (SmallBasicSet s : c) { // add all sets
			immediateS.put(s, new TreeSet<SmallBasicSet>());
			immediateP.put(s, new TreeSet<SmallBasicSet>());
			}
		for (SmallBasicSet s : c)
			for (SmallBasicSet x : s.immediateSubSets())
				if (c.contains(x)) {
					immediateS.get(x).add(s); // s is immediate successor of x
					immediateP.get(s).add(x); // x is immediate predecessor of s
				}
		
		inverseLevel.put(1,new TreeSet<SmallBasicSet>());
		for (SmallBasicSet s : c) {
			if (immediateP.get(s).isEmpty()) { // s has no immediate predecessors
				level.put(s, 1);
				inverseLevel.get(1).add(s);
			}
		}
		
		TreeSet<SmallBasicSet> l = inverseLevel.get(1);
		int lev = 1;
		while (!l.isEmpty()) {
			lev++;
			inverseLevel.put(lev, new TreeSet<SmallBasicSet>());			
			for (SmallBasicSet s : l) {
				TreeSet<SmallBasicSet> succ = immediateS.get(s);
				inverseLevel.get(lev).addAll(succ);
				for (SmallBasicSet x : succ) level.put(x, lev);
			}
			l = inverseLevel.get(lev);
		}
	}
	
	/**
	 * label with respect to increasing level, number of succesors
	 */
	public TreeMap<SmallBasicSet,Integer> labelIncreasingLevelSuccessors() {
		
		if (level.size() == 0) return new TreeMap<SmallBasicSet,Integer>();
		
		SmallBasicSet [] row = new SmallBasicSet[level.size()];
		row = level.keySet().toArray(row);
		Comparator<SmallBasicSet> cmp = new Comparator<SmallBasicSet>() {
			// comparison based on the level, number of successors
			// equality is not equivalent with SmallBasicSet equality
			@Override
			public int compare(SmallBasicSet o1, SmallBasicSet o2) {
				int levelDiff = level.get(o1) - level.get(o2);
				if (levelDiff != 0) return levelDiff;
				int succDiff = immediateS.get(o1).size() - immediateS.get(o2).size();
				if (succDiff != 0) return succDiff;
				return immediateP.get(o1).size() - immediateP.get(o2).size();
			}		
		};
		Arrays.sort(row,cmp);
		int label = 1;
		int equal = 1;
		final TreeMap<SmallBasicSet,Integer> res = new TreeMap<SmallBasicSet,Integer>();
		for (int i = 0;i < row.length -1;i++) {
			res.put(row[i], label);
			if (cmp.compare(row[i],row[i+1]) != 0) {
				label+=equal;
				equal = 1;
			}
			else equal++;
		}
		res.put(row[row.length-1], label);

		cmp = new Comparator<SmallBasicSet>() {
			// comparison based on the existing label, string representation of set of labels of successors
			// equality is not equivalent with SmallBasicSet equality
			@Override
			public int compare(SmallBasicSet o1, SmallBasicSet o2) {
//				int labelDiff = res.get(o1) - res.get(o2);
//				if (labelDiff != 0) return labelDiff;

				TreeSet<Integer> labelsO1 = new TreeSet<Integer>();
				TreeSet<Integer> labelsO2 = new TreeSet<Integer>();
				for (SmallBasicSet s : immediateS.get(o1)) labelsO1.add(res.get(s));
				for (SmallBasicSet s : immediateS.get(o2)) labelsO2.add(res.get(s));
				
				return labelsO1.toString().compareTo(labelsO2.toString());
			}		
		};
		
		for (int r = row.length-1;r > 0;) {
			int currentLabel = res.get(row[r]);
			int p = r-1;
			while (p >= 0 && res.get(row[p]) == currentLabel) p--;
			if (p < r-1) {
				Arrays.sort(row,p+1,r+1,cmp);
				label = res.get(row[p+1]);
				equal = 1;
				for (int i = p+1;i<r;i++) {
					res.put(row[i], label++);
/**					if (cmp.compare(row[i],row[i+1]) != 0) {
						label+=equal;
						equal = 1;
					}
					else equal++;			
*/				}
				res.put(row[r], label);
			}
			r = p;
		}
		
		return res;
	}
	
	public String toString() {
		String res = "";
		for (SmallBasicSet s : inverseLevel.get(1))
			res += listSuccessors("",s);
		return res;
	}


	private String listSuccessors(String string, SmallBasicSet s) {
		TreeSet<SmallBasicSet> succ = immediateS.get(s);
		if (succ.isEmpty()) return string + s + "\n";
		String res = "";
		for (SmallBasicSet x : succ) res += listSuccessors(string + s + "->", x);
		return res;
	}
	
	public String toPoSetDescription() {
		TreeMap<SmallBasicSet,Integer> labels = labelIncreasingLevelSuccessors();
		makeUniqueLabels(labels);
		TreeMap<Integer,TreeSet<Integer>> wLab = new TreeMap<Integer,TreeSet<Integer>>();
		for (SmallBasicSet s : labels.keySet()) {
			Integer k = labels.get(s);
			TreeSet<Integer> lab = new TreeSet<Integer>();
			for (SmallBasicSet x : immediateS.get(s)) {
				lab.add(labels.get(x));
			}
			wLab.put(k, lab);
		}
		String res = "";
		for (Integer k : wLab.keySet()) res += k + "" + wLab.get(k);
		return res;
	}
	
	public String normalisedPoSet() {
		return new PoSetOld(toPoSetDescription()).toString();
	}

	private void makeUniqueLabels(TreeMap<SmallBasicSet, Integer> labels) {
		TreeSet<Integer> lbls = new TreeSet<Integer>();
		for (SmallBasicSet s : labels.keySet()) {
			int x = labels.get(s);
			while (lbls.contains(x)) x++;
			labels.put(s, x);
			lbls.add(x);
		}
	}
}
