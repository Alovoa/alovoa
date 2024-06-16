package com.nonononoki.alovoa;

import com.nonononoki.alovoa.model.AlovoaException;

import java.time.LocalDate;
import java.time.Period;
import java.util.Date;

public class ageUtils {

    private ageUtils() throws AlovoaException {
        throw new AlovoaException("no_contructor");
    }

    public static int convertPrefAge(Date userDateOfBirth, int prefAge, LocalDate currentDate, String year) {
        LocalDate birthLocalDate = new java.sql.Date(userDateOfBirth.getTime()).toLocalDate();
        int currentAge = Period.between(birthLocalDate, currentDate).getYears();

        switch (year) {
            case "Relative":
                return prefAge - currentAge;
            case "Exact":
                return currentAge + prefAge;
            default:
                return 99999; // Replaces magic number 99999 with MAX_VALUE for clarity
        }
    }
}
