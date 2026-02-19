import { spawn } from "node:child_process";
import { mkdirSync, readdirSync, rmSync, statSync, writeFileSync } from "node:fs";
import path from "node:path";

function requireEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Missing required env var: ${name}`);
  }
  return value;
}

function parseList(raw: string | undefined, fallback: string[]): string[] {
  if (!raw || !raw.trim()) {
    return fallback;
  }
  return raw
    .split(",")
    .map((x) => x.trim())
    .filter(Boolean);
}

function parseModels(rawModel: string | undefined, rawModels: string | undefined): string[] {
  const normalized = (rawModel || "").trim().toLowerCase();
  if (normalized === "both") {
    return ["implicit", "lightfm"];
  }
  const explicit = parseList(rawModels, []);
  if (explicit.length > 0) {
    return explicit
      .map((x) => x.toLowerCase())
      .filter((x) => x === "implicit" || x === "lightfm");
  }
  if (normalized === "implicit" || normalized === "lightfm") {
    return [normalized];
  }
  return ["implicit"];
}

function toSafeToken(value: string): string {
  return value
    .toLowerCase()
    .replace(/[^a-z0-9._-]+/g, "_")
    .replace(/^_+|_+$/g, "")
    .slice(0, 80) || "default";
}

function runPython(python: string, args: string[]): Promise<void> {
  return new Promise<void>((resolve, reject) => {
    const child = spawn(python, args, { stdio: "inherit" });
    child.on("error", reject);
    child.on("exit", (code) => {
      if (code === 0) {
        resolve();
      } else {
        reject(new Error(`Collaborative prior trainer exited with code ${code}`));
      }
    });
  });
}

function purgeExpiredArtifacts(dir: string, retentionDays: number, keepMinFiles: number): void {
  const entries = readdirSync(dir, { withFileTypes: true })
    .filter((entry) => entry.isFile())
    .map((entry) => ({
      name: entry.name,
      filePath: path.join(dir, entry.name),
      mtimeMs: statSync(path.join(dir, entry.name)).mtimeMs,
    }))
    .sort((a, b) => b.mtimeMs - a.mtimeMs);

  if (entries.length <= keepMinFiles) {
    return;
  }

  const cutoff = Date.now() - retentionDays * 24 * 60 * 60 * 1000;
  let kept = 0;
  for (const entry of entries) {
    kept += 1;
    if (kept <= keepMinFiles) {
      continue;
    }
    if (entry.mtimeMs >= cutoff) {
      continue;
    }
    rmSync(entry.filePath, { force: true });
  }
}

async function run(): Promise<void> {
  const root = path.resolve(process.cwd(), "..", "..");
  const scriptPath =
    process.env.CF_TRAINER_SCRIPT || path.join(root, "scripts", "recommender", "train_cf_priors.py");

  const dbUrl = requireEnv("CF_DB_URL");
  const python = process.env.PYTHON_BIN || "python3";
  const artifactDir = process.env.CF_ARTIFACT_DIR || path.join(root, "logs", "cf-priors");
  const retentionDays = Math.max(1, Number(process.env.CF_RETENTION_DAYS || 30));
  const keepMinFiles = Math.max(1, Number(process.env.CF_KEEP_MIN_FILES || 20));
  const models = parseModels(process.env.CF_MODEL, process.env.CF_MODELS);
  const segments = parseList(process.env.CF_SEGMENT_KEYS, [process.env.CF_SEGMENT_KEY || "*"]);

  mkdirSync(artifactDir, { recursive: true });

  const timestamp = new Date().toISOString().replace(/[:.]/g, "-");
  const outputs: Array<{
    model: string;
    segmentKey: string;
    sqlPath: string;
  }> = [];

  for (const model of models) {
    for (const segmentKey of segments) {
      const fileName = `${timestamp}__${toSafeToken(model)}__${toSafeToken(segmentKey)}.sql`;
      const outputSql = path.join(artifactDir, fileName);

      const args = [
        scriptPath,
        "--db-url", dbUrl,
        "--model", model,
        "--segment-key", segmentKey,
        "--since-days", process.env.CF_SINCE_DAYS || "90",
        "--factors", process.env.CF_FACTORS || "32",
        "--regularization", process.env.CF_REGULARIZATION || "0.01",
        "--iterations", process.env.CF_ITERATIONS || "20",
        "--epochs", process.env.CF_EPOCHS || "20",
        "--n0", process.env.CF_N0 || "200",
        "--output-sql", outputSql,
      ];

      await runPython(python, args);
      outputs.push({ model, segmentKey, sqlPath: outputSql });
    }
  }

  const manifestPath = path.join(artifactDir, `${timestamp}__manifest.json`);
  writeFileSync(
    manifestPath,
    JSON.stringify(
      {
        generatedAt: new Date().toISOString(),
        models,
        segments,
        outputs,
      },
      null,
      2,
    ),
    "utf-8",
  );

  purgeExpiredArtifacts(artifactDir, retentionDays, keepMinFiles);
}

run().catch((error) => {
  // eslint-disable-next-line no-console
  console.error(error);
  process.exit(1);
});
