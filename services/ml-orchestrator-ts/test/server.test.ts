import { describe, expect, it } from "vitest";
import { buildServer } from "../src/server";
import { OrchestratorConfig } from "../src/config";

function testConfig(): OrchestratorConfig {
  return {
    nodeEnv: "test",
    host: "127.0.0.1",
    port: 8081,
    requestTimeoutMs: 100,
    pythonMediaServiceUrl: "http://127.0.0.1:59999",
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

describe("ml-orchestrator-ts", () => {
  it("returns health", async () => {
    const app = buildServer(testConfig());
    const response = await app.inject({ method: "GET", url: "/health" });

    expect(response.statusCode).toBe(200);
    const body = response.json() as Record<string, unknown>;
    expect(body.status).toBe("healthy");
    expect(body.service).toBe("ml-orchestrator-ts");

    await app.close();
  });

  it("validates moderation payload", async () => {
    const app = buildServer(testConfig());
    const response = await app.inject({
      method: "POST",
      url: "/moderation/image",
      payload: {},
    });

    expect(response.statusCode).toBe(400);
    await app.close();
  });

  it("validates text moderation payload", async () => {
    const app = buildServer(testConfig());
    const response = await app.inject({
      method: "POST",
      url: "/moderation/text",
      payload: {},
    });

    expect(response.statusCode).toBe(400);
    await app.close();
  });

  it("validates face quality payload", async () => {
    const app = buildServer(testConfig());
    const response = await app.inject({
      method: "POST",
      url: "/quality/face",
      payload: {},
    });

    expect(response.statusCode).toBe(400);
    await app.close();
  });

  it("validates candidate enrichment query parameters", async () => {
    const app = buildServer(testConfig());
    const response = await app.inject({
      method: "GET",
      url: "/integrations/qdrant/candidate-enrichment",
    });

    expect(response.statusCode).toBe(400);
    await app.close();
  });
});
