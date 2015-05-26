package net.melissam.powerlog.local.db;

import net.melissam.powerlog.utils.DbUtils;

public class LocalDbUtils  extends DbUtils {
	
	public LocalDbUtils(String host, int port, String dbName, String username, String password) throws Exception {
		super(host, port, dbName, username, password);
	}


}
