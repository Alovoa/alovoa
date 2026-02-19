import { OrchestratorConfig } from "../config";

export class OpenFgaClient {
  private readonly enabled: boolean;
  private readonly baseUrl: string;
  private readonly storeId: string;
  private readonly authzModelId: string;
  private readonly apiToken: string;

  constructor(config: OrchestratorConfig) {
    this.enabled = config.enableOpenFga;
    this.baseUrl = config.openFgaUrl.replace(/\/$/, "");
    this.storeId = config.openFgaStoreId;
    this.authzModelId = config.openFgaAuthzModelId;
    this.apiToken = config.openFgaApiToken;
  }

  isEnabled(): boolean {
    return this.enabled;
  }

  async health(): Promise<{ ok: boolean; message: string }> {
    if (!this.enabled) {
      return { ok: false, message: "disabled" };
    }

    if (!this.storeId) {
      return { ok: false, message: "missing_store_id" };
    }

    try {
      const response = await fetch(`${this.baseUrl}/stores/${this.storeId}`, {
        headers: this.headers(),
      });
      if (!response.ok) {
        return { ok: false, message: `http_${response.status}` };
      }
      return { ok: true, message: "ok" };
    } catch (error) {
      return { ok: false, message: `error:${String(error)}` };
    }
  }

  async checkAccess(input: {
    user: string;
    relation: string;
    object: string;
  }): Promise<boolean> {
    if (!this.enabled || !this.storeId) {
      return false;
    }

    try {
      const response = await fetch(`${this.baseUrl}/stores/${this.storeId}/check`, {
        method: "POST",
        headers: {
          ...this.headers(),
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          tuple_key: {
            user: input.user,
            relation: input.relation,
            object: input.object,
          },
          authorization_model_id: this.authzModelId || undefined,
        }),
      });

      if (!response.ok) {
        return false;
      }

      const payload = (await response.json()) as { allowed?: boolean };
      return payload.allowed ?? false;
    } catch {
      return false;
    }
  }

  private headers(): Record<string, string> {
    return this.apiToken
      ? {
          Authorization: `Bearer ${this.apiToken}`,
        }
      : {};
  }
}
