package com.visaeu.rcs.application.tools.common;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Random;

/**
 * User: Kennedy Date: 13-May-2009 Time: 19:11:33
 */
public class BatchBuilder
{
	private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy");
	private static final String DEFAULT_BII_PROC_BIN = "401770";
	public static final String BATCH_TRAILER_CODE = "91";
	public static final String FILE_HEADER_CODE = "90";
	public static final String FILE_TRAILER_CODE = "92";
	private static final int BATCH_NUM_START = 44;
	private static final int BATCH_NUM_END = 50;
	private static final int DEST_AMT_START = 17;
	private static final int DEST_AMT_END = 32;
	private static final int TXN_COUNT_START = 76;
	private static final int TXN_COUNT_END = 85;
	private static final int TCR_COUNT_END = 62;
	private static final int SRC_AMT_START = 103;
	private static final int SRC_AMT_END = 118;
	private static final int CENTER_ID_START = 68;	
	
	//transaction count also includes tc 91
	private int transactionCount = 1;
	//tcr count also includes tc 91 tcr 0
	private int tcrCount = 1;
	private int monTxnCount = 0;
	private long destAmt = 0L;
	private long sourceAmt = 0L;	
	private boolean convertFromEBCDIC = true;
	private String processingBin = null;
	private String processingDate = null;
	private String securityCode = null;
	private static String fileType = null;
	private static String fileMode = null;
	private HashMap<String, Long> currMap = new HashMap<String, Long>();
	private HashMap<String, Long> srcBin = new HashMap<String, Long>();
	private StringBuffer fileId = new StringBuffer();
	
	public BatchBuilder()
	{
		this(false,null,null,null,null,null);
	}

	public BatchBuilder(boolean isInputAscii,String procBin, String procDate, String secCode, String fType, String fMode)
	{
		convertFromEBCDIC = !isInputAscii;
		processingBin = procBin;
		processingDate = procDate;
		securityCode = secCode;
		fileType = fType;
		fileMode = fMode;
		
	}

	public byte[] reconstructBatch(byte[] batchTransactions, byte[] origTrailer, String encoding, long counter) throws Exception
	{
		TransactionIterator iter = new TransactionIterator();
		iter.setData(ByteBuffer.wrap(batchTransactions));
		iter.setCodePageEBCDIC(convertFromEBCDIC);
		iter.setEncoding(encoding);
		
		while (iter.hasNext())
		{
			ByteBuffer dataTransaction = (ByteBuffer) iter.next();
			String txnString = StringUtil.decodeByteArray(dataTransaction, encoding).toString();
			final short transactionCode = iter.getTransactionCode();
			if (transactionCode == 0 || transactionCode == 54)
			{
				continue;
			}

			if (isFinancialTransaction(transactionCode))
			{
				String sourceCurrCode = null;
				long currSourceAmt = 0L;				
				if (isTC10orTC20(transactionCode))
				{
					currSourceAmt = Long.valueOf(txnString.substring(64, 75));
					sourceAmt += Long.parseLong(txnString.substring(64, 75));
					destAmt += Long.parseLong(txnString.substring(49, 60));
					sourceCurrCode = txnString.substring(75, 78);					
				}
				else
				{
					currSourceAmt = Long.valueOf(txnString.substring(79, 90));
					sourceAmt += Long.parseLong(txnString.substring(79, 90));
					destAmt += Long.parseLong(txnString.substring(64, 75));
					sourceCurrCode = txnString.substring(90, 93);		
					
				}


				
				if (currMap.containsKey(sourceCurrCode))
				{
					currSourceAmt += currMap.get(sourceCurrCode);
					currMap.put(sourceCurrCode, currSourceAmt);
				}
				else
				{
					currMap.put(sourceCurrCode, currSourceAmt);
				}
				monTxnCount++;
			}
			else
			{
				System.out.println("TC" + transactionCode + " is non-financial");
			}

			transactionCount++;
			
			if (convertFromEBCDIC && dataTransaction.limit() % 170 != 0)
			{
				throw new IllegalStateException("Transaction size not a multiple of 170 " + dataTransaction.limit());
			}

			tcrCount += dataTransaction.limit() / 170;
		}

		StringBuilder batchTrailer = null;

		String bin = null;
		String date = null;
		if (origTrailer != null)
		{
			String OrigTrailerStr = StringUtil.decodeByteArray(origTrailer, encoding).toString();
			batchTrailer = new StringBuilder(OrigTrailerStr);
			bin = getProcessingBin(origTrailer, encoding);
			date = getProcessingDate(origTrailer, encoding);
		}
		else
		{
			//create new batch trailer with default values
			batchTrailer = buildBatchTrailer();
		}
		
		if (isDataPresent(processingBin))
		{
			bin = String.valueOf(processingBin);
			batchTrailer.replace(6, 12, StringUtil.padLeft(bin, 6));
		}
		
		if(isDataPresent(processingDate))
		{
			date = getJulienDate(processingDate);
			batchTrailer.replace(12, 17, StringUtil.padLeft(date, 5));	
		}
		else
		{
			date = getJulianDate();
		}

		System.out.println("************************ Batch Trailer Summary *********************");
		System.out.println("Total TCR Count  = " + tcrCount);
		System.out.println("Total Source Amount = " + sourceAmt);
		System.out.println("Total Destination Amount = " + destAmt);
		System.out.println("Total Transaction Count = " + transactionCount);
		System.out.println("Total Txn Monetary Count = " + monTxnCount);
		System.out.println("Source Currency Code List = " + currMap);
		System.out.println("Issuer Bin List = " + srcBin);
		System.out.println("Processing Date (YYDDD) = " + date);
		System.out.println("Processing Bin = " + bin);
		System.out.println("*********************************************************************");

		batchTrailer.replace(DEST_AMT_START, DEST_AMT_END, StringUtil.padLeft(String.valueOf(destAmt), 15));
		batchTrailer.replace(DEST_AMT_START, DEST_AMT_END, StringUtil.padLeft(String.valueOf(destAmt), 15));
		batchTrailer.replace(DEST_AMT_END, BATCH_NUM_START, StringUtil.padLeft(String.valueOf(monTxnCount), 12));
		//batchTrailer.replace(BATCH_NUM_START, BATCH_NUM_END, StringUtil.padLeft(1, 6)); SATHISH BOLLAM
		batchTrailer.replace(BATCH_NUM_START, BATCH_NUM_END, StringUtil.padLeft(counter, 6));
		batchTrailer.replace(BATCH_NUM_END, TCR_COUNT_END, StringUtil.padLeft(String.valueOf(tcrCount), 12));
		batchTrailer.replace(CENTER_ID_START, TXN_COUNT_START, "        ");
		batchTrailer.replace(TXN_COUNT_START, TXN_COUNT_END, StringUtil.padLeft(String.valueOf(transactionCount), 9));
		batchTrailer.replace(SRC_AMT_START, SRC_AMT_END, StringUtil.padLeft(String.valueOf(sourceAmt), 15));
		
		return StringUtil.convertToByteArray(batchTrailer.toString(), encoding);

	}

	public static int getTransactionPadding(byte[] dataTransaction)
	{
		int numberOfRows = 0; 
		if(dataTransaction.length %2040 !=0)
		{
			//get total number of TCRS in batch
			final int tcrCount = (dataTransaction.length / 170);
			int mod = (tcrCount % 12);
			
			//if the number of blocks in the batch is not a multiple of the BLOCK_SIZE the we need to
			//pad the transactions in the batch with zeroes
			if(mod != 0)
			{
				//get the number of block in the batch (this will be a decimal value if )
				double numBlocksInBatch = ((double)tcrCount/(double)12);
				//get the required TCR to make this a valid block (multiple of 2040)
				int requiredTCRCount = 12 * (int)Math.ceil(numBlocksInBatch);
				//get the number of lines(rows) required for padding
				 numberOfRows = requiredTCRCount - tcrCount;
			}
		}
		return numberOfRows;
	}
	
	public byte[] reconstructBatchTrailer(byte[] batchTransactions,String encoding, long counter) throws Exception
	{
		return reconstructBatch(batchTransactions,null,encoding, counter);
	}
	
	public boolean isDataPresent(String data)
	{
		return StringUtil.isDataPresent(data);
	}

	private String getJulienDate(String dateStr)
	{
		try
		{
			DATE_FORMATTER.parse(dateStr);
		}
		catch (ParseException e)
		{
			throw new IllegalArgumentException("Unable to parse date " + dateStr, e);
		}

		String [] dateTokens = dateStr.split("/");
		if (dateTokens.length != 3)
		{
			throw new IllegalArgumentException("Invalid date format expected date format is dd/MM/yyyy but date is "
					+ dateStr);
		}
		int day = Integer.parseInt(dateTokens[0]);
		int month = Integer.parseInt(dateTokens[1]);
		int year = Integer.parseInt(dateTokens[2]);
		return getDate(new GregorianCalendar(year,month-1,day));
	}

	private String getJulianDate()
	{
		Calendar gc = GregorianCalendar.getInstance();
		return getDate(gc);
	}

	private String getDate(Calendar gc)
	{
		int day = gc.get(Calendar.DAY_OF_YEAR);
		String year = String.valueOf(gc.get(Calendar.YEAR)).substring(2);
		String dayString = StringUtil.padLeft(day + "", 3);
		return year + dayString;
	}

	private boolean isTC10orTC20(final short transactionCode)
	{
		return transactionCode == 10 || transactionCode == 20;
	}
	
	private boolean isFinancialTransaction(short tcCode)
	{
		boolean isFinancial = false;
		switch (tcCode)
		{
			case 1:
			case 2:
			case 5:
			case 6:
			case 7:
			case 10:
			case 20:
			case 15:
			case 16:
			case 17:
			case 25:
			case 26:
			case 27:
			case 35:
			case 36:
			case 37:
				isFinancial = true;
		}
		return isFinancial;
	}

	private static String rewriteFileTrailer(String batchTrailer, int multiplier)
	{
		if (batchTrailer == null)
		{
			return "";
		}
		//String destAmt = batchTrailer.substring(DEST_AMT_START, DEST_AMT_END);//SATHISH BOLLAM
		System.out.println("destAmt before ££££££ "+batchTrailer.substring(DEST_AMT_START, DEST_AMT_END));
		String destAmt = Long.toString(Long.parseLong(batchTrailer.substring(DEST_AMT_START, DEST_AMT_END))*multiplier);//SATHISH BOLLAM
		System.out.println("destAmt after ££££££ "+destAmt);
		//String monTxnCount = batchTrailer.substring(DEST_AMT_END, BATCH_NUM_START);//SATHISH BOLLAM
		String monTxnCount = batchTrailer.substring(DEST_AMT_END, BATCH_NUM_START);//SATHISH BOLLAM
		monTxnCount = Long.toString(Long.parseLong(monTxnCount) * multiplier); //SATHISH BOLLAM
		String batchNumber = batchTrailer.substring(BATCH_NUM_START, BATCH_NUM_END);
		System.out.println("££££££ "+batchTrailer.substring(TXN_COUNT_START, TXN_COUNT_END));
		//int transactionCount = Integer.parseInt(batchTrailer.substring(TXN_COUNT_START, TXN_COUNT_END)) + 1;//SATHISH BOLLAM
		Long transactionCount = (Long.parseLong(batchTrailer.substring(TXN_COUNT_START, TXN_COUNT_END))*multiplier) + 1;//SATHISH BOLLAM
		System.out.println("transactionCount in file trailer "+transactionCount);
		//int tcrCount = Integer.parseInt(batchTrailer.substring(BATCH_NUM_END, TCR_COUNT_END)) + 1;//SATHISH BOLLAM
		Long tcrCount = (Long.parseLong(batchTrailer.substring(BATCH_NUM_END, TCR_COUNT_END))*multiplier) + 1;//SATHISH BOLLAM
		//String sourceAmt = batchTrailer.substring(SRC_AMT_START, SRC_AMT_END);//SATHISH BOLLAM
		System.out.println("srcAmt before ££££££ "+batchTrailer.substring(SRC_AMT_START, SRC_AMT_END));
		String sourceAmt = Long.toString(Long.parseLong(batchTrailer.substring(SRC_AMT_START, SRC_AMT_END))*multiplier);
		System.out.println("srcAmt after ££££££ "+sourceAmt);
		StringBuffer fileTrailer = new StringBuffer(batchTrailer);
		fileTrailer.replace(0, 2, FILE_TRAILER_CODE);
		fileTrailer.replace(DEST_AMT_START, DEST_AMT_END, StringUtil.padLeft(destAmt, 15));		
		fileTrailer.replace(DEST_AMT_END, BATCH_NUM_START, StringUtil.padLeft(monTxnCount, 12));
		System.out.println("batchNumber     "+batchNumber);
		fileTrailer.replace(BATCH_NUM_START, BATCH_NUM_END, StringUtil.padLeft(batchNumber, 6));
		fileTrailer.replace(BATCH_NUM_END, TCR_COUNT_END, StringUtil.padLeft(tcrCount, 12));
		if(fileType.equalsIgnoreCase("D"))
		{
			fileTrailer.replace(TCR_COUNT_END, CENTER_ID_START, StringUtil.padLeft("1", 6));
		}
		else
		{
			fileTrailer.replace(TCR_COUNT_END, CENTER_ID_START, StringUtil.padLeft("0", 6));
		}		
		fileTrailer.replace(CENTER_ID_START, TXN_COUNT_START, "        ");
		fileTrailer.replace(TXN_COUNT_START, TXN_COUNT_END, StringUtil.padLeft(transactionCount, 9));
		fileTrailer.replace(SRC_AMT_START, SRC_AMT_END, StringUtil.padLeft(sourceAmt, 15));		
		return fileTrailer.toString();
	}
	
	public String getUniqueFileId(String processingBin, String procDate, String fileMode)
	{
		String[] tokens = procDate.split("/");
		
		SimpleDateFormat sdf = new SimpleDateFormat("mmss");
		fileId.append(processingBin);
		fileId.append("0");
		fileId.append(tokens[2]);
		fileId.append(tokens[1]);
		fileId.append(tokens[0]);
		
		//fileId.append("P");
		
		if(fileMode.equalsIgnoreCase("T")){
			fileId.append("T");
		}else{
			fileId.append("P");
		}
		
		//fileId.append("000000");
		Random r = new Random();
		//fileId.append(1000 + r.nextInt() * 900000);
		
		fileId.append(sdf.format(new Date())+ r.nextInt(1000));
		fileId.append("EU");
		return fileId.toString();
	}
	
	public StringBuilder buildFileHeader()
	{
		StringBuilder fileHeader = new StringBuilder();		
		fileHeader.append("90");
		fileHeader.append("  "); // Hash code
		fileHeader.append(this.processingBin);// processing bin
		fileHeader.append(getJulienDate(this.processingDate));// cpd
		fileHeader.append(" ");// continuation tape indicator
		fileHeader.append(" ");// group data indicator
		if(fileType.equalsIgnoreCase("C"))
		{
			fileHeader.append("0000");// table file version for collection files
			if(this.processingBin.equalsIgnoreCase("401770"))
			{
				fileHeader.append(getJulienDate(this.processingDate));// settlement Date
			}
			else 
			{
				fileHeader.append("00000");// settlement Date
			}
		}
		else if(fileType.equalsIgnoreCase("D"))
		{
			if(this.processingBin.equalsIgnoreCase("401770"))
			{
				fileHeader.append("    ");// table file version for BaseII delivery files
			}
			else
			{
				fileHeader.append("0000");// table file version for member delivery files
			}
			fileHeader.append(getJulienDate(this.processingDate));// settlement Date
		}
		else
		{
			throw new NullPointerException("Invalid File Type");
		}
		//Reserved Field
		if(this.processingBin.equalsIgnoreCase("401770") && fileType.equalsIgnoreCase("C"))
		{
			fileHeader.append("F0");// reserved
		}
		else
		{
			fileHeader.append("00");// reserved
		}		
		fileHeader.append("300");// release number
		
		if (fileMode.equalsIgnoreCase("T"))
		{
			if(fileType.equalsIgnoreCase("C"))
			{
				if(this.processingBin.equalsIgnoreCase("401770"))
				{	
					fileHeader.append("TEST");// test option
					fileHeader.append("0");// process option for BaseII
				}
				else
				{
					fileHeader.append("    ");// test option
					fileHeader.append("I");// process option for member
				}
			}
			else if(fileType.equalsIgnoreCase("D"))
			{
				fileHeader.append("TEST");// test option
				if(this.processingBin.equalsIgnoreCase("401770"))
				{
					fileHeader.append("I");// process option for BaseII
				}
				else
				{
					fileHeader.append("0");// process option for member
				}	
			}
		}
		else
		{
			if(fileType.equalsIgnoreCase("C"))
			{
				if(this.processingBin.equalsIgnoreCase("401770"))
				{	
					fileHeader.append("0000");// test option
					fileHeader.append("0");// process option for BaseII
				}
				else
				{
					fileHeader.append("    ");// test option
					fileHeader.append("I");// process option for member
				}
			}
			else if(fileType.equalsIgnoreCase("D"))
			{
				fileHeader.append("0000");// test option
				if(this.processingBin.equalsIgnoreCase("401770"))
				{
					fileHeader.append("I");// process option for BaseII
				}
				else
				{
					fileHeader.append("0");// process option for member
				}	
			}
		}
		fileHeader.append("0000");// reserved
		fileHeader.append("000001");// starting batch number
		fileHeader.append(StringUtil.padLeft("0",18));// reserved
		if(this.securityCode == null)
		{
			throw new NullPointerException("Security Code cannot be Empty String");
		}
		fileHeader.append(StringUtil.padRight(securityCode," ",8));// securityCode
		if(fileType.equalsIgnoreCase("D"))
		{
			if(this.processingBin.equalsIgnoreCase("401770"))
			{
				fileHeader.append(" ");// Delivery Code
			}
			else
			{
				fileHeader.append("D");// Delivery Code
			}
		}
		else if(fileType.equalsIgnoreCase("C"))
		{
			if(this.processingBin.equalsIgnoreCase("401770"))
			{
				fileHeader.append("D");// Delivery Code
			}
			else
			{
				fileHeader.append(" ");// Delivery Code
			}
		}
		fileHeader.append("     "); // baseII file type
		fileHeader.append(StringUtil.padRight(fileId.toString()," ",30));// unique file id
		fileHeader.append("     ");// customised delivery file type
		//For Source and Destination Name
		if(fileType.equalsIgnoreCase("D"))
		{
			//Member Delivery Files
			if(this.processingBin.equalsIgnoreCase("401770" ))
			{
				fileHeader.append(StringUtil.padRight(this.processingBin, " ", 12));// source name
				fileHeader.append(StringUtil.padRight("BANKCARD", " ", 45));// destination name
			}
			//BaseII Delivery Files
			else
			{
				fileHeader.append(StringUtil.padRight("RCS"," ", 12));// source name
				fileHeader.append(StringUtil.padRight(this.processingBin+"3"," ", 45));// destination name				
			}
		}

		else if(fileType.equalsIgnoreCase("C"))
		{
			//Member Collection Files
			if(this.processingBin.equalsIgnoreCase("401770"))
			{
				fileHeader.append(StringUtil.padRight("BANKCARD", " ", 12));// source name
				fileHeader.append(StringUtil.padRight(this.processingBin+"1", " ", 45));// destination name
			}
			//BaseII Collection Files
			else
			{
				fileHeader.append(StringUtil.padRight(this.processingBin, " ", 12));// source name
				fileHeader.append(StringUtil.padRight("BANKCARD", " ", 45));// destination name				
			}
		}
		if (!convertFromEBCDIC)
		{
			fileHeader.append("\n");
		}
		return fileHeader;
	}

	private StringBuilder buildBatchTrailer()
	{
		StringBuilder batchTrailer = new StringBuilder(170);
		batchTrailer.append(BATCH_TRAILER_CODE); // Hash code
		batchTrailer.append("  "); // Hash code
		batchTrailer.append(0); // Transaction Code Qualifier
		batchTrailer.append(0); // Transaction Component Sequence Number
		batchTrailer.append(DEFAULT_BII_PROC_BIN);// processing bin
		batchTrailer.append("     ");// cpd
		batchTrailer.append(StringUtil.padLeft(0,15));//destination amount
		batchTrailer.append(StringUtil.padLeft(0, 12));//monetary txn count
		batchTrailer.append(StringUtil.padLeft(1, 6));//batchNumber
		batchTrailer.append(StringUtil.padLeft(0, 12));//number of tcrs
		batchTrailer.append(StringUtil.padLeft(0, 6));//file continuation count
		batchTrailer.append(StringUtil.padLeft(" "," ", 8));//center batch id
		batchTrailer.append(StringUtil.padLeft(0, 9));//transaction count
		batchTrailer.append(StringUtil.padLeft(0,18));// reserved
		batchTrailer.append(StringUtil.padLeft(0, 15)); //source amount
		batchTrailer.append(StringUtil.padLeft(0,15));// reserved
		batchTrailer.append(StringUtil.padLeft(0,15));// reserved
		batchTrailer.append(StringUtil.padLeft(0,15));// reserved
		batchTrailer.append(StringUtil.padLeft(" "," ",7));// reserved
		if (!convertFromEBCDIC)
		{
			batchTrailer.append("\n");
		}		
		return batchTrailer;
	}
	
	public static byte[] writeFileTrailer(byte[] arr, String encoding, int multiplier) throws UnsupportedEncodingException
	{
		String value = new String(arr, encoding);
		return rewriteFileTrailer(value, multiplier).getBytes(encoding);
	}

	public static String getFileName(byte[] tcr, String encoding) throws Exception
	{
		return getData(tcr, 79, 30, encoding);
	}

	public static String getBatchNumber(byte[] tcr, String encoding) throws Exception
	{
		return getData(tcr, 45, 6, encoding);
	}

	public static String getProcessingBin(byte[] tcr, String encoding) throws Exception
	{
		return getData(tcr, 7, 6, encoding);
	}

	public static String getProcessingDate(byte[] tcr, String encoding) throws Exception
	{
		return getData(tcr, 13, 5, encoding);
	}

	private static String getData(byte[] tcr, int offset, int count, String encoding) throws Exception
	{
		CharBuffer tcrData = StringUtil.decodeByteArray(tcr, encoding);
		return String.copyValueOf(tcrData.array(), offset - 1, count);
	}

}
