package com.tcl.smartapp.fileutils;

import android.util.Log;
import android.widget.Toast;

import com.tcl.smartapp.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class FileUtils
{
    private static final String TAG = "FileUtils";

    public static final int READ_BYTES = 10 * 1024;
    /**
     * file delete
     *
     * @param rootFile
     */
    public static void fileDel(File rootFile)
    {
        long totalTime = 0;
        long benchmark = System.currentTimeMillis();
        if(!rootFile.exists())
            return;
        ArrayList<File> files = getCompleteMenuList(rootFile);
        int fileLength = files.size();
        for (int i = files.size() - 1; i >= 0; i--)
        {
            if (!files.get(i).delete()) {
                Log.d(TAG,"Failed to delete the file : " + files.get(i).getAbsolutePath());
            }else {
                Log.d(TAG,"Success to delete the file : " + files.get(i).getAbsolutePath());
            }
        }
    }


    public static ArrayList<File> getFiles(File bt)
    {
        ArrayList<File> arrayList = new ArrayList<File>();
        if (bt == null)
            throw new NullPointerException();
        Queqe<File> queqe = new Queqe<File>();
        queqe.enQueqe(bt);
        if (bt.isFile())
        {
            arrayList.add(bt);
            return arrayList;
        }
        File popFile = null;
        while (!queqe.isEmpty())
        {
            popFile = queqe.deQueqe();
            File[] files = popFile.listFiles();
            if (files != null && files.length > 0)
                for (int i = 0; i < files.length; i++)
                {
                    if (files[i].isDirectory())
                        queqe.enQueqe(files[i]);
                    else
                    {
                        arrayList.add(files[i]);
                    }
                }
        }
        return arrayList;
    }
    /**
     * return the whole file by byte codes
     *
     * @param file
     * @return
     */
    public static CharSequence getFileBytes(File file)
    {
        return getFileBytes(file, -1);
    }
    /**
     * return byte array with length limited
     *
     * @param file
     * @param length
     * @return
     */
    public static CharSequence getFileBytes(File file, int length)
    {
        if (!file.exists())
            throw new NullPointerException();
        if (file.isDirectory())
            return null;
        byte[] datas = null;
        StringBuilder builder = new StringBuilder();
        RandomAccessFile accessFile = null;
        int ch = 0;
        long byteCount = 0;
        try
        {
            accessFile = new RandomAccessFile(file, "rw");
            byteCount = accessFile.length();
            if (length > 20000)
                return null;
            else if (length > byteCount || length == -1)
            {
                while ((ch = accessFile.read()) != -1)
                    builder.append(ch + " ");
            }
            else
            {
                accessFile.seek(byteCount - 20);
                for (int i = 0; i < length; i++)
                {
                    ch = accessFile.read();
                    builder.append(ch + " ");
                }
            }
            accessFile.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return builder;
    }
    /**
     * 得到root路径下的所有文件的File 文件，包括目录
     *
     * @param root
     * @return
     */
    public static ArrayList<File> getCompleteMenuList(File root)
    {
        ArrayList<File> wholeList = null;
        if (root == null)
            throw new NullPointerException();
        if (!root.exists())
            throw new IllegalArgumentException("指定的文件不存在");
        wholeList = new ArrayList<File>();
        Queqe<File> queqe = new Queqe<File>();
        queqe.enQueqe(root);
        File frontFile = null;
        File[] subFiles = null;
        while ((frontFile = queqe.deQueqe()) != null)
        {
            wholeList.add(frontFile);
            if (frontFile.isDirectory())
            {
                subFiles = frontFile.listFiles();
                if (subFiles != null || subFiles.length > 0)
                {
                    for (File file : subFiles)
                        queqe.enQueqe(file);
                }
            }
        }
        return wholeList;
    }

    

   //get file name without dir(but has the suffix)
    public static String getFileName(String pathandname){  
        
        int start=pathandname.lastIndexOf("/");  
        int end=pathandname.length();
        if(start!=-1 && end!=-1){  
            return pathandname.substring(start+1,end);    
        }else{  
            return null;  
        }  
          
    }
    
    
	//get file name without the suffix
	public static String getFileNameWithOutSuffix(String pathandname) {

		int start = pathandname.lastIndexOf("/");
		int end=pathandname.lastIndexOf(".");
		if (start != -1 && end != -1) {
			return pathandname.substring(start + 1, end);
		} else {
			return null;
		}
	}
    
    /**
     * copy file
     * @param oldPath String old filepath as：c:/fqf.txt
     * @param newPath String new file path as：f:/fqf.txt
     * @return boolean 
     */ 
   public static void copyFile(String oldPath, String newPath) { 
       try { 
           int bytesum = 0; 
           int byteread = 0; 
           File oldfile = new File(oldPath); 
           if (oldfile.exists()) { //文件存在时 
               InputStream inStream = new FileInputStream(oldPath); //读入原文件 
               FileOutputStream fs = new FileOutputStream(newPath); 
               byte[] buffer = new byte[1444]; 
               int length; 
               while ( (byteread = inStream.read(buffer)) != -1) { 
                   bytesum += byteread; //字节数 文件大小 
                   System.out.println(bytesum); 
                   fs.write(buffer, 0, byteread); 
               } 
               inStream.close(); 
           } 
       } 
       catch (Exception e) { 
           System.out.println("复制单个文件操作出错"); 
           e.printStackTrace(); 

       } 

   } 


    /**
     * judge string in file
     * @param str String string as：helloworld
     * @param file File filepath as：f:/fqf/ff/test.txt
     * @return void
     */
    public static boolean stringIsInFile(File file,String str) {

        boolean boolbackvalue = false;
        try {
            StringBuffer sb = new StringBuffer();
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = "";
            while ((line = br.readLine()) != null) {
                if ((FileUtils.containsAny(line, str))) {
                    return boolbackvalue = true;
                }
                sb.append(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return boolbackvalue;
    }
    
   
   /** 
    * write string to file
    * @param str String   input string as：helloworld
    * @param file File  filepath as：f:/fqf/ff/test.txt
    * @return void 
    */ 
	public static void saveStringToFile(File file,String str) {

		RandomAccessFile ra = null;
		try {
			ra = new RandomAccessFile(file, "rw");
			ra.seek(ra.length());
			//ra.writeBytes(str);
            ra.write(str.getBytes());
			//ra.write("\r\n".getBytes("GBK"));
            ra.writeBytes(System.getProperty("line.separator"));

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException el) {
			el.printStackTrace();
		}

	}

	public static boolean containsAny(String str, String searchChars) {
		if (str.length() != str.replace(searchChars, "").length()) {
			return true;
		}
		return false;
	}

    public static String getNailName(String filePathStr){
        int start = filePathStr.lastIndexOf(".");
        int end = filePathStr.length();
        String nailname = null;
        if (start != -1 && end != -1) {
            nailname = filePathStr.substring(start + 1, end).toString();
            nailname = nailname.replaceAll("\r|\n", "");
        }
        return nailname;
    }

    public static boolean checkFilePathExist(String filePath) {
        File bt_allPrivFilePath = new File(filePath);
        try {
            if (!bt_allPrivFilePath.exists()) {
                Log.d(TAG,"feedback recoder is no exist, and it is creating");
                bt_allPrivFilePath.createNewFile();
            }
            Log.d(TAG,"feedback recoder is exist");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG,"feedback recoder can't  create, Exception");
            return false;
        }
        return true;
    }


}
