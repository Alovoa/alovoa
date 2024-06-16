package com.nonononoki.alovoa;

import com.nonononoki.alovoa.model.AlovoaException;
public class countryUtils {
    private countryUtils() throws AlovoaException {
        throw new AlovoaException("no_contructor");
    }

    public static String getCountryEmoji(String isoCode) {
        if (isoCode != null && isoCode.length() == 2) {
            int offset = 0x1F1E6;
            int firstChar = isoCode.codePointAt(0) - 'A' + offset;
            int secondChar = isoCode.codePointAt(1) - 'A' + offset;
            return new String(Character.toChars(firstChar)) + new String(Character.toChars(secondChar));
        }
        return null;
    }

}

