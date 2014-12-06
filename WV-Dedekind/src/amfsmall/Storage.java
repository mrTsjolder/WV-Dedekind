package amfsmall;

import java.util.SortedMap;

public class Storage {

	public static <T extends Comparable<T>> void store(SortedMap<T , Long> s, T v) {
		store(s,v,1L);
	}

	public static <T extends Comparable<T>> void store(SortedMap<T , Long> s, T v, Long coeff) {
		if (!s.containsKey(v)) s.put(v, 0L);
		s.put(v, s.get(v) + coeff);
	}

}
