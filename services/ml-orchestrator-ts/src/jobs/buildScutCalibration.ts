import { spawn } from "node:child_process";
import path from "node:path";

async function run(): Promise<void> {
  const root = path.resolve(process.cwd(), "..", "..");
  const scriptPath =
    process.env.SCUT_CALIBRATION_SCRIPT ||
    path.join(root, "scripts", "recommender", "build_scut_calibration.py");

  const repoRoot =
    process.env.SCUT_CALIBRATION_REPO_ROOT ||
    path.join(root, "third_party", "repos", "SCUT-FBP5500-Database-Release");

  const outputJson =
    process.env.SCUT_CALIBRATION_OUTPUT ||
    path.join(root, "configs", "attractiveness", "scut_score_calibration.json");

  const quantiles = process.env.SCUT_CALIBRATION_QUANTILES || "201";
  const python = process.env.PYTHON_BIN || "python3";

  const args = [
    scriptPath,
    "--repo-root", repoRoot,
    "--output-json", outputJson,
    "--quantiles", quantiles,
  ];

  await new Promise<void>((resolve, reject) => {
    const child = spawn(python, args, { stdio: "inherit" });
    child.on("error", (error) => reject(error));
    child.on("exit", (code) => {
      if (code === 0) {
        resolve();
      } else {
        reject(new Error(`SCUT calibration builder exited with code ${code}`));
      }
    });
  });
}

run().catch((error) => {
  // eslint-disable-next-line no-console
  console.error(error);
  process.exit(1);
});
