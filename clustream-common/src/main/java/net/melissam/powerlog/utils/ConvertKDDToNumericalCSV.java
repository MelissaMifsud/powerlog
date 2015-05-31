package net.melissam.powerlog.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.CopyOnWriteArrayList;

import net.melissam.powerlog.clustering.FeatureVector;

import org.apache.derby.impl.store.raw.log.CheckpointOperation;


/**
 * Creates a normalised version of the KDD-Cup set.
 * This was used to attempt to perform kmeans clustering in matlab.
 * 
 * @author melissam
 *
 */
public class ConvertKDDToNumericalCSV {
	
	double[] mean;
	double[] sd;
	
	public void setup() throws IOException {
		
		long start = System.currentTimeMillis();
		
		double[] sum = null;
		int size = 0;
		
		// read the file
		File fin = new File("kdd-data.csv");
		FileInputStream fis = new FileInputStream(fin);
		BufferedReader reader = new BufferedReader(new FileReader(fin));
	
		String line = null;
		while((line = reader.readLine()) != null){
			
			++size;
			
			String[] args = line.split(",");
			
			// initialise the sum of all attribute values 
			if (sum == null) {
				sum = new double[args.length];
				for (int i = 0; i < sum.length; i++){
					sum[i] = 0;
				}
			}
			
			for (int i = 0; i < args.length; i++){
				sum[i] += Double.parseDouble(args[i]);
			}
		}
		
		
		// calculate the mean from the sum
		this.mean = new double[sum.length];
		this.sd = new double[sum.length];
		for (int i = 0; i < sum.length; i++){
			this.mean[i] = sum[i] / size;
			this.sd[i] = (sum[i] - mean[i]) / size;
		}
		
		fis.close();
		reader.close();
		
	}
	
	private void normaliseData() throws IOException{
		
		// read the file
		File fin = new File("kdd-data.csv");
		FileInputStream fis = new FileInputStream(fin);
		BufferedReader reader = new BufferedReader(new FileReader(fin));
		
		File fout = new File("kdd-data-normalised.csv");
		FileOutputStream fos = new FileOutputStream(fout);
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
		
		String line = null;
		while((line = reader.readLine()) != null){

			String[] parts = line.split(",");

			if (parts.length == 34){

				StringBuilder sb = new StringBuilder();

				for (int i = 0; i < parts.length; i++){
					
					if (sd[i] != 0) sb.append((Double.parseDouble(parts[i]) - mean[i]) / sd[i]);
					else sb.append(Double.parseDouble(parts[i]) - mean[i]);
					
					if (i < 33) sb.append(",");
				}
				
				writer.write(sb.toString());
				writer.newLine();

			}else{

				System.out.println("Found " + parts.length + " fields. Expected " + 34);

			}


		}

		reader.close();
		writer.close();
		
		
	}
	
	
	private void convert() throws IOException{

		// read the file
		File fin = new File("C:/Users/missa/Downloads/kddcup.data_10_percent_corrected");
		FileInputStream fis = new FileInputStream(fin);
		BufferedReader reader = new BufferedReader(new FileReader(fin));

		File fout = new File("kdd-data-withlabels.csv");
		FileOutputStream fos = new FileOutputStream(fout);
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
		
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<34; i++){
			sb.append(i);
			if (i<33) sb.append(",");
		}
		
		writer.write(sb.toString());
		writer.newLine();


		String line = null;
		while((line = reader.readLine()) != null){

			String[] parts = line.split(",");

			if (parts.length == 42){

				sb = new StringBuilder();

				sb.append(parts[0]).append(",");  
				sb.append(parts[4]).append(",");
				sb.append(parts[5]).append(",");
				sb.append(parts[7]).append(",");
				sb.append(parts[8]).append(",");
				sb.append(parts[9]).append(",");
				sb.append(parts[10]).append(",");
				sb.append(parts[12]).append(",");
				sb.append(parts[13]).append(",");
				sb.append(parts[14]).append(",");
				sb.append(parts[15]).append(",");
				sb.append(parts[16]).append(",");
				sb.append(parts[17]).append(",");
				sb.append(parts[18]).append(",");
				sb.append(parts[19]).append(",");

				for (int i = 22; i < 41; i++){
					sb.append(parts[i]);
					if (i < 40) sb.append(",");
				}

				checkNumeric(sb.toString().split(","));

				writer.write(sb.toString());
				writer.newLine();

			}else{

				System.out.println("Found " + parts.length + " fields. Expected " + 42);

			}


		}

		reader.close();
		writer.close();
	}

	public static void main(String[] args) throws Exception{
		
		ConvertKDDToNumericalCSV runner = new ConvertKDDToNumericalCSV();
		runner.convert();
		//runner.setup();
		//runner.normaliseData();
		
	}

	
	private static void checkNumeric(String[] parts){
		
		int i = 0;
		try{
			
			for (i = 0; i < parts.length; i++){
				Double.parseDouble(parts[i]);
			}
			
		}catch(NumberFormatException ex){
			System.out.println(i + " is not a number " + parts[i]);
		}
		
	}
}
