import path from "node:path";

function parseBool(value: string | undefined, fallback: boolean): boolean {
  if (value == null) {
    return fallback;
  }
  return ["1", "true", "yes", "on"].includes(value.toLowerCase());
}

function parseNumber(value: string | undefined, fallback: number): number {
  if (!value) {
    return fallback;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function shellQuote(value: string): string {
  return `'${value.replaceAll("'", "'\\''")}'`;
}

function normalizeProviderName(value: string): string {
  return value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9_]+/g, "_")
    .replace(/^_+|_+$/g, "");
}

export interface AttractivenessProviderCommand {
  name: string;
  cmd: string;
  weight: number;
  modelVersion?: string;
}

export interface ModerationSegmentPolicy {
  threshold?: number;
  baseline?: number;
  openNsfw2Weight?: number;
  nudeNetWeight?: number;
  clipNsfwWeight?: number;
}

export interface TextModerationSegmentPolicy {
  blockThreshold?: number;
  warnThreshold?: number;
}

function parseAttractivenessProviderCommands(raw: string | undefined): AttractivenessProviderCommand[] {
  if (!raw) {
    return [];
  }

  try {
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) {
      return [];
    }

    const out: AttractivenessProviderCommand[] = [];
    for (const item of parsed) {
      if (!item || typeof item !== "object") {
        continue;
      }
      const name = typeof item.name === "string" ? item.name.trim() : "";
      const cmd = typeof item.cmd === "string" ? item.cmd.trim() : "";
      const weight = typeof item.weight === "number" ? item.weight : Number(item.weight ?? NaN);
      const modelVersionRaw =
        typeof item.modelVersion === "string"
          ? item.modelVersion.trim()
          : (typeof item.model_version === "string" ? item.model_version.trim() : "");
      if (!name || !cmd || !Number.isFinite(weight) || weight <= 0) {
        continue;
      }
      const next: AttractivenessProviderCommand = {
        name,
        cmd,
        weight,
      };
      if (modelVersionRaw) {
        next.modelVersion = modelVersionRaw;
      }
      out.push(next);
    }
    return out;
  } catch {
    return [];
  }
}

function parseProviderAbRates(raw: string | undefined): Record<string, number> {
  if (!raw) {
    return {};
  }
  try {
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
      return {};
    }

    const out: Record<string, number> = {};
    for (const [nameRaw, value] of Object.entries(parsed)) {
      const name = normalizeProviderName(String(nameRaw));
      const rate = typeof value === "number" ? value : Number(value);
      if (!name || !Number.isFinite(rate)) {
        continue;
      }
      out[name] = Math.max(0, Math.min(1, rate));
    }
    return out;
  } catch {
    return {};
  }
}

function parseModerationSegmentPolicies(raw: string | undefined): Record<string, ModerationSegmentPolicy> {
  if (!raw) {
    return {};
  }
  try {
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
      return {};
    }
    const out: Record<string, ModerationSegmentPolicy> = {};
    for (const [segmentRaw, policy] of Object.entries(parsed)) {
      if (!segmentRaw || typeof policy !== "object" || !policy || Array.isArray(policy)) {
        continue;
      }
      const normalizedKey = segmentRaw.trim();
      if (!normalizedKey) {
        continue;
      }

      const next: ModerationSegmentPolicy = {};
      const threshold = Number((policy as Record<string, unknown>).threshold ?? NaN);
      const baseline = Number((policy as Record<string, unknown>).baseline ?? NaN);
      const openNsfw2Weight = Number((policy as Record<string, unknown>).openNsfw2Weight ?? NaN);
      const nudeNetWeight = Number((policy as Record<string, unknown>).nudeNetWeight ?? NaN);
      const clipNsfwWeight = Number((policy as Record<string, unknown>).clipNsfwWeight ?? NaN);

      if (Number.isFinite(threshold)) {
        next.threshold = Math.max(0, Math.min(1, threshold));
      }
      if (Number.isFinite(baseline)) {
        next.baseline = Math.max(0, Math.min(1, baseline));
      }
      if (Number.isFinite(openNsfw2Weight)) {
        next.openNsfw2Weight = Math.max(0, openNsfw2Weight);
      }
      if (Number.isFinite(nudeNetWeight)) {
        next.nudeNetWeight = Math.max(0, nudeNetWeight);
      }
      if (Number.isFinite(clipNsfwWeight)) {
        next.clipNsfwWeight = Math.max(0, clipNsfwWeight);
      }

      if (Object.keys(next).length > 0) {
        out[normalizedKey] = next;
      }
    }
    return out;
  } catch {
    return {};
  }
}

function parseTextModerationSegmentPolicies(raw: string | undefined): Record<string, TextModerationSegmentPolicy> {
  if (!raw) {
    return {};
  }
  try {
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
      return {};
    }
    const out: Record<string, TextModerationSegmentPolicy> = {};
    for (const [segmentRaw, policy] of Object.entries(parsed)) {
      if (!segmentRaw || typeof policy !== "object" || !policy || Array.isArray(policy)) {
        continue;
      }
      const normalizedKey = segmentRaw.trim();
      if (!normalizedKey) {
        continue;
      }
      const next: TextModerationSegmentPolicy = {};
      const blockThreshold = Number((policy as Record<string, unknown>).blockThreshold ?? NaN);
      const warnThreshold = Number((policy as Record<string, unknown>).warnThreshold ?? NaN);
      if (Number.isFinite(blockThreshold)) {
        next.blockThreshold = Math.max(0, Math.min(1, blockThreshold));
      }
      if (Number.isFinite(warnThreshold)) {
        next.warnThreshold = Math.max(0, Math.min(1, warnThreshold));
      }
      if (Object.keys(next).length > 0) {
        out[normalizedKey] = next;
      }
    }
    return out;
  } catch {
    return {};
  }
}

function buildDefaultAttractivenessProviderCommands(root: string): AttractivenessProviderCommand[] {
  const out: AttractivenessProviderCommand[] = [];

  if (process.env.COMBOLOSS_MODEL_PATH) {
    out.push({
      name: "comboloss",
      cmd:
        process.env.COMBOLOSS_CMD ||
        `python3 ${path.join(root, "services", "media-service", "scripts", "attractiveness_comboloss_adapter.py")} --image {image_path} --model ${shellQuote(process.env.COMBOLOSS_MODEL_PATH)}`,
      weight: parseNumber(process.env.COMBOLOSS_WEIGHT, 0.35),
      modelVersion: process.env.COMBOLOSS_MODEL_VERSION || "comboloss_v1",
    });
  }

  if (process.env.FACIAL_BEAUTY_MODEL_PATH) {
    out.push({
      name: "facial_beauty_prediction",
      cmd:
        process.env.FACIAL_BEAUTY_CMD ||
        `python3 ${path.join(root, "services", "media-service", "scripts", "attractiveness_facial_beauty_adapter.py")} --image {image_path} --model ${shellQuote(process.env.FACIAL_BEAUTY_MODEL_PATH)}`,
      weight: parseNumber(process.env.FACIAL_BEAUTY_WEIGHT, 0.30),
      modelVersion: process.env.FACIAL_BEAUTY_MODEL_VERSION || "facial_beauty_prediction_v1",
    });
  }

  if (process.env.BEAUTYPREDICT_MODEL_PATH) {
    out.push({
      name: "beautypredict",
      cmd:
        process.env.BEAUTYPREDICT_CMD ||
        `python3 ${path.join(root, "services", "media-service", "scripts", "attractiveness_beautypredict_adapter.py")} --image {image_path} --model ${shellQuote(process.env.BEAUTYPREDICT_MODEL_PATH)}`,
      weight: parseNumber(process.env.BEAUTYPREDICT_WEIGHT, 0.25),
      modelVersion: process.env.BEAUTYPREDICT_MODEL_VERSION || "beautypredict_v1",
    });
  }

  if (process.env.FACEATTRACT_MODEL_PATH) {
    out.push({
      name: "faceattract",
      cmd:
        process.env.FACEATTRACT_CMD ||
        `python3 ${path.join(root, "services", "media-service", "scripts", "attractiveness_faceattract_adapter.py")} --image {image_path} --model ${shellQuote(process.env.FACEATTRACT_MODEL_PATH)}`,
      weight: parseNumber(process.env.FACEATTRACT_WEIGHT, 0.30),
      modelVersion: process.env.FACEATTRACT_MODEL_VERSION || "faceattract_v1",
    });
  }

  if (process.env.METAFBP_MODEL_PATH) {
    out.push({
      name: "metafbp",
      cmd:
        process.env.METAFBP_CMD ||
        `python3 ${path.join(root, "services", "media-service", "scripts", "attractiveness_metafbp_adapter.py")} --image {image_path} --model ${shellQuote(process.env.METAFBP_MODEL_PATH)}`,
      weight: parseNumber(process.env.METAFBP_WEIGHT, 0.25),
      modelVersion: process.env.METAFBP_MODEL_VERSION || "metafbp_v1",
    });
  }

  if (parseBool(process.env.DECA_ENABLE, false) || Boolean(process.env.DECA_CMD)) {
    out.push({
      name: "deca_geometry",
      cmd:
        process.env.DECA_CMD ||
        `python3 ${path.join(root, "services", "media-service", "scripts", "attractiveness_deca_geometry_adapter.py")} --image {image_path}`,
      weight: parseNumber(process.env.DECA_WEIGHT, 0.15),
      modelVersion: process.env.DECA_MODEL_VERSION || "deca_geometry_v1",
    });
  }

  if (parseBool(process.env.DEEP3D_ENABLE, false) || Boolean(process.env.DEEP3D_CMD)) {
    out.push({
      name: "deep3d_geometry",
      cmd:
        process.env.DEEP3D_CMD ||
        `python3 ${path.join(root, "services", "media-service", "scripts", "attractiveness_deep3d_geometry_adapter.py")} --image {image_path}`,
      weight: parseNumber(process.env.DEEP3D_WEIGHT, 0.15),
      modelVersion: process.env.DEEP3D_MODEL_VERSION || "deep3d_geometry_v1",
    });
  }

  if (parseBool(process.env.ATTRACTIVENESS_INSIGHTFACE_ENABLE, false) || Boolean(process.env.ATTRACTIVENESS_INSIGHTFACE_CMD)) {
    out.push({
      name: "insightface_geometry",
      cmd:
        process.env.ATTRACTIVENESS_INSIGHTFACE_CMD ||
        `python3 ${path.join(root, "services", "media-service", "scripts", "attractiveness_insightface_adapter.py")} --image {image_path}`,
      weight: parseNumber(process.env.ATTRACTIVENESS_INSIGHTFACE_WEIGHT, 0.12),
      modelVersion: process.env.ATTRACTIVENESS_INSIGHTFACE_MODEL_VERSION || "insightface_geometry_v1",
    });
  }

  if (parseBool(process.env.ATTRACTIVENESS_MEDIAPIPE_ENABLE, false) || Boolean(process.env.ATTRACTIVENESS_MEDIAPIPE_CMD)) {
    out.push({
      name: "mediapipe_geometry",
      cmd:
        process.env.ATTRACTIVENESS_MEDIAPIPE_CMD ||
        `python3 ${path.join(root, "services", "media-service", "scripts", "attractiveness_mediapipe_geometry_adapter.py")} --image {image_path}`,
      weight: parseNumber(process.env.ATTRACTIVENESS_MEDIAPIPE_WEIGHT, 0.10),
      modelVersion: process.env.ATTRACTIVENESS_MEDIAPIPE_MODEL_VERSION || "mediapipe_geometry_v1",
    });
  }

  if (parseBool(process.env.ATTRACTIVENESS_DEEPFACE_ENABLE, false) || Boolean(process.env.ATTRACTIVENESS_DEEPFACE_CMD)) {
    out.push({
      name: "deepface_quality",
      cmd:
        process.env.ATTRACTIVENESS_DEEPFACE_CMD ||
        `python3 ${path.join(root, "services", "media-service", "scripts", "attractiveness_deepface_adapter.py")} --image {image_path}`,
      weight: parseNumber(process.env.ATTRACTIVENESS_DEEPFACE_WEIGHT, 0.10),
      modelVersion: process.env.ATTRACTIVENESS_DEEPFACE_MODEL_VERSION || "deepface_quality_v1",
    });
  }

  if (parseBool(process.env.FACE_ALIGNMENT_ENABLE, false) || Boolean(process.env.FACE_ALIGNMENT_CMD)) {
    out.push({
      name: "face_alignment_geometry",
      cmd:
        process.env.FACE_ALIGNMENT_CMD ||
        `python3 ${path.join(root, "services", "media-service", "scripts", "attractiveness_face_alignment_adapter.py")} --image {image_path}`,
      weight: parseNumber(process.env.FACE_ALIGNMENT_WEIGHT, 0.12),
      modelVersion: process.env.FACE_ALIGNMENT_MODEL_VERSION || "face_alignment_v1",
    });
  }

  if (parseBool(process.env.DLIB_ENABLE, false) || Boolean(process.env.DLIB_CMD)) {
    out.push({
      name: "dlib_geometry",
      cmd:
        process.env.DLIB_CMD ||
        `python3 ${path.join(root, "services", "media-service", "scripts", "attractiveness_dlib_geometry_adapter.py")} --image {image_path}`,
      weight: parseNumber(process.env.DLIB_WEIGHT, 0.10),
      modelVersion: process.env.DLIB_MODEL_VERSION || "dlib_geometry_v1",
    });
  }

  return out;
}

export interface OrchestratorConfig {
  nodeEnv: string;
  host: string;
  port: number;
  requestTimeoutMs: number;
  pythonMediaServiceUrl: string;

  enableLocalModeration: boolean;
  nsfwThreshold: number;
  nsfwBaseline: number;
  nsfwUseOpenNsfw2: boolean;
  nsfwUseNudeNet: boolean;
  nsfwUseClipNsfw: boolean;
  nsfwOpenNsfw2Cmd: string;
  nsfwNudeNetCmd: string;
  nsfwClipCmd: string;
  moderationSegmentPolicies: Record<string, ModerationSegmentPolicy>;
  nsfwCalibrationEnabled: boolean;
  nsfwCalibrationFile: string;
  moderationUseUnleashFlag: boolean;
  moderationUnleashFlagName: string;
  moderationOpenFgaEnforce: boolean;
  moderationOpenFgaObject: string;
  moderationOpenFgaRelation: string;

  enableLocalTextModeration: boolean;
  textModerationCmd: string;
  textModerationModel: string;
  textModerationBlockThreshold: number;
  textModerationWarnThreshold: number;
  textModerationSegmentPolicies: Record<string, TextModerationSegmentPolicy>;
  textModerationUseUnleashFlag: boolean;
  textModerationUnleashFlagName: string;
  textModerationOpenFgaEnforce: boolean;
  textModerationOpenFgaObject: string;
  textModerationOpenFgaRelation: string;

  enableLocalFaceQuality: boolean;
  faceQualityCmd: string;
  faceQualityProviderName: string;
  faceQualityModelVersion: string;
  faceQualityUseUnleashFlag: boolean;
  faceQualityUnleashFlagName: string;
  faceQualityOpenFgaEnforce: boolean;
  faceQualityOpenFgaObject: string;
  faceQualityOpenFgaRelation: string;
  safetySignalLogEnabled: boolean;
  safetySignalLogFile: string;

  enableLocalAttractiveness: boolean;
  attractivenessBaseline: number;
  attractivenessFrontWeight: number;
  attractivenessSideWeight: number;
  attractivenessCmd: string;
  attractivenessProviderName: string;
  attractivenessModelVersion: string;
  attractivenessProviderCommands: AttractivenessProviderCommand[];
  attractivenessProviderAbRates: Record<string, number>;
  attractivenessCalibrationEnabled: boolean;
  attractivenessCalibrationFile: string;
  attractivenessUseUnleashFlag: boolean;
  attractivenessUnleashFlagName: string;
  attractivenessOpenFgaEnforce: boolean;
  attractivenessOpenFgaObject: string;
  attractivenessOpenFgaRelation: string;
  qdrantAttractivenessHintEnabled: boolean;
  qdrantAttractivenessCollection: string;
  qdrantAttractivenessHintMaxDelta: number;
  qdrantCandidateEnrichmentEnabled: boolean;
  qdrantCandidateCollection: string;
  qdrantCandidateEnrichmentLimit: number;

  reposRoot: string;
  openSourceRepoRefs: string[];

  enableQdrant: boolean;
  qdrantUrl: string;
  qdrantApiKey: string;

  enableUnleash: boolean;
  unleashUrl: string;
  unleashApiToken: string;
  unleashAppName: string;

  enableOpenFga: boolean;
  openFgaUrl: string;
  openFgaStoreId: string;
  openFgaAuthzModelId: string;
  openFgaApiToken: string;
}

export function loadConfig(): OrchestratorConfig {
  const root = path.resolve(process.cwd(), "..", "..");
  const reposRoot = process.env.REPOS_ROOT || path.join(root, "third_party", "repos");
  const parsedProviderCommands = parseAttractivenessProviderCommands(
    process.env.ATTRACTIVENESS_PROVIDER_CMDS,
  );
  const providerAbRates = parseProviderAbRates(process.env.ATTRACTIVENESS_PROVIDER_AB_RATES);
  const moderationSegmentPolicies = parseModerationSegmentPolicies(process.env.NSFW_SEGMENT_POLICIES_JSON);
  const textModerationSegmentPolicies = parseTextModerationSegmentPolicies(
    process.env.TEXT_MODERATION_SEGMENT_POLICIES_JSON,
  );

  return {
    nodeEnv: process.env.NODE_ENV || "development",
    host: process.env.HOST || "0.0.0.0",
    port: parseNumber(process.env.PORT, 8081),
    requestTimeoutMs: parseNumber(process.env.REQUEST_TIMEOUT_MS, 15000),
    pythonMediaServiceUrl: process.env.PYTHON_MEDIA_SERVICE_URL || "http://localhost:8001",

    enableLocalModeration: parseBool(process.env.ENABLE_LOCAL_MODERATION, false),
    nsfwThreshold: parseNumber(process.env.NSFW_THRESHOLD, 0.60),
    nsfwBaseline: parseNumber(process.env.NSFW_BASELINE, 0.02),
    nsfwUseOpenNsfw2: parseBool(process.env.NSFW_USE_OPENNSFW2, false),
    nsfwUseNudeNet: parseBool(process.env.NSFW_USE_NUDENET, false),
    nsfwUseClipNsfw: parseBool(process.env.NSFW_USE_CLIP_NSFW, false),
    nsfwOpenNsfw2Cmd:
      process.env.NSFW_OPENNSFW2_CMD ||
      `python3 ${path.join(root, "services", "media-service", "scripts", "nsfw_opennsfw2_adapter.py")} --image {image_path}`,
    nsfwNudeNetCmd:
      process.env.NSFW_NUDENET_CMD ||
      `python3 ${path.join(root, "services", "media-service", "scripts", "nsfw_nudenet_adapter.py")} --image {image_path}`,
    nsfwClipCmd:
      process.env.NSFW_CLIP_CMD ||
      `python3 ${path.join(root, "services", "media-service", "scripts", "nsfw_clip_adapter.py")} --image {image_path}`,
    moderationSegmentPolicies,
    nsfwCalibrationEnabled: parseBool(process.env.NSFW_CALIBRATION_ENABLE, false),
    nsfwCalibrationFile:
      process.env.NSFW_CALIBRATION_FILE ||
      path.join(root, "configs", "moderation", "nsfw_thresholds.json"),
    moderationUseUnleashFlag: parseBool(process.env.MODERATION_USE_UNLEASH_FLAG, false),
    moderationUnleashFlagName:
      process.env.MODERATION_UNLEASH_FLAG_NAME || "moderation_local_enabled",
    moderationOpenFgaEnforce: parseBool(process.env.MODERATION_OPENFGA_ENFORCE, false),
    moderationOpenFgaObject:
      process.env.MODERATION_OPENFGA_OBJECT || "feature:moderation_local",
    moderationOpenFgaRelation:
      process.env.MODERATION_OPENFGA_RELATION || "can_use",

    enableLocalTextModeration: parseBool(process.env.ENABLE_LOCAL_TEXT_MODERATION, false),
    textModerationCmd:
      process.env.TEXT_MODERATION_CMD ||
      `python3 ${path.join(root, "services", "media-service", "scripts", "text_detoxify_adapter.py")} --text {text}`,
    textModerationModel: process.env.TEXT_MODERATION_MODEL || "multilingual",
    textModerationBlockThreshold: parseNumber(process.env.TEXT_MODERATION_BLOCK_THRESHOLD, 0.72),
    textModerationWarnThreshold: parseNumber(process.env.TEXT_MODERATION_WARN_THRESHOLD, 0.52),
    textModerationSegmentPolicies,
    textModerationUseUnleashFlag: parseBool(process.env.TEXT_MODERATION_USE_UNLEASH_FLAG, false),
    textModerationUnleashFlagName:
      process.env.TEXT_MODERATION_UNLEASH_FLAG_NAME || "moderation_text_local_enabled",
    textModerationOpenFgaEnforce: parseBool(process.env.TEXT_MODERATION_OPENFGA_ENFORCE, false),
    textModerationOpenFgaObject:
      process.env.TEXT_MODERATION_OPENFGA_OBJECT || "feature:moderation_text_local",
    textModerationOpenFgaRelation:
      process.env.TEXT_MODERATION_OPENFGA_RELATION || "can_use",

    enableLocalFaceQuality: parseBool(process.env.ENABLE_LOCAL_FACE_QUALITY, false),
    faceQualityCmd:
      process.env.FACE_QUALITY_CMD ||
      `python3 ${path.join(root, "services", "media-service", "scripts", "faceqan_adapter.py")} --image {image_path}`,
    faceQualityProviderName: process.env.FACE_QUALITY_PROVIDER || "faceqan",
    faceQualityModelVersion: process.env.FACE_QUALITY_MODEL_VERSION || "faceqan_v1",
    faceQualityUseUnleashFlag: parseBool(process.env.FACE_QUALITY_USE_UNLEASH_FLAG, false),
    faceQualityUnleashFlagName:
      process.env.FACE_QUALITY_UNLEASH_FLAG_NAME || "face_quality_gate_enabled",
    faceQualityOpenFgaEnforce: parseBool(process.env.FACE_QUALITY_OPENFGA_ENFORCE, false),
    faceQualityOpenFgaObject:
      process.env.FACE_QUALITY_OPENFGA_OBJECT || "feature:face_quality_gate",
    faceQualityOpenFgaRelation:
      process.env.FACE_QUALITY_OPENFGA_RELATION || "can_use",
    safetySignalLogEnabled: parseBool(process.env.SAFETY_SIGNAL_LOG_ENABLE, false),
    safetySignalLogFile:
      process.env.SAFETY_SIGNAL_LOG_FILE ||
      path.join(root, "logs", "safety_signal_events.jsonl"),

    enableLocalAttractiveness: parseBool(process.env.ENABLE_LOCAL_ATTRACTIVENESS, false),
    attractivenessBaseline: parseNumber(process.env.ATTRACTIVENESS_BASELINE, 0.50),
    attractivenessFrontWeight: parseNumber(process.env.ATTRACTIVENESS_FRONT_WEIGHT, 0.70),
    attractivenessSideWeight: parseNumber(process.env.ATTRACTIVENESS_SIDE_WEIGHT, 0.30),
    attractivenessCmd:
      process.env.ATTRACTIVENESS_CMD ||
      `python3 ${path.join(root, "services", "media-service", "scripts", "attractiveness_oss_adapter.py")} --image {image_path} --view {view}`,
    attractivenessProviderName: process.env.ATTRACTIVENESS_PROVIDER || "external",
    attractivenessModelVersion: process.env.ATTRACTIVENESS_MODEL_VERSION || "oss_v1",
    attractivenessProviderCommands:
      parsedProviderCommands.length > 0
        ? parsedProviderCommands
        : buildDefaultAttractivenessProviderCommands(root),
    attractivenessProviderAbRates: providerAbRates,
    attractivenessCalibrationEnabled: parseBool(process.env.ATTRACTIVENESS_CALIBRATION_ENABLE, false),
    attractivenessCalibrationFile:
      process.env.ATTRACTIVENESS_CALIBRATION_FILE ||
      path.join(root, "configs", "attractiveness", "scut_score_calibration.json"),
    attractivenessUseUnleashFlag: parseBool(process.env.ATTRACTIVENESS_USE_UNLEASH_FLAG, false),
    attractivenessUnleashFlagName:
      process.env.ATTRACTIVENESS_UNLEASH_FLAG_NAME || "attractiveness_local_enabled",
    attractivenessOpenFgaEnforce: parseBool(process.env.ATTRACTIVENESS_OPENFGA_ENFORCE, false),
    attractivenessOpenFgaObject:
      process.env.ATTRACTIVENESS_OPENFGA_OBJECT || "feature:attractiveness_local",
    attractivenessOpenFgaRelation:
      process.env.ATTRACTIVENESS_OPENFGA_RELATION || "can_use",
    qdrantAttractivenessHintEnabled: parseBool(process.env.QDRANT_ATTRACTIVENESS_HINT_ENABLE, false),
    qdrantAttractivenessCollection:
      process.env.QDRANT_ATTRACTIVENESS_COLLECTION || "attractiveness_hints",
    qdrantAttractivenessHintMaxDelta: parseNumber(process.env.QDRANT_ATTRACTIVENESS_HINT_MAX_DELTA, 0.08),
    qdrantCandidateEnrichmentEnabled: parseBool(process.env.QDRANT_CANDIDATE_ENRICHMENT_ENABLE, false),
    qdrantCandidateCollection:
      process.env.QDRANT_CANDIDATE_COLLECTION || "candidate_enrichment",
    qdrantCandidateEnrichmentLimit: Math.max(1, Math.min(100, parseNumber(process.env.QDRANT_CANDIDATE_ENRICHMENT_LIMIT, 20))),

    reposRoot,
    openSourceRepoRefs: [
      "serengil/deepface",
      "deepinsight/insightface",
      "google-ai-edge/mediapipe",
      "minivision-ai/Silent-Face-Anti-Spoofing",
      "bhky/opennsfw2",
      "notAI-tech/NudeNet",
      "HCIILAB/SCUT-FBP5500-Database-Release",
      "lucasxlu/ComboLoss",
      "ustcqidi/BeautyPredict",
      "fei-aiart/FaceAttract",
      "MetaVisionLab/MetaFBP",
      "etrain-xyz/facial-beauty-prediction",
      "davisking/dlib",
      "1adrianb/face-alignment",
      "yfeng95/DECA",
      "sicxu/Deep3DFaceRecon_pytorch",
      "unitaryai/detoxify",
      "LSIbabnikz/FaceQAN",
      "LAION-AI/CLIP-based-NSFW-Detector",
      "SCLBD/DeepfakeBench",
      "recommenders-team/recommenders",
      "benfred/implicit",
      "lyst/lightfm",
      "openfga/openfga",
      "Unleash/unleash",
      "qdrant/qdrant",
    ],

    enableQdrant: parseBool(process.env.ENABLE_QDRANT, false),
    qdrantUrl: process.env.QDRANT_URL || "http://localhost:6333",
    qdrantApiKey: process.env.QDRANT_API_KEY || "",

    enableUnleash: parseBool(process.env.ENABLE_UNLEASH, false),
    unleashUrl: process.env.UNLEASH_URL || "http://localhost:4242/api",
    unleashApiToken: process.env.UNLEASH_API_TOKEN || "",
    unleashAppName: process.env.UNLEASH_APP_NAME || "aura-ml-orchestrator",

    enableOpenFga: parseBool(process.env.ENABLE_OPENFGA, false),
    openFgaUrl: process.env.OPENFGA_URL || "http://localhost:8080",
    openFgaStoreId: process.env.OPENFGA_STORE_ID || "",
    openFgaAuthzModelId: process.env.OPENFGA_AUTHZ_MODEL_ID || "",
    openFgaApiToken: process.env.OPENFGA_API_TOKEN || "",
  };
}
