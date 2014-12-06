package amfsmall;

public interface LatticeElement {
	public LatticeElement join(LatticeElement e);
	public LatticeElement meet(LatticeElement e);
	public LatticeElement times(LatticeElement e);
	public LatticeElement dual();
	public boolean ge(LatticeElement e1);
	public boolean le(LatticeElement e1);
	public boolean equals(LatticeElement e);
}
