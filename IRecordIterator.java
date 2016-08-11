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

public interface IRecordIterator
{
	public static final int TRANSACTION_ITF_LENGTH = 170;
	public static final int MAX_TC_TRANSACTION_LENGTH = TRANSACTION_ITF_LENGTH * 9;

	void setData(ByteBuffer data) throws Exception;

	boolean hasNext();

	Object next() throws Exception;
}
