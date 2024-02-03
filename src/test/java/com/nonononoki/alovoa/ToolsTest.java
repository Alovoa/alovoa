package com.nonononoki.alovoa;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Gender;
import com.nonononoki.alovoa.entity.user.UserDates;
import com.nonononoki.alovoa.entity.user.UserIntention;
import com.nonononoki.alovoa.entity.user.UserLike;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ToolsTest {

    @Test
    void test() throws Exception {
        int dist = (int) Math.round(Tools.calcDistanceKm(0, 0, 0, 0));
        assertEquals(0, dist);

        int dist2 = (int) Math.round(Tools.calcDistanceKm(0.45, 0, 0, 0));
        assertEquals(50, dist2);

        assertTrue(Tools.isTextContainingLineFromFile(Tools.TEMP_EMAIL_FILE_NAME, "jmpant.com"));
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
        meet.setId(1L);
        meet.setText("meet");
        date.setId(2L);
        date.setText("date");

        final Gender male = new Gender();
        male.setText("male");

        final Gender female = new Gender();
        female.setText("female");

        UserLike like = new UserLike();
        like.setUserTo(user1);
        like.setUserFrom(user2);

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
        user2.setPreferedMaxAge(20);
        user2.setPreferedMinAge(20);

        assertTrue(Tools.usersCompatible(user1, user2, false));

        user1.setIntention(date);
        assertFalse(Tools.usersCompatible(user1, user2, false));
        assertTrue(Tools.usersCompatible(user1, user2, true));
        user2.setLikes(List.of(like));
        assertFalse(Tools.usersCompatible(user2, user1, false));
        assertTrue(Tools.usersCompatible(user1, user2, false));
        user2.setLikes(List.of());
        user1.setIntention(meet);

        user1.setPreferedMaxAge(19);
        assertFalse(Tools.usersCompatible(user1, user2, false));
        user1.setPreferedMaxAge(20);

        user1.setGender(female);
        assertFalse(Tools.usersCompatible(user1, user2, false));
        user1.setGender(male);

        user1.setPreferedMaxAge(19);
        assertFalse(Tools.usersCompatible(user1, user2, false));
        user1.setPreferedMaxAge(20);

        user1.setPreferedMinAge(21);
        assertFalse(Tools.usersCompatible(user1, user2, false));
        user1.setPreferedMinAge(20);

        userDates1.setDateOfBirth(Tools.localDateToDate(LocalDate.now().minusYears(16)));
        user1.setDates(userDates1);
        assertFalse(Tools.usersCompatible(user1, user2, false));
    }

    @Test
    void testMinMaxAgePreferences() {
        User user1 = new User("test@test.com");

        UserDates userDates1 = new UserDates();
        userDates1.setDateOfBirth(Tools.localDateToDate(LocalDate.now().minusYears(20)));
        user1.setDates(userDates1);

        user1.setPreferedMinAge(18);
        user1.setPreferedMaxAge(45);

        assertEquals(18, user1.getPreferedMinAge());
        assertEquals(45, user1.getPreferedMaxAge());

        userDates1.setDateOfBirth(Tools.localDateToDate(LocalDate.now().minusYears(21)));

        assertEquals(19, user1.getPreferedMinAge());
        assertEquals(46, user1.getPreferedMaxAge());

        user1.setPreferedMinAge(25);
        user1.setPreferedMaxAge(55);

        assertEquals(25, user1.getPreferedMinAge());
        assertEquals(55, user1.getPreferedMaxAge());
    }

    @Test
    void testLargeNumberToString() {
        assertEquals("1.23B", Tools.largeNumberToString(1230000000));
        assertEquals("12.34M", Tools.largeNumberToString(12340000));
        assertEquals("1K", Tools.largeNumberToString(1000));
        assertEquals("123", Tools.largeNumberToString(123));
    }
}
