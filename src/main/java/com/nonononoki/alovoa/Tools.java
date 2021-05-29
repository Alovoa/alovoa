package com.nonononoki.alovoa;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;

public class Tools {
	
	private Tools() throws AlovoaException {
		throw new AlovoaException("no_contructor");
	}
	
	public static final float BASE64FACTOR = 0.75f;
	public static final int MILLION = 1000000;
	public static final int THOUSAND = 1000;
	public static final String B64IMAGEPREFIX = "data:image/";
	public static final String B64AUDIOPREFIX = "data:audio/";
	public static final String B64PREFIX = ";base64,";

	public static final String TEST = "test";
	public static final String PROD = "prod";
	public static final String DEV = "dev";

	public static final String MAIL_TEST_DOMAIN = "@mailinator.com";
	public static final String MAIL_GMAIL_DOMAIN = "@gmail.com";

	public static Locale getUserLocale(User user) {
		String language = user.getLanguage();
		if (language != null) {
			return StringUtils.parseLocale(language);
		} else {
			return Locale.ENGLISH;
		}
	}

	public static Date ageToDate(int age) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.YEAR, age * (-1));
		return calendar.getTime();
	}

	public static Date localDateToDate(LocalDate localDate) {
		return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	public static LocalDate dateToLocalDate(Date date) {
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

	public static int calcUserAge(User user) {
		LocalDate currentDate = LocalDate.now();
		return Period.between(Tools.dateToLocalDate(user.getDates().getDateOfBirth()), currentDate).getYears();
	}

	public static int calcUserAge(Date dateOfBirth) {
		LocalDate currentDate = LocalDate.now();
		return Period.between(Tools.dateToLocalDate(dateOfBirth), currentDate).getYears();
	}

	public static String inputStreamToString(InputStream inputStream) throws IOException {
		return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
	}

	public static String getResourceText(String path) throws IOException {
		Resource resource = new ClassPathResource(path);
		return inputStreamToString(resource.getInputStream());
	}

	public static String resourceToB64(String path) throws IOException {
		Resource resource = new ClassPathResource(path);
		byte[] bytes = StreamUtils.copyToByteArray(resource.getInputStream());
		return Base64.getEncoder().encodeToString(bytes);
	}

	public static String imageToB64(String path, String mime) throws IOException {
		String b64 = resourceToB64(path);
		return B64IMAGEPREFIX + mime + B64PREFIX + b64;
	}

	public static boolean isTextContainingLineFromFile(String path, String text) throws IOException {

		String content = getResourceText(path);
		String[] lines = content.split(System.getProperty("line.separator"));

		for (int i = 0; i < lines.length; i++) {
			if (text.contains(lines[i])) {
				return true;
			}
		}
		return false;
	}

	public static boolean binaryStringToBoolean(String b) {
		return !"0".equals(b);
	}

	public static int getDistanceToUser(User user, User currUser) {
		try {
			return calcDistance(user.getLocationLatitude(), user.getLocationLongitude(),
					currUser.getLocationLatitude(), currUser.getLocationLongitude());
		} catch (Exception e) {
			return 99999;
		}
	}

	// https://stackoverflow.com/a/45732035
	public static Double getBase64Size(String base64String) {
		Double result = -1.0;
		if (!base64String.isEmpty()) {
			Integer padding = 0;
			if (base64String.endsWith("==")) {
				padding = 2;
			} else {
				if (base64String.endsWith("="))
					padding = 1;
			}
			result = Math.ceil((double) base64String.length() / 4) * 3 - padding;
		}
		return result;
	}

	// https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude/20410612#20410612
	// to km
	public static int calcDistance(double lat1, double lng1, double lat2, double lng2) {
		double a = (lat1 - lat2) * distPerLat(lat1);
		double b = (lng1 - lng2) * distPerLng(lat1);
		double dist = Math.sqrt(a * a + b * b);
		return (int) dist / THOUSAND;
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
