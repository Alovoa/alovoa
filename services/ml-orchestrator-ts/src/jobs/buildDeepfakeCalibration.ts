import { spawn } from "node:child_process";
import path from "node:path";

function requireEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Missing required env var: ${name}`);
  }
  return value;
}

async function run(): Promise<void> {
  const root = path.resolve(process.cwd(), "..", "..");
  const scriptPath =
    process.env.DEEPFAKE_CALIBRATION_SCRIPT ||
    path.join(root, "scripts", "recommender", "build_deepfake_calibration.py");

  const inputJsonl = requireEnv("DEEPFAKE_CALIBRATION_INPUT_JSONL");
  const outputJson =
    process.env.DEEPFAKE_CALIBRATION_OUTPUT ||
    path.join(root, "configs", "moderation", "deepfake_thresholds.json");

  const python = process.env.PYTHON_BIN || "python3";
  const minSamples = process.env.DEEPFAKE_CALIBRATION_MIN_SAMPLES || "30";
  const targetPrecision = process.env.DEEPFAKE_CALIBRATION_TARGET_PRECISION || "0.90";

  const args = [
    scriptPath,
    "--input-jsonl", inputJsonl,
    "--output-json", outputJson,
    "--min-samples-per-segment", minSamples,
    "--target-precision", targetPrecision,
  ];

  await new Promise<void>((resolve, reject) => {
    const child = spawn(python, args, { stdio: "inherit" });
    child.on("error", (error) => reject(error));
    child.on("exit", (code) => {
      if (code === 0) {
        resolve();
      } else {
        reject(new Error(`Deepfake calibration builder exited with code ${code}`));
      }
    });
  });
}

run().catch((error) => {
  // eslint-disable-next-line no-console
  console.error(error);
  process.exit(1);
});
