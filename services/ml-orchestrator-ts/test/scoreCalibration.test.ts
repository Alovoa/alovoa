import { mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { describe, expect, it } from "vitest";
import { loadScoreCalibration } from "../src/utils/scoreCalibration";

describe("scoreCalibration", () => {
  it("loads calibration and interpolates score percentiles", () => {
    const dir = mkdtempSync(path.join(tmpdir(), "aura-calibration-test-"));
    const file = path.join(dir, "calibration.json");
    writeFileSync(
      file,
      JSON.stringify({
        source: "SCUT-FBP5500",
        quantiles: [
          { q: 0.0, score: 0.0 },
          { q: 0.4, score: 0.3 },
          { q: 0.8, score: 0.6 },
          { q: 1.0, score: 1.0 },
        ],
      }),
      "utf-8",
    );

    try {
      const calibration = loadScoreCalibration(file);
      expect(calibration).not.toBeNull();
      expect(calibration?.source).toBe("SCUT-FBP5500");
      expect(calibration?.apply(0.0)).toBe(0);
      expect(calibration?.apply(1.0)).toBe(1);
      expect(calibration?.apply(0.45)).toBeCloseTo(0.6, 6);
    } finally {
      rmSync(dir, { recursive: true, force: true });
    }
  });

  it("returns null for missing or invalid calibration files", () => {
    expect(loadScoreCalibration("/tmp/path-that-does-not-exist.json")).toBeNull();

    const dir = mkdtempSync(path.join(tmpdir(), "aura-calibration-invalid-"));
    const file = path.join(dir, "invalid.json");
    writeFileSync(file, JSON.stringify({ source: "x", quantiles: [{ q: 0.1 }] }), "utf-8");

    try {
      expect(loadScoreCalibration(file)).toBeNull();
    } finally {
      rmSync(dir, { recursive: true, force: true });
    }
  });
});
