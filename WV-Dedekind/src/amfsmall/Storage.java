package amfsmall;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class Storage {

	public static <T extends Comparable<T>> void store(SortedMap<T , Long> s, T v) {
		store(s,v,1L);
	}

	public static <T extends Comparable<T>> void store(SortedMap<T , Long> s, T v, Long coeff) {
		if (!s.containsKey(v)) s.put(v, 0L);
		s.put(v, s.get(v) + coeff);
	}

	public static <T extends Comparable<T>> String toString(SortedMap<T, Long> list, Formatter<T> f) {
		int count = 0;
		String res = "";
		for (T v : list.keySet()) {
			if (count++ % f.getNumberOfItemsPerLine() == 0) res += String.format("\n%10d:", count);
			res += String.format(f.getFormatString(), "+" + list.get(v) + "*" + f.toString(v));
		}
		res += String.format("\n%10d:", count) + " ____________";
		return res;
	}

	public static <T extends Comparable<T>> void store(SortedMap<T, Long> list,
			SortedMap<T, Long> smallList) {
		for (T t : smallList.keySet()) store(list,t,smallList.get(t));
	}

	public static <T extends Comparable<T>> void store(SortedMap<T, Long> list,
			SortedMap<T, Long> smallList, Long coeff) {
		for (T t : smallList.keySet()) store(list,t,smallList.get(t)*coeff);
	}

	public static <T extends Comparable<T>> void store(
			SortedMap<T, BigInteger> list,
			T code, BigInteger size) {
		if (!list.containsKey(code)) list.put(code, BigInteger.ZERO);
		list.put(code, list.get(code).add(size));
	}

	public static  <T extends Comparable<T>> void store(SortedMap<T, SortedSet<T>> rep,
			T fb, T f) {
		if (!rep.containsKey(fb)) rep.put(fb, new TreeSet<T>());
		rep.get(fb).add(f);
	}

	public static  <T extends Comparable<T>, S> 
		void store(SortedMap<T, Set<S>> rep,T fb, S f) {
		if (!rep.containsKey(fb)) rep.put(fb, new HashSet<S>());
		rep.get(fb).add(f);
	}

	public static <T> Object[] toArray(SortedMap<Integer, T> p, T dummy) {
		int f = p.firstKey();
		Object[] res = new Object[p.lastKey()-f+1];
		for (int t = 0;t<res.length;t++) 
			if (p.containsKey(f+t)) res[t] = p.get(f+t);
			else res[t] = dummy;
		return res;
	}


}
