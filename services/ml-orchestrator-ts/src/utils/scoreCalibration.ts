import { existsSync, readFileSync } from "node:fs";

function clamp01(value: number): number {
  if (!Number.isFinite(value)) return 0;
  if (value < 0) return 0;
  if (value > 1) return 1;
  return value;
}

interface CalibrationPoint {
  q: number;
  score: number;
}

interface CalibrationFilePayload {
  type?: string;
  source?: string;
  quantiles?: CalibrationPoint[];
}

export class ScoreCalibration {
  readonly source: string;
  private readonly points: CalibrationPoint[];

  constructor(source: string, points: CalibrationPoint[]) {
    this.source = source;
    this.points = points;
  }

  apply(score: number): number {
    const clamped = clamp01(score);
    if (this.points.length < 2) {
      return clamped;
    }

    if (clamped <= this.points[0].score) {
      return clamp01(this.points[0].q);
    }

    const last = this.points[this.points.length - 1];
    if (clamped >= last.score) {
      return clamp01(last.q);
    }

    for (let idx = 1; idx < this.points.length; idx += 1) {
      const left = this.points[idx - 1];
      const right = this.points[idx];
      if (clamped > right.score) {
        continue;
      }

      const width = right.score - left.score;
      if (width <= 1e-9) {
        return clamp01(Math.max(left.q, right.q));
      }

      const t = clamp01((clamped - left.score) / width);
      return clamp01(left.q + (right.q - left.q) * t);
    }

    return clamp01(last.q);
  }
}

function normalizePoints(rawPoints: CalibrationPoint[]): CalibrationPoint[] {
  const parsed: CalibrationPoint[] = [];
  for (const point of rawPoints) {
    if (!point || typeof point !== "object") {
      continue;
    }
    const q = Number((point as CalibrationPoint).q);
    const score = Number((point as CalibrationPoint).score);
    if (!Number.isFinite(q) || !Number.isFinite(score)) {
      continue;
    }
    parsed.push({
      q: clamp01(q),
      score: clamp01(score),
    });
  }

  if (parsed.length < 2) {
    return [];
  }

  parsed.sort((a, b) => a.score - b.score || a.q - b.q);
  return parsed;
}

export function loadScoreCalibration(calibrationPath: string): ScoreCalibration | null {
  if (!calibrationPath || !existsSync(calibrationPath)) {
    return null;
  }

  try {
    const raw = readFileSync(calibrationPath, "utf-8");
    const payload = JSON.parse(raw) as CalibrationFilePayload;
    const points = normalizePoints(payload.quantiles ?? []);
    if (points.length < 2) {
      return null;
    }

    const source = payload.source && payload.source.trim() ? payload.source.trim() : "unknown";
    return new ScoreCalibration(source, points);
  } catch {
    return null;
  }
}
