package net.melissam.powerlog.global.db;

import net.melissam.powerlog.utils.DbUtils;

/**
 * Class for executing queries on central instance.
 * 
 * @author melissam
 *
 */
public class GlobalDbUtils  extends DbUtils {
	
	public GlobalDbUtils(String host, int port, String dbName, String username, String password) throws Exception {
		super(host, port, dbName, username, password);
	}


}
