import { OrchestratorConfig } from "../config";
import { FaceQualityRequest, FaceQualityResponse } from "../types/contracts";
import { runJsonCommand } from "../utils/commandRunner";
import { safeUnlink, writeImageSourceToTempFile } from "../utils/tempImage";
import { FaceQualityScriptPayload } from "./interfaces";

function clamp01(value: number): number {
  if (!Number.isFinite(value)) return 0;
  if (value < 0) return 0;
  if (value > 1) return 1;
  return value;
}

function round(value: number): number {
  return Math.round(value * 1_000_000) / 1_000_000;
}

export class LocalFaceQualityProvider {
  private readonly config: OrchestratorConfig;

  constructor(config: OrchestratorConfig) {
    this.config = config;
  }

  async score(request: FaceQualityRequest): Promise<FaceQualityResponse> {
    if (!request.image_base64 && !request.image_url) {
      throw new Error("image_base64 or image_url is required");
    }

    const imagePath = await writeImageSourceToTempFile({
      imageBase64: request.image_base64,
      imageUrl: request.image_url,
    });

    try {
      if (!this.config.faceQualityCmd) {
        return this.fallbackResponse();
      }

      const payload = await runJsonCommand<FaceQualityScriptPayload>(
        this.config.faceQualityCmd,
        { image_path: imagePath },
        this.config.requestTimeoutMs,
      );

      const qualityRaw = Number.isFinite(Number(payload.quality_score))
        ? Number(payload.quality_score)
        : Number(payload.score ?? NaN);

      const quality = clamp01(Number.isFinite(qualityRaw) ? qualityRaw : 0.5);
      const confidence = clamp01(Number(payload.confidence ?? 0.4));

      return {
        quality_score: round(quality),
        confidence: round(confidence),
        provider: payload.provider || this.config.faceQualityProviderName,
        model_version: payload.model_version || this.config.faceQualityModelVersion,
        signals: sanitizeSignals(payload.signals),
        repo_refs: this.config.openSourceRepoRefs,
      };
    } catch {
      return this.fallbackResponse();
    } finally {
      await safeUnlink(imagePath);
    }
  }

  private fallbackResponse(): FaceQualityResponse {
    return {
      quality_score: 0.5,
      confidence: 0.0,
      provider: "face_quality_fallback",
      model_version: "heuristic_v1",
      signals: {
        fallback_used: 1,
      },
      repo_refs: this.config.openSourceRepoRefs,
    };
  }
}

function sanitizeSignals(input: Record<string, number> | undefined): Record<string, number> {
  if (!input) {
    return {};
  }

  const out: Record<string, number> = {};
  for (const [key, value] of Object.entries(input)) {
    if (typeof value === "number" && Number.isFinite(value)) {
      out[sanitizeKey(key)] = round(clamp01(value));
    }
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
