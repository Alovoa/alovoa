import { describe, expect, it } from "vitest";
import { OrchestratorConfig } from "../src/config";
import { OrchestratorService } from "../src/providers/orchestratorService";
import { MediaProvider } from "../src/providers/interfaces";

function baseConfig(): OrchestratorConfig {
  return {
    nodeEnv: "test",
    host: "127.0.0.1",
    port: 8081,
    requestTimeoutMs: 2000,
    pythonMediaServiceUrl: "http://127.0.0.1:9999",
    enableLocalModeration: true,
    nsfwThreshold: 0.6,
    nsfwBaseline: 0.02,
    nsfwUseOpenNsfw2: false,
    nsfwUseNudeNet: false,
    nsfwUseClipNsfw: false,
    nsfwOpenNsfw2Cmd: "",
    nsfwNudeNetCmd: "",
    nsfwClipCmd: "",
    moderationSegmentPolicies: {},
    nsfwCalibrationEnabled: false,
    nsfwCalibrationFile: "",
    moderationUseUnleashFlag: false,
    moderationUnleashFlagName: "moderation_local_enabled",
    moderationOpenFgaEnforce: false,
    moderationOpenFgaObject: "feature:moderation_local",
    moderationOpenFgaRelation: "can_use",
    enableLocalTextModeration: false,
    textModerationCmd: "",
    textModerationModel: "multilingual",
    textModerationBlockThreshold: 0.72,
    textModerationWarnThreshold: 0.52,
    textModerationSegmentPolicies: {},
    textModerationUseUnleashFlag: false,
    textModerationUnleashFlagName: "moderation_text_local_enabled",
    textModerationOpenFgaEnforce: false,
    textModerationOpenFgaObject: "feature:moderation_text_local",
    textModerationOpenFgaRelation: "can_use",
    enableLocalFaceQuality: false,
    faceQualityCmd: "",
    faceQualityProviderName: "faceqan",
    faceQualityModelVersion: "faceqan_v1",
    faceQualityUseUnleashFlag: false,
    faceQualityUnleashFlagName: "face_quality_gate_enabled",
    faceQualityOpenFgaEnforce: false,
    faceQualityOpenFgaObject: "feature:face_quality_gate",
    faceQualityOpenFgaRelation: "can_use",
    safetySignalLogEnabled: false,
    safetySignalLogFile: "",
    enableLocalAttractiveness: false,
    attractivenessBaseline: 0.5,
    attractivenessFrontWeight: 0.7,
    attractivenessSideWeight: 0.3,
    attractivenessCmd: "",
    attractivenessProviderName: "local",
    attractivenessModelVersion: "local_v1",
    attractivenessProviderCommands: [],
    attractivenessProviderAbRates: {},
    attractivenessCalibrationEnabled: false,
    attractivenessCalibrationFile: "",
    attractivenessUseUnleashFlag: false,
    attractivenessUnleashFlagName: "attractiveness_local_enabled",
    attractivenessOpenFgaEnforce: false,
    attractivenessOpenFgaObject: "feature:attractiveness_local",
    attractivenessOpenFgaRelation: "can_use",
    qdrantAttractivenessHintEnabled: false,
    qdrantAttractivenessCollection: "attractiveness_hints",
    qdrantAttractivenessHintMaxDelta: 0.08,
    qdrantCandidateEnrichmentEnabled: false,
    qdrantCandidateCollection: "candidate_enrichment",
    qdrantCandidateEnrichmentLimit: 20,
    reposRoot: "",
    openSourceRepoRefs: [],
    enableQdrant: false,
    qdrantUrl: "",
    qdrantApiKey: "",
    enableUnleash: false,
    unleashUrl: "",
    unleashApiToken: "",
    unleashAppName: "test",
    enableOpenFga: false,
    openFgaUrl: "",
    openFgaStoreId: "",
    openFgaAuthzModelId: "",
    openFgaApiToken: "",
  };
}

function proxyStub(): MediaProvider {
  return {
    async getLivenessChallenges() {
      return { session_id: "s", challenges: [], timeout: 10, total_timeout: 30 };
    },
    async verifyFace() {
      return { verified: true, face_match_score: 1, liveness_score: 1, deepfake_score: 1, issues: [] };
    },
    async analyzeVideo() {
      return {};
    },
    async scoreAttractiveness() {
      return {
        score: 0.5,
        confidence: 0.4,
        provider: "proxy",
        model_version: "proxy_v1",
        signals: { proxy_used: 1 },
        repo_refs: [],
      };
    },
    async moderateImage() {
      return {
        is_safe: true,
        nsfw_score: 0.11,
        confidence: 0.65,
        action: "ALLOW",
        provider: "proxy",
        categories: { nsfw: 0.11 },
        signals: { proxy_used: 1 },
        repo_refs: [],
      };
    },
    async moderateText() {
      return {
        is_allowed: true,
        decision: "ALLOW",
        toxicity_score: 0.03,
        max_label: "toxicity",
        blocked_categories: [],
        labels: { toxicity: 0.03 },
        reason: null,
        provider: "proxy",
        model_version: "proxy_v1",
        signals: { proxy_used: 1 },
        repo_refs: [],
      };
    },
    async scoreFaceQuality() {
      return {
        quality_score: 0.44,
        confidence: 0.5,
        provider: "proxy",
        model_version: "proxy_v1",
        signals: { proxy_used: 1 },
        repo_refs: [],
      };
    },
  };
}

describe("orchestrator moderation and enrichment", () => {
  it("falls back to proxy moderation when unleash gate is disabled", async () => {
    const config = baseConfig();
    config.moderationUseUnleashFlag = true;

    const service = new OrchestratorService(config, proxyStub(), {
      localModeration: {
        async moderate() {
          throw new Error("local moderation should not run");
        },
      } as any,
      unleashClient: {
        async health() {
          return { ok: true, message: "ok" };
        },
        async isFeatureEnabled() {
          return false;
        },
      },
      openFgaClient: {
        async health() {
          return { ok: false, message: "disabled" };
        },
        async checkAccess() {
          return true;
        },
      },
      qdrantClient: {
        async health() {
          return { ok: false, message: "disabled" };
        },
        async getAttractivenessHint() {
          return null;
        },
        async getCandidateEnrichment() {
          return null;
        },
      },
    });

    const response = await service.moderateImage({
      user_id: 1001,
      image_url: "https://example.com/image.jpg",
    });

    expect(response.provider).toBe("proxy");
    expect(response.signals.proxy_used).toBe(1);
  });

  it("falls back to proxy moderation when openfga denies access", async () => {
    const config = baseConfig();
    config.moderationOpenFgaEnforce = true;

    const service = new OrchestratorService(config, proxyStub(), {
      localModeration: {
        async moderate() {
          throw new Error("local moderation should not run");
        },
      } as any,
      unleashClient: {
        async health() {
          return { ok: false, message: "disabled" };
        },
        async isFeatureEnabled() {
          return true;
        },
      },
      openFgaClient: {
        async health() {
          return { ok: true, message: "ok" };
        },
        async checkAccess() {
          return false;
        },
      },
      qdrantClient: {
        async health() {
          return { ok: false, message: "disabled" };
        },
        async getAttractivenessHint() {
          return null;
        },
        async getCandidateEnrichment() {
          return null;
        },
      },
    });

    const response = await service.moderateImage({
      user_id: 1002,
      image_url: "https://example.com/image.jpg",
    });

    expect(response.provider).toBe("proxy");
  });

  it("returns qdrant candidate enrichment list when enabled", async () => {
    const config = baseConfig();
    config.qdrantCandidateEnrichmentEnabled = true;
    config.qdrantCandidateCollection = "candidate_enrichment";
    config.qdrantCandidateEnrichmentLimit = 5;

    const service = new OrchestratorService(config, proxyStub(), {
      localModeration: {
        async moderate() {
          return {
            is_safe: true,
            nsfw_score: 0.01,
            confidence: 0.99,
            action: "ALLOW",
            provider: "local",
            categories: { nsfw: 0.01 },
            signals: {},
            repo_refs: [],
          };
        },
      } as any,
      unleashClient: {
        async health() {
          return { ok: false, message: "disabled" };
        },
        async isFeatureEnabled() {
          return true;
        },
      },
      openFgaClient: {
        async health() {
          return { ok: false, message: "disabled" };
        },
        async checkAccess() {
          return true;
        },
      },
      qdrantClient: {
        async health() {
          return { ok: true, message: "ok" };
        },
        async getAttractivenessHint() {
          return null;
        },
        async getCandidateEnrichment() {
          return { candidateIds: [41, 22, 7], source: "qdrant:recommend_v2" };
        },
      },
    });

    const response = await service.getCandidateEnrichment({
      userId: 55,
      segmentKey: "segment:test",
      limit: 3,
    });

    expect(response.user_id).toBe(55);
    expect(response.segment_key).toBe("segment:test");
    expect(response.source).toBe("qdrant:recommend_v2");
    expect(response.candidate_ids).toEqual([41, 22, 7]);
  });

  it("uses local text moderation when enabled", async () => {
    const config = baseConfig();
    config.enableLocalTextModeration = true;

    const service = new OrchestratorService(config, proxyStub(), {
      localTextModeration: {
        async moderate() {
          return {
            is_allowed: false,
            decision: "BLOCK",
            toxicity_score: 0.91,
            max_label: "toxicity",
            blocked_categories: ["TOXICITY"],
            labels: { toxicity: 0.91 },
            reason: "blocked",
            provider: "detoxify",
            model_version: "detoxify_test",
            signals: { local_used: 1 },
            repo_refs: [],
          };
        },
      } as any,
    });

    const response = await service.moderateText({
      user_id: 22,
      text: "example",
      segment_key: "segment:test",
    });

    expect(response.provider).toBe("detoxify");
    expect(response.decision).toBe("BLOCK");
  });

  it("uses local face quality when enabled", async () => {
    const config = baseConfig();
    config.enableLocalFaceQuality = true;

    const service = new OrchestratorService(config, proxyStub(), {
      localFaceQuality: {
        async score() {
          return {
            quality_score: 0.71,
            confidence: 0.83,
            provider: "faceqan",
            model_version: "faceqan_v1",
            signals: { local_used: 1 },
            repo_refs: [],
          };
        },
      } as any,
    });

    const response = await service.scoreFaceQuality({
      user_id: 22,
      image_url: "https://example.com/image.jpg",
      segment_key: "segment:test",
    });

    expect(response.provider).toBe("faceqan");
    expect(response.quality_score).toBe(0.71);
  });
});
