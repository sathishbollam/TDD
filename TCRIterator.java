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


/**
 * @author bhamrar
 */
public class TCRIterator implements IRecordIterator
{
	private ByteBuffer data;
	private int itfLength = TRANSACTION_ITF_LENGTH;
	private boolean codePageEBCDICFlag = false;

	public TCRIterator()
	{
	}

	public void setData(ByteBuffer data)
	{
		this.data = data;
	}

	/**
	 * Must be called before calling next().
	 * 
	 * @return true or false
	 */
	public boolean hasNext()
	{
		boolean result = true;
		
		if (data.remaining() < itfLength)
		{
			result = false;
		}
		return result;
	}

	/**
	 * Call hasNext() before calling this method.
	 * 
	 * @return a TCR
	 */
	public Object next()
	{
		byte[] tcr = new byte[itfLength];
		data.get(tcr, 0, itfLength);

		return ByteBuffer.wrap(tcr);
	}

	public boolean isCodePageEBCDICFlag()
	{
		return codePageEBCDICFlag;
	}

	public void setCodePageEBCDICFlag(boolean codePageEBCDICFlag)
	{
		this.codePageEBCDICFlag = codePageEBCDICFlag;
		if (!codePageEBCDICFlag)
		{
			itfLength = TRANSACTION_ITF_LENGTH + 1;
		}
	}
}
