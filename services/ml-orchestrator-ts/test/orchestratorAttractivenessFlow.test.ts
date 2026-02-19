import { describe, expect, it } from "vitest";
import { OrchestratorConfig } from "../src/config";
import { OrchestratorService } from "../src/providers/orchestratorService";
import { MediaProvider } from "../src/providers/interfaces";
import { AttractivenessResponse } from "../src/types/contracts";

function baseConfig(): OrchestratorConfig {
  return {
    nodeEnv: "test",
    host: "127.0.0.1",
    port: 8081,
    requestTimeoutMs: 2000,
    pythonMediaServiceUrl: "http://127.0.0.1:9999",
    enableLocalModeration: false,
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
    enableLocalAttractiveness: true,
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
        score: 0.42,
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
        nsfw_score: 0.02,
        confidence: 0.5,
        action: "ALLOW",
        provider: "proxy",
        categories: { nsfw: 0.02 },
        signals: {},
        repo_refs: [],
      };
    },
  };
}

function localResponse(score = 0.5): AttractivenessResponse {
  return {
    score,
    confidence: 0.9,
    provider: "local",
    model_version: "local_v1",
    provider_versions: {
      primary: "local_v1",
    },
    signals: {
      local_used: 1,
    },
    repo_refs: [],
  };
}

describe("orchestrator attractiveness flow", () => {
  it("falls back to proxy when unleash gate is disabled", async () => {
    const config = baseConfig();
    config.attractivenessUseUnleashFlag = true;

    const service = new OrchestratorService(config, proxyStub(), {
      localAttractiveness: {
        async score() {
          throw new Error("local should not run");
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

    const response = await service.scoreAttractiveness({
      user_id: 123,
      front_image_url: "https://example.com/front.jpg",
    });

    expect(response.provider).toBe("proxy");
    expect(response.score).toBe(0.42);
  });

  it("falls back to proxy when openfga enforcement denies access", async () => {
    const config = baseConfig();
    config.attractivenessOpenFgaEnforce = true;

    const service = new OrchestratorService(config, proxyStub(), {
      localAttractiveness: {
        async score() {
          throw new Error("local should not run");
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

    const response = await service.scoreAttractiveness({
      user_id: 456,
      front_image_url: "https://example.com/front.jpg",
    });

    expect(response.provider).toBe("proxy");
    expect(response.score).toBe(0.42);
  });

  it("applies qdrant hint to local attractiveness score", async () => {
    const config = baseConfig();
    config.qdrantAttractivenessHintEnabled = true;
    config.qdrantAttractivenessHintMaxDelta = 0.1;

    const service = new OrchestratorService(config, proxyStub(), {
      localAttractiveness: {
        async score() {
          return localResponse(0.5);
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
          return { boost: 0.07, source: "qdrant:test_model" };
        },
        async getCandidateEnrichment() {
          return null;
        },
      },
    });

    const response = await service.scoreAttractiveness({
      user_id: 77,
      front_image_url: "https://example.com/front.jpg",
    });

    expect(response.provider).toBe("local");
    expect(response.score).toBe(0.57);
    expect(response.signals.qdrant_hint_applied).toBe(1);
    expect(response.signals.qdrant_hint_delta).toBe(0.07);
    expect(response.provider_versions?.qdrant_hint).toBe("qdrant:test_model");
  });
});
