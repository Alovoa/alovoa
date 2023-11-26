package com.nonononoki.alovoa;

import com.nonononoki.alovoa.model.AlovoaException;

public class CountryUtils {

    private CountryUtils() throws AlovoaException {
        throw new AlovoaException("no_contructor");
    }

    public static String getCountryEmoji(String countryIso) {
        if (countryIso != null) {
            int firstLetter = Character.codePointAt(countryIso, 0) - 0x41 + 0x1F1E6;
            int secondLetter = Character.codePointAt(countryIso, 1) - 0x41 + 0x1F1E6;
            return new String(Character.toChars(firstLetter)) + new String(Character.toChars(secondLetter));
        }
        return null;
    }

}
