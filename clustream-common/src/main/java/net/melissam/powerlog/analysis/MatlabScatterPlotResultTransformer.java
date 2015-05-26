package net.melissam.powerlog.analysis;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MatlabScatterPlotResultTransformer {
	
	private enum Colour{
		
		RED,
		BLUE,
		YELLOW,
		GREEN,
		MAGENTA;
		
	}
	
	public static void transform(String source, String destination) throws IOException{
		
		/** Lines with centroid values of macro clusters. */
		List<String> macroCentroids = new ArrayList<String>();
		
		/** Radii of macro clusters. */
		List<String> macroRadii = new ArrayList<String>();
		
		/** Lines with centroid values of micro clusters. */
		List<String> microCentroids = new ArrayList<String>();
		
		/** Array of colours for each centroid. */
		List<String> colours = new ArrayList<String>();
		Colour colour = null;
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(source)));
		
		String line = null;
		int linesRead = 0;
		while( (line = reader.readLine()) != null ){
			
			++linesRead;
			
			// new macrocluster!
			if (line.contains("MacroCluster=")){
				
				// add centroid: locate center array, remove all commas
				macroCentroids.add(line.substring(line.indexOf("center=[") + 8, line.indexOf("], radius=")).replace(",", ""));
				
				// add radius
				macroRadii.add(line.substring(line.indexOf("radius=") + 7, line.lastIndexOf(",")));
				
				// we need to set a new colour
				if (colour == null) colour = Colour.values()[0];
				else colour = Colour.values()[colour.ordinal() + 1]; // next colour 
				continue;
			}
			
			// new microcluster!
			if (line.contains("MicroCluster=")){
				// add colour to use when plotting this microcluster
				colours.add(colour.name().toLowerCase());
				
				// add centroid: locate center array, remove all commas
				microCentroids.add(line.substring(line.indexOf("center=[") + 8, line.indexOf("], radius=")).replace(",", ""));
			}
			
		}
		
		// ready reading
		reader.close();
		
		// start writing
		StringBuilder sb = new StringBuilder("macroClusters=[");
		for(int i=0; i<macroCentroids.size(); i++){
			sb.append(macroCentroids.get(i));
			if (i + 1 < macroCentroids.size()) sb.append(";");
			sb.append("\n");
		}
		sb.append("];");
		sb.append("\n");
		
		sb.append("radii=[");
		for(int i=0; i<macroRadii.size(); i++){
			sb.append(macroRadii.get(i));
			if (i + 1 < macroRadii.size()) sb.append(" ");
		}
		sb.append("];");
		sb.append("\n");
				
		sb.append("microClusters=[");
		for (int i = 0; i < microCentroids.size(); i++){
			sb.append(microCentroids.get(i));
			if (i + 1 < microCentroids.size()) sb.append(";");
			sb.append("\n");
		}		
		sb.append("];");
		sb.append("\n");
		
		sb.append("colours=[");
		
		for (int i = 0; i < colours.size(); i++){
			sb.append("'").append(colours.get(i).charAt(0)).append("'");
			if (i + 1 < colours.size()) sb.append(";");
		}
		
		sb.append("];");
		sb.append("\n");
		sb.append("microclusters=").append(microCentroids.size()).append(",colours=").append(colours.size());
		
		System.out.println(sb.toString());
		
	}

	
	public static void main(String args[]) throws Exception{
		
		MatlabScatterPlotResultTransformer.transform("results/201505241200_Global.txt", null);
		
	}
}
