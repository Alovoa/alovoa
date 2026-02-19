import { describe, expect, it } from "vitest";
import { loadConfig } from "../src/config";

function withEnv<T>(entries: Record<string, string | undefined>, fn: () => T): T {
  const original: Record<string, string | undefined> = {};
  for (const key of Object.keys(entries)) {
    original[key] = process.env[key];
    if (entries[key] === undefined) {
      delete process.env[key];
    } else {
      process.env[key] = entries[key];
    }
  }
  try {
    return fn();
  } finally {
    for (const key of Object.keys(entries)) {
      if (original[key] === undefined) {
        delete process.env[key];
      } else {
        process.env[key] = original[key];
      }
    }
  }
}

describe("config", () => {
  it("parses ATTRACTIVENESS_PROVIDER_CMDS JSON", () => {
    const cfg = withEnv(
      {
        ATTRACTIVENESS_PROVIDER_CMDS: JSON.stringify([
          {
            name: "comboloss",
            cmd: "python3 run.py --image {image_path}",
            weight: 0.4,
            model_version: "combo_custom_v2",
          },
          { name: "invalid" },
        ]),
        COMBOLOSS_MODEL_PATH: undefined,
        FACIAL_BEAUTY_MODEL_PATH: undefined,
      },
      () => loadConfig(),
    );

    expect(cfg.attractivenessProviderCommands).toEqual([
      {
        name: "comboloss",
        cmd: "python3 run.py --image {image_path}",
        weight: 0.4,
        modelVersion: "combo_custom_v2",
      },
    ]);
  });

  it("builds default repo commands from model path env vars", () => {
    const cfg = withEnv(
      {
        ATTRACTIVENESS_PROVIDER_CMDS: undefined,
        COMBOLOSS_MODEL_PATH: "/tmp/comboloss/model.pth",
        FACIAL_BEAUTY_MODEL_PATH: "/tmp/fbp/model.pt",
        BEAUTYPREDICT_MODEL_PATH: "/tmp/beautypredict/model.h5",
        FACEATTRACT_MODEL_PATH: "/tmp/faceattract/model.weight",
        METAFBP_MODEL_PATH: "/tmp/metafbp/model.pth",
        DECA_ENABLE: "true",
        DEEP3D_ENABLE: "true",
        ATTRACTIVENESS_INSIGHTFACE_ENABLE: "true",
        ATTRACTIVENESS_MEDIAPIPE_ENABLE: "true",
        ATTRACTIVENESS_DEEPFACE_ENABLE: "true",
        FACE_ALIGNMENT_ENABLE: "true",
        DLIB_ENABLE: "true",
        ATTRACTIVENESS_CALIBRATION_ENABLE: "true",
        ATTRACTIVENESS_CALIBRATION_FILE: "/tmp/scut-calibration.json",
        ATTRACTIVENESS_USE_UNLEASH_FLAG: "true",
        ATTRACTIVENESS_UNLEASH_FLAG_NAME: "attractiveness_rollout",
        ATTRACTIVENESS_OPENFGA_ENFORCE: "true",
        ATTRACTIVENESS_OPENFGA_OBJECT: "feature:attractiveness_local",
        ATTRACTIVENESS_OPENFGA_RELATION: "can_use",
        QDRANT_ATTRACTIVENESS_HINT_ENABLE: "true",
        QDRANT_ATTRACTIVENESS_COLLECTION: "attractiveness_hints",
        QDRANT_ATTRACTIVENESS_HINT_MAX_DELTA: "0.12",
        QDRANT_CANDIDATE_ENRICHMENT_ENABLE: "true",
        QDRANT_CANDIDATE_COLLECTION: "candidate_enrichment_v1",
        QDRANT_CANDIDATE_ENRICHMENT_LIMIT: "15",
        NSFW_USE_CLIP_NSFW: "true",
        NSFW_CLIP_CMD: "python3 nsfw_clip_adapter.py --image {image_path}",
        NSFW_CALIBRATION_ENABLE: "true",
        NSFW_CALIBRATION_FILE: "/tmp/nsfw-calibration.json",
        MODERATION_USE_UNLEASH_FLAG: "true",
        MODERATION_UNLEASH_FLAG_NAME: "moderation_rollout",
        MODERATION_OPENFGA_ENFORCE: "true",
        MODERATION_OPENFGA_OBJECT: "feature:moderation_local",
        MODERATION_OPENFGA_RELATION: "can_use",
        ENABLE_LOCAL_TEXT_MODERATION: "true",
        TEXT_MODERATION_CMD: "python3 text_detoxify_adapter.py --text {text}",
        TEXT_MODERATION_MODEL: "multilingual",
        TEXT_MODERATION_BLOCK_THRESHOLD: "0.7",
        TEXT_MODERATION_WARN_THRESHOLD: "0.5",
        TEXT_MODERATION_SEGMENT_POLICIES_JSON: JSON.stringify({
          "us_f_25_34_nyc": { blockThreshold: 0.66, warnThreshold: 0.49 },
        }),
        TEXT_MODERATION_USE_UNLEASH_FLAG: "true",
        TEXT_MODERATION_UNLEASH_FLAG_NAME: "moderation_text_rollout",
        TEXT_MODERATION_OPENFGA_ENFORCE: "true",
        TEXT_MODERATION_OPENFGA_OBJECT: "feature:moderation_text_local",
        TEXT_MODERATION_OPENFGA_RELATION: "can_use",
        ENABLE_LOCAL_FACE_QUALITY: "true",
        FACE_QUALITY_CMD: "python3 faceqan_adapter.py --image {image_path}",
        FACE_QUALITY_PROVIDER: "faceqan",
        FACE_QUALITY_MODEL_VERSION: "faceqan_v2",
        FACE_QUALITY_USE_UNLEASH_FLAG: "true",
        FACE_QUALITY_UNLEASH_FLAG_NAME: "face_quality_rollout",
        FACE_QUALITY_OPENFGA_ENFORCE: "true",
        FACE_QUALITY_OPENFGA_OBJECT: "feature:face_quality_gate",
        FACE_QUALITY_OPENFGA_RELATION: "can_use",
        SAFETY_SIGNAL_LOG_ENABLE: "true",
        SAFETY_SIGNAL_LOG_FILE: "/tmp/safety-signal-events.jsonl",
        NSFW_SEGMENT_POLICIES_JSON: JSON.stringify({
          "us_f_25_34_nyc": { threshold: 0.52, baseline: 0.03, openNsfw2Weight: 0.3, nudeNetWeight: 0.5, clipNsfwWeight: 0.2 },
        }),
        ATTRACTIVENESS_PROVIDER_AB_RATES: JSON.stringify({
          comboloss: 0.5,
          face_alignment_geometry: 0.25,
          dlib_geometry: 0.1,
        }),
      },
      () => loadConfig(),
    );

    const names = cfg.attractivenessProviderCommands.map((x) => x.name);
    expect(names).toContain("comboloss");
    expect(names).toContain("facial_beauty_prediction");
    expect(names).toContain("beautypredict");
    expect(names).toContain("faceattract");
    expect(names).toContain("metafbp");
    expect(names).toContain("deca_geometry");
    expect(names).toContain("deep3d_geometry");
    expect(names).toContain("insightface_geometry");
    expect(names).toContain("mediapipe_geometry");
    expect(names).toContain("deepface_quality");
    expect(names).toContain("face_alignment_geometry");
    expect(names).toContain("dlib_geometry");
    expect(cfg.attractivenessProviderAbRates).toEqual({
      comboloss: 0.5,
      face_alignment_geometry: 0.25,
      dlib_geometry: 0.1,
    });
    expect(cfg.attractivenessCalibrationEnabled).toBe(true);
    expect(cfg.attractivenessCalibrationFile).toBe("/tmp/scut-calibration.json");
    expect(cfg.attractivenessUseUnleashFlag).toBe(true);
    expect(cfg.attractivenessUnleashFlagName).toBe("attractiveness_rollout");
    expect(cfg.attractivenessOpenFgaEnforce).toBe(true);
    expect(cfg.attractivenessOpenFgaObject).toBe("feature:attractiveness_local");
    expect(cfg.attractivenessOpenFgaRelation).toBe("can_use");
    expect(cfg.qdrantAttractivenessHintEnabled).toBe(true);
    expect(cfg.qdrantAttractivenessCollection).toBe("attractiveness_hints");
    expect(cfg.qdrantAttractivenessHintMaxDelta).toBe(0.12);
    expect(cfg.qdrantCandidateEnrichmentEnabled).toBe(true);
    expect(cfg.qdrantCandidateCollection).toBe("candidate_enrichment_v1");
    expect(cfg.qdrantCandidateEnrichmentLimit).toBe(15);
    expect(cfg.nsfwUseClipNsfw).toBe(true);
    expect(cfg.nsfwClipCmd).toBe("python3 nsfw_clip_adapter.py --image {image_path}");
    expect(cfg.nsfwCalibrationEnabled).toBe(true);
    expect(cfg.nsfwCalibrationFile).toBe("/tmp/nsfw-calibration.json");
    expect(cfg.moderationUseUnleashFlag).toBe(true);
    expect(cfg.moderationUnleashFlagName).toBe("moderation_rollout");
    expect(cfg.moderationOpenFgaEnforce).toBe(true);
    expect(cfg.moderationOpenFgaObject).toBe("feature:moderation_local");
    expect(cfg.moderationOpenFgaRelation).toBe("can_use");
    expect(cfg.enableLocalTextModeration).toBe(true);
    expect(cfg.textModerationCmd).toBe("python3 text_detoxify_adapter.py --text {text}");
    expect(cfg.textModerationModel).toBe("multilingual");
    expect(cfg.textModerationBlockThreshold).toBe(0.7);
    expect(cfg.textModerationWarnThreshold).toBe(0.5);
    expect(cfg.textModerationSegmentPolicies).toEqual({
      us_f_25_34_nyc: {
        blockThreshold: 0.66,
        warnThreshold: 0.49,
      },
    });
    expect(cfg.textModerationUseUnleashFlag).toBe(true);
    expect(cfg.textModerationUnleashFlagName).toBe("moderation_text_rollout");
    expect(cfg.textModerationOpenFgaEnforce).toBe(true);
    expect(cfg.textModerationOpenFgaObject).toBe("feature:moderation_text_local");
    expect(cfg.textModerationOpenFgaRelation).toBe("can_use");
    expect(cfg.enableLocalFaceQuality).toBe(true);
    expect(cfg.faceQualityCmd).toBe("python3 faceqan_adapter.py --image {image_path}");
    expect(cfg.faceQualityProviderName).toBe("faceqan");
    expect(cfg.faceQualityModelVersion).toBe("faceqan_v2");
    expect(cfg.faceQualityUseUnleashFlag).toBe(true);
    expect(cfg.faceQualityUnleashFlagName).toBe("face_quality_rollout");
    expect(cfg.faceQualityOpenFgaEnforce).toBe(true);
    expect(cfg.faceQualityOpenFgaObject).toBe("feature:face_quality_gate");
    expect(cfg.faceQualityOpenFgaRelation).toBe("can_use");
    expect(cfg.safetySignalLogEnabled).toBe(true);
    expect(cfg.safetySignalLogFile).toBe("/tmp/safety-signal-events.jsonl");
    expect(cfg.moderationSegmentPolicies).toEqual({
      us_f_25_34_nyc: {
        threshold: 0.52,
        baseline: 0.03,
        openNsfw2Weight: 0.3,
        nudeNetWeight: 0.5,
        clipNsfwWeight: 0.2,
      },
    });
  });
});
