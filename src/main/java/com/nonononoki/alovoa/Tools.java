package com.nonononoki.alovoa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

import com.nonononoki.alovoa.entity.User;

public class Tools {

	public static final float BASE64FACTOR = 0.75f;
	public static final int MILLION = 1000000;
	public static final int THOUSAND = 1000;
	public static final String B64IMAGEPREFIX = "data:image/";
	public static final String B64AUDIOPREFIX = "data:audio/";
	public static final String B64PREFIX = ";base64,";
	
	public static final String TEST = "test";
	public static final String PROD = "prod";
	public static final String DEV = "dev";
	
	public static final String MAIL_TEST_DOMAIN= "@mailinator.com";
	
	public static LocalDate dateToLocalDate(Date date) {
		return date.toInstant()
			      .atZone(ZoneId.systemDefault())
			      .toLocalDate();
	}
	
	public static int calcUserAge(User user) {
		LocalDate currentDate = LocalDate.now();
		return Period.between(Tools.dateToLocalDate(user.getDates().getDateOfBirth()), currentDate).getYears();
	}
	
	public static int calcUserAge(Date dateOfBirth) {
		LocalDate currentDate = LocalDate.now();
		return Period.between(Tools.dateToLocalDate(dateOfBirth), currentDate).getYears();
	}

	public static File getFileFromResources(String fileName) {
		URL res = getUrlFromResources(fileName);
		if (res != null) {
			return new File(res.getFile());
		} else {
			return null;
		}
	}
	
	public static URL getUrlFromResources(String fileName) {
		ClassLoader classLoader = Tools.class.getClassLoader();
		return classLoader.getResource(fileName);
	}
	
	public static String getResourceText(String path) throws IOException {
		File file = getFileFromResources(path);
		byte[] bytes = Files.readAllBytes(file.toPath());
		String content = new String(bytes, StandardCharsets.UTF_8);
        return content;
	}
	
	public static String resourceToB64(String path) throws IOException {
		File file = getFileFromResources(path);
		byte[] fileContent = Files.readAllBytes(file.toPath());
		return Base64.getEncoder().encodeToString(fileContent);
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

	public static int getDistanceToUser(User user, User currUser) {
		int dist = calcDistance(user.getLocationLatitude(),
				user.getLocationLongitude(),
				currUser.getLocationLatitude(),
				currUser.getLocationLongitude());
		return dist;
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
	//to km
	public static int calcDistance(double lat1, double lng1, double lat2, double lng2) {
		double a = (lat1 - lat2) * distPerLat(lat1);
		double b = (lng1 - lng2) * distPerLng(lat1);
		double dist = Math.sqrt(a * a + b * b);
		return (int) dist/THOUSAND;
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
