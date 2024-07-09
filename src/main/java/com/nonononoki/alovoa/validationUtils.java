package com.nonononoki.alovoa;

import java.net.URL;

import com.nonononoki.alovoa.model.AlovoaException;
public class validationUtils {
    private validationUtils() throws AlovoaException {
        throw new AlovoaException("no_contructor");
    }

    /**
     * Validates the format of a given URL string.
     * @param urlString The URL text to be validated.
     * @return true if the URL is in a valid format; false otherwise.
     */
    public static boolean isURLValid(String urlString) {
        try {
            URL url = new URL(urlString);
            url.toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
