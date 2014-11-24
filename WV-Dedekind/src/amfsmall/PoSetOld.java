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
public class PoSetOld {

	/**
	 * the level of each set in the poSet
	 */
	private TreeMap<Integer,Integer> level;
	private TreeMap<Integer, TreeSet<Integer>> inverseLevel;
	private TreeMap<Integer,TreeSet<Integer>> immediateS; // list of immediate successors
	private TreeMap<Integer,TreeSet<Integer>> immediateP; // list of immediate predecessors

	/**
	 * @pre no gaps in s
	 * @param c
	 */
	public PoSetOld(String description) {
		level = new TreeMap<Integer,Integer>();
		inverseLevel = new TreeMap<Integer, TreeSet<Integer>>();
		immediateS = new TreeMap<Integer, TreeSet<Integer>>();
		immediateP = new TreeMap<Integer, TreeSet<Integer>>();
		
		parse(description,level,inverseLevel,immediateS,immediateP);
/*		for (Integer s : c) { // add all sets
			immediateS.put(s, new TreeSet<Integer>());
			immediateP.put(s, new TreeSet<Integer>());
			}
		for (Integer s : c)
			for (Integer x : s.immediateSubSets())
				if (c.contains(x)) {
					immediateS.get(x).add(s); // s is immediate successor of x
					immediateP.get(s).add(x); // x is immediate predecessor of s
				}
		
		inverseLevel.put(1,new TreeSet<Integer>());
		for (Integer s : c) {
			if (immediateP.get(s).isEmpty()) { // s has no immediate predecessors
				level.put(s, 1);
				inverseLevel.get(1).add(s);
			}
		}
		
		TreeSet<Integer> l = inverseLevel.get(1);
		int lev = 1;
		while (!l.isEmpty()) {
			lev++;
			inverseLevel.put(lev, new TreeSet<Integer>());			
			for (Integer s : l) {
				TreeSet<Integer> succ = immediateS.get(s);
				inverseLevel.get(lev).addAll(succ);
				for (Integer x : succ) level.put(x, lev);
			}
			l = inverseLevel.get(lev);
		}
*/	}
	
	private void parse(String description, TreeMap<Integer, Integer> level2,
			TreeMap<Integer, TreeSet<Integer>> inverseLevel2,
			TreeMap<Integer, TreeSet<Integer>> immediateS2,
			TreeMap<Integer, TreeSet<Integer>> immediateP2) {
		while (description.length() != 0) {
			int p = 0;
			// read next node
			
			while (description.charAt(p) != '[') p++;
			Integer nxt = Integer.valueOf(description.substring(0, p));

			immediateS2.put(nxt, new TreeSet<Integer>());
			immediateP2.put(nxt, new TreeSet<Integer>());

			description = description.substring(p+1);
			char nChar;
			do {
				p = 0; 
				nChar = description.charAt(p);
				while (nChar != ',' && nChar!= ']') {
					p++;
					nChar = description.charAt(p);
				}
				if (p != 0) {
					int sp = 0;
					while (description.charAt(sp) == ' ') sp++;
					immediateS2.get(nxt).add(Integer.valueOf(description.substring(sp,p)));
				}
				description = description.substring(p+1);
			} while (nChar != ']');
		}
		
		for (Integer t : immediateS2.keySet()) {
			for (Integer x : immediateS2.get(t)) {
				immediateP2.get(x).add(t);
			}
		}
		
		for (Integer t : immediateS2.keySet()) {
			if (immediateP2.get(t).isEmpty()) setLevel(t,1,immediateS2, level2,inverseLevel2);
		}
		
	}

	private void setLevel(Integer t, int i, TreeMap<Integer, TreeSet<Integer>> iS2, TreeMap<Integer, Integer> l2,
			TreeMap<Integer, TreeSet<Integer>> iL2) {
		if (!iL2.containsKey(i)) iL2.put(i, new TreeSet<Integer>());
		iL2.get(i).add(t);
		l2.put(t,i);
		for (Integer x : iS2.get(t)) setLevel(x,i+1,iS2,l2,iL2); 
	}

	/**
	 * label with respect to increasing level, number of succesors
	 */
	public TreeMap<Integer,Integer> labelIncreasingLevelSuccessors() {
		Integer [] row = new Integer[level.size()];
		row = level.keySet().toArray(row);
		Comparator<Integer> cmp = new Comparator<Integer>() {
			// comparison based on the level, number of successors
			// equality is not equivalent with Integer equality
			@Override
			public int compare(Integer o1, Integer o2) {
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
		TreeMap<Integer,Integer>res = new TreeMap<Integer,Integer>();
		for (int i = 0;i < row.length -1;i++) {
			res.put(row[i], label);
			if (cmp.compare(row[i],row[i+1]) != 0) {
				label+=equal;
				equal = 1;
			}
			else equal++;
		}
		res.put(row[row.length-1], label);
		return res;
	}
	
	public String toString() {
		String res = "";
		for (Integer s : labelIncreasingLevelSuccessors().keySet())
			res += "" + s + immediateS.get(s);
		return res;
	}

	public BigInteger sizeOfLattice() {
		if (level.size() == 1) return BigInteger.valueOf(2);
		if (level.size() == 0) return BigInteger.ONE;
		int top = inverseLevel.lastKey();
		while (top > 0 && inverseLevel.get(top).size() <= 1) top--;
		if (top == 0) return BigInteger.valueOf(level.size());
		Integer x = inverseLevel.get(top).first();
		PoSetOld[] twoParts = splitOff(x);
		return twoParts[0].sizeOfLattice().multiply(twoParts[1].sizeOfLattice());
	}

	public PoSetOld[] splitOff(Integer x) {
		PoSetOld[] res = new PoSetOld[2];
		String[] description = new String[2];
		description[0] = "";
		description[1] = "";
		
		TreeSet<Integer> subX = under(x);
			
		TreeSet<Integer> notSubX = new TreeSet<Integer>(level.keySet());
		notSubX.removeAll(subX);

		for (int p : subX) {
			TreeSet<Integer> succP = new TreeSet<Integer>(immediateS.get(p));
			succP.removeAll(notSubX);
			description[1] = description[1] + p + succP;
		}
		for (int p : notSubX) {
			TreeSet<Integer> succP = new TreeSet<Integer>(immediateS.get(p));
			succP.removeAll(subX);
			description[0] = description[0] + p + succP;
		}

		res[0] = new PoSetOld(description[0]);
		res[1] = new PoSetOld(description[1]);
 		
		return res;
	}

	private TreeSet<Integer> under(Integer x) {
		TreeSet<Integer> res = new TreeSet<Integer>();
		res.add(x);
		TreeSet<Integer> pX = immediateP.get(x);
		if (pX.isEmpty()) return res;
		for (int p : pX) res.addAll(under(p));
		return res;
	}

	private String listSuccessors(String string, Integer s) {
		TreeSet<Integer> succ = immediateS.get(s);
		if (succ.isEmpty()) return string + s + "\n";
		String res = "";
		for (Integer x : succ) res += listSuccessors(string + s + "->", x);
		return res;
	}
}
