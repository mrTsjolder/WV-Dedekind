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
	 * Constructors, adders, getters and setters			*
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
		theAntiChain = ac.theAntiChain;
		this.setUniverse(ac.getUniverse());
	}
	
	/**
	 * Create an antichain based on a long.
	 */
	public SmallAntiChain(long[] l) {
		theAntiChain = BitSet.valueOf(l);
	}
	
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
	
	/********************************************************
	 * Basic antichains										*
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
	
	/********************************************************
	 * Implementing methods									*
	 ********************************************************/
	
	@Override
	public LatticeElement join(LatticeElement e) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LatticeElement meet(LatticeElement e) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LatticeElement times(LatticeElement e) {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int compareTo(SmallAntiChain o) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	//TODO: delete
	public static void main(String[] args) {
		System.out.println(String.format("%3d, %16x", 1L << 4, 0L << 5));
	}
}