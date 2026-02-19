import { spawn } from "node:child_process";
import path from "node:path";

function requireEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Missing required env var: ${name}`);
  }
  return value;
}

function parseBool(value: string | undefined): boolean {
  if (!value) {
    return false;
  }
  return ["1", "true", "yes", "on"].includes(value.toLowerCase());
}

async function run(): Promise<void> {
  const root = path.resolve(process.cwd(), "..", "..");
  const scriptPath =
    process.env.RERANKER_ROLLOUT_SCRIPT ||
    path.join(root, "scripts", "recommender", "manage_reranker_rollout.py");

  const dbUrl = requireEnv("ROLLOUT_DB_URL");
  const python = process.env.PYTHON_BIN || "python3";

  const args = [
    scriptPath,
    "--db-url", dbUrl,
    "--flag-name", process.env.ROLLOUT_FLAG_NAME || "MATCH_RERANKER",
    "--segment-key", process.env.ROLLOUT_SEGMENT_KEY || "*",
    "--control-variant", process.env.ROLLOUT_CONTROL_VARIANT || "control",
    "--treatment-variant", process.env.ROLLOUT_TREATMENT_VARIANT || "treatment",
    "--window-hours", process.env.ROLLOUT_WINDOW_HOURS || "24",
    "--min-treatment-impressions", process.env.ROLLOUT_MIN_TREATMENT_IMPRESSIONS || "500",
    "--max-conv-drop", process.env.ROLLOUT_MAX_CONV_DROP || "0.05",
    "--max-report-rate-increase", process.env.ROLLOUT_MAX_REPORT_RATE_INCREASE || "0.20",
    "--max-block-rate-increase", process.env.ROLLOUT_MAX_BLOCK_RATE_INCREASE || "0.20",
    "--max-top-decile-share-increase", process.env.ROLLOUT_MAX_TOP_DECILE_SHARE_INCREASE || "0.05",
    "--rollout-stages", process.env.ROLLOUT_STAGES || "1,10,50,100",
  ];

  if (parseBool(process.env.ROLLOUT_APPLY)) {
    args.push("--apply");
  }

  await new Promise<void>((resolve, reject) => {
    const child = spawn(python, args, { stdio: "inherit" });
    child.on("error", (error) => reject(error));
    child.on("exit", (code) => {
      if (code === 0) {
        resolve();
      } else {
        reject(new Error(`Reranker rollout manager exited with code ${code}`));
      }
    });
  });
}

run().catch((error) => {
  // eslint-disable-next-line no-console
  console.error(error);
  process.exit(1);
});
