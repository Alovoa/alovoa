import { OrchestratorConfig } from "../config";

export class UnleashClient {
  private readonly enabled: boolean;
  private readonly baseUrl: string;
  private readonly apiToken: string;
  private readonly appName: string;

  constructor(config: OrchestratorConfig) {
    this.enabled = config.enableUnleash;
    this.baseUrl = config.unleashUrl.replace(/\/$/, "");
    this.apiToken = config.unleashApiToken;
    this.appName = config.unleashAppName;
  }

  isEnabled(): boolean {
    return this.enabled;
  }

  async health(): Promise<{ ok: boolean; message: string }> {
    if (!this.enabled) {
      return { ok: false, message: "disabled" };
    }

    try {
      const response = await fetch(`${this.baseUrl}/admin/projects/default/features`, {
        headers: {
          Authorization: this.apiToken,
          Accept: "application/json",
        },
      });
      if (!response.ok) {
        return { ok: false, message: `http_${response.status}` };
      }
      return { ok: true, message: "ok" };
    } catch (error) {
      return { ok: false, message: `error:${String(error)}` };
    }
  }

  async isFeatureEnabled(featureName: string, context: Record<string, string>): Promise<boolean> {
    if (!this.enabled) {
      return false;
    }

    try {
      const response = await fetch(`${this.baseUrl}/client/features`, {
        method: "POST",
        headers: {
          Authorization: this.apiToken,
          "Content-Type": "application/json",
          "UNLEASH-APPNAME": this.appName,
        },
        body: JSON.stringify({
          context,
        }),
      });

      if (!response.ok) {
        return false;
      }

      const payload = (await response.json()) as {
        features?: Array<{ name: string; enabled: boolean }>;
      };

      return payload.features?.some((f) => f.name === featureName && f.enabled) ?? false;
    } catch {
      return false;
    }
  }
}
