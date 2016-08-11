package com.visaeu.rcs.application.tools.common;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.io.UnsupportedEncodingException;

public final class StringUtil
{
	private StringUtil()
	{
	}

	public static CharBuffer decodeByteArray(byte[] inputData, String encoding)
		throws UnsupportedEncodingException
	{
		if(encoding == null)
		{
			throw new IllegalArgumentException("The encoding cannot be null.");
		}

		return CharBuffer.wrap(new String(inputData, encoding).toCharArray());
	}

	public static CharBuffer decodeByteArray(ByteBuffer inputData, String encoding)
		throws UnsupportedEncodingException
	{
		return decodeByteArray(inputData.array(), encoding);
	}

	public static byte[] convertToByteArray(String data, String encoding) throws UnsupportedEncodingException 
	{
		if(encoding == null)
		{
			throw new IllegalArgumentException("The encoding cannot be null.");
		}
		return data.getBytes(encoding);
	}
	
	/**
	 * Trim the leading zeros from the front of a value.
	 * 
	 * @param obj
	 *            the String to trim
	 */

	public static String trimLeadingZeros(Object obj)
	{
		// Check the parameter and cast to String

		if (obj == null)
		{
			return null;
		}

		String testString = (String) obj;

		// Trim the leading Zeros

		String retString = trimLeft(obj, '0');

		if ((retString.length() == 0) && (testString.length() > 0))
		{
			return "0";
		}

		return retString;

	}

	/**
	 * Trim a specified character from the beginning of a value.
	 * 
	 * @param obj
	 *            the value to trim characters from
	 * @param ch
	 *            the character to trim from the front of the value
	 */

	public static String trimLeft(Object obj, char ch)
	{
		// Check the value and cast to a String

		if (obj == null)
		{
			return null;
		}

		String str = (String) obj;

		// Trim the specifed value from the front of the Value

		int length = str.length();
		int st = 0;

		while ((st < (length - 1)) && (str.charAt(st) == ch))
		{
			st++;
		}

		if (st > 0)
		{
			return str.substring(st);
		}

		return str;
	}

	public static String padLeft(String value, int length)
	{
		return padLeft(value, "0", length);
	}

	public static String padLeft(String value, String padString, int length)
	{
		final int size = value.length();
		if (size >= length)
		{
			return value;
		}

		if (padString == null)
		{
			return value;
		}

		StringBuilder builder = new StringBuilder(value);
		for (int i = 0; i < (length - size); i++)
		{
			builder.insert(0, padString.charAt(0));
		}
		return builder.toString();
	}

	public static String padRight(String value, String padString, int length)
	{
		final int size = value.length();
		if (size >= length)
		{
			return value;
		}

		if (padString == null)
		{
			return value;
		}

		StringBuilder builder = new StringBuilder(value);
		for (int i = 0; i < (length - size); i++)
		{
			builder.append(padString.charAt(0));
		}
		return builder.toString();
	}
	
	
	public static String padLeft(int value, int length)
	{
		return padLeft(String.valueOf(value), "0", length);
	}
	
	public static String padLeft(long value, int length)
	{
		return padLeft(String.valueOf(value), "0", length);
	}

	public static boolean isDataPresent(String data)
	{
		return data != null && data.trim().length() > 0;
	}
}
