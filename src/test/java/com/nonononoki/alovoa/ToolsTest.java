package com.nonononoki.alovoa;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Gender;
import com.nonononoki.alovoa.entity.user.UserDates;
import com.nonononoki.alovoa.entity.user.UserIntention;

class ToolsTest {

	@Test
	void test() throws Exception {
		int dist = (int) Math.round(Tools.calcDistanceKm(0, 0, 0, 0));
		Assert.assertEquals(0, dist);

		int dist2 = (int) Math.round(Tools.calcDistanceKm(0.45, 0, 0, 0));
		Assert.assertEquals(49, dist2);

		int dist3 = (int) Math.round(Tools.calcDistanceKm(0.46, 0, 0, 0));
		Assert.assertEquals(50, dist3);

		Assert.assertTrue(Tools.isTextContainingLineFromFile(Tools.TEMP_EMAIL_FILE_NAME, "jmpant.com"));
	}

//	return user2Age < AGE_LEGAL == user1Age < AGE_LEGAL
//			&& user2.getPreferedGenders().contains(user1.getGender())
//			&& user1.getPreferedGenders().contains(user2.getGender())
//			&& user1.getPreferedMaxAge() >= user2Age
//			&& user1.getPreferedMinAge() <= user2Age
//			&& user2.getIntention().getText().equals(user1.getIntention().getText());

	@Test
	void testUsersCompatible() {
		User user1 = new User("test@test.com");
		User user2 = new User("test2@test.com");

		UserDates userDates1 = new UserDates();
		UserDates userDates2 = new UserDates();
		userDates1.setDateOfBirth(Tools.localDateToDate(LocalDate.now().minusYears(20)));
		userDates2.setDateOfBirth(Tools.localDateToDate(LocalDate.now().minusYears(20)));

		UserIntention meet = new UserIntention();
		UserIntention date = new UserIntention();
		meet.setText("meet");
		date.setText("date");

		final Gender male = new Gender();
		male.setText("male");

		final Gender female = new Gender();
		female.setText("female");

		user1.setDates(userDates1);
		user2.setDates(userDates2);
		user1.setIntention(meet);
		user2.setIntention(meet);
		user1.setGender(male);
		user1.setPreferedGenders(Collections.singleton(female));
		user2.setGender(female);
		user2.setPreferedGenders(Collections.singleton(male));
		user1.setPreferedMaxAge(20);
		user1.setPreferedMinAge(20);

		Assert.assertTrue(Tools.usersCompatible(user1, user2));

		user1.setPreferedMaxAge(19);
		Assert.assertFalse(Tools.usersCompatible(user1, user2));
		user1.setPreferedMaxAge(20);

		user1.setGender(female);
		Assert.assertFalse(Tools.usersCompatible(user1, user2));
		user1.setGender(male);

		user1.setPreferedMaxAge(19);
		Assert.assertFalse(Tools.usersCompatible(user1, user2));
		user1.setPreferedMaxAge(20);

		user1.setPreferedMinAge(21);
		Assert.assertFalse(Tools.usersCompatible(user1, user2));
		user1.setPreferedMinAge(20);

		user1.setIntention(date);
		Assert.assertFalse(Tools.usersCompatible(user1, user2));
		user1.setIntention(meet);

		userDates1.setDateOfBirth(Tools.localDateToDate(LocalDate.now().minusYears(16)));
		user1.setDates(userDates1);
		Assert.assertFalse(Tools.usersCompatible(user1, user2));
	}
}
