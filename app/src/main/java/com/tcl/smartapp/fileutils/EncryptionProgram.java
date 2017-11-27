package com.tcl.smartapp.fileutils;

import java.io.File;
import java.io.RandomAccessFile;


public class EncryptionProgram
{
	final static long KEY = (long) (Math.pow(2, 62) - 87853);
	final static long KEY_FLAG = (long) (Math.pow(2, 53) - 783);     //9.007199254741 * 10（15）  -783
	final static long MINI_KEY_CODE_PART = 10;
	final static long MIDDLE_KEY_CODE_PART = 1000;
	final static long MAX_KEY_CODE_PART = 100000;
	final static long SUPER_MAX_KEY_CODE_PART = 100000;
	final static long MINI_FILE_BENCHMARK = 1024 * 1024;    //1M
	final static long MIDDLE_FILE_BENCHMARK = 100 * 1024 * 1024;   //100M
	final static long MAX_FILE_BENCHMARK = 10000 * 1024 * 1024;    //10000M

	/**
	 * Such method is to encrypt the specified file after encrypted ,the end of
	 * the file will be added another 8 bytes
	 * 
	 * @param args
	 */

	public static void encryption(File file)
	{

		RandomAccessFile accessFile = null;
		long fileLength = 0;
		long encodeLength = 0;
		long lastLongCode = 0;
		try
		{
			accessFile = new RandomAccessFile(file, "rw");
			fileLength = accessFile.length();
			encodeLength = getCryptionLength(fileLength);
			accessFile.seek(fileLength - 8);
			lastLongCode = accessFile.readLong();
			if (lastLongCode != KEY_FLAG)
			{
				mainOperation(accessFile, encodeLength);
				System.out.println(file.getName() + "加密完成");
				accessFile.setLength(fileLength + 8);
				accessFile.seek(fileLength);
				accessFile.writeLong(KEY_FLAG);
				accessFile.close();
			}
		}
		catch (Exception e)
		{
			// TODO: handle exception
			e.printStackTrace();
		}

	}

	private static long getCryptionLength(long fileLength)
	{
		// TODO Auto-generated method stub
		long editLength = 0;
		if (fileLength < MINI_FILE_BENCHMARK)
			editLength = fileLength / MINI_KEY_CODE_PART;
		else if (fileLength >= MINI_FILE_BENCHMARK
				&& fileLength < MIDDLE_FILE_BENCHMARK)
			editLength = fileLength / MIDDLE_KEY_CODE_PART;
		else if (fileLength >= MIDDLE_FILE_BENCHMARK
				&& fileLength < MAX_FILE_BENCHMARK)
			editLength = fileLength / MAX_KEY_CODE_PART;
		else
		{
			editLength = fileLength / SUPER_MAX_KEY_CODE_PART;
		}

		return editLength;
	}

	public static void decryption(File file)
	{
		RandomAccessFile accessFile = null;
		long fileLength = 0;
		long decodedLenghth = 0;
		long lastLongCode = 0;
		try
		{
			accessFile = new RandomAccessFile(file, "rw");
			fileLength = accessFile.length();
			decodedLenghth = getCryptionLength(fileLength - 8);
			accessFile.seek(fileLength - 8);
			lastLongCode = accessFile.readLong();
			if (lastLongCode == KEY_FLAG)
			{
				mainOperation(accessFile, decodedLenghth);
				accessFile.setLength(fileLength - 8);
				System.out.println(file.getName() + ":解密完成");
				accessFile.close();
			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}

	/**
	 * the arighmetic before is faulty,now has been modified.
	 */
	private static void mainOperation(RandomAccessFile accessFile,
			long encodeLength)
	{
		// TODO Auto-generated method stub
		long xorCode = 0;
		int encodeRound = (int) (encodeLength / 8);
		encodeRound = 10;
		int i = 0;
		try
		{
			accessFile.seek(0);
			while (i < encodeRound)
			{
				long beforeEncoded = accessFile.readLong();
				accessFile.seek(accessFile.getFilePointer() - 8);
				xorCode = beforeEncoded ^ KEY;
				accessFile.writeLong(xorCode);
				i++;
			}
		}

		catch (Exception e)
		{
			e.printStackTrace();
		}
	}


}
