package amfsmall;

import java.util.BitSet;
import java.util.Collection;

/**
 * More efficient representation for AntiChains.
 */
public class SmallAntiChain implements Comparable<SmallAntiChain>, LatticeElement {

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
		SmallAntiChain result = emptyAntiChain;
		result.setUniverse(u);
		return result;
	}
	
	public static void makeAntiChain(SmallAntiChain pseudoAC) {
		for(int i = pseudoAC.theAntiChain.nextSetBit(0); i >= 0; i = pseudoAC.theAntiChain.nextSetBit(i+1)) {
			//TODO?
		}
	}
	
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
	
	//TODO: nodig? ArrayList?
	public int[] getIndices() {
		int[] result = new int[theAntiChain.cardinality()];
		int k = 0;
		for(int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1)) {
			result[k++] = i;
		}
		return result;
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
		for(int i : x.getIndices()) addConditionally(new SmallBasicSet(i));
	}
	
	//TODO: efficiency?
	public SmallBasicSet sp() {
		SmallBasicSet span = SmallBasicSet.emptySet();
		for(int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1)) {
			span.union(new SmallBasicSet(i));
		}
		return span;
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
		SmallAntiChain result = new SmallAntiChain(this);
		for(int i = theAntiChain.nextSetBit(0); i >= 0; i = theAntiChain.nextSetBit(i+1))
			for(int j = ((SmallAntiChain) e).theAntiChain.nextSetBit(0); j >= 0; j = ((SmallAntiChain) e).theAntiChain.nextSetBit(j+1))
				result.addConditionally(new SmallBasicSet(i).intersection(new SmallBasicSet(j)));
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
		SmallBasicSet spthis = this.sp(), spe = ((AntiChain) e).sp();
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean ge(LatticeElement e1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean le(LatticeElement e1) {
		// TODO Auto-generated method stub
		return false;
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
	
	//TODO: delete
	public static void main(String[] args) {
		
	}
}