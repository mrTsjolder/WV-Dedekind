package posets;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
/**
 * In a leveled posets, all elements have a unique level
 * The lowest level is level 1
 * Elements at level 1 have no predecessors
 * An element of level n>1 is an immediate successor of at least one element of level n-1
 * An element of level n>1 is either an immediate successor of any element of level n-1 or it is not comparable to this element
 * The difference between levels of comparable elements is called the distance between the elements 
 * @author patrickdecausmaecker
 *
 * @param <T>
 */
public interface LeveledPoset<T extends Comparable<T>> {
	/**
	 * get successors and predecessors of a value
	 * @param v
	 * @return
	 */
	public SortedSet<T> getSuccessors(T v);
	public SortedSet<T> getPredecessors(T v);

	/**
	 * get the set of elements before(after) element v
	 * @param v
	 * @return {x | x <(>) v}
	 */
	public SortedSet<T> getBefore(T v);
	public SortedSet<T> getAfter(T v);
	
	/**
	 * the set of elements in the poset
	 * @return
	 */
	public SortedSet<T> getPosetElements();
	
	/**
	 * the number of elements in the poset
	 * @effect getPosetElements().size()
	 * 
	 */
	public long getSize();
	
	/**
	 * the size of the level with most elements 
	 * @return max({getLevel(i).size()|1<=i<=getMaxLevel()}
	 */
	public long getWidth();
	/**
	 * Computes the components in this poset.
	 * 
	 * @return a list of sets of posetelements with no predecessors. Each set is the set
	 * of minimal elements in a component
	 */
	public List<Set<T>> getComponents();

	/**
	 * get the set of posetelements with distance n-1 to an element of level 1
	 * the lowest level is level 1
	 * elements at level n are either not comparable to elements of level 1 or they are at distance n-1
	 * @param n
	 * @return
	 */
	public SortedSet<T> getLevel(int n);
	/**
	 * get the number of the level with getWidth() elements.
	 * @return
	 */
	public int getMaximalLevelNumber();

	/**
	 * get the the level with getWidth() elements.
	 * @return
	 */
	public Set<T> getMaximalLevel();
	
	/**
	 * Compute the size of the spanned lattice
	 * @return return == the size of the lattice spanned by this poset
	 */
	public long getLatticeSize();
	public long getLatticeSize(long exp,SortedSet<T> alfa,int l);
	long getLatticeSize(boolean odd);
	
	/**
	 * build the sub-poset with the elements in bottom as smallest elements
	 * 
	 * @param bottom
	 * @return the poset with the elements in bottom as its smallest elements
	 */
	public SimplePosetSize<T> getPosetFrom(SortedSet<T> bottom);

	/**
	 * Compute the level of element v in the poset
	 * @pre v is a poset element
	 * 
	 * @param v
	 * @return the level l such that getLevel(l) contains v
	 */
	public int getLevel(T v);
	public int getMaxLevel();
	
	/**
	 * iterator over the subsets in a SortedSet
	 */
	public Iterator<SortedSet<T>> subSetIterator(SortedSet<T> s);

}
