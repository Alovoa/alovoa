import { OrchestratorConfig, TextModerationSegmentPolicy } from "../config";
import {
  TextModerationRequest,
  TextModerationResponse,
} from "../types/contracts";
import { runJsonCommand } from "../utils/commandRunner";
import { TextModerationScriptPayload } from "./interfaces";

function clamp01(value: number): number {
  if (!Number.isFinite(value)) return 0;
  if (value < 0) return 0;
  if (value > 1) return 1;
  return value;
}

function round(value: number): number {
  return Math.round(value * 1_000_000) / 1_000_000;
}

const HIGH_RISK_TERMS = [
  "kill",
  "die",
  "rape",
  "terrorist",
  "nazi",
  "slur",
  "hate",
  "fag",
  "nigger",
  "kike",
  "bitch",
  "whore",
  "cunt",
  "asshole",
  "retard",
];

export class LocalTextModerationProvider {
  private readonly config: OrchestratorConfig;

  constructor(config: OrchestratorConfig) {
    this.config = config;
  }

  async moderate(request: TextModerationRequest): Promise<TextModerationResponse> {
    const text = (request.text || "").trim();
    if (!text) {
      throw new Error("Text is required");
    }

    try {
      const payload = await runJsonCommand<TextModerationScriptPayload>(
        this.config.textModerationCmd,
        { text },
        this.config.requestTimeoutMs,
      );

      return this.toResponse(payload, request);
    } catch {
      return this.heuristicFallback(request);
    }
  }

  private toResponse(
    payload: TextModerationScriptPayload,
    request: TextModerationRequest,
  ): TextModerationResponse {
    const policy = this.resolvePolicy(request.segment_key);
    const labels = sanitizeLabels(payload.labels);

    const computedMax = this.maxLabel(labels);
    const toxicity = clamp01(
      Number.isFinite(Number(payload.toxicity_score))
        ? Number(payload.toxicity_score)
        : (computedMax?.value ?? 0),
    );

    const blockedCategories = Object.entries(labels)
      .filter(([, value]) => value >= policy.blockThreshold)
      .map(([key]) => key.toUpperCase());

    let decision: "ALLOW" | "WARN" | "BLOCK" = "ALLOW";
    let reason: string | null = null;
    if (toxicity >= policy.blockThreshold || blockedCategories.length > 0) {
      decision = "BLOCK";
      reason = "Content flagged by toxicity model";
    } else if (toxicity >= policy.warnThreshold) {
      decision = "WARN";
      reason = "Content near moderation threshold";
    }

    return {
      is_allowed: decision !== "BLOCK",
      decision,
      toxicity_score: round(toxicity),
      max_label: payload.max_label || computedMax?.key || null,
      blocked_categories: blockedCategories,
      labels,
      reason,
      provider: payload.provider || "detoxify",
      model_version: payload.model_version || this.config.textModerationModel,
      signals: {
        threshold_block: round(policy.blockThreshold),
        threshold_warn: round(policy.warnThreshold),
        segment_policy_applied: policy.segmentPolicyApplied ? 1 : 0,
      },
      repo_refs: this.config.openSourceRepoRefs,
    };
  }

  private heuristicFallback(request: TextModerationRequest): TextModerationResponse {
    const policy = this.resolvePolicy(request.segment_key);
    const text = (request.text || "").toLowerCase();

    let riskHits = 0;
    for (const term of HIGH_RISK_TERMS) {
      if (text.includes(term)) {
        riskHits += 1;
      }
    }

    const toxicity = clamp01(0.08 + Math.min(0.85, riskHits * 0.18));
    let decision: "ALLOW" | "WARN" | "BLOCK" = "ALLOW";
    if (toxicity >= policy.blockThreshold) {
      decision = "BLOCK";
    } else if (toxicity >= policy.warnThreshold) {
      decision = "WARN";
    }

    return {
      is_allowed: decision !== "BLOCK",
      decision,
      toxicity_score: round(toxicity),
      max_label: "toxicity",
      blocked_categories: decision === "BLOCK" ? ["TOXICITY"] : [],
      labels: {
        toxicity: round(toxicity),
      },
      reason: decision === "ALLOW" ? null : "Heuristic moderation fallback",
      provider: "keyword_heuristic",
      model_version: "heuristic_v1",
      signals: {
        fallback_used: 1,
        term_hits: riskHits,
        threshold_block: round(policy.blockThreshold),
        threshold_warn: round(policy.warnThreshold),
        segment_policy_applied: policy.segmentPolicyApplied ? 1 : 0,
      },
      repo_refs: this.config.openSourceRepoRefs,
    };
  }

  private maxLabel(labels: Record<string, number>): { key: string; value: number } | null {
    let bestKey = "";
    let bestValue = -1;
    for (const [key, value] of Object.entries(labels)) {
      if (value > bestValue) {
        bestValue = value;
        bestKey = key;
      }
    }
    if (bestValue < 0) {
      return null;
    }
    return { key: bestKey, value: bestValue };
  }

  private resolvePolicy(segmentKey: string | undefined): {
    blockThreshold: number;
    warnThreshold: number;
    segmentPolicyApplied: boolean;
  } {
    const policy = this.getSegmentPolicy(segmentKey);
    return {
      blockThreshold: clamp01(policy?.blockThreshold ?? this.config.textModerationBlockThreshold),
      warnThreshold: clamp01(policy?.warnThreshold ?? this.config.textModerationWarnThreshold),
      segmentPolicyApplied: Boolean(policy),
    };
  }

  private getSegmentPolicy(segmentKey: string | undefined): TextModerationSegmentPolicy | null {
    if (segmentKey) {
      const exact = this.config.textModerationSegmentPolicies[segmentKey];
      if (exact) {
        return exact;
      }
    }
    return this.config.textModerationSegmentPolicies["*"] || null;
  }
}

function sanitizeLabels(input: Record<string, number> | undefined): Record<string, number> {
  if (!input) {
    return {};
  }
  const out: Record<string, number> = {};
  for (const [k, v] of Object.entries(input)) {
    const key = String(k).trim().toLowerCase();
    if (!key) {
      continue;
    }
    if (typeof v !== "number" || !Number.isFinite(v)) {
      continue;
    }
    out[key] = round(clamp01(v));
  }
  return out;
}
