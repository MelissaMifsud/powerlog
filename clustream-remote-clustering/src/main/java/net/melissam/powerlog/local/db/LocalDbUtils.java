package net.melissam.powerlog.local.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import net.melissam.powerlog.clustering.FeatureVector;
import net.melissam.powerlog.clustering.MicroCluster;
import net.melissam.powerlog.utils.DbUtils;

/**
 * Handling of database at local sites.
 * 
 * @author melissam
 *
 */
public class LocalDbUtils  extends DbUtils {
	
	private Gson gsonWriter;
	
	private static final Logger LOG = LogManager.getLogger(LocalDbUtils.class);
	
	public LocalDbUtils(String host, int port, String dbName, String username, String password) throws Exception {
		super(host, port, dbName, username, password);
		this.gsonWriter = new Gson();
	}

	/**
	 * Saves the feature vector information, keeping track of which cluster the feature was assigned to.
	 *
	 * @param featureVector The feature vector to persist.
	 */
	public void saveFeature(FeatureVector featureVector){
		
		Connection conn = null;
		PreparedStatement stmt = null;
		
		try{
			
			stmt = conn.prepareStatement("INSERT INTO Feature (id, time, feature, label, clusterId) "
											+ "VALUES (?, ?, ?, ?, ?)");
			
			stmt.setInt(1, featureVector.getId());
			stmt.setLong(2, featureVector.getTimestamp());
			stmt.setString(3, gsonWriter.toJson(featureVector.getPoint()));
			stmt.setString(4, featureVector.getGroundTruthLable());
			stmt.setInt(5, featureVector.getInitialCluster());
			
			stmt.executeUpdate();
			
		}catch(SQLException ex){
			
			LOG.error("Failed to store feature: feature={}", featureVector.getId(), ex);
			
		}finally{
			
			if (conn != null) try{ conn.close(); } catch(SQLException ex) { /* close quietly. */}
			
		}

		
	}
	
	
	/**
	 * Saves the snapshot taken at a particular time.
	 * 
	 * @param microClusters The micoclusters at that point in time.
	 * @param timestamp		The timestamp at which the snapshot was taken.
	 */
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
