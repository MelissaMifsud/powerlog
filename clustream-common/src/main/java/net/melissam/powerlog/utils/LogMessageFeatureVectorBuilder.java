package net.melissam.powerlog.utils;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.melissam.powerlog.clustering.FeatureVector;
import net.melissam.powerlog.hashing.StringHashingFunction;

public class LogMessageFeatureVectorBuilder{

	private Pattern logMessageMatcher;	
	private StringHashingFunction hashingFunction;
	
	private int vectorSize;
	
	public LogMessageFeatureVectorBuilder(String logMessageRegex, StringHashingFunction hashingFunction, int vectorSize){
	
		this.logMessageMatcher = Pattern.compile(logMessageRegex);
		this.hashingFunction = hashingFunction;
		this.vectorSize = vectorSize;
		
	}
	
	public FeatureVector getFeatureVector(Date timestamp, String logMessage) throws InvalidLogMessage{
		
		FeatureVector vector = null;
		
		// check that the logMessage matches the regex
		Matcher matcher = logMessageMatcher.matcher(logMessage);
		if (matcher.matches()){
			
			if (matcher.groupCount() == vectorSize){
				
				vector = new FeatureVector();
				vector.setTimestamp(timestamp.getTime());
				for (int i = 1; i <= vectorSize; i++){
					vector.add((double)hashingFunction.hash(matcher.group(i)));
				}
				
			}else{
				throw new InvalidLogMessage("");
			}
			
		}else{
			throw new InvalidLogMessage(String.format("logMessage=%s does not match the regex=%s.", logMessage, logMessageMatcher.pattern()));
		}
		
		return vector;
		
	}
}
