package net.melissam.powerlog.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.sql.Clob;
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

public class DbUtils {
	
	/** String to acquire db connection. */
	private static final String OPEN_CONNECTION = "jdbc:derby://localhost:1527/powerlog;create=true;user=melissa;password=assilem";
	
	/** Connection string that deletes the database. */
	private static final String DELETE_DATABASE = "jdbc:derby://localhost:1527/powerlog;drop=true;user=melissa;password=assilem";
	
	/** Connection string the shuts down the database. */
	// private final static String SHUTDOWN_DATABASE = "jdbc:derby://localhost:1527/powerlog;shutdown=true";
	
	/** Derby Embedded driver class name. */
	private static final String DRIVER_NAME = "org.apache.derby.jdbc.ClientDriver";
	
	/** Class logger. */
	private static final Logger LOG = LogManager.getLogger(DbUtils.class);
	
	public static void build(String script) throws PowerLogException{

		// let's check if the database already exists
		// this will create an empty database
		// Class.forName(DRIVER_NAME);    		
		// DriverManager.getConnection(DELETE_DATABASE).close();
		// LOG.info("Database dropped.");

		Connection conn = null;
		OutputStream os = null;
		try{
			
			// connect and recreate
			conn = DriverManager.getConnection(OPEN_CONNECTION);
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
			deleteDatabase();

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
	
	
	/**
	 * Deletes the database files.
	 */
	public static void deleteDatabase(){
				
		try{
   		
    		DriverManager.getConnection(DELETE_DATABASE).close();    		
    		
    	}catch(SQLException ex){
    		LOG.warn("Unable to clean up database files.", ex);
    	}
		
	}
	
	public static void saveNewFeature(FeatureVector vector, int cluster) throws SQLException{
		
		Connection conn = DriverManager.getConnection(OPEN_CONNECTION);
		PreparedStatement stmt = conn.prepareStatement("INSERT INTO Feature (id, feature, cluster) VALUES (?, ?, ?)");
		stmt.setInt(1, vector.getId());
		stmt.setString(2, new Gson().toJson(vector.getPoint()));
		stmt.setInt(3, cluster);
		
		stmt.executeUpdate();
	}
	
	public static void saveClusters(int featureCount, int lastFeatureId, List<MicroCluster> clusters) throws SQLException{
		
		Connection conn = DriverManager.getConnection(OPEN_CONNECTION);
		PreparedStatement stmt = conn.prepareStatement("INSERT INTO State (timestamp, featureCount, lastFeature, clusters) VALUES (?, ?, ?, ?)");
		stmt.setLong(1, System.currentTimeMillis());
		stmt.setInt(2, featureCount);
		stmt.setInt(3, lastFeatureId);
		stmt.setClob(4, new StringReader(new Gson().toJson(clusters)));
		
		stmt.executeUpdate();
	}
}
