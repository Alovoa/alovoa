import { describe, expect, it } from "vitest";
import { OrchestratorConfig } from "../src/config";
import { LocalFaceQualityProvider } from "../src/providers/localFaceQuality";
import { LocalTextModerationProvider } from "../src/providers/localTextModeration";

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
    enableLocalTextModeration: true,
    textModerationCmd:
      "node -e \"console.log(JSON.stringify({labels:{toxicity:0.81,insult:0.65,threat:0.3},toxicity_score:0.81,max_label:'toxicity',provider:'detoxify',model_version:'detoxify_test'}))\"",
    textModerationModel: "multilingual",
    textModerationBlockThreshold: 0.72,
    textModerationWarnThreshold: 0.52,
    textModerationSegmentPolicies: {},
    textModerationUseUnleashFlag: false,
    textModerationUnleashFlagName: "moderation_text_local_enabled",
    textModerationOpenFgaEnforce: false,
    textModerationOpenFgaObject: "feature:moderation_text_local",
    textModerationOpenFgaRelation: "can_use",
    enableLocalFaceQuality: true,
    faceQualityCmd:
      "node -e \"console.log(JSON.stringify({quality_score:0.66,confidence:0.8,provider:'faceqan',model_version:'faceqan_test',signals:{sharpness:0.7}}))\"",
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

describe("local text moderation and face quality providers", () => {
  it("blocks content when toxicity is above threshold", async () => {
    const config = baseConfig();
    const provider = new LocalTextModerationProvider(config);

    const response = await provider.moderate({
      text: "this is toxic",
      segment_key: "segment:test",
    });

    expect(response.is_allowed).toBe(false);
    expect(response.decision).toBe("BLOCK");
    expect(response.toxicity_score).toBe(0.81);
    expect(response.blocked_categories).toContain("TOXICITY");
  });

  it("returns face quality score payload", async () => {
    const config = baseConfig();
    const provider = new LocalFaceQualityProvider(config);

    const response = await provider.score({
      image_base64: TEST_IMAGE_BASE64,
    });

    expect(response.provider).toBe("faceqan");
    expect(response.model_version).toBe("faceqan_test");
    expect(response.quality_score).toBe(0.66);
    expect(response.confidence).toBe(0.8);
    expect(response.signals.sharpness).toBe(0.7);
  });
});
