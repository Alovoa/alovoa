import { OrchestratorConfig } from "../config";
import { OpenFgaClient } from "../integrations/openFgaClient";
import { QdrantClient } from "../integrations/qdrantClient";
import { UnleashClient } from "../integrations/unleashClient";
import {
  AttractivenessRequest,
  AttractivenessResponse,
  FaceQualityRequest,
  FaceQualityResponse,
  LivenessRequest,
  LivenessResponse,
  ModerationRequest,
  ModerationResponse,
  TextModerationRequest,
  TextModerationResponse,
  VerificationRequest,
  VerificationResponse,
  VideoAnalysisRequest,
  VideoAnalysisResponse,
} from "../types/contracts";
import { SafetySignalLogger } from "../utils/safetySignalLogger";
import { LocalAttractivenessProvider } from "./localAttractiveness";
import { LocalFaceQualityProvider } from "./localFaceQuality";
import { LocalModerationProvider } from "./localModeration";
import { LocalTextModerationProvider } from "./localTextModeration";
import { MediaProvider } from "./interfaces";

function clamp01(value: number): number {
  if (!Number.isFinite(value)) return 0;
  if (value < 0) return 0;
  if (value > 1) return 1;
  return value;
}

function round(value: number): number {
  return Math.round(value * 1_000_000) / 1_000_000;
}

interface OrchestratorDeps {
  localModeration?: LocalModerationProvider;
  localAttractiveness?: LocalAttractivenessProvider;
  localTextModeration?: LocalTextModerationProvider;
  localFaceQuality?: LocalFaceQualityProvider;
  qdrantClient?: QdrantClientLike;
  unleashClient?: UnleashClientLike;
  openFgaClient?: OpenFgaClientLike;
  safetySignalLogger?: SafetySignalLogger;
}

interface QdrantClientLike {
  health(): Promise<{ ok: boolean; message: string }>;
  getAttractivenessHint(input: {
    userId: number;
    segmentKey?: string;
    collection: string;
    maxAbsDelta: number;
  }): Promise<{ boost: number; source: string } | null>;
  getCandidateEnrichment(input: {
    userId: number;
    segmentKey?: string;
    collection: string;
    limit: number;
  }): Promise<{ candidateIds: number[]; source: string } | null>;
}

interface UnleashClientLike {
  health(): Promise<{ ok: boolean; message: string }>;
  isFeatureEnabled(featureName: string, context: Record<string, string>): Promise<boolean>;
}

interface OpenFgaClientLike {
  health(): Promise<{ ok: boolean; message: string }>;
  checkAccess(input: { user: string; relation: string; object: string }): Promise<boolean>;
}

export class OrchestratorService {
  private readonly config: OrchestratorConfig;
  private readonly proxy: MediaProvider;
  private readonly localModeration: LocalModerationProvider;
  private readonly localAttractiveness: LocalAttractivenessProvider;
  private readonly localTextModeration: LocalTextModerationProvider;
  private readonly localFaceQuality: LocalFaceQualityProvider;
  private readonly qdrantClient: QdrantClientLike;
  private readonly unleashClient: UnleashClientLike;
  private readonly openFgaClient: OpenFgaClientLike;
  private readonly safetySignalLogger: SafetySignalLogger;

  constructor(config: OrchestratorConfig, proxy: MediaProvider, deps?: OrchestratorDeps) {
    this.config = config;
    this.proxy = proxy;
    this.localModeration = deps?.localModeration || new LocalModerationProvider(config);
    this.localAttractiveness = deps?.localAttractiveness || new LocalAttractivenessProvider(config);
    this.localTextModeration = deps?.localTextModeration || new LocalTextModerationProvider(config);
    this.localFaceQuality = deps?.localFaceQuality || new LocalFaceQualityProvider(config);
    this.qdrantClient = deps?.qdrantClient || new QdrantClient(config);
    this.unleashClient = deps?.unleashClient || new UnleashClient(config);
    this.openFgaClient = deps?.openFgaClient || new OpenFgaClient(config);
    this.safetySignalLogger =
      deps?.safetySignalLogger ||
      new SafetySignalLogger({
        enabled: config.safetySignalLogEnabled,
        filePath: config.safetySignalLogFile,
      });
  }

  async getLivenessChallenges(request: LivenessRequest): Promise<LivenessResponse> {
    return this.proxy.getLivenessChallenges(request);
  }

  async verifyFace(request: VerificationRequest): Promise<VerificationResponse> {
    return this.proxy.verifyFace(request);
  }

  async analyzeVideo(request: VideoAnalysisRequest): Promise<VideoAnalysisResponse> {
    return this.proxy.analyzeVideo(request);
  }

  async scoreAttractiveness(request: AttractivenessRequest): Promise<AttractivenessResponse> {
    if (!this.config.enableLocalAttractiveness) {
      const fallback = await this.proxy.scoreAttractiveness(request);
      this.logAttractivenessEvent("proxy", request, fallback);
      return fallback;
    }

    if (!(await this.allowLocalAttractiveness(request))) {
      const fallback = await this.proxy.scoreAttractiveness(request);
      this.logAttractivenessEvent("proxy", request, fallback);
      return fallback;
    }

    try {
      const localResponse = await this.localAttractiveness.score(request);
      const qualityAdjusted = await this.applyFaceQualityToAttractiveness(localResponse, request);
      const qdrantAdjusted = await this.applyQdrantAttractivenessHint(qualityAdjusted, request);
      this.logAttractivenessEvent("local", request, qdrantAdjusted);
      return qdrantAdjusted;
    } catch {
      const fallback = await this.proxy.scoreAttractiveness(request);
      this.logAttractivenessEvent("proxy", request, fallback);
      return fallback;
    }
  }

  async moderateImage(request: ModerationRequest): Promise<ModerationResponse> {
    if (this.config.enableLocalModeration && (await this.allowLocalModeration(request))) {
      try {
        const response = await this.localModeration.moderate(request);
        const qualityAdjusted = await this.applyFaceQualityToModeration(response, request);
        this.logModerationEvent("local", request, qualityAdjusted);
        return qualityAdjusted;
      } catch {
        // fallback to proxy
      }
    }
    const fallback = await this.proxy.moderateImage(request);
    this.logModerationEvent("proxy", request, fallback);
    return fallback;
  }

  async moderateText(request: TextModerationRequest): Promise<TextModerationResponse> {
    if (!this.config.enableLocalTextModeration) {
      if (this.proxy.moderateText) {
        try {
          const fallback = await this.proxy.moderateText(request);
          this.logTextModerationEvent("proxy", request, fallback);
          return fallback;
        } catch {
          const baseline = this.defaultTextModerationFallback(request);
          this.logTextModerationEvent("proxy", request, baseline);
          return baseline;
        }
      }
      const baseline = this.defaultTextModerationFallback(request);
      this.logTextModerationEvent("proxy", request, baseline);
      return baseline;
    }

    if (!(await this.allowLocalTextModeration(request))) {
      if (this.proxy.moderateText) {
        try {
          const fallback = await this.proxy.moderateText(request);
          this.logTextModerationEvent("proxy", request, fallback);
          return fallback;
        } catch {
          const baseline = this.defaultTextModerationFallback(request);
          this.logTextModerationEvent("proxy", request, baseline);
          return baseline;
        }
      }
      const baseline = this.defaultTextModerationFallback(request);
      this.logTextModerationEvent("proxy", request, baseline);
      return baseline;
    }

    try {
      const local = await this.localTextModeration.moderate(request);
      this.logTextModerationEvent("local", request, local);
      return local;
    } catch {
      if (this.proxy.moderateText) {
        try {
          const fallback = await this.proxy.moderateText(request);
          this.logTextModerationEvent("proxy", request, fallback);
          return fallback;
        } catch {
          const baseline = this.defaultTextModerationFallback(request);
          this.logTextModerationEvent("proxy", request, baseline);
          return baseline;
        }
      }
      const baseline = this.defaultTextModerationFallback(request);
      this.logTextModerationEvent("proxy", request, baseline);
      return baseline;
    }
  }

  async scoreFaceQuality(request: FaceQualityRequest): Promise<FaceQualityResponse> {
    if (!request.image_base64 && !request.image_url) {
      throw new Error("Image is required");
    }

    if (!this.config.enableLocalFaceQuality) {
      if (this.proxy.scoreFaceQuality) {
        try {
          const fallback = await this.proxy.scoreFaceQuality(request);
          this.logFaceQualityEvent("proxy", request, fallback);
          return fallback;
        } catch {
          const baseline = this.defaultFaceQualityFallback();
          this.logFaceQualityEvent("proxy", request, baseline);
          return baseline;
        }
      }
      const baseline = this.defaultFaceQualityFallback();
      this.logFaceQualityEvent("proxy", request, baseline);
      return baseline;
    }

    if (!(await this.allowLocalFaceQuality(request))) {
      if (this.proxy.scoreFaceQuality) {
        try {
          const fallback = await this.proxy.scoreFaceQuality(request);
          this.logFaceQualityEvent("proxy", request, fallback);
          return fallback;
        } catch {
          const baseline = this.defaultFaceQualityFallback();
          this.logFaceQualityEvent("proxy", request, baseline);
          return baseline;
        }
      }
      const baseline = this.defaultFaceQualityFallback();
      this.logFaceQualityEvent("proxy", request, baseline);
      return baseline;
    }

    try {
      const local = await this.localFaceQuality.score(request);
      this.logFaceQualityEvent("local", request, local);
      return local;
    } catch {
      if (this.proxy.scoreFaceQuality) {
        try {
          const fallback = await this.proxy.scoreFaceQuality(request);
          this.logFaceQualityEvent("proxy", request, fallback);
          return fallback;
        } catch {
          const baseline = this.defaultFaceQualityFallback();
          this.logFaceQualityEvent("proxy", request, baseline);
          return baseline;
        }
      }
      const baseline = this.defaultFaceQualityFallback();
      this.logFaceQualityEvent("proxy", request, baseline);
      return baseline;
    }
  }

  async transcribeVideo(request: Record<string, unknown>): Promise<Record<string, unknown>> {
    if (!this.proxy.transcribeVideo) {
      throw new Error("transcribeVideo_not_supported");
    }
    return this.proxy.transcribeVideo(request);
  }

  async analyzeTranscript(request: Record<string, unknown>): Promise<Record<string, unknown>> {
    if (!this.proxy.analyzeTranscript) {
      throw new Error("analyzeTranscript_not_supported");
    }
    return this.proxy.analyzeTranscript(request);
  }

  async integrationStatus(): Promise<Record<string, unknown>> {
    const [qdrant, unleash, openfga] = await Promise.all([
      this.qdrantClient.health(),
      this.unleashClient.health(),
      this.openFgaClient.health(),
    ]);

    return {
      qdrant,
      unleash,
      openfga,
      flags: {
        localModeration: this.config.enableLocalModeration,
        localTextModeration: this.config.enableLocalTextModeration,
        localFaceQuality: this.config.enableLocalFaceQuality,
        localAttractiveness: this.config.enableLocalAttractiveness,
        moderationOpenFgaEnforce: this.config.moderationOpenFgaEnforce,
        moderationUseUnleashFlag: this.config.moderationUseUnleashFlag,
        textModerationOpenFgaEnforce: this.config.textModerationOpenFgaEnforce,
        textModerationUseUnleashFlag: this.config.textModerationUseUnleashFlag,
        faceQualityOpenFgaEnforce: this.config.faceQualityOpenFgaEnforce,
        faceQualityUseUnleashFlag: this.config.faceQualityUseUnleashFlag,
        qdrantCandidateEnrichmentEnabled: this.config.qdrantCandidateEnrichmentEnabled,
        qdrantAttractivenessHintEnabled: this.config.qdrantAttractivenessHintEnabled,
      },
    };
  }

  async getCandidateEnrichment(input: {
    userId: number;
    segmentKey?: string;
    limit?: number;
  }): Promise<{ user_id: number; segment_key: string; source: string; candidate_ids: number[] }> {
    const segmentKey = input.segmentKey || "default";
    const fallback = {
      user_id: input.userId,
      segment_key: segmentKey,
      source: "disabled",
      candidate_ids: [] as number[],
    };

    if (!this.config.qdrantCandidateEnrichmentEnabled) {
      return fallback;
    }

    const result = await this.qdrantClient.getCandidateEnrichment({
      userId: input.userId,
      segmentKey: input.segmentKey,
      collection: this.config.qdrantCandidateCollection,
      limit: input.limit ?? this.config.qdrantCandidateEnrichmentLimit,
    });

    if (!result) {
      return fallback;
    }

    return {
      user_id: input.userId,
      segment_key: segmentKey,
      source: result.source,
      candidate_ids: result.candidateIds,
    };
  }

  private async allowLocalAttractiveness(request: AttractivenessRequest): Promise<boolean> {
    if (this.config.attractivenessUseUnleashFlag) {
      const enabledByFlag = await this.unleashClient.isFeatureEnabled(
        this.config.attractivenessUnleashFlagName,
        {
          userId: request.user_id != null ? String(request.user_id) : "anonymous",
          segmentKey: request.segment_key || "default",
        },
      );
      if (!enabledByFlag) {
        return false;
      }
    }

    if (this.config.attractivenessOpenFgaEnforce && request.user_id != null) {
      const allowed = await this.openFgaClient.checkAccess({
        user: `user:${request.user_id}`,
        relation: this.config.attractivenessOpenFgaRelation,
        object: this.config.attractivenessOpenFgaObject,
      });
      if (!allowed) {
        return false;
      }
    }

    return true;
  }

  private async allowLocalModeration(request: ModerationRequest): Promise<boolean> {
    if (this.config.moderationUseUnleashFlag) {
      const enabledByFlag = await this.unleashClient.isFeatureEnabled(
        this.config.moderationUnleashFlagName,
        {
          userId: request.user_id != null ? String(request.user_id) : "anonymous",
          segmentKey: request.segment_key || "default",
        },
      );
      if (!enabledByFlag) {
        return false;
      }
    }

    if (this.config.moderationOpenFgaEnforce && request.user_id != null) {
      const allowed = await this.openFgaClient.checkAccess({
        user: `user:${request.user_id}`,
        relation: this.config.moderationOpenFgaRelation,
        object: this.config.moderationOpenFgaObject,
      });
      if (!allowed) {
        return false;
      }
    }

    return true;
  }

  private async allowLocalTextModeration(request: TextModerationRequest): Promise<boolean> {
    if (this.config.textModerationUseUnleashFlag) {
      const enabledByFlag = await this.unleashClient.isFeatureEnabled(
        this.config.textModerationUnleashFlagName,
        {
          userId: request.user_id != null ? String(request.user_id) : "anonymous",
          segmentKey: request.segment_key || "default",
        },
      );
      if (!enabledByFlag) {
        return false;
      }
    }

    if (this.config.textModerationOpenFgaEnforce && request.user_id != null) {
      const allowed = await this.openFgaClient.checkAccess({
        user: `user:${request.user_id}`,
        relation: this.config.textModerationOpenFgaRelation,
        object: this.config.textModerationOpenFgaObject,
      });
      if (!allowed) {
        return false;
      }
    }
    return true;
  }

  private async allowLocalFaceQuality(request: FaceQualityRequest): Promise<boolean> {
    if (this.config.faceQualityUseUnleashFlag) {
      const enabledByFlag = await this.unleashClient.isFeatureEnabled(
        this.config.faceQualityUnleashFlagName,
        {
          userId: request.user_id != null ? String(request.user_id) : "anonymous",
          segmentKey: request.segment_key || "default",
        },
      );
      if (!enabledByFlag) {
        return false;
      }
    }

    if (this.config.faceQualityOpenFgaEnforce && request.user_id != null) {
      const allowed = await this.openFgaClient.checkAccess({
        user: `user:${request.user_id}`,
        relation: this.config.faceQualityOpenFgaRelation,
        object: this.config.faceQualityOpenFgaObject,
      });
      if (!allowed) {
        return false;
      }
    }
    return true;
  }

  private async applyQdrantAttractivenessHint(
    response: AttractivenessResponse,
    request: AttractivenessRequest,
  ): Promise<AttractivenessResponse> {
    if (!this.config.qdrantAttractivenessHintEnabled || request.user_id == null) {
      return response;
    }

    const hint = await this.qdrantClient.getAttractivenessHint({
      userId: request.user_id,
      segmentKey: request.segment_key,
      collection: this.config.qdrantAttractivenessCollection,
      maxAbsDelta: this.config.qdrantAttractivenessHintMaxDelta,
    });

    if (!hint) {
      return {
        ...response,
        signals: {
          ...response.signals,
          qdrant_hint_applied: 0,
        },
      };
    }

    const adjusted = clamp01(response.score + hint.boost);
    const providerVersions = response.provider_versions
      ? { ...response.provider_versions, qdrant_hint: hint.source }
      : { qdrant_hint: hint.source };

    return {
      ...response,
      score: round(adjusted),
      provider_versions: providerVersions,
      signals: {
        ...response.signals,
        qdrant_hint_applied: 1,
        qdrant_hint_delta: round(hint.boost),
        qdrant_hint_pre_score: round(response.score),
        qdrant_hint_post_score: round(adjusted),
      },
    };
  }

  private async applyFaceQualityToAttractiveness(
    response: AttractivenessResponse,
    request: AttractivenessRequest,
  ): Promise<AttractivenessResponse> {
    if (!this.config.enableLocalFaceQuality) {
      return response;
    }

    try {
      const quality = await this.scoreFaceQuality({
        user_id: request.user_id,
        image_base64: request.front_image_base64,
        image_url: request.front_image_url,
        surface: "attractiveness_front",
        segment_key: request.segment_key,
      });
      const adjustedScore = clamp01((0.85 * response.score) + (0.15 * quality.quality_score));
      const adjustedConfidence = clamp01(response.confidence * (0.6 + 0.4 * quality.quality_score));
      const providerVersions = response.provider_versions
        ? { ...response.provider_versions, face_quality: quality.model_version }
        : { face_quality: quality.model_version };

      return {
        ...response,
        score: round(adjustedScore),
        confidence: round(adjustedConfidence),
        provider_versions: providerVersions,
        signals: {
          ...response.signals,
          face_quality_applied: 1,
          face_quality_score: round(quality.quality_score),
          face_quality_confidence: round(quality.confidence),
          pre_face_quality_score: round(response.score),
          post_face_quality_score: round(adjustedScore),
        },
      };
    } catch {
      return response;
    }
  }

  private async applyFaceQualityToModeration(
    response: ModerationResponse,
    request: ModerationRequest,
  ): Promise<ModerationResponse> {
    if (!this.config.enableLocalFaceQuality) {
      return response;
    }

    try {
      const quality = await this.scoreFaceQuality({
        user_id: request.user_id,
        image_base64: request.image_base64,
        image_url: request.image_url,
        surface: request.image_type || "profile",
        segment_key: request.segment_key,
      });
      const adjustedConfidence = clamp01(response.confidence * (0.5 + 0.5 * quality.quality_score));
      return {
        ...response,
        confidence: round(adjustedConfidence),
        signals: {
          ...response.signals,
          face_quality_applied: 1,
          face_quality_score: round(quality.quality_score),
          face_quality_confidence: round(quality.confidence),
        },
      };
    } catch {
      return response;
    }
  }

  private defaultTextModerationFallback(request: TextModerationRequest): TextModerationResponse {
    const score = 0.0;
    return {
      is_allowed: true,
      decision: "ALLOW",
      toxicity_score: score,
      max_label: null,
      blocked_categories: [],
      labels: {
        toxicity: score,
      },
      reason: null,
      provider: "proxy_unavailable_fallback",
      model_version: "baseline_v1",
      signals: {
        fallback_used: 1,
      },
      repo_refs: this.config.openSourceRepoRefs,
    };
  }

  private defaultFaceQualityFallback(): FaceQualityResponse {
    return {
      quality_score: 0.5,
      confidence: 0.0,
      provider: "proxy_unavailable_fallback",
      model_version: "baseline_v1",
      signals: {
        fallback_used: 1,
      },
      repo_refs: this.config.openSourceRepoRefs,
    };
  }

  private logModerationEvent(
    mode: "local" | "proxy",
    request: ModerationRequest,
    response: ModerationResponse,
  ): void {
    this.safetySignalLogger.log({
      ts: new Date().toISOString(),
      kind: "moderation",
      mode,
      user_id: request.user_id ?? null,
      segment_key: request.segment_key ?? "default",
      provider: response.provider,
      action: response.action,
      nsfw_score: response.nsfw_score,
      confidence: response.confidence,
      signals: response.signals,
      categories: response.categories,
    });
  }

  private logAttractivenessEvent(
    mode: "local" | "proxy",
    request: AttractivenessRequest,
    response: AttractivenessResponse,
  ): void {
    this.safetySignalLogger.log({
      ts: new Date().toISOString(),
      kind: "attractiveness",
      mode,
      user_id: request.user_id ?? null,
      segment_key: request.segment_key ?? "default",
      provider: response.provider,
      score: response.score,
      confidence: response.confidence,
      provider_versions: response.provider_versions ?? {},
      signals: response.signals,
    });
  }

  private logTextModerationEvent(
    mode: "local" | "proxy",
    request: TextModerationRequest,
    response: TextModerationResponse,
  ): void {
    this.safetySignalLogger.log({
      ts: new Date().toISOString(),
      kind: "text_moderation",
      mode,
      user_id: request.user_id ?? null,
      segment_key: request.segment_key ?? "default",
      provider: response.provider,
      action: response.decision,
      toxicity_score: response.toxicity_score,
      max_label: response.max_label ?? "toxicity",
      blocked_categories: response.blocked_categories,
      signals: response.signals,
    });
  }

  private logFaceQualityEvent(
    mode: "local" | "proxy",
    request: FaceQualityRequest,
    response: FaceQualityResponse,
  ): void {
    this.safetySignalLogger.log({
      ts: new Date().toISOString(),
      kind: "face_quality",
      mode,
      user_id: request.user_id ?? null,
      segment_key: request.segment_key ?? "default",
      provider: response.provider,
      score: response.quality_score,
      confidence: response.confidence,
      signals: response.signals,
    });
  }
}
