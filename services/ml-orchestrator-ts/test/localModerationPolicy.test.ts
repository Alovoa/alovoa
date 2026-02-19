import { describe, expect, it } from "vitest";
import { mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { OrchestratorConfig } from "../src/config";
import { LocalModerationProvider } from "../src/providers/localModeration";

const TEST_IMAGE_BASE64 =
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO2fMtkAAAAASUVORK5CYII=";

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
    nsfwUseOpenNsfw2: true,
    nsfwUseNudeNet: false,
    nsfwUseClipNsfw: false,
    nsfwOpenNsfw2Cmd:
      "node -e \"console.log(JSON.stringify({nsfw_score:0.65,confidence:0.9,categories:{explicit:0.65},signals:{model:1}}))\"",
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
    attractivenessProviderName: "external",
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

describe("local moderation policies", () => {
  it("applies per-segment threshold overrides", async () => {
    const config = baseConfig();
    config.moderationSegmentPolicies = {
      "segment:strict": {
        threshold: 0.5,
      },
    };

    const provider = new LocalModerationProvider(config);
    const strict = await provider.moderate({
      image_base64: TEST_IMAGE_BASE64,
      segment_key: "segment:strict",
    });
    const defaultSeg = await provider.moderate({
      image_base64: TEST_IMAGE_BASE64,
      segment_key: "segment:default",
    });

    expect(strict.action).toBe("BLOCK");
    expect(strict.signals.threshold).toBe(0.5);
    expect(strict.signals.segment_policy_applied).toBe(1);

    expect(defaultSeg.action).toBe("ALLOW");
    expect(defaultSeg.signals.threshold).toBe(0.6);
    expect(defaultSeg.signals.segment_policy_applied).toBe(0);
  });

  it("applies calibration threshold when available", async () => {
    const config = baseConfig();
    const tmpRoot = mkdtempSync(path.join(tmpdir(), "aura-nsfw-calib-"));
    const calibrationPath = path.join(tmpRoot, "nsfw_thresholds.json");
    writeFileSync(
      calibrationPath,
      JSON.stringify({
        type: "nsfw_thresholds_v1",
        thresholds: {
          "segment:strict": 0.4,
          "*": 0.6,
        },
      }),
      "utf-8",
    );
    config.nsfwCalibrationEnabled = true;
    config.nsfwCalibrationFile = calibrationPath;

    try {
      const provider = new LocalModerationProvider(config);
      const strict = await provider.moderate({
        image_base64: TEST_IMAGE_BASE64,
        segment_key: "segment:strict",
      });
      const fallback = await provider.moderate({
        image_base64: TEST_IMAGE_BASE64,
        segment_key: "segment:unknown",
      });

      expect(strict.action).toBe("BLOCK");
      expect(strict.signals.threshold).toBe(0.4);
      expect(strict.signals.calibration_applied).toBe(1);

      expect(fallback.action).toBe("ALLOW");
      expect(fallback.signals.threshold).toBe(0.6);
      expect(fallback.signals.calibration_applied).toBe(1);
    } finally {
      rmSync(tmpRoot, { recursive: true, force: true });
    }
  });

  it("uses clip nsfw provider when enabled", async () => {
    const config = baseConfig();
    config.nsfwUseClipNsfw = true;
    config.nsfwClipCmd =
      "node -e \"console.log(JSON.stringify({nsfw_score:0.95,confidence:0.9,categories:{nsfw:0.95},signals:{clip:1}}))\"";
    config.moderationSegmentPolicies = {
      "*": {
        openNsfw2Weight: 0.1,
        nudeNetWeight: 0.1,
        clipNsfwWeight: 0.8,
      },
    };

    const provider = new LocalModerationProvider(config);
    const response = await provider.moderate({
      image_base64: TEST_IMAGE_BASE64,
      segment_key: "segment:test",
    });

    expect(response.action).toBe("BLOCK");
    expect(response.signals.clipnsfw_weight).toBeCloseTo(0.888889, 6);
  });
});
