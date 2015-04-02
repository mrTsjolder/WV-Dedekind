package amfsmall;

import java.util.Map;

public class Storage {

	public static synchronized <T extends Comparable<T>> void store(Map<T , Long> s, T v) {
		store(s,v,1L);
	}

	public static synchronized <T extends Comparable<T>> void store(Map<T , Long> s, T v, Long coeff) {
		if (!s.containsKey(v)) s.put(v, 0L);
		s.put(v, s.get(v) + coeff);
	}

}
