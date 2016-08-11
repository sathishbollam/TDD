package com.visaeu.rcs.application.tools.common;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public abstract class FileBase {
	public static final int EBCDIC_BLOCK_LENGTH = 170;
	public static final int EBCDIC_BLOCK_SIZE = EBCDIC_BLOCK_LENGTH * 12;// 2040
	public static final int ASCII_BLOCK_LENGTH = 171; // 171st character must be
	// a new line character
	public static final int ASCII_BLOCK_SIZE = ASCII_BLOCK_LENGTH * 12; // 2052
	// -
	// including
	// 12
	// new
	// line
	// characters
	public static final String DEFAULT_ASCII_CHARSET = "ISO8859_1";
	public static final String DEFAULT_EDCDIC_CHARSET = "cp285";
	public static final String FILE_EXT = ".TC";
	// public String fileName;

	private boolean isInputAscii = false;
	private String asciiEncoding = null;
	private String ebcdicEncoding = null;
	private String processingBin = null;
	private String processingDate = null;
	protected BatchBuilder batchBuilder;

	public FileBase() {

	}

	public FileBase(boolean isAscii) {
		this(DEFAULT_ASCII_CHARSET, DEFAULT_EDCDIC_CHARSET, isAscii);
	}

	public FileBase(String asciiCharset, String ebcdicCharset, boolean isAscii) {
		this.asciiEncoding = asciiCharset;
		this.ebcdicEncoding = ebcdicCharset;
		this.isInputAscii = isAscii;
	}

	/**
	 * Determines if the content of the given input file is valid and can be
	 * decoded correctly using the charset defined. If the input file is ascii
	 * then the ascii charset else the ebcdic charset (cp285) is used
	 * 
	 * @param file
	 *            . This check reads the first to bytes data from the input file
	 *            and converts to it to string. As the file should should be a
	 *            Transction file then these first two bytes should represent a
	 *            valid transaction code and they will be numeric.
	 * @return <code>true</code> if the first two bytes read are both numeric
	 *         and <code>false</code> if they are not data cannot be decode.
	 * @throws IOException
	 */
	private boolean isDataEncodingValid(File file) throws IOException {
		FileInputStream stream = new FileInputStream(file);
		try {
			// read the first two bytes from the file
			byte[] value = new byte[2];
			stream.read(value, 0, 2);

			String tcCode = new String(value, getEncoding());
			for (int i = 0; i < tcCode.length(); i++) {
				if (!Character.isDigit(tcCode.charAt(i))) {
					return false;
				}
			}
		} finally {
			stream.close();
		}
		return true;
	}

	protected void validateDataEncoding(final File file) throws IOException {
		if (!isDataEncodingValid(file)) {
			boolean isAscii = isInputAscii();
			String dataType = "ascii";
			if (!isAscii) {
				dataType = "ebcdic";
			}
			final String errMsg = "Property 'input.ascii=" + isAscii + "' but data in file " + file.getAbsoluteFile() + " is not " + dataType;
			throw new RuntimeException(errMsg);
		}
	}

	public byte[] getZeroPadding(int rows) throws UnsupportedEncodingException {
		if (isInputAscii()) {
			return getAsciiZeroPadding(rows, ASCII_BLOCK_LENGTH, asciiEncoding);
		}
		return getZeroPadding(rows, EBCDIC_BLOCK_LENGTH, ebcdicEncoding);
	}

	private byte[] getZeroPadding(int rows, int blockLength, String encoding) throws UnsupportedEncodingException {
		StringBuffer data = new StringBuffer(rows * blockLength);
		for (int i = 0; i < rows * blockLength; i++) {
			data.append('0');
		}
		return data.toString().getBytes(encoding);
	}

	/**
	 * For ASCII Format the after every 170 characters, there has to be a new
	 * line character
	 * 
	 * @param rows
	 * @param blockLength
	 * @param encoding
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private byte[] getAsciiZeroPadding(int rows, int blockLength, String encoding) throws UnsupportedEncodingException {
		StringBuffer data = new StringBuffer(rows * blockLength);
		for (int i = 0; i < rows * blockLength; i++) {
			/**
			 * i+1 needs to be used to compensate for starting array index from
			 * 0
			 */
			if (i != 0 && ((i + 1) % (blockLength)) == 0) {
				data.append('\n');
				continue;
			}
			data.append('0');
		}
		return data.toString().getBytes(encoding);
	}

	protected File buildDestFile(String path, String fileType, String processingBin, String procDate, String fileMode) {
//		if (batchBuilder == null) {
//			batchBuilder = new BatchBuilder();
//		}
		String fileName = batchBuilder.getUniqueFileId(processingBin, procDate, fileMode);
		StringBuffer destFileName = new StringBuffer();
		if (fileType.equalsIgnoreCase("C")) {
			destFileName.append("BC" + fileName);
		} else {
			destFileName.append("BD" + fileName);
		}

		return new File(path, destFileName.toString());
	}

	public void closeStream(Closeable cl) throws IOException {
		if (cl != null)
			cl.close();
	}

	public void flushStream(Flushable fl) throws IOException {
		if (fl != null)
			fl.flush();
	}

	public String getFinishTime(long startTime, long endTime) {
		long normTime = ((endTime - startTime) / 1000000000L);
		String finshTime = normTime + ((normTime == 1) ? " second." : " seconds.");
		if (normTime <= 0) {
			normTime = ((endTime - startTime) / 1000000L);
			finshTime = normTime + " milliseconds.";
		}
		return finshTime;
	}

	public void setInputAscii(boolean isInputAscii) {
		this.isInputAscii = isInputAscii;
	}

	public boolean isInputAscii() {
		return isInputAscii;
	}

	public void setAsciiEncoding(String asciiEncoding) {
		this.asciiEncoding = asciiEncoding;
	}

	public void setEbcdicEncoding(String ebcdicEncoding) {
		this.ebcdicEncoding = ebcdicEncoding;
	}

	public void setProcessingBin(String processingBin) {
		this.processingBin = processingBin;
	}

	public String getProcessingBin() {
		return processingBin;
	}

	public String getProcessingDate() {
		return processingDate;
	}

	public void setProcessingDate(String processingDate) {
		this.processingDate = processingDate;
	}

	public String getEncoding() {
		if (isInputAscii) {
			return asciiEncoding;
		}
		return ebcdicEncoding;
	}

	public int getBlockLength() {
		if (isInputAscii) {
			return ASCII_BLOCK_LENGTH;
		}
		return EBCDIC_BLOCK_LENGTH;
	}

	public int getBlockSize() {
		if (isInputAscii) {
			return ASCII_BLOCK_SIZE;
		}
		return EBCDIC_BLOCK_SIZE;
	}
}
