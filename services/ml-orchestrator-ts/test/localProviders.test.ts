import { describe, expect, it } from "vitest";
import { mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { OrchestratorConfig } from "../src/config";
import { LocalAttractivenessProvider } from "../src/providers/localAttractiveness";
import { OrchestratorService } from "../src/providers/orchestratorService";
import { MediaProvider } from "../src/providers/interfaces";

const TEST_IMAGE_BASE64 =
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO2fMtkAAAAASUVORK5CYII=";

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
    attractivenessProviderName: "oss-hybrid-test",
    attractivenessModelVersion: "oss_v1",
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

function createProxyStub(): MediaProvider {
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
        score: 0.61,
        confidence: 0.42,
        provider: "proxy",
        model_version: "proxy_v1",
        signals: {},
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

describe("local providers", () => {
  it("computes local attractiveness score via command payload", async () => {
    const config = baseConfig();
    config.attractivenessCmd =
      "node -e \"console.log(JSON.stringify({score:0.8,confidence:0.9,signals:{quality:0.4}}))\"";

    const provider = new LocalAttractivenessProvider(config);
    const response = await provider.score({
      front_image_base64: TEST_IMAGE_BASE64,
    });

    expect(response.provider).toBe("oss-hybrid-test");
    expect(response.score).toBe(0.74); // 0.2*0.5 + 0.8*0.8
    expect(response.confidence).toBe(0.9);
    expect(response.signals.primary_front_quality).toBe(0.4);
    expect(response.provider_versions?.primary).toBe("oss_v1");
  });

  it("falls back to proxy attractiveness when local command fails", async () => {
    const config = baseConfig();
    config.attractivenessCmd = "node -e \"process.exit(5)\"";

    const proxy = createProxyStub();
    const service = new OrchestratorService(config, proxy);

    const response = await service.scoreAttractiveness({
      front_image_base64: TEST_IMAGE_BASE64,
    });

    expect(response.provider).toBe("proxy");
    expect(response.score).toBe(0.61);
  });

  it("blends multiple configured attractiveness providers", async () => {
    const config = baseConfig();
    config.attractivenessCmd =
      "node -e \"console.log(JSON.stringify({score:0.6,confidence:0.9,signals:{quality:0.6}}))\"";
    config.attractivenessProviderCommands = [
      {
        name: "comboloss",
        cmd: "node -e \"console.log(JSON.stringify({score:0.9,confidence:0.5,signals:{beauty:0.8}}))\"",
        weight: 0.5,
      },
    ];

    const provider = new LocalAttractivenessProvider(config);
    const response = await provider.score({
      front_image_base64: TEST_IMAGE_BASE64,
    });

    expect(response.score).toBe(0.632174);
    expect(response.confidence).toBe(0.766667);
    expect(response.signals.primary_front_quality).toBe(0.6);
    expect(response.signals.comboloss_front_beauty).toBe(0.8);
    expect(response.signals.ensemble_provider_count).toBe(2);
    expect(response.provider_versions?.comboloss).toBe("unknown");
  });

  it("supports provider-level rollout toggles for A/B", async () => {
    const config = baseConfig();
    config.attractivenessCmd =
      "node -e \"console.log(JSON.stringify({score:0.5,confidence:0.6,signals:{quality:0.5},model_version:'primary_v2'}))\"";
    config.attractivenessProviderCommands = [
      {
        name: "comboloss",
        cmd: "node -e \"console.log(JSON.stringify({score:1,confidence:1,signals:{beauty:1}}))\"",
        weight: 1.0,
        modelVersion: "combo_v1",
      },
    ];
    config.attractivenessProviderAbRates = { comboloss: 0.0 };

    const provider = new LocalAttractivenessProvider(config);
    const response = await provider.score({
      user_id: 77,
      front_image_base64: TEST_IMAGE_BASE64,
    });

    expect(response.signals.ab_skip_comboloss).toBe(1);
    expect(response.signals.ensemble_provider_count).toBe(1);
    expect(response.score).toBe(0.5);
    expect(response.provider_versions?.primary).toBe("primary_v2");
    expect(response.provider_versions?.comboloss).toBeUndefined();
  });

  it("applies SCUT calibration mapping when enabled", async () => {
    const config = baseConfig();
    config.attractivenessCmd =
      "node -e \"console.log(JSON.stringify({score:0.5,confidence:0.9,signals:{quality:0.5}}))\"";

    const tmpRoot = mkdtempSync(path.join(tmpdir(), "aura-calib-"));
    const calibrationPath = path.join(tmpRoot, "scut.json");
    writeFileSync(
      calibrationPath,
      JSON.stringify({
        type: "score_percentile_v1",
        source: "SCUT-FBP5500",
        quantiles: [
          { q: 0.0, score: 0.0 },
          { q: 0.2, score: 0.3 },
          { q: 0.9, score: 0.5 },
          { q: 1.0, score: 1.0 },
        ],
      }),
      "utf-8",
    );

    config.attractivenessCalibrationEnabled = true;
    config.attractivenessCalibrationFile = calibrationPath;

    try {
      const provider = new LocalAttractivenessProvider(config);
      const response = await provider.score({
        front_image_base64: TEST_IMAGE_BASE64,
      });

      // Pre-calibration score is 0.5. With mapping above it becomes 0.9.
      expect(response.score).toBe(0.9);
      expect(response.signals.calibration_applied).toBe(1);
      expect(response.signals.calibration_input_score).toBe(0.5);
      expect(response.signals.calibration_output_score).toBe(0.9);
      expect(response.provider_versions?.scut_calibration).toBe("SCUT-FBP5500");
    } finally {
      rmSync(tmpRoot, { recursive: true, force: true });
    }
  });
});
