package com.visaeu.rcs.application.tools.TestfileTool;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This class given a input containing just transactions to be sent to BASEII
 * and no File header, batch and file trailer, this class will take the input
 * transactions and generate a new input file containing the required File
 * header , batch trailer and file trailer information so that it can be
 * delivered to baseII
 * 
 * @author ikwuemek
 */
public class BaseIIFileConstructionTool {
	private static Map<String, String> securityCodeMap = new HashMap<String, String>();

	/**
	 * This is the main method to construct file for Base II. Input to this main
	 * method are supplied from the Properties File.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		boolean isInputAscii = true;

		// Read property file
		File properties = new File("fileconstructor.properties");
		
		if (properties.exists()) {
			try {
				System.out.println("Reading parameters from " + properties.getName() + " ...");
				FileInputStream propFile = new FileInputStream(properties);
				Properties p = new Properties(System.getProperties());
				p.load(propFile);
				System.setProperties(p);
				String inPath = System.getProperty("input.path");
				String outPath = System.getProperty("output.path");
				String inFiles = System.getProperty("input.file");
				if ((inPath == null) || inPath.trim().length() < 1) {
					throw new IllegalArgumentException("Input file path for BASEII File Construction cannot be null or empty");
				}

				isInputAscii = new Boolean(System.getProperty("input.ascii"));

				String asciiEncoding = System.getProperty("ascii.charset");
				String ebcdicEncoding = System.getProperty("ebcdic.charset");
				

				if (asciiEncoding == null) {
					asciiEncoding = "ISO8859_1";
					System.out.println("ASCII Encoding value not available in properties. Using the default value:" + asciiEncoding);
				}
				if (ebcdicEncoding == null) {
					ebcdicEncoding = "cp285";
					System.out.println("ASCII Encoding value not available in properties. Using the default value:" + ebcdicEncoding);
				}

				System.out.println("Started File construction for transactions in file " + inFiles);

				BaseIIFileConstructor builder = new BaseIIFileConstructor(asciiEncoding, ebcdicEncoding, isInputAscii);
				// Pick if the path is mentioned
				processFiles(inPath, inFiles,outPath, builder);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Error occured while reading property file: " + properties.getAbsolutePath());
				System.err.println("Exiting...");
				return;
			}
		} else {
			System.err.println("ERROR: Property file:" + properties.getAbsolutePath() + " not found !");
			System.err.println("Exiting...");
			return;
		}
	}

	private static void processFiles(String inPath, String inFiles, String outPath,BaseIIFileConstructor builder) {
		if (inPath != null) {
			File srcTarget = new File(inPath);
			if(outPath!=null){
				File destTarget = new File(outPath);
				System.out.println("outPath1111 : "+outPath);
				/*if(destTarget.isDirectory() && !outPath.endsWith("\\")){
					outPath =outPath+"\\";
				}*/
				System.out.println("outPath2222 : "+outPath);
			}
			if (srcTarget.isDirectory() && (inFiles == null) || (inFiles.trim().length() == 0)) {
				inFiles = "*";
			}

			String fileFilter = inFiles;
			File[] listFiles = srcTarget.listFiles(new FileFilter() {

				@Override
				public boolean accept(File file) {
					boolean b = !file.isDirectory() && (file.length() % 170L == 0L);
					return b;
				}
			});
			String processingBin = System.getProperty("processing.bin");
			String procDate = System.getProperty("processing.date");
			/*boolean takeFileDate = false;
			if (procDate == null || procDate.isEmpty()) {
				takeFileDate = true;
			}*/
			String securityCodeList = System.getProperty("security.code");
			createSecurityMap(securityCodeList);
			String fileType = System.getProperty("file.type");
			String fileMode = System.getProperty("file.mode");
			
			//Sathish Bollam
			int multiplier = Integer.parseInt(System.getProperty("multiplier"));
			//Sathish Bollam
			
			if (listFiles != null) {
				for (File file : listFiles) {

					String fileName = file.getName();
					//processingBin = fileName.substring(2, 8);

					/*if (takeFileDate) {
						procDate = fileName.substring(15, 17) + "/" + fileName.substring(13, 15) + "/" + fileName.substring(9, 13);
					}*/
					// fileName = fileName.substring(0,17);
					System.out.println("Start processing File:: " + fileName);
					long startTime = System.currentTimeMillis();
					try {
						String securityCode = securityCodeMap.get(processingBin);
						if (securityCode == null) {
							throw new Exception("No SecurityCode found for the processor bin: " + processingBin);
						}
						builder.constructFile(file.getAbsolutePath(), processingBin, procDate, securityCode, fileType, fileMode,outPath, multiplier);
					} catch (Exception e) {
						e.printStackTrace();
						//System.out.println("Error processing File:: " + fileName);
					}
					System.out.println("End processing File:: " + fileName);
					long endTime = System.currentTimeMillis();
					System.out.println("Time taken " + (endTime -startTime)/(60*60));
				}
			}

		}
	}

	private static void createSecurityMap(String securityCode) {
		if (securityCode != null && !securityCode.isEmpty()) {
			String[] secutyCodes = securityCode.trim().split(",");
			for (String code : secutyCodes) {
				String[] perCode = code.split("\\|");
				if (perCode[0] != null && perCode[1] != null) {
					securityCodeMap.put(perCode[0], perCode[1]);
				}
			}
		}
	}
}
