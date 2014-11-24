package posets;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

public class PosetDescriptor {

	final private Set<PosetDescriptor> roots;
	final private Set<PosetDescriptor> tops;
	final private Set<PosetDescriptor> future;
	final private Set<PosetDescriptor> past;
	final private String forwardLabel, backwardLabel;

	public String getForwardLabel() {return forwardLabel;}
	public String getBackwardLabel() {return backwardLabel;}
	public String getfbLabel() {return getForwardLabel() + getBackwardLabel();}

	public Set<PosetDescriptor> getSuccessors() {return roots;}
	public Set<PosetDescriptor> getPredecessors() {return tops;}
	
	public static PosetDescriptor getEmpty() {return new PosetDescriptor();}

	protected PosetDescriptor() {
		roots = new HashSet<PosetDescriptor>();
		tops = new HashSet<PosetDescriptor>();
		forwardLabel = labelForward();
		backwardLabel = labelBackward();
		future = future();
		past = past();
	}
	
	protected PosetDescriptor(Set<PosetDescriptor> r, Set<PosetDescriptor> t) {
		roots = r;
		tops = t;
		forwardLabel = labelForward();
		backwardLabel = labelBackward();
		future = future();
		past = past();
	}
	
	public PosetDescriptor addSuccessor(PosetDescriptor p) {
		if (roots.contains(p)) return this;
		Set<PosetDescriptor> ret = new HashSet<PosetDescriptor>();
		ret.addAll(roots);
		ret.add(p);
		return new PosetDescriptor(ret,tops);
	}
	
	public PosetDescriptor removeSuccessor(PosetDescriptor p) {
		if (!roots.contains(p)) return this;
		Set<PosetDescriptor> ret = new HashSet<PosetDescriptor>();
		ret.addAll(roots);
		ret.remove(p);
		return new PosetDescriptor(ret,tops);
	}
	
	public PosetDescriptor addPredecessor(PosetDescriptor p) {
		if (tops.contains(p)) return this;
		Set<PosetDescriptor> ret = new HashSet<PosetDescriptor>();
		ret.addAll(tops);
		ret.add(p);
		return new PosetDescriptor(roots,ret);
	}
	
	public PosetDescriptor removePredecessor(PosetDescriptor p) {
		if (!tops.contains(p)) return this;
		Set<PosetDescriptor> ret = new HashSet<PosetDescriptor>();
		ret.addAll(tops);
		ret.remove(p);
		return new PosetDescriptor(roots,ret);
	}
	
	/**
	 * prints the future of this 
	 */
	public String toString() {
		HashMap<PosetDescriptor,Integer> visited = new HashMap<PosetDescriptor,Integer>();
		numberNodes(visited,1);
		return toString(visited,new TreeSet<Integer>());
	}
	
	private String toString(HashMap<PosetDescriptor, Integer> nodeNumbers, Set<Integer> described) {
		if (described.contains(nodeNumbers.get(this))) return ""; // do not describe twice
		String res = nodeNumbers.get(this) + "[";
		for (PosetDescriptor p : roots)
			res += nodeNumbers.get(p) + " ";
		res += "]";
		described.add(nodeNumbers.get(this));
		for (PosetDescriptor p : roots)
			res += p.toString(nodeNumbers,described);
		return res;
	}

	private int numberNodes(HashMap<PosetDescriptor, Integer> visited, int nextNumber) {
		if (!visited.containsKey(this)) {
			visited.put(this, nextNumber++);
			for (PosetDescriptor p : roots) nextNumber = p.numberNodes(visited,nextNumber);
		}
		return nextNumber;
	}

	private String labelForward() {
		SortedMap<String,Integer> labels = new TreeMap<String,Integer>();
		for (PosetDescriptor r : roots) 
			store(labels, r.getForwardLabel());
		return labels.toString();
	}
	
	private String labelBackward() {
		SortedMap<String,Integer> labels = new TreeMap<String,Integer>();
		for (PosetDescriptor t : tops) 
			store(labels, t.getBackwardLabel());
		return labels.toString();
	}
	
	private Set<PosetDescriptor> future() {
		Set<PosetDescriptor> f = new HashSet<PosetDescriptor>();
		for (PosetDescriptor p : roots) f.addAll(p.future);
		return f;
	}

	private Set<PosetDescriptor> past() {
		Set<PosetDescriptor> f = new HashSet<PosetDescriptor>();
		for (PosetDescriptor p : tops) f.addAll(p.past);
		return f;
	}
	
	public BigInteger fInteger() {
		BigInteger ret = BigInteger.ZERO;
		SortedMap<String,Integer> labels = new TreeMap<String,Integer>();
		for (PosetDescriptor r : roots) 
			store(labels, r.getForwardLabel());
		for (String l : labels.keySet()) {
			int cnt = labels.get(l);
			if (cnt > 1) {
			ret = push2Bits(ret,0);
			ret = pushNumber(ret,cnt);
			ret = push2Bits(ret,0);
			}
			for (char c : l.toCharArray()) {
				if (c == '{') ret = push2Bits(ret,1);
				else if (c == '}') ret = push2Bits(ret,2);
			}
		}
		return ret;
	}

	public BigInteger bInteger() {
		BigInteger ret = BigInteger.ZERO;
		SortedMap<String,Integer> labels = new TreeMap<String,Integer>();
		for (PosetDescriptor r : tops) 
			store(labels, r.getBackwardLabel());
		for (String l : labels.keySet()) {
			int cnt = labels.get(l);
			if (cnt > 1) {
			ret = push2Bits(ret,0);
			ret = pushNumber(ret,cnt);
			ret = push2Bits(ret,0);
			}
			for (char c : l.toCharArray()) {
				if (c == '{') ret = push2Bits(ret,1);
				else if (c == '}') ret = push2Bits(ret,2);
			}
		}
		return ret;
	}
	
	public BigInteger fbInteger() {
		BigInteger f = fInteger();
		BigInteger b = bInteger();
		return push2Bits(f,3).shiftLeft(b.bitLength()).or(b);
	}
	
	private BigInteger pushNumber(BigInteger ret, int cnt) {
		BigInteger cNt = BigInteger.valueOf(cnt);
		return ret.shiftLeft(cNt.bitLength()).or(cNt);
	}
	
	private BigInteger push2Bits(BigInteger ret, int i) {
		return ret.shiftLeft(2).or(BigInteger.valueOf(i));
	}

	private void store(SortedMap<String, Integer> labels, String label) {
		if (labels.containsKey(label)) labels.put(label, labels.get(label) + 1);
		else labels.put(label, 1);
	}

	
}
