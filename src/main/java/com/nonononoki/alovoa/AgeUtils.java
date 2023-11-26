package com.nonononoki.alovoa;

import com.nonononoki.alovoa.model.AlovoaException;

import java.time.LocalDate;
import java.time.Period;
import java.util.Date;

public class AgeUtils {

    private AgeUtils() throws AlovoaException {
        throw new AlovoaException("no_contructor");
    }

    public static int convertPrefAge(Date userDateOfBirth, int prefAge, LocalDate currentDate, String year)
    {
        if(year.equals("RelativeYear"))
            return prefAge - Period.between(Tools.dateToLocalDate(userDateOfBirth), currentDate).getYears();
        if(year.equals("ExactYear"))
            return Period.between(Tools.dateToLocalDate(userDateOfBirth), currentDate).getYears() + prefAge;
        return 99999;
    }
}
