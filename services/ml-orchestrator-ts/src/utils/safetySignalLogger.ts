import { appendFileSync, mkdirSync } from "node:fs";
import path from "node:path";

interface SafetyLoggerConfig {
  enabled: boolean;
  filePath: string;
}

export class SafetySignalLogger {
  private readonly enabled: boolean;
  private readonly filePath: string;
  private initialized = false;

  constructor(config: SafetyLoggerConfig) {
    this.enabled = config.enabled;
    this.filePath = config.filePath;
  }

  log(event: Record<string, unknown>): void {
    if (!this.enabled) {
      return;
    }
    try {
      this.ensureDir();
      appendFileSync(this.filePath, `${JSON.stringify(event)}\n`, "utf-8");
    } catch {
      // best-effort only; never fail request path
    }
  }

  private ensureDir(): void {
    if (this.initialized) {
      return;
    }
    mkdirSync(path.dirname(this.filePath), { recursive: true });
    this.initialized = true;
  }
}
