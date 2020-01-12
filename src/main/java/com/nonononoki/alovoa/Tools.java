package com.nonononoki.alovoa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

public class Tools {
	
	public static final float BASE64FACTOR = 0.75f;
	public static final int MILLION = 1000000;
	
    public static File getFileFromResources(String fileName) {

        ClassLoader classLoader = Tools.class.getClassLoader();

        URL res = classLoader.getResource(fileName);
        if (res != null) {
            return new File(res.getFile());
        } else {
        	return null;
        }

    }
    
    public static boolean isTextContainingLineFromFile(File file, String text) throws IOException {

        if (file == null) 
        	return false;

        try (FileReader fr = new FileReader(file);
             BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                if(text.contains(line)) {
                	return true;
                }
            }
        }
        
        return false;
    }
    
    
    public static boolean binaryStringToBoolean(String b) {
    	if(b.equals("0")) {
    		return false;
    	} else {
    		return true;
    	}
    }
}
