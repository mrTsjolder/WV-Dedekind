package posets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public abstract class SimplePosetSize<T extends Comparable<T>> implements LeveledPoset<T> {

	@Override
	public long getSize() {
		return getPosetElements().size();
	}

	@Override
	public long getWidth() {
		long max = 0;
		for (int i=1;i<=getMaxLevel();i++) {
			max = Math.max(getLevel(i).size(), max);
		}
		return max;
	}

	@Override
	public int getMaximalLevelNumber() {
		long max = getWidth();
		for (int i=1;i<=getMaxLevel();i++) {
			if (getLevel(i).size() == max)
				return i;
		}
		return 1;
	}
	
	@Override
	public Set<T> getMaximalLevel() {
		return getLevel(getMaximalLevelNumber());
	}
	

	/**
	 * the number of elements in the lattice for which 
	 */
	@Override
	public long getLatticeSize(long exp,SortedSet<T> alfa,int l) {
		long res = 0L;

		SortedSet<T> alfap = getPlus(alfa,l);
		SortedSet<T> alfam = getMinus(alfa,l);
		SortedSet<T> alfapp = getPlus(alfap,l+1);
		
		if (alfapp.isEmpty()) return pow(exp - alfam.size() + alfap.size());
		
		SortedSet<T> tau = new TreeSet<T>();
		res += doWhatIsIn(alfap.size(),tau,alfapp,l+2);
		return pow(exp-alfam.size())*res;
	}

	protected long pow(long exp) {
		return 1L << exp;
	}

	private SortedSet<T> getMinus(SortedSet<T> alfa, int l) {
		SortedSet<T> res = new TreeSet<T>();
		for (T v : alfa) res.addAll(this.getPredecessors(v));
		return res;
	}

	private SortedSet<T> getPlus(SortedSet<T> alfa, int l) {
		SortedSet<T> res = new TreeSet<T>();
		for (T v : this.getLevel(l+1)) {
			if (alfa.containsAll(this.getPredecessors(v))) res.add(v);
		}
		return res;
	}

	/**
	 * For all extensions e of tau with elements from the set alfapp, compute the size of the lattice above e multiplied by 
	 * 2^(exp-exp.getMinus().size())
	 * exp is the number of elements available just below alfapp
	 * @param exp
	 * @param tau
	 * @param alfapp
	 * @param l
	 * @return the latice size multiplied by an appropriate factor
	 */
	private long doWhatIsIn(long exp,SortedSet<T> tau, SortedSet<T> alfapp, int l) {
		long res = 0L;

		if (alfapp.isEmpty()) return getLatticeSize(exp,tau,l);
		
		T first = alfapp.first();
		T second = null;
		int cnt = 2;
		for (T v : alfapp) {
			if (--cnt == 0) {second = v;break;}
		}
		if (cnt == 0) {
			tau.add(first);
			res +=  doWhatIsIn(exp,tau,alfapp.tailSet(second),l);
			tau.remove(first);
			res +=  doWhatIsIn(exp,tau,alfapp.tailSet(second),l);
		}
		else {
			tau.add(first);
			res += getLatticeSize(exp,tau,l);
			tau.remove(first);
			res += getLatticeSize(exp,tau,l);
		}
		return res;
	}

	@Override
	public Iterator<SortedSet<T>> subSetIterator(final SortedSet<T> s) {
			if (s.size() == 0) return  emptySetIterator();
			else if (s.size() == 1) return  singletonIterator(s.first());
			else {
				return new Iterator<SortedSet<T>>() {

					Iterator<SortedSet<T>> current;
					Iterator<SortedSet<T>> rest;
					SortedSet<T> head, tail;
					boolean hasNext;
					T second;
					{
						hasNext = true;
						current = singletonIterator(s.first());
						int cnt = 2;
						second = null;
						for (T v : s) if (--cnt == 0) {second = v;break;}
						if (second == null) rest = emptySetIterator();
						else rest = subSetIterator(s.tailSet(second));
						head = current.next();
						tail = rest.next();
					}
					@Override
					public boolean hasNext() {
						return hasNext;
					}

					@Override
					public SortedSet<T> next() {
						SortedSet<T> res = new TreeSet<T>(head);
						res.addAll(tail);
						if (rest.hasNext()) tail = rest.next();
						else {
							if (current.hasNext()) {
								head = current.next();
								if (second == null) rest = emptySetIterator();
								else rest = subSetIterator(s.tailSet(second));
								tail = rest.next();
							} else hasNext = false;
						}
						return res;
					}

					@Override
					public void remove() {
						// TODO Auto-generated method stub
						
					}
					
				};
			}
	}

	/**
	 * Computes the components in this poset.
	 * 
	 * @return a list of sets of posetelements with no predecessors. Each set is the set
	 * of minimal elements in a component
	 */
	public List<Set<T>> getComponents() {
		Map<T, Set<T>> isConnectedWith = new HashMap<T, Set<T>>();
		Map<T, Set<T>> totalFuture = new HashMap<T, Set<T>>();
		// look up the elements without a predecessor
		for (T v : this.getPosetElements()) {
			if (this.getPredecessors(v).isEmpty()) {
				isConnectedWith.put(v, new HashSet<T>());
				isConnectedWith.get(v).add(v);
				totalFuture.put(v,new TreeSet<T>(this.getAfter(v)));
//				totalFuture.get(v).add(v);
			}
		}
		// find connections
		Set<T> bad = new HashSet<T>();
		do {
			bad.clear();
			for (T v : isConnectedWith.keySet())
				for (T w : isConnectedWith.keySet()) {
					if (w.compareTo(v) > 0) {
						if (intersecting(totalFuture.get(v),totalFuture.get(w))) {
							totalFuture.get(v).addAll(totalFuture.get(w));
							totalFuture.get(w).clear();
							isConnectedWith.get(v).addAll(isConnectedWith.get(w));
							isConnectedWith.get(w).clear();
							bad.add(w);
						}
					}
				}
			for (T b : bad) {
				isConnectedWith.remove(b);
				totalFuture.remove(b);
			}
			
		} while (!bad.isEmpty());
		List<Set<T>> res = new ArrayList<Set<T>>();
		for(T v : isConnectedWith.keySet()) res.add(isConnectedWith.get(v));
		return res;
	}
	
	private boolean intersecting(Set<T> set, Set<T> set2) {
		if (set.size() > set2.size()){
			Set<T> h = set;set = set2;set2 = h;
		}
		for (T v : set) if (set2.contains(v)) return true;
		return false;
	}
	
	private Iterator<SortedSet<T>> singletonIterator(final T first) {
		return new Iterator<SortedSet<T>>() {

			int cnt = 0;
			T e = first;
			@Override
			public boolean hasNext() {
				return cnt < 2;
			}

			@Override
			public SortedSet<T> next() {
				if (++cnt == 1) return new TreeSet<T>();
				else {
					SortedSet<T> ret = new TreeSet<T>();
					ret.add(e);
					return ret;
				}
			}

			@Override
			public void remove() {
				// TODO Auto-generated method stub
				
			}
			
		};
	}

	private Iterator<SortedSet<T>> emptySetIterator() {
		return new Iterator<SortedSet<T>>() {

			boolean hasNext = true;
			@Override
			public boolean hasNext() {
				return hasNext;
			}

			@Override
			public SortedSet<T> next() {
				hasNext = false;
				return new TreeSet<T>();
			}

			@Override
			public void remove() {
				// TODO Auto-generated method stub
				
			}
		};
	}

	/**
	 */
	@Override
	public int getLevel(T v) {
		for (int i=1;i< getMaxLevel();i++) 
			if (getLevel(i).contains(v)) return i;
		return 0;
	}
}
