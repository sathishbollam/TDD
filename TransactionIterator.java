package com.visaeu.rcs.application.tools.common;

/***********************************************************************************************************************
 * 
 * RC&S Implementation Class
 * 
 * Copyright 2005 - 2007 by Visa Europe.
 * 
 * All Rights Reserved
 * 
 * Visa Europe – Internal Use Only
 * 
 **********************************************************************************************************************/

import java.nio.ByteBuffer;

public class TransactionIterator implements IRecordIterator
{
	private ByteBuffer data;

	private TCRIterator tcrIter;

	private short transactionCode = -1;

	private int position = 0;
	
	private int itfLength = TRANSACTION_ITF_LENGTH;
	
	private int maxTCLength = MAX_TC_TRANSACTION_LENGTH;

	private CharBuffer decodePreamble = null;

	private String encoding = null;

	private static final String INTERNAL_TCRS = "ABCFG";
	
	private boolean isHeader;
	
	private boolean isBatchTrailer;

	
	public TransactionIterator()
	{
		tcrIter = new TCRIterator();
	}

	/*
	 * @see com.visaeu.rcs.application.cas.parser.IRecordIterator#setData(java.nio.CharBuffer)
	 */
	public void setData(ByteBuffer data)
	{
		this.data = data;
		tcrIter.setData(data);
	}

	public boolean isHeader()
	{
		return isHeader;
	}

	public void setCodePageEBCDIC(boolean eBCDICFlag)
	{
		tcrIter.setCodePageEBCDICFlag(eBCDICFlag);
		if(!eBCDICFlag)
		{
			//assumes additional new line character is added for ascii files
			itfLength = TRANSACTION_ITF_LENGTH+1;
			maxTCLength = itfLength*9;
		}

	}

	public boolean hasNext()
	{
		boolean result = true;
		
		if (data.remaining() < itfLength)
		{
			result = false;
		}

		return result;
	}

	/*
	 * @see com.visaeu.rcs.application.cas.parser.IRecordIterator#next()
	 */
	public Object next() throws Exception
	{
		boolean firstTcr0 = true;
		ByteBuffer tcr;
		int transactionComponentSequenceNumber = -1;
		int lastTcr = -1;
		short lastTcCode = -1;
		short currentTransactionCode = -1;
		isHeader = false;
		isBatchTrailer = false;

		if(encoding == null)
		{
			throw new RuntimeException("The required encoding (ascii/ebcdic) has not been defined.");
		}

		ByteBuffer buffer = ByteBuffer.allocate(maxTCLength);

		while (tcrIter.hasNext())
		{
			tcr = (ByteBuffer) tcrIter.next();
			lastTcr = transactionComponentSequenceNumber;
			transactionComponentSequenceNumber = getTransactionComponentSequenceNumber(tcr);
			currentTransactionCode = getCurrentTransaction();
			lastTcCode = getTransactionCode();

			if (transactionComponentSequenceNumber == 0 || (currentTransactionCode != lastTcCode))
			{
				if (firstTcr0 && (lastTcr < transactionComponentSequenceNumber))
				{
					buffer.put(tcr);
					parseTransactionCode();
					firstTcr0 = false;
				}
				else
				{
					data.position(data.position() - itfLength);
					break;
				}
			}
			else if ((lastTcr >= transactionComponentSequenceNumber))
			{
				data.position(data.position() - itfLength);
				break;
			}
			else
			{
				/**
				 * this conditional forces to ignore TCRs 8, A, B, C, F, G. It checks that the tcr being processed is
				 * not internal
				 */
				boolean ignoreTCR = false;//shouldIgnoreTCR8(currentTransactionCode, transactionComponentSequenceNumber);
				if (INTERNAL_TCRS.indexOf(transactionComponentSequenceNumber + "") < 0 && !ignoreTCR)
				{
					buffer.put(tcr);
				}
				else
				{
					System.out.println("Ignoring TCR 8, A, B, C, F or G: " + transactionComponentSequenceNumber);
				}
			}

			if (currentTransactionCode == 90)
			{
				isHeader = true;
			}

			if (currentTransactionCode == 91)
			{
				isBatchTrailer = true;
			}
		}

		if (buffer.position() != 0)
		{
			buffer.limit(buffer.position());
			buffer.rewind();
			return buffer;
		}

		return null;
	}

	public boolean shouldIgnoreTCR8(short tcCode, int tcr)
	{
		return tcr == 8 && !(tcCode == 1 || tcCode == 2 || tcCode == 3 || tcCode == 44);
	}

	
	private int getTransactionComponentSequenceNumber(ByteBuffer tcr) throws Exception
	{
		int transactionComponentSequenceNumber = -1;
		byte[] preamble = new byte[6];
		tcr.get(preamble, 0, 6);
		decodePreamble = StringUtil.decodeByteArray(preamble, this.encoding);
		char[] decodePrembleArray = decodePreamble.array();

		// if the tcr number is character e.g., then return tcr number as an ascii value
		if ((int) decodePrembleArray[5] >= 65)
		{
			transactionComponentSequenceNumber = (int) decodePrembleArray[5];
		}
		// otherwise the tcr number is the 0-9
		else
		{
			transactionComponentSequenceNumber = Character.getNumericValue(decodePrembleArray[5]);
		}
		if (tcr.position() != 0)
		{
			tcr.rewind();
		}
		return transactionComponentSequenceNumber;
	}


	private void parseTransactionCode() throws Exception
	{
		transactionCode = getCurrentTransaction();
	}

	private short getCurrentTransaction() throws Exception
	{
		String tcCode = null;
		try
		{
			tcCode = String.copyValueOf(decodePreamble.array(), 0, 2);
			return Short.parseShort(tcCode);
		}
		catch (NumberFormatException nfe)
		{
			nfe.printStackTrace();
			// throw new Exception("Cannot read transaction code "+tcCode, nfe);
		}
		return -1;
	}

	public short getTransactionCode()
	{
		return transactionCode;
	}

	public int getPosition()
	{
		return position;
	}

	public boolean isBatchTrailer()
	{
		return isBatchTrailer;
	}

	public String getEncoding()
	{
		return encoding;
	}

	public void setEncoding(String encoding)
	{
		this.encoding = encoding;
	}
}
