package com.nitroxy.wmz.module;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Utils {

	public static String readFile(String filename) {
		String content = null;
		File file = new File(filename); //for ex foo.txt
		try {
			FileReader reader = new FileReader(file);
			char[] chars = new char[(int) file.length()];
			reader.read(chars);
			content = new String(chars);
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return content;
	}
	
	public static void writeFile(String filename, String data) {
		File file = new File(filename);
		try {
			FileWriter writer = new FileWriter(file);
			writer.write(data);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String fileExtension(String fileName) {
		String extension = "";

		int i = fileName.lastIndexOf('.');
		if (i >= 0) {
		    extension = fileName.substring(i+1);
		}
		return extension;
	}
}
