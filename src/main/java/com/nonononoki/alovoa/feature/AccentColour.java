package com.nonononoki.alovoa.feature;

public enum AccentColour {
    PINK("pink"),
    BLUE("blue"),
    ORANGE("orange"),
    PURPLE("purple");

    private final String color;
    AccentColour(String color) {
        this.color = color;
    }
    public String getColor() {
        return color;
    }
}
