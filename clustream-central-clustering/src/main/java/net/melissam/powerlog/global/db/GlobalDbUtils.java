package net.melissam.powerlog.global.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import net.melissam.powerlog.clustering.FeatureVector;
import net.melissam.powerlog.clustering.MicroCluster;
import net.melissam.powerlog.utils.DbUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

/**
 * Class for executing queries on central instance.
 * 
 * @author melissam
 *
 */
public class GlobalDbUtils extends DbUtils {
	
	private Gson gsonWriter;
	
	private static final Logger LOG = LogManager.getLogger(GlobalDbUtils.class);
	
	public GlobalDbUtils(String host, int port, String dbName, String username, String password) throws Exception {
		super(host, port, dbName, username, password);
		this.gsonWriter = new Gson();
	}

	
	/**
	 * Saves feature vector information.
	 * 
	 * @param featureVector		The feature vector to save.
	 * @param origin			The remote micro cluster the feature originates from
	 */
	public void saveFeature(FeatureVector featureVector, MicroCluster origin){
		
		Connection conn = null;
		PreparedStatement stmt = null;
		
		try{	
			
			conn = super.getConnection();
			stmt = conn.prepareStatement("INSERT INTO Feature (id, gtime, icluster, idList, sumOfValues, sumSquareOfValues, size ) "
											+ "VALUES (?, ?, ?, ?, ?, ?, ?)");
			
			stmt.setInt(1, featureVector.getId());
			stmt.setLong(2, featureVector.getTimestamp());
			stmt.setLong(3, origin.getIdList().get(0));
			stmt.setString(4, gsonWriter.toJson(origin.getIdList()));
			stmt.setString(5,  gsonWriter.toJson(origin.getSumOfValues()));
			stmt.setString(6, gsonWriter.toJson(origin.getSumOfSquaresOfValues()));
			stmt.setLong(7, (long)origin.getSize());
			
			stmt.executeUpdate();
			
		}catch(SQLException ex){
			
			LOG.error("Failed to store feature={}", featureVector.getId(), ex);
			
		}finally{
			
			if (conn != null) try{ conn.close(); } catch(SQLException ex) { /* close quietly. */}
			
		}
					
	}
	
	
	public void savePlacement(int featureId, int clusterId){
		
		Connection conn = null;
		PreparedStatement stmt = null;
		
		try{	
			
			conn = super.getConnection();
			stmt = conn.prepareStatement("INSERT INTO Placement (featureId, clusterid) "
											+ "VALUES (?, ?)");
			
			stmt.setInt(1, featureId);
			stmt.setLong(2, clusterId);
			
			stmt.executeUpdate();
			
		}catch(SQLException ex){
			
			LOG.error("Failed to store placement: feature={}, cluster={}", featureId, clusterId, ex);
			
		}finally{
			
			if (conn != null) try{ conn.close(); } catch(SQLException ex) { /* close quietly. */}
			
		}
		
	}
	
	
	public void saveSnapshot(List<MicroCluster> microClusters, long timestamp){
		
		if (microClusters != null && !microClusters.isEmpty()){
			
			Connection conn = null;
			Statement stmt = null;
			
			try{
				
				conn = super.getConnection();
				stmt = conn.createStatement();
				
				// build the statement
				StringBuilder sb = new StringBuilder("INSERT INTO Snapshot(time, clusterId, idList, sumOfValues, sumSquareOfValues, size) VALUES");
				
				int mc = 0;
				for (MicroCluster cluster : microClusters){
					
					sb.append("(").append(timestamp).append(",").append(cluster.getIdList().get(0)).append(",")
								.append(gsonWriter.toJson(cluster.getIdList())).append(",").append(gsonWriter.toJson(cluster.getSumOfValues()))
								.append(",").append(gsonWriter.toJson(cluster.getSumOfSquaresOfValues())).append(",").append((long)cluster.getSize())
								.append(")");
					
					if (++mc < microClusters.size()) sb.append(",");
				}
				
				stmt.executeUpdate(sb.toString());
				
			}catch(SQLException ex){
				
				LOG.error("Failed to store snapshot: time={}", timestamp, ex);
				
			}finally{
				
				if (conn != null) try{ conn.close(); } catch(SQLException ex) { /* close quietly. */}
				
			}
			
		}
		
	}
	
}
