package amfsmall;

import java.util.SortedMap;

public class Storage {

	/**
	 * Add one to the long is stored in a given map for a given key in a thread-safe way.
	 * 
	 * @param map The map to work on
	 * @param key The key for the value to add one to
	 */
	public static synchronized <T extends Comparable<T>> void store(SortedMap<T , Long> map, T key) {
		store(map,key,1L);
	}

	/**
	 * Add a certain number to the long is stored in a given map for a given key in a thread-safe way.
	 * 
	 * @param map The map to work on
	 * @param key The key for the value to add to
	 * @param coeff The number to add to the value
	 */
	public static synchronized <T extends Comparable<T>> void store(SortedMap<T , Long> map, T key, Long coeff) {
		if (!map.containsKey(key)) map.put(key, 0L);
		map.put(key, map.get(key) + coeff);
	}

}
