package com.nonononoki.alovoa;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

import com.nonononoki.alovoa.config.SecurityConfig;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;

public class Tools {

	private Tools() throws AlovoaException {
		throw new AlovoaException("no_contructor");
	}

	public static final float BASE64FACTOR = 0.75f;
	private static final int BILLION = 1000000000;
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

	public static final String TEMP_EMAIL_FILE_NAME = "temp-mail.txt";

	public static final int AGE_LEGAL = 18;

	public static final long GENDER_MALE_ID = 1;
	public static final long GENDER_FEMALE_ID = 2;
	public static final long GENDER_OTHER_ID = 3;

	public static final double REFERRED_AMOUNT = 0.5;

	public static String cleanEmail(String email) {
		if (email == null) {
			return null;
		}
		return email.toLowerCase();
	}

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

	public static LocalDateTime dateToLocalDateTime(Date date) {
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
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
			return calcDistanceKm(user.getLocationLatitude(), user.getLocationLongitude(),
					currUser.getLocationLatitude(), currUser.getLocationLongitude());
		} catch (Exception e) {
			return 99999;
		}
	}

	// https://stackoverflow.com/a/45732035
	// CC BY-SA 3.0, Pedro Silva
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

	// https://stackoverflow.com/questions/27928/calculate-distance-between-two-latitude-longitude-points-haversine-formula/12600225#12600225
	public static final double AVERAGE_RADIUS_OF_EARTH_KM = 6371;

	public static int calcDistanceKm(double userLat, double userLng, double venueLat, double venueLng) {

		double latDistance = Math.toRadians(userLat - venueLat);
		double lngDistance = Math.toRadians(userLng - venueLng);

		double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(userLat))
				* Math.cos(Math.toRadians(venueLat)) * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return (int) (Math.round(AVERAGE_RADIUS_OF_EARTH_KM * c));
	}

	public static boolean usersCompatible(User user1, User user2) {
		if (user2.getPreferedGenders() == null || user1.getPreferedGenders() == null || user1.getDates() == null
				|| user2.getDates() == null) {
			return false;
		}

		int user1Age = calcUserAge(user1);
		int user2Age = calcUserAge(user2);

		return user2Age < AGE_LEGAL == user1Age < AGE_LEGAL && user2.getPreferedGenders().contains(user1.getGender())
				&& user1.getPreferedGenders().contains(user2.getGender()) && user1.getPreferedMaxAge() >= user2Age
				&& user1.getPreferedMinAge() <= user2Age && user2.getPreferedMaxAge() >= user1Age
				&& user2.getPreferedMinAge() <= user1Age
				&& user2.getIntention().getText().equals(user1.getIntention().getText());
	}

	// This method subtracts user date of birth with passed preferred min/max age
	public static int convertPrefAgeToRelativeYear(Date userDateOfBirth, int prefAge) {
		LocalDate currentDate = LocalDate.now();
		return prefAge - Period.between(Tools.dateToLocalDate(userDateOfBirth), currentDate).getYears();
	}

	public static int convertPrefAgeToExactYear(Date userDateOfBirth, int prefAge) {
		LocalDate currentDate = LocalDate.now();
		return Period.between(Tools.dateToLocalDate(userDateOfBirth), currentDate).getYears() + prefAge;
	}

	private static final String STR_NUM_BILLION = "B";
	private static final String STR_NUM_MILLION = "M";
	private static final String STR_NUM_THOUSAND = "K";

	public static String largeNumberToString(long num) {
		if (num < THOUSAND) {
			return String.valueOf(num);
		}
		DecimalFormat df = new DecimalFormat("#.###");
		df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));

		if (num >= BILLION) {
			double d = (double) num / BILLION;
			return df.format(d) + STR_NUM_BILLION;
		} else if (num >= MILLION) {
			double d = (double) num / MILLION;
			return df.format(d) + STR_NUM_MILLION;
		} else {
			double d = (double) num / THOUSAND;
			return df.format(d) + STR_NUM_THOUSAND;
		}
	}

	public static String getCountryEmoji(String countryIso) {
		if (countryIso != null) {
			int firstLetter = Character.codePointAt(countryIso, 0) - 0x41 + 0x1F1E6;
			int secondLetter = Character.codePointAt(countryIso, 1) - 0x41 + 0x1F1E6;
			return new String(Character.toChars(firstLetter)) + new String(Character.toChars(secondLetter));
		}
		return null;
	}

	public static boolean isURLValid(String urlString) {
		try {
			URL url = new URL(urlString);
			url.toURI();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static String getAuthParams(SecurityConfig securityConfig, String httpSessionId, String username,
			String firstName, int page, String password) {
		String cookieData = securityConfig.getOAuthRememberMeServices().getRememberMeCookieData(username, password);
		StringBuilder builder = new StringBuilder();
		builder.append("?remember-me=").append(cookieData).append("&jsessionid=").append(httpSessionId).append("&page=")
				.append(page);
		if (firstName != null) {
			builder.append("&firstName=").append(firstName);
		}
		return builder.toString();
	}

	public static String getAuthParams(SecurityConfig securityConfig, String httpSessionId, String username,
			String firstName, int page) {
		return getAuthParams(securityConfig, httpSessionId, username, firstName, page, null);
	}
}
