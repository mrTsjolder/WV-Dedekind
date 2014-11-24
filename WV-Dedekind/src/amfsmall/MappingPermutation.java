package amfsmall;
import java.util.Arrays;
import java.util.Iterator;

import auxiliary.Pair;

// import com.sun.tools.javac.util.Pair;

/**
 * permutes the two tables which should be considered as a mapping and its inverse
 * does not copy the tables, so these will be permuted in the space of the caller
 * @author patrickdecausmaecker
 *
 */
public class MappingPermutation implements Iterable<Pair<int[],int[]>> {

	static 	public Iterator<Pair<int[],int[]>> getIterator(int[] map, int[] inverseMap,
			int length) {
		return new MappingPermutation(Arrays.copyOf(map, map.length), Arrays.copyOf(inverseMap, inverseMap.length),map.length).iterator();
	}

	final private int[] theTable;
	final private int[] theInverse;
	final private int theLength;

	protected MappingPermutation(int[] table, int[] inverse, int length) {
		theTable = table;
		theInverse = inverse;
		theLength = length;
	}
	
	@Override
	public Iterator<Pair<int[],int[]>> iterator() {
		if (theLength == 0) { // iteration over an empty set
			return new Iterator<Pair<int[],int[]>>() {		
				private boolean asked = false;
				@Override
				public boolean hasNext() {return !asked ;}
				@Override
				public Pair<int[], int[]> next() {
					asked = true;
					return new Pair<int[], int[]>(theTable,theInverse);
				}
				@Override
				public void remove() {
					throw new UnsupportedOperationException("Permutations cannot be removed");
				}
			};
		}
		else {
			return new Iterator<Pair<int[],int[]>>() {				
				private Iterator<Pair<int[], int[]>> permutation;
				private int nextInsertionPoint;
				private boolean finished;
				{
					finished = false;
					nextInsertionPoint = 0;
					permutation = new MappingPermutation(theTable,theInverse,theLength-1).iterator();
				}
				@Override
				public boolean hasNext() {
					boolean ret = permutation.hasNext() || nextInsertionPoint != theLength-1;
					if (!ret && !finished && nextInsertionPoint > 0) {
						swap(nextInsertionPoint,nextInsertionPoint-1);
						finished = true;
					}
					return ret;
				}
				@Override
				public Pair<int[], int[]> next() {
					if (permutation.hasNext()) return permutation.next();
					if (nextInsertionPoint > 0) swap(theLength-1,nextInsertionPoint-1);
					swap(theLength-1,nextInsertionPoint);
					nextInsertionPoint++;
					permutation = new MappingPermutation(theTable,theInverse,theLength-1).iterator();
					return permutation.next();
				}
				private void swap(int i, int j) {
					int hi = theTable[i];
					int hj = theTable[j];
					theTable[i] = hj;
					theTable[j] = hi;
					theInverse[hi] = j;
					theInverse[hj] = i;
				}
				@Override
				public void remove() {
					throw new UnsupportedOperationException("Permutations cannot be removed");
				}
			};
		}
	}

}
