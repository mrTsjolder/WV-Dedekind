package amfsmall;
/**
 * @invar for all i:getRange() map(imap(i)) == i
 * @invar for all i:getRange() imap(map(i)) == i
 * @author patrickdecausmaecker
 *
 */
public interface Mapping {
	/**
	 * Compute the image of an integer
	 * bijective
	 * @pre getRange().contains(i)
	 * @param i
	 * @return getRange().contains(return)
	 */
	public int map(int i);
	/**
	 * Compute the inverse image of an integer
	 * Should be bijective
	 * @pre getRange().contains(i)
	 * @param i
	 * @return getRange().contains(return)
	 */
	public int iMap(int i);
	/**
	 * @return The range of this mapping
	 */
	public SmallBasicSet getRange();
}
