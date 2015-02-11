package amfsmall;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;

/**
 * More efficient representation for AntiChains.
 */
public class SmallAntiChain implements Iterable<SmallBasicSet>, Comparable<SmallAntiChain>, LatticeElement {

	private BitSet theAntiChain = new BitSet();
	private SmallBasicSet universe = SmallBasicSet.universe();
	
	/********************************************************
	 * Constructors											*
	 ********************************************************/
	
	/**
	 * Create an antichain based on set of SmallBasicSets
	 */
	public SmallAntiChain(Collection<SmallBasicSet> C) {
		for(SmallBasicSet s : C) 
			theAntiChain.set(s.toIntRepresentation());
	}
	
	/**
	 * Create an antichain with another antichain.
	 */
	public SmallAntiChain(SmallAntiChain ac) {
		//TODO: clone?
		theAntiChain = (BitSet) ac.theAntiChain.clone();
		this.setUniverse(ac.getUniverse());
	}
	
	/**
	 * Create an antichain based on a long.
	 */
	public SmallAntiChain(long[] l) {
		theAntiChain = BitSet.valueOf(l);
	}

	/********************************************************
	 * Basic antichains	& other static methods				*
	 ********************************************************/

	private static SmallAntiChain emptyAntiChain = new SmallAntiChain();
	private static SmallAntiChain emptySetAntiChain = new SmallAntiChain();
		static {emptySetAntiChain.theAntiChain.set(1);}
	
	public static SmallAntiChain emptyAntiChain() {
		return new SmallAntiChain(emptyAntiChain);
	}
	
	public static SmallAntiChain emptySetAntiChain() {
		return new SmallAntiChain(emptySetAntiChain);
	}
	
	public static SmallAntiChain singletonAntiChain(int l) {
		if(l == 0)
			return emptySetAntiChain();
		SmallAntiChain result = new SmallAntiChain();
		result.theAntiChain.set((int) (1L << (l-1)));
		return result;
	}
	
	public static SmallAntiChain universeAntiChain(int n) {
		SmallAntiChain result = new SmallAntiChain();
		result.theAntiChain.set(((int) (1L << n) - 1));
		result.setUniverse(SmallBasicSet.universe(n));
		return result;
	}
	
	public static SmallAntiChain oneSetAntiChain(SmallBasicSet x) {
		SmallAntiChain result = new SmallAntiChain();
		result.theAntiChain.set(x.toIntRepresentation());
		return result;
	}
	
	public static SmallAntiChain emptyAntiChain(SmallBasicSet u) {
		SmallAntiChain result = emptyAntiChain();
		result.setUniverse(u);
		return result;
	}
	
//	public static void makeAntiChain(SmallAntiChain pseudoAC) {
//		for(int i = pseudoAC.theAntiChain.nextSetBit(0); i >= 0; i = pseudoAC.theAntiChain.nextSetBit(i+1)) {
//			//TODO:?
//		}
//	}
	
	/********************************************************
	 * Utilities											*
	 ********************************************************/
	
	/**
	 * Create an empty antichain
	 */
	public SmallAntiChain() {
		theAntiChain = new BitSet(1);
	}
	
	private void setUniverse(SmallBasicSet s) {
		universe = s;
	}
	
	protected SmallBasicSet getUniverse() {
		return universe;
	}
	
	/**
	 * Add a set to this antichain
	 * 
	 * @param 	s
	 * 			a set that's in the universe of this antichain
	 * @return	true if set was added, false when s was no subset of this.getUniverse()
	 */
	public boolean add(SmallBasicSet s) {
		if(universe.hasAsSubset(s)) {
			theAntiChain.set(s.toIntRepresentation());
			return true;
		}
		return false;
	}
	
	/**
	 * Add a set to this antichain, so that the resulting antichain is 
	 * the supremum of the union of this and {x}.
	 * 
	 * @param x
	 */
	private void addConditionally(SmallBasicSet x) {
		SmallBasicSet a;
		for(int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1)) {
			a = new SmallBasicSet(i);
			if(a.hasAsSubset(x)) return;
			if(x.hasAsSubset(a)) theAntiChain.set(i, false);
		}
		theAntiChain.set(x.toIntRepresentation());
	}

	private void addConditionallyAll(SmallAntiChain x) {
		for(int i = x.theAntiChain.nextSetBit(0); i >= 0; i = x.theAntiChain.nextSetBit(i+1)) {
			addConditionally(new SmallBasicSet(i));
		}
	}
	
	//TODO: efficiency?
	public SmallBasicSet sp() {
		SmallBasicSet span = SmallBasicSet.emptySet();
		for(int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1)) {
			span = span.union(new SmallBasicSet(i));
		}
		return span;
	}
	
	//TODO: useful?
/*	public SmallAntiChain sup() {
		SmallAntiChain result = new SmallAntiChain(this);
		for(int i = result.theAntiChain.length(); (i = result.theAntiChain.previousSetBit(i-1)) >= 0; ) {
			if(Integer.bitCount(i) == 1) {
				//for(int j = )
			}
		}
		return result;
	}*/
	
	public boolean ge(SmallBasicSet x) {
		for(int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1)) {
			if(new SmallBasicSet(i).hasAsSubset(x))
				return true;
		}
		return false;
	}
	
	/********************************************************
	 * Implementing methods									*
	 ********************************************************/
	
	@Override
	public LatticeElement join(LatticeElement e) {
		SmallAntiChain result = new SmallAntiChain(this);
		//TODO: optie? result.theAntiChain.or(((SmallAntiChain) e).theAntiChain); ...
		result.addConditionallyAll((SmallAntiChain) e);
		return result;
	}

	@Override
	public LatticeElement meet(LatticeElement e) {
		SmallAntiChain result = SmallAntiChain.emptyAntiChain(this.getUniverse());
		for(int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1))
			for(int j = ((SmallAntiChain) e).theAntiChain.nextSetBit(0); j >= 0; j = ((SmallAntiChain) e).theAntiChain.nextSetBit(j+1)) 
				result.addConditionally(new SmallBasicSet(j).intersection(new SmallBasicSet(i)));
		return result;
	}

	@Override
	public LatticeElement times(LatticeElement e) {
		SmallAntiChain result = new SmallAntiChain(this);
		if(theAntiChain.equals(SmallAntiChain.emptyAntiChain) || 
				((SmallAntiChain) e).theAntiChain.equals(SmallAntiChain.emptyAntiChain))
			return SmallAntiChain.emptyAntiChain();
		if(theAntiChain.equals(SmallAntiChain.emptySetAntiChain))
			return new SmallAntiChain((AntiChain) e);
		if(((SmallAntiChain) e).theAntiChain.equals(SmallAntiChain.emptySetAntiChain))
			return new SmallAntiChain(this);
		
		SmallBasicSet x,y;
		SmallBasicSet spthis = this.sp(), spe = ((SmallAntiChain) e).sp();
		for(int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1))
			for(int j = ((SmallAntiChain) e).theAntiChain.nextSetBit(0); j >= 0; j = ((SmallAntiChain) e).theAntiChain.nextSetBit(j+1)) {
				x = new SmallBasicSet(i);
				y = new SmallBasicSet(j);
				result.addConditionally(x.minus(spe).union(y.minus(spthis)).union(x.intersection(y)));
			}
		return result;
	}

	@Override
	public LatticeElement dual() {
		SmallAntiChain tres, result = SmallAntiChain.emptyAntiChain(this.getUniverse());
		result.add(this.getUniverse());
		SmallBasicSet max, a, k;
		for(int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1)) {
			tres = SmallAntiChain.emptyAntiChain(this.getUniverse());
			k = new SmallBasicSet(i);
			max = this.getUniverse().minus(k);
			for (int j = result.theAntiChain.nextSetBit(0); j >= 0; j = result.theAntiChain.nextSetBit(j+1)) {
				a = new SmallBasicSet(j);
				if (!a.hasAsSubset(max)) tres.addConditionally(a);
				else for (int x:max) tres.addConditionally(a.minus(x));
			}
			result = tres;
		}
		return result;
	}

	@Override
	public boolean ge(LatticeElement e1) {
		for(int i = ((SmallAntiChain) e1).theAntiChain.nextSetBit(0); i >= 0; i = ((SmallAntiChain) e1).theAntiChain.nextSetBit(i+1)) {
			if(!ge(new SmallBasicSet(i)))
				return false;
		}
		return true;
	}

	@Override
	public boolean le(LatticeElement e1) {
		boolean ok;
		for(int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1)) {
			ok = false;
			for(int j = ((SmallAntiChain) e1).theAntiChain.nextSetBit(0); j >= 0; j = ((SmallAntiChain) e1).theAntiChain.nextSetBit(j+1))
				if(new SmallBasicSet(j).hasAsSubset(new SmallBasicSet(i))) {
					ok = true;
					break;
				}
			if(!ok) return false;
		}
		return true;
	}

	@Override
	public boolean equals(LatticeElement e) {
		return theAntiChain.equals(((SmallAntiChain) e).theAntiChain);
	}

	@Override
	public int compareTo(SmallAntiChain o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Iterator<SmallBasicSet> iterator() {
		return new Iterator<SmallBasicSet>() {
			
			private int current = 0;

			@Override
			public boolean hasNext() {
				return theAntiChain.nextSetBit(current + 1) >= 0;
			}

			@Override
			public SmallBasicSet next() {
				current = theAntiChain.nextSetBit(current + 1);
				return new SmallBasicSet(current);
			}

			@Override
			public void remove() {
				// throw new NotImplementedException();
				
			}
			
		};
	}
	
	/**
	 * produce a string for display
	 */
	@Override
	public String toString() {
		if (theAntiChain.isEmpty()) return "{}";
		String res = "{";
		for (int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1)) {
			res += new SmallBasicSet(i) + ",";
		}
		return res.substring(0, res.lastIndexOf(',')) + "}";
	}
	
	//TODO: delete
	public static void main(String[] args) {
		
	}
}