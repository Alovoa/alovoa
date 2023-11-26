package com.nonononoki.alovoa;

import java.net.URL;

import com.nonononoki.alovoa.model.AlovoaException;

public class ValidationUtils {

    private ValidationUtils() throws AlovoaException {
        throw new AlovoaException("no_contructor");
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

}
