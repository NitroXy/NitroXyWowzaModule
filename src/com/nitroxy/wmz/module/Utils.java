package com.nitroxy.wmz.module;

import java.io.File;
import java.io.FileReader;
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
			return null;
		}
		return content;
	}
}
