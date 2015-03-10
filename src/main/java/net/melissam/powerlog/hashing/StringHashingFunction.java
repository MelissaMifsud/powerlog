package net.melissam.powerlog.hashing;

/**
 * Hashing function implementation. 
 * 
 * @author melissam
 *
 */
public interface StringHashingFunction {

	/**
	 * Returns the hash value of a string.
	 * 
	 * @param s The string to hash.
	 * @return The hash value of the string.
	 */
	int hash(String s);
	
}
