import { ModerationSegmentPolicy, OrchestratorConfig } from "../config";
import { ModerationRequest, ModerationResponse } from "../types/contracts";
import { runJsonCommand } from "../utils/commandRunner";
import { loadNsfwCalibration, NsfwCalibration } from "../utils/nsfwCalibration";
import { safeUnlink, writeImageSourceToTempFile } from "../utils/tempImage";
import { NsfwScriptPayload } from "./interfaces";

function clamp01(value: number): number {
  if (value < 0) return 0;
  if (value > 1) return 1;
  return value;
}

export class LocalModerationProvider {
  private readonly config: OrchestratorConfig;
  private readonly calibration: NsfwCalibration | null;

  constructor(config: OrchestratorConfig) {
    this.config = config;
    this.calibration =
      config.nsfwCalibrationEnabled && config.nsfwCalibrationFile
        ? loadNsfwCalibration(config.nsfwCalibrationFile)
        : null;
  }

  async moderate(request: ModerationRequest): Promise<ModerationResponse> {
    if (!request.image_base64 && !request.image_url) {
      throw new Error("Image is required");
    }

    const imagePath = await writeImageSourceToTempFile({
      imageBase64: request.image_base64,
      imageUrl: request.image_url,
    });

    try {
      const policy = this.resolvePolicy(request.segment_key);
      const categories: Record<string, number> = {};
      const signals: Record<string, number> = {};
      let weightedScore = 0;
      let weightedConfidence = 0;
      let totalWeight = 0;

      if (this.config.nsfwUseOpenNsfw2 && this.config.nsfwOpenNsfw2Cmd) {
        const payload = await this.runNsfwCommand(this.config.nsfwOpenNsfw2Cmd, imagePath);
        if (payload) {
          const confidence = clamp01(payload.confidence ?? 0.65);
          weightedScore += payload.nsfw_score * policy.openNsfw2Weight * Math.max(0.15, confidence);
          weightedConfidence += confidence * policy.openNsfw2Weight;
          totalWeight += policy.openNsfw2Weight;
          Object.assign(categories, prefixKeys(payload.categories ?? {}, "opennsfw2_"));
          Object.assign(signals, prefixKeys(payload.signals ?? {}, "opennsfw2_"));
        }
      }

      if (this.config.nsfwUseNudeNet && this.config.nsfwNudeNetCmd) {
        const payload = await this.runNsfwCommand(this.config.nsfwNudeNetCmd, imagePath);
        if (payload) {
          const confidence = clamp01(payload.confidence ?? 0.70);
          weightedScore += payload.nsfw_score * policy.nudeNetWeight * Math.max(0.15, confidence);
          weightedConfidence += confidence * policy.nudeNetWeight;
          totalWeight += policy.nudeNetWeight;
          Object.assign(categories, prefixKeys(payload.categories ?? {}, "nudenet_"));
          Object.assign(signals, prefixKeys(payload.signals ?? {}, "nudenet_"));
        }
      }

      if (this.config.nsfwUseClipNsfw && this.config.nsfwClipCmd) {
        const payload = await this.runNsfwCommand(this.config.nsfwClipCmd, imagePath);
        if (payload) {
          const confidence = clamp01(payload.confidence ?? 0.70);
          weightedScore += payload.nsfw_score * policy.clipNsfwWeight * Math.max(0.15, confidence);
          weightedConfidence += confidence * policy.clipNsfwWeight;
          totalWeight += policy.clipNsfwWeight;
          Object.assign(categories, prefixKeys(payload.categories ?? {}, "clipnsfw_"));
          Object.assign(signals, prefixKeys(payload.signals ?? {}, "clipnsfw_"));
        }
      }

      const baseline = clamp01(policy.baseline);
      const score = totalWeight > 0 ? clamp01(weightedScore / totalWeight) : baseline;
      const confidence = totalWeight > 0 ? clamp01(weightedConfidence / totalWeight) : 0;
      const isSafe = score < policy.threshold;

      categories.nsfw = round(score);
      categories.sfw = round(1.0 - score);
      signals.threshold = round(policy.threshold);
      signals.baseline = round(policy.baseline);
      signals.segment_policy_applied = policy.segmentPolicyApplied ? 1 : 0;
      signals.calibration_applied = policy.calibrationApplied ? 1 : 0;
      signals.opennsfw2_weight = round(policy.openNsfw2Weight);
      signals.nudenet_weight = round(policy.nudeNetWeight);
      signals.clipnsfw_weight = round(policy.clipNsfwWeight);

      return {
        is_safe: isSafe,
        nsfw_score: round(score),
        confidence: round(confidence),
        action: isSafe ? "ALLOW" : "BLOCK",
        provider: "ts-local-nsfw",
        categories,
        signals,
        repo_refs: this.config.openSourceRepoRefs,
      };
    } finally {
      await safeUnlink(imagePath);
    }
  }

  private resolvePolicy(segmentKey: string | undefined): ResolvedModerationPolicy {
    const segmentPolicy = this.getSegmentPolicy(segmentKey);
    const calibrationThreshold = this.calibration?.thresholdFor(segmentKey);
    const threshold = clamp01(
      calibrationThreshold
        ?? segmentPolicy?.threshold
        ?? this.config.nsfwThreshold,
    );
    const baseline = clamp01(segmentPolicy?.baseline ?? this.config.nsfwBaseline);

    const openWeightRaw = segmentPolicy?.openNsfw2Weight;
    const nudeWeightRaw = segmentPolicy?.nudeNetWeight;
    const clipWeightRaw = segmentPolicy?.clipNsfwWeight;
    const defaultOpen = this.config.nsfwUseOpenNsfw2 ? 0.45 : 0.0;
    const defaultNude = this.config.nsfwUseNudeNet ? 0.55 : 0.0;
    const defaultClip = this.config.nsfwUseClipNsfw ? 0.20 : 0.0;
    let openNsfw2Weight = Number.isFinite(openWeightRaw ?? NaN) ? Math.max(0, openWeightRaw as number) : defaultOpen;
    let nudeNetWeight = Number.isFinite(nudeWeightRaw ?? NaN) ? Math.max(0, nudeWeightRaw as number) : defaultNude;
    let clipNsfwWeight = Number.isFinite(clipWeightRaw ?? NaN) ? Math.max(0, clipWeightRaw as number) : defaultClip;

    if (!this.config.nsfwUseOpenNsfw2) {
      openNsfw2Weight = 0;
    }
    if (!this.config.nsfwUseNudeNet) {
      nudeNetWeight = 0;
    }
    if (!this.config.nsfwUseClipNsfw) {
      clipNsfwWeight = 0;
    }

    const total = openNsfw2Weight + nudeNetWeight + clipNsfwWeight;
    if (total <= 0) {
      openNsfw2Weight = defaultOpen;
      nudeNetWeight = defaultNude;
      clipNsfwWeight = defaultClip;
    } else {
      openNsfw2Weight = openNsfw2Weight / total;
      nudeNetWeight = nudeNetWeight / total;
      clipNsfwWeight = clipNsfwWeight / total;
    }

    return {
      threshold,
      baseline,
      openNsfw2Weight,
      nudeNetWeight,
      clipNsfwWeight,
      segmentPolicyApplied: Boolean(segmentPolicy),
      calibrationApplied: calibrationThreshold != null,
    };
  }

  private getSegmentPolicy(segmentKey: string | undefined): ModerationSegmentPolicy | null {
    if (segmentKey) {
      const exact = this.config.moderationSegmentPolicies[segmentKey];
      if (exact) {
        return exact;
      }
    }
    return this.config.moderationSegmentPolicies["*"] || null;
  }

  private async runNsfwCommand(command: string, imagePath: string): Promise<NsfwScriptPayload | null> {
    try {
      const payload = await runJsonCommand<NsfwScriptPayload>(
        command,
        { image_path: imagePath },
        this.config.requestTimeoutMs,
      );

      return {
        nsfw_score: clamp01(payload.nsfw_score),
        confidence: clamp01(payload.confidence),
        categories: sanitizeNumberMap(payload.categories),
        signals: sanitizeNumberMap(payload.signals),
      };
    } catch {
      return null;
    }
  }
}

function sanitizeNumberMap(input: Record<string, number> | undefined): Record<string, number> {
  if (!input) {
    return {};
  }
  const out: Record<string, number> = {};
  for (const [k, v] of Object.entries(input)) {
    if (typeof v === "number" && Number.isFinite(v)) {
      out[k] = round(clamp01(v));
    }
  }
  return out;
}

function prefixKeys(input: Record<string, number>, prefix: string): Record<string, number> {
  const out: Record<string, number> = {};
  for (const [k, v] of Object.entries(input)) {
    out[`${prefix}${k}`] = round(v);
  }
  return out;
}

function round(value: number): number {
  return Math.round(value * 1_000_000) / 1_000_000;
}

interface ResolvedModerationPolicy {
  threshold: number;
  baseline: number;
  openNsfw2Weight: number;
  nudeNetWeight: number;
  clipNsfwWeight: number;
  segmentPolicyApplied: boolean;
  calibrationApplied: boolean;
}
