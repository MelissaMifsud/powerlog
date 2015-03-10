package net.melissam.powerlog.common;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
				
				vector = new FeatureVector(timestamp, vectorSize);
				for (int i = 1; i <= vectorSize; i++){
					vector.add(hashingFunction.hash(matcher.group(i)));
				}
				
			}else{
				
			}
			
		}else{
			throw new InvalidLogMessage(String.format("logMessage=%s does not match the regex=%s.", logMessage, logMessageMatcher.pattern()));
		}
		
		FeatureVector featureVector = new FeatureVector(timestamp);
		
	}
}
