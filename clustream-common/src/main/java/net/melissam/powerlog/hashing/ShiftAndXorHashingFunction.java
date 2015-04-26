package net.melissam.powerlog.hashing;


/**
 * Implements Shift-And-Xor string hashing.
 * 
 * @author melissam
 *
 */
public class ShiftAndXorHashingFunction implements StringHashingFunction{

	private final int LEFT_SHIFT = 5;
	private final int RIGHT_SHIFT = 2;
	
	/**
	 * @see StringHashingFunction#hash(String)
	 */
	public int hash(String s) {

		int hash = 0;

		for (int i = 0; i < s.length(); i++){
			hash ^= (hash << LEFT_SHIFT) + (hash >> RIGHT_SHIFT) + s.charAt(i);
		}

		return hash;

	}

}