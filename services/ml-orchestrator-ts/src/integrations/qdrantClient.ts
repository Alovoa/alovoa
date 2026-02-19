import { OrchestratorConfig } from "../config";

function clamp(value: number, min: number, max: number): number {
  if (!Number.isFinite(value)) return min;
  if (value < min) return min;
  if (value > max) return max;
  return value;
}

export class QdrantClient {
  private readonly enabled: boolean;
  private readonly url: string;
  private readonly apiKey: string;

  constructor(config: OrchestratorConfig) {
    this.enabled = config.enableQdrant;
    this.url = config.qdrantUrl.replace(/\/$/, "");
    this.apiKey = config.qdrantApiKey;
  }

  isEnabled(): boolean {
    return this.enabled;
  }

  async health(): Promise<{ ok: boolean; message: string }> {
    if (!this.enabled) {
      return { ok: false, message: "disabled" };
    }

    try {
      const response = await fetch(`${this.url}/collections`, {
        headers: this.apiKey ? { "api-key": this.apiKey } : {},
      });
      if (!response.ok) {
        return { ok: false, message: `http_${response.status}` };
      }
      return { ok: true, message: "ok" };
    } catch (error) {
      return { ok: false, message: `error:${String(error)}` };
    }
  }

  async getAttractivenessHint(input: {
    userId: number;
    segmentKey?: string;
    collection: string;
    maxAbsDelta: number;
  }): Promise<{ boost: number; source: string } | null> {
    if (!this.enabled || !input.collection || !Number.isFinite(input.userId)) {
      return null;
    }

    const maxAbs = Math.max(0, Math.min(0.5, Number(input.maxAbsDelta) || 0));
    if (maxAbs <= 0) {
      return null;
    }

    const pointId = encodeURIComponent(`user:${input.userId}`);
    const collection = encodeURIComponent(input.collection);
    const requestUrl = `${this.url}/collections/${collection}/points/${pointId}?with_payload=true&with_vector=false`;

    try {
      const response = await fetch(requestUrl, {
        headers: this.apiKey ? { "api-key": this.apiKey } : {},
      });
      if (!response.ok) {
        return null;
      }

      const payload = (await response.json()) as {
        result?: {
          payload?: Record<string, unknown>;
        };
      };
      const pointPayload = payload.result?.payload;
      if (!pointPayload || typeof pointPayload !== "object") {
        return null;
      }

      let boost = this.readNumber(pointPayload.attractiveness_boost);
      const segmentBoost = this.readSegmentBoost(pointPayload.segment_boosts, input.segmentKey);
      if (segmentBoost != null) {
        boost += segmentBoost;
      }
      if (!Number.isFinite(boost) || Math.abs(boost) <= 1e-9) {
        return null;
      }

      const sourceRaw = pointPayload.source_model;
      const source =
        typeof sourceRaw === "string" && sourceRaw.trim()
          ? `qdrant:${sourceRaw.trim()}`
          : "qdrant:payload";

      return {
        boost: clamp(boost, -maxAbs, maxAbs),
        source,
      };
    } catch {
      return null;
    }
  }

  async getCandidateEnrichment(input: {
    userId: number;
    segmentKey?: string;
    collection: string;
    limit: number;
  }): Promise<{ candidateIds: number[]; source: string } | null> {
    if (!this.enabled || !input.collection || !Number.isFinite(input.userId)) {
      return null;
    }

    const limit = Math.max(1, Math.min(100, Number(input.limit) || 0));
    if (limit <= 0) {
      return null;
    }

    const pointId = encodeURIComponent(`user:${input.userId}`);
    const collection = encodeURIComponent(input.collection);
    const requestUrl = `${this.url}/collections/${collection}/points/${pointId}?with_payload=true&with_vector=false`;

    try {
      const response = await fetch(requestUrl, {
        headers: this.apiKey ? { "api-key": this.apiKey } : {},
      });
      if (!response.ok) {
        return null;
      }

      const payload = (await response.json()) as {
        result?: {
          payload?: Record<string, unknown>;
        };
      };
      const pointPayload = payload.result?.payload;
      if (!pointPayload || typeof pointPayload !== "object") {
        return null;
      }

      const candidates = this.readCandidateIds(pointPayload, input.segmentKey, limit);
      if (candidates.length === 0) {
        return null;
      }

      const sourceRaw = pointPayload.source_model;
      const source =
        typeof sourceRaw === "string" && sourceRaw.trim()
          ? `qdrant:${sourceRaw.trim()}`
          : "qdrant:payload";

      return {
        candidateIds: candidates,
        source,
      };
    } catch {
      return null;
    }
  }

  private readNumber(value: unknown): number {
    const parsed = typeof value === "number" ? value : Number(value ?? NaN);
    return Number.isFinite(parsed) ? parsed : 0;
  }

  private readSegmentBoost(value: unknown, segmentKey: string | undefined): number | null {
    if (!segmentKey || !value || typeof value !== "object" || Array.isArray(value)) {
      return null;
    }
    const raw = (value as Record<string, unknown>)[segmentKey];
    const parsed = typeof raw === "number" ? raw : Number(raw ?? NaN);
    return Number.isFinite(parsed) ? parsed : null;
  }

  private readCandidateIds(
    payload: Record<string, unknown>,
    segmentKey: string | undefined,
    limit: number,
  ): number[] {
    const seen = new Set<number>();
    const out: number[] = [];

    const pushIds = (raw: unknown) => {
      if (!Array.isArray(raw)) {
        return;
      }
      for (const item of raw) {
        const id = Number(item);
        if (!Number.isInteger(id) || id <= 0 || seen.has(id)) {
          continue;
        }
        seen.add(id);
        out.push(id);
        if (out.length >= limit) {
          return;
        }
      }
    };

    if (segmentKey) {
      const segmentBuckets = payload.segment_candidate_ids;
      if (segmentBuckets && typeof segmentBuckets === "object" && !Array.isArray(segmentBuckets)) {
        pushIds((segmentBuckets as Record<string, unknown>)[segmentKey]);
      }
    }

    if (out.length < limit) {
      pushIds(payload.candidate_ids);
    }

    return out.slice(0, limit);
  }
}
