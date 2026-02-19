import { existsSync, readFileSync } from "node:fs";

function clamp01(value: number): number {
  if (!Number.isFinite(value)) return 0;
  if (value < 0) return 0;
  if (value > 1) return 1;
  return value;
}

interface CalibrationPayload {
  thresholds?: Record<string, unknown>;
}

export class NsfwCalibration {
  private readonly thresholds: Record<string, number>;

  constructor(thresholds: Record<string, number>) {
    this.thresholds = thresholds;
  }

  thresholdFor(segmentKey: string | undefined): number | null {
    if (segmentKey && Number.isFinite(this.thresholds[segmentKey])) {
      return this.thresholds[segmentKey];
    }
    if (Number.isFinite(this.thresholds["*"])) {
      return this.thresholds["*"];
    }
    return null;
  }
}

function sanitizeThresholds(input: Record<string, unknown> | undefined): Record<string, number> {
  if (!input) {
    return {};
  }
  const out: Record<string, number> = {};
  for (const [segment, raw] of Object.entries(input)) {
    const key = segment.trim();
    if (!key) {
      continue;
    }
    const parsed = typeof raw === "number" ? raw : Number(raw);
    if (!Number.isFinite(parsed)) {
      continue;
    }
    out[key] = clamp01(parsed);
  }
  return out;
}

export function loadNsfwCalibration(calibrationPath: string): NsfwCalibration | null {
  if (!calibrationPath || !existsSync(calibrationPath)) {
    return null;
  }

  try {
    const raw = readFileSync(calibrationPath, "utf-8");
    const payload = JSON.parse(raw) as CalibrationPayload | Record<string, unknown>;
    const thresholdsCandidate =
      payload && typeof payload === "object" && "thresholds" in payload
        ? (payload as CalibrationPayload).thresholds
        : (payload as Record<string, unknown>);

    const thresholds = sanitizeThresholds(
      thresholdsCandidate && typeof thresholdsCandidate === "object" && !Array.isArray(thresholdsCandidate)
        ? thresholdsCandidate as Record<string, unknown>
        : undefined,
    );
    if (Object.keys(thresholds).length === 0) {
      return null;
    }

    return new NsfwCalibration(thresholds);
  } catch {
    return null;
  }
}
