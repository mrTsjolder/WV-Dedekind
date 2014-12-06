package amfsmall;

import java.util.BitSet;
import java.util.Collection;

/**
 * More efficient representation for AntiChains.
 */
public class SmallAntiChain implements Comparable<SmallAntiChain>, LatticeElement {

	private BitSet theAntiChain;
	private SmallBasicSet universe = SmallBasicSet.universe();
	
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
		theAntiChain = ac.theAntiChain;
		this.setUniverse(ac.getUniverse());
	}
	
	/**
	 * Create an antichain on universe
	 */
	public SmallAntiChain(SmallBasicSet u) {
		this.setUniverse(u);
	}
	
	/**
	 * Create an antichain based on a long.
	 */
	public SmallAntiChain(long l) {
		theAntiChain = BitSet.valueOf(new long[]{l});
	}
	
	/**
	 * Create an empty antichain
	 */
	public SmallAntiChain() {
		
	}
	
	private void setUniverse(SmallBasicSet s) {
		this.universe = s;
	}
	
	private SmallBasicSet getUniverse() {
		return this.universe;
	}

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
	
	
}