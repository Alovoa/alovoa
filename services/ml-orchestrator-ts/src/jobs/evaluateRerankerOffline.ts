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
    process.env.RERANKER_EVAL_SCRIPT ||
    path.join(root, "scripts", "recommender", "evaluate_reranker_offline.py");

  const inputCsv = requireEnv("RERANKER_EVAL_INPUT_CSV");
  const outputJson =
    process.env.RERANKER_EVAL_OUTPUT_JSON ||
    path.join(root, "logs", "reranker-eval", "latest_report.json");

  const python = process.env.PYTHON_BIN || "python3";

  const args = [
    scriptPath,
    "--input-csv", inputCsv,
    "--output-json", outputJson,
    "--k", process.env.RERANKER_EVAL_K || "20",
    "--control-col", process.env.RERANKER_EVAL_CONTROL_COL || "score_control",
    "--treatment-col", process.env.RERANKER_EVAL_TREATMENT_COL || "score_treatment",
    "--label-col", process.env.RERANKER_EVAL_LABEL_COL || "label",
    "--user-col", process.env.RERANKER_EVAL_USER_COL || "user_id",
    "--item-col", process.env.RERANKER_EVAL_ITEM_COL || "candidate_id",
    "--group-col", process.env.RERANKER_EVAL_GROUP_COL || "segment_key",
  ];

  await new Promise<void>((resolve, reject) => {
    const child = spawn(python, args, { stdio: "inherit" });
    child.on("error", (error) => reject(error));
    child.on("exit", (code) => {
      if (code === 0) {
        resolve();
      } else {
        reject(new Error(`Reranker offline eval exited with code ${code}`));
      }
    });
  });
}

run().catch((error) => {
  // eslint-disable-next-line no-console
  console.error(error);
  process.exit(1);
});
