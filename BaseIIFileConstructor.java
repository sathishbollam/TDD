package com.visaeu.rcs.application.tools.TestfileTool;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;

import com.visaeu.rcs.application.tools.common.BatchBuilder;
import com.visaeu.rcs.application.tools.common.FileBase;

/**
 * This class given a input containing just transactions to be sent to BASEII
 * and no File header, batch and file trailer, this class will take the input
 * transactions and generate a new input file containing the required File
 * header , batch trailer and file trailer information so that it can be
 * delivered to baseII
 * 
 * @author ikwuemek
 */
public class BaseIIFileConstructor extends FileBase {

	public BaseIIFileConstructor(String asciiCharset, String ebcdicCharset,
			boolean isAscii) {
		super(asciiCharset, ebcdicCharset, isAscii);
	}

	// File Construction method
	public void constructFile(String fileName, String processingBin,
			String procDate, String securityCode, String fileType,
			String fileMode, String outPath, int multiplier) throws Exception {
		BufferedOutputStream fos = null;
		FileInputStream fis = null;
		batchBuilder = new BatchBuilder(isInputAscii(), processingBin,
				procDate, securityCode, fileType, fileMode);
		final String encoding = getEncoding();
		final File inputFile = new File(fileName);
		String path = inputFile.getParent();
		if (outPath != null && !outPath.isEmpty()) {
			path = outPath;
		}
		//System.out.println("outPathAAAA : "+outPath);
		final File outFile = buildDestFile(path, fileType, processingBin,
				procDate, fileMode);
		//System.out.println("outFileBBBB : "+outFile);
		fis = new FileInputStream(fileName);

		try {

			// Validate the input encoding
			validateDataEncoding(inputFile);

			// read the transactions in the input file.
			byte[] txnData = new byte[(int) inputFile.length()];
			fis.read(txnData);
			
			final ByteArrayOutputStream ipbaos = new ByteArrayOutputStream();
			ipbaos.write(txnData);

			fos = new BufferedOutputStream(new FileOutputStream(outFile));
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			//ByteArrayOutputStream baos1 = new ByteArrayOutputStream();

			String fileHeader = batchBuilder.buildFileHeader().toString();

			// write file header
			baos.write(fileHeader.toString().getBytes(encoding));
			baos.write(getZeroPadding(11));

			
			int infilesize = ipbaos.size()/1024;
			System.out.println("infilesize "+infilesize);
			
			int count = 0;
			
			//int multiplier = 10;
			System.out.println("multiplier "+multiplier);
			//byte[] AllbatchTrailers = new byte[];
			for (long i = 1; i <= multiplier; i++) {
				
				// write transactions
				baos.write(txnData);
				//baos1 = new ByteArrayOutputStream();
				//baos1.write(txnData);
				//count = 0;
				//if(i >=204 && i % 204==0){
					//count++;
					System.out.print("..");
					
					
					int txnPadding = BatchBuilder.getTransactionPadding(txnData);
					if (txnPadding > 0) {
						baos.write(getZeroPadding(txnPadding));
					}
					
					batchBuilder = new BatchBuilder(isInputAscii(), processingBin,
							procDate, securityCode, fileType, fileMode);
					byte[] batchTrailer = batchBuilder.reconstructBatchTrailer(txnData,
							encoding, i);
					baos.write(batchTrailer);
					baos.write(getZeroPadding(11));
					
					//System.arraycopy(baos.toByteArray(),0,AllbatchTrailers,AllbatchTrailers.length,baos.toByteArray().length);
					
					
					
					if(i==multiplier){
						//batchTrailer = batchBuilder.reconstructBatchTrailer(txnData,
						//		encoding, i);
						baos.write(BatchBuilder.writeFileTrailer(batchTrailer, encoding,multiplier));
						baos.write(getZeroPadding(11));
					}
					
					baos.writeTo(fos);
					baos.reset();
					baos.flush();
					//baos1.reset();
					
					

				//}
				
				/*if(i >=204 && i % 204==0){
					baos.writeTo(fos);
					baos.reset();
					System.out.print(".");
				}*/
				
				
				//baos1.write(txnData);
			}
			
			/*int txnPadding = BatchBuilder.getTransactionPadding(baos.toByteArray());
			if (txnPadding > 0) {
				baos.write(getZeroPadding(txnPadding));
			}

			byte[] batchTrailer = batchBuilder.reconstructBatchTrailer(baos.toByteArray(),
					encoding);
			baos.write(batchTrailer);
			baos.write(getZeroPadding(11));
			baos.write(BatchBuilder.writeFileTrailer(batchTrailer, encoding));
			baos.write(getZeroPadding(11));*/

			//if(count==0){
			//	byte[] batchTrailer = batchBuilder.reconstructBatchTrailer(baos.toByteArray(),
			//			encoding);
			//	baos.write(batchTrailer);
			//	baos.write(getZeroPadding(11));
				//baos.write(BatchBuilder.writeFileTrailer(batchTrailer, encoding));
			//}			
			//else
			//	baos.write(BatchBuilder.writeFileTrailer(AllbatchTrailers, encoding));
			
			//baos.write(getZeroPadding(11));

			// write all out data to the output stream
			//baos.writeTo(fos);

			System.out.println("Writing created batch to file: "
					+ outFile.getAbsoluteFile());
			baos.flush();
			//baos1.flush();
		} catch(Exception e){
			e.printStackTrace();
		}finally {
			flushStream(fos);
			//removeHash(outFile);
			closeStream(fos);
			closeStream(fis);
		}

	}

	private static void removeHash(File infile) throws Exception {

		File outFile;
		RandomAccessFile rafin = null, rafOut = null;
		try {
			rafin = new RandomAccessFile(infile, "r");

			// outFile = new File(infile.getAbsolutePath());
			outFile = new File(infile.getAbsolutePath());
			rafOut = new RandomAccessFile(outFile, "rw");

			while (rafin.getFilePointer() != infile.length()) {

				byte[] readBytes = new byte[170];
				rafin.read(readBytes);
				String tcr = new String(readBytes, "cp285");

				int tcCode = Integer.parseInt(tcr.substring(0, 2));

				if (tcCode != 0) {

					rafOut.write(readBytes, 0, 2);
					rafOut.write(new String("  ").getBytes("cp037"));
					rafOut.write(readBytes, 4, 166);
				} else {
					rafOut.write(readBytes);
				}
			}

		} catch (Exception e) {

			e.printStackTrace();

		}

		finally {
			rafin.close();
			rafOut.close();
		}

	}
}
