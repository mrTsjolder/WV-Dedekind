package auxiliary;

/**
 * A naive implementation of part of the BigInteger interface
 * Can only be used for BigIntegers behaving as int64's
 * @author patrickdecausmaecker
 *
 */
public class BInteger {

	private BInteger(long v) {
		value = v;
	}
	
	final private long value;
	public long longValue() {
		return value;
	}

	static public BInteger ZERO = new BInteger(0);
	static public BInteger ONE = new BInteger(1);
	
	public BInteger add(BInteger f) {
		return new BInteger(longValue() + f.longValue());
	}

	public BInteger multiply(BInteger f) {
		return new BInteger(longValue()*f.longValue());
	}
	
	static public BInteger valueOf(long v) {
		return new BInteger(v);
	}
	
	public String toString() {
		return "" + longValue();
	}
}
