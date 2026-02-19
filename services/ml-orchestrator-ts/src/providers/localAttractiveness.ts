import { AttractivenessProviderCommand, OrchestratorConfig } from "../config";
import { AttractivenessRequest, AttractivenessResponse } from "../types/contracts";
import { runJsonCommand } from "../utils/commandRunner";
import { loadScoreCalibration, ScoreCalibration } from "../utils/scoreCalibration";
import { safeUnlink, writeImageSourceToTempFile } from "../utils/tempImage";
import { AttractivenessScriptPayload } from "./interfaces";

function clamp01(value: number): number {
  if (value < 0) return 0;
  if (value > 1) return 1;
  return value;
}

function bucketFromSeed(seed: string): number {
  let hash = 0x811c9dc5;
  for (let i = 0; i < seed.length; i++) {
    hash ^= seed.charCodeAt(i);
    hash = Math.imul(hash, 0x01000193);
  }
  const positive = hash >>> 0;
  return (positive % 10_000) / 10_000;
}

export class LocalAttractivenessProvider {
  private readonly config: OrchestratorConfig;
  private readonly calibration: ScoreCalibration | null;

  constructor(config: OrchestratorConfig) {
    this.config = config;
    this.calibration =
      config.attractivenessCalibrationEnabled && config.attractivenessCalibrationFile
        ? loadScoreCalibration(config.attractivenessCalibrationFile)
        : null;
  }

  async score(request: AttractivenessRequest): Promise<AttractivenessResponse> {
    if (!request.front_image_base64 && !request.front_image_url) {
      throw new Error("Front image is required");
    }

    if (!this.config.attractivenessCmd && this.config.attractivenessProviderCommands.length === 0) {
      throw new Error("ATTRACTIVENESS_CMD is not configured");
    }

    const frontPath = await writeImageSourceToTempFile({
      imageBase64: request.front_image_base64,
      imageUrl: request.front_image_url,
    });

    let sidePath: string | null = null;
    if (request.side_image_base64 || request.side_image_url) {
      sidePath = await writeImageSourceToTempFile({
        imageBase64: request.side_image_base64,
        imageUrl: request.side_image_url,
      });
    }

    try {
      const samples: ProviderSample[] = [];
      const signals: Record<string, number> = {};
      const providerVersions: Record<string, string> = {};
      let abSkipped = 0;

      const primary = await this.runProvider(
        {
          name: "primary",
          cmd: this.config.attractivenessCmd,
          modelVersion: this.config.attractivenessModelVersion,
        },
        frontPath,
        sidePath,
      );
      if (primary) {
        samples.push(primary);
        Object.assign(signals, primary.signals);
        providerVersions[sanitizeKey(primary.name)] = primary.modelVersion;
      }

      for (const provider of this.config.attractivenessProviderCommands) {
        if (!this.isProviderEnabledForRequest(provider.name, request)) {
          abSkipped += 1;
          signals[`ab_skip_${sanitizeKey(provider.name)}`] = 1;
          continue;
        }

        const sample = await this.runProvider(provider, frontPath, sidePath);
        if (!sample) {
          continue;
        }
        samples.push(sample);
        Object.assign(signals, sample.signals);
        providerVersions[sanitizeKey(sample.name)] = sample.modelVersion;
      }

      if (samples.length === 0) {
        throw new Error("No attractiveness providers produced a score");
      }

      const blended = this.blendSamples(samples, this.config.attractivenessProviderCommands);
      let score = clamp01(0.2 * this.config.attractivenessBaseline + 0.8 * blended.score);
      let confidence = clamp01(blended.confidence);
      const preCalibrationScore = score;

      if (this.calibration) {
        score = clamp01(this.calibration.apply(score));
        signals.calibration_applied = 1;
        signals.calibration_input_score = round(preCalibrationScore);
        signals.calibration_output_score = round(score);
        providerVersions.scut_calibration = this.calibration.source;
      } else {
        signals.calibration_applied = 0;
      }

      signals.ensemble_provider_count = samples.length;
      signals.ensemble_total_weight = round(blended.totalWeight);
      signals.ensemble_blended_score = round(score);
      signals.ensemble_blended_confidence = round(confidence);
      signals.ensemble_ab_skipped_count = abSkipped;

      return {
        score: round(score),
        confidence: round(confidence),
        provider: this.config.attractivenessProviderName,
        model_version: this.config.attractivenessModelVersion,
        provider_versions: providerVersions,
        signals,
        repo_refs: this.config.openSourceRepoRefs,
      };
    } finally {
      await safeUnlink(frontPath);
      if (sidePath) {
        await safeUnlink(sidePath);
      }
    }
  }

  private async runProvider(
    provider: ProviderInvocation,
    frontPath: string,
    sidePath: string | null,
  ): Promise<ProviderSample | null> {
    if (!provider.cmd) {
      return null;
    }

    try {
      const front = await this.runScore(provider.cmd, frontPath, "front");
      const prefixedProvider = sanitizeKey(provider.name);
      const signals: Record<string, number> = {
        [`${prefixedProvider}_score`]: round(front.score),
        [`${prefixedProvider}_confidence`]: round(front.confidence),
      };
      Object.assign(signals, prefixSignals(front.signals ?? {}, `${prefixedProvider}_front_`));

      let score = front.score;
      let confidence = front.confidence;
      let modelVersion = front.model_version || provider.modelVersion || "unknown";

      if (sidePath) {
        const side = await this.runScore(provider.cmd, sidePath, "side");
        Object.assign(signals, prefixSignals(side.signals ?? {}, `${prefixedProvider}_side_`));
        signals[`${prefixedProvider}_side_score`] = round(side.score);
        signals[`${prefixedProvider}_side_confidence`] = round(side.confidence);
        if (!front.model_version && side.model_version) {
          modelVersion = side.model_version;
        }

        const fw = Math.max(0, this.config.attractivenessFrontWeight);
        const sw = Math.max(0, this.config.attractivenessSideWeight);
        const denom = fw + sw > 0 ? fw + sw : 1;
        score = (fw * front.score + sw * side.score) / denom;
        confidence = (fw * front.confidence + sw * side.confidence) / denom;
      }

      return {
        name: provider.name,
        modelVersion,
        score: clamp01(score),
        confidence: clamp01(confidence),
        signals,
      };
    } catch {
      return null;
    }
  }

  private blendSamples(
    samples: ProviderSample[],
    configuredProviders: AttractivenessProviderCommand[],
  ): { score: number; confidence: number; totalWeight: number } {
    const configuredWeight = new Map<string, number>();
    for (const provider of configuredProviders) {
      configuredWeight.set(sanitizeKey(provider.name), Math.max(0, provider.weight));
    }

    let weightedScore = 0;
    let weightedConfidence = 0;
    let totalScoreWeight = 0;
    let totalConfidenceWeight = 0;

    for (const sample of samples) {
      const sampleKey = sanitizeKey(sample.name);
      const baseWeight = sampleKey === "primary" ? 1.0 : (configuredWeight.get(sampleKey) ?? 0.0);
      const confidenceFactor = Math.max(0.1, sample.confidence);
      const scoreWeight = baseWeight * confidenceFactor;
      const confidenceWeight = baseWeight;

      if (scoreWeight <= 0 || confidenceWeight <= 0) {
        continue;
      }

      weightedScore += sample.score * scoreWeight;
      weightedConfidence += sample.confidence * confidenceWeight;
      totalScoreWeight += scoreWeight;
      totalConfidenceWeight += confidenceWeight;
    }

    if (totalScoreWeight <= 0) {
      return {
        score: this.config.attractivenessBaseline,
        confidence: 0,
        totalWeight: 0,
      };
    }

    return {
      score: clamp01(weightedScore / totalScoreWeight),
      confidence: clamp01(weightedConfidence / Math.max(1e-9, totalConfidenceWeight)),
      totalWeight: totalScoreWeight,
    };
  }

  private async runScore(command: string, imagePath: string, view: string): Promise<AttractivenessScriptPayload> {
    const payload = await runJsonCommand<AttractivenessScriptPayload>(
      command,
      { image_path: imagePath, view },
      this.config.requestTimeoutMs,
    );

    return {
      score: clamp01(payload.score),
      confidence: clamp01(payload.confidence),
      provider: typeof payload.provider === "string" ? payload.provider : undefined,
      model_version: typeof payload.model_version === "string" ? payload.model_version : undefined,
      signals: sanitizeSignals(payload.signals),
    };
  }

  private isProviderEnabledForRequest(providerName: string, request: AttractivenessRequest): boolean {
    const providerKey = sanitizeKey(providerName);
    const rollout = this.config.attractivenessProviderAbRates[providerKey];
    if (rollout == null) {
      return true;
    }
    if (rollout <= 0) {
      return false;
    }
    if (rollout >= 1) {
      return true;
    }

    const subject =
      request.user_id != null
        ? `user:${request.user_id}`
        : (request.segment_key ? `segment:${request.segment_key}` : "global");
    const bucket = bucketFromSeed(`${providerKey}:${subject}`);
    return bucket < rollout;
  }
}

function sanitizeSignals(signals: Record<string, number> | undefined): Record<string, number> {
  if (!signals) return {};
  const out: Record<string, number> = {};
  for (const [k, v] of Object.entries(signals)) {
    if (typeof v === "number" && Number.isFinite(v)) {
      out[sanitizeKey(k)] = round(v);
    }
  }
  return out;
}

function prefixSignals(signals: Record<string, number>, prefix: string): Record<string, number> {
  const out: Record<string, number> = {};
  const normalizedPrefix = sanitizeKey(prefix);
  const prefixToken = normalizedPrefix ? `${normalizedPrefix}_` : "";
  for (const [k, v] of Object.entries(signals)) {
    out[`${prefixToken}${sanitizeKey(k)}`] = round(v);
  }
  return out;
}

function sanitizeKey(value: string): string {
  return String(value)
    .trim()
    .replace(/[^a-zA-Z0-9_]+/g, "_")
    .replace(/^_+|_+$/g, "")
    .toLowerCase();
}

interface ProviderSample {
  name: string;
  modelVersion: string;
  score: number;
  confidence: number;
  signals: Record<string, number>;
}

interface ProviderInvocation {
  name: string;
  cmd: string;
  modelVersion?: string;
}

function round(value: number): number {
  return Math.round(value * 1_000_000) / 1_000_000;
}
