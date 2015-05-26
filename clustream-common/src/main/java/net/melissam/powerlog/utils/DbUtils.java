package net.melissam.powerlog.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import net.melissam.powerlog.clustering.FeatureVector;
import net.melissam.powerlog.clustering.MicroCluster;

import org.apache.derby.tools.ij;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

/**
 * Abstract class for different instances to use, specifying their own database and connection settings.
 * 
 * @author melissam
 *
 */
public class DbUtils {
	
	/** Host running database one. */
	private String host;
	
	/** Port running the database on. */
	private int port;
	
	/** Name of database. */
	private String name;
	
	/** Username to connect to database. */
	private String username;
	
	/** Password to connect to database with. */
	private String password;
	
	/** String to acquire db connection. */
	private static final String CONNECT = "jdbc:derby://%s:%s/%s;create=true;user=%s;password=%s";
	
	/** Connection string that deletes the database. */
	private static final String DELETE = "jdbc:derby://%s:%s/%s;drop=true;user=%s;password=%s";
	
	/** Connection string the shuts down the database. */
	private final static String SHUTDOWN = "jdbc:derby://%s:%s/%s;shutdown=true";
	
	/** Derby Embedded driver class name. */
	private static final String DRIVER_NAME = "org.apache.derby.jdbc.ClientDriver";
	
	/** Class logger. */
	private static final Logger LOG = LogManager.getLogger(DbUtils.class);
	
	
	public DbUtils(String host, int port, String name,String username, String password) throws Exception{
		
		this.host = host;
		this.port = port;
		this.name = name;
		this.username = username;
		this.password = password;
		
		///set up db driver
		Class.forName(DRIVER_NAME).newInstance();
	}
	
	
	/**
	 * Get a connection to the database for querying.
	 * @return	A connection to the database for querying.
	 */
	public Connection getConnection() throws SQLException{	
		return  DriverManager.getConnection(String.format(CONNECT, host, port, name, username, password));		
	}
	
	/**
	 * Drop the database.
	 */
	public void dropDatabase() {
		
		try{
			
    		DriverManager.getConnection(String.format(DELETE, host, port, name, username, password)).close();    		
    		
    	}catch(SQLException ex){
    		LOG.warn("Unable to drop database.", ex);
    	}
		
	}
	
	
	public void shutdownDatabase(){
		
		try{
			
    		DriverManager.getConnection(String.format(SHUTDOWN, host, port, name, username, password)).close();    		
    		
    	}catch(SQLException ex){
    		LOG.warn("Unable to shutdown database.", ex);
    	}
		
	}
	
	
	/**
	 * Create the database by running the specified script.
	 * 
	 * @param script				The script to run.
	 * @throws PowerLogException	Thrown if there was a problem creating the database.
	 */
	public void build(String script) throws PowerLogException{

		// let's check if the database already exists
		// this will create an empty database
		// Class.forName(DRIVER_NAME);    		
		// DriverManager.getConnection(DELETE_DATABASE).close();
		// LOG.info("Database dropped.");

		Connection conn = null;
		OutputStream os = null;
		try{
			
			// connect and recreate
			conn = getConnection();
			LOG.info("Creating database...");

			long start = System.currentTimeMillis();

			try{
				File derbyLog = new File("derby.log");
				os = new FileOutputStream(derbyLog);
			}catch(FileNotFoundException ex){
				LOG.warn("Could not open derby.log. Writing to default output.");
				os = System.out;
			}

			int errors = ij.runScript(conn, DbUtils.class.getResourceAsStream(script),"UTF-8", os, "UTF-8");
			if (errors > 0){								
				throw new PowerLogException("Database creation was unsuccessful.");					
			}

			LOG.info("Database created in {}ms.", (System.currentTimeMillis() - start));

		}catch(SQLException | UnsupportedEncodingException ex){

			// delete db
			dropDatabase();

			throw new PowerLogException("Unable to create database schema.", ex);

		}finally{

			if (conn != null){
				try{
					conn.close();
				}catch(SQLException ex){
					LOG.warn("Unable to close connection.", ex);
				}
			}

			if (os != null && os instanceof FileOutputStream){
				try{
					os.close();
				}catch(IOException ex){
					LOG.warn("Could not close derby output stream. This error can be ignored.");
				}
			}
		}

	}
	
	
	public void saveNewFeature(FeatureVector vector, int cluster) throws SQLException{
		
		Connection conn = getConnection();
		PreparedStatement stmt = conn.prepareStatement("INSERT INTO Feature (id, feature, cluster) VALUES (?, ?, ?)");
		stmt.setInt(1, vector.getId());
		stmt.setString(2, new Gson().toJson(vector.getPoint()));
		stmt.setInt(3, cluster);
		
		stmt.executeUpdate();
	}
	
	public void saveClusters(int featureCount, int lastFeatureId, List<MicroCluster> clusters) throws SQLException{
		
		Connection conn = getConnection();
		PreparedStatement stmt = conn.prepareStatement("INSERT INTO State (timestamp, featureCount, lastFeature, clusters) VALUES (?, ?, ?, ?)");
		stmt.setLong(1, System.currentTimeMillis());
		stmt.setInt(2, featureCount);
		stmt.setInt(3, lastFeatureId);
		stmt.setClob(4, new StringReader(new Gson().toJson(clusters)));
		
		stmt.executeUpdate();
	}
}
