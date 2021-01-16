package com.nonononoki.alovoa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;

import com.nonononoki.alovoa.entity.User;

public class Tools {

	public static final float BASE64FACTOR = 0.75f;
	public static final int MILLION = 1000000;
	public static final int THOUSAND = 1000;
	public static final String B64IMAGEPREFIX = "data:image/";
	public static final String B64AUDIOPREFIX = "data:audio/";
	public static final String B64PREFIX = ";base64,";

	public static File getFileFromResources(String fileName) {

		ClassLoader classLoader = Tools.class.getClassLoader();

		URL res = classLoader.getResource(fileName);
		if (res != null) {
			return new File(res.getFile());
		} else {
			return null;
		}

	}

	public static String imageToB64(File file, String mime) throws IOException {
		byte[] fileContent = Files.readAllBytes(file.toPath());
		String b64 = Base64.getEncoder().encodeToString(fileContent);
		return B64IMAGEPREFIX + mime + B64PREFIX + b64;
	}

	public static boolean isTextContainingLineFromFile(File file, String text) throws IOException {

		if (file == null)
			return false;

		try (FileReader fr = new FileReader(file); BufferedReader br = new BufferedReader(fr)) {
			String line;
			while ((line = br.readLine()) != null) {
				if (text.contains(line)) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean binaryStringToBoolean(String b) {
		if (b.equals("0")) {
			return false;
		} else {
			return true;
		}
	}

	public static double getDistanceToUser(User user, User currUser) {
		return calcDistance(Double.parseDouble(user.getLastLocation().getLatitude()),
				Double.parseDouble(user.getLastLocation().getLongitude()),
				Double.parseDouble(currUser.getLastLocation().getLatitude()),
				Double.parseDouble(currUser.getLastLocation().getLongitude()));
	}
	
	//https://stackoverflow.com/a/45732035
	public static Double getBase64Size(String base64String) {
	    Double result = -1.0;
	    if(!base64String.isEmpty()) {
	        Integer padding = 0;
	        if(base64String.endsWith("==")) {
	            padding = 2;
	        }
	        else {
	            if (base64String.endsWith("=")) padding = 1;
	        }
	        result = (Math.ceil(base64String.length() / 4) * 3 ) - padding;
	    }
	    return result / (double) MILLION; //MB
	}

	// https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude/20410612#20410612
	private static double calcDistance(double lat1, double lng1, double lat2, double lng2) {
		double a = (lat1 - lat2) * distPerLat(lat1);
		double b = (lng1 - lng2) * distPerLng(lat1);
		return Math.sqrt(a * a + b * b);
	}

	private static double distPerLng(double lat) {
		return 0.0003121092 * Math.pow(lat, 4) + 0.0101182384 * Math.pow(lat, 3) - 17.2385140059 * lat * lat
				+ 5.5485277537 * lat + 111301.967182595;
	}

	private static double distPerLat(double lat) {
		return -0.000000487305676 * Math.pow(lat, 4) - 0.0033668574 * Math.pow(lat, 3) + 0.4601181791 * lat * lat
				- 1.4558127346 * lat + 110579.25662316;
	}
}
