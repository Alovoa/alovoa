package com.nonononoki.alovoa.matching.rerank.policy.impl;

public final class FormulaUtils {

    private FormulaUtils() {
    }

    public static double clamp01(double value) {
        if (value < 0.0) return 0.0;
        if (value > 1.0) return 1.0;
        return value;
    }

    public static double smoothingWeight(double n, double n0) {
        if (n <= 0.0) {
            return 0.0;
        }
        return n / (n + Math.max(1e-9, n0));
    }

    public static double smooth(double raw, double n, double baseline, double n0) {
        double w = smoothingWeight(n, n0);
        return w * raw + (1.0 - w) * baseline;
    }
}
