package com.nonononoki.alovoa.jobs.ml;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

public class MlJobsRunner {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            System.exit(2);
        }

        String job = args[0].trim().toLowerCase(Locale.ROOT);
        Path root = Paths.get(envOrDefault("AURA_ML_ROOT", System.getProperty("user.dir"))).toAbsolutePath();
        String python = envOrDefault("PYTHON_BIN", "python3");

        switch (job) {
            case "build-scut-calibration" -> runBuildScutCalibration(root, python);
            case "build-nsfw-calibration" -> runBuildNsfwCalibration(root, python);
            case "build-deepfake-calibration" -> runBuildDeepfakeCalibration(root, python);
            case "evaluate-reranker-offline" -> runEvaluateRerankerOffline(root, python);
            case "manage-reranker-rollout" -> runManageRerankerRollout(root, python);
            case "train-cf-priors" -> runTrainCollaborativePriors(root, python);
            default -> {
                System.err.println("Unknown job: " + job);
                printUsage();
                System.exit(2);
            }
        }
    }

    private static void runBuildScutCalibration(Path root, String python) throws Exception {
        Path script = Paths.get(envOrDefault("SCUT_CALIBRATION_SCRIPT",
                root.resolve("scripts/recommender/build_scut_calibration.py").toString()));
        Path repoRoot = Paths.get(envOrDefault("SCUT_CALIBRATION_REPO_ROOT",
                root.resolve("third_party/repos/SCUT-FBP5500-Database-Release").toString()));
        Path outputJson = Paths.get(envOrDefault("SCUT_CALIBRATION_OUTPUT",
                root.resolve("configs/attractiveness/scut_score_calibration.json").toString()));
        String quantiles = envOrDefault("SCUT_CALIBRATION_QUANTILES", "201");

        Files.createDirectories(outputJson.getParent());
        runProcess(List.of(
                python,
                script.toString(),
                "--repo-root", repoRoot.toString(),
                "--output-json", outputJson.toString(),
                "--quantiles", quantiles
        ));
    }

    private static void runBuildNsfwCalibration(Path root, String python) throws Exception {
        Path script = Paths.get(envOrDefault("NSFW_CALIBRATION_SCRIPT",
                root.resolve("scripts/recommender/build_nsfw_calibration.py").toString()));
        String inputJsonl = requireEnv("NSFW_CALIBRATION_INPUT_JSONL");
        Path outputJson = Paths.get(envOrDefault("NSFW_CALIBRATION_OUTPUT",
                root.resolve("configs/moderation/nsfw_thresholds.json").toString()));
        String defaultThreshold = envOrDefault("NSFW_CALIBRATION_DEFAULT_THRESHOLD", "0.60");
        String minSamples = envOrDefault("NSFW_CALIBRATION_MIN_SAMPLES", "30");

        Files.createDirectories(outputJson.getParent());
        runProcess(List.of(
                python,
                script.toString(),
                "--input-jsonl", inputJsonl,
                "--output-json", outputJson.toString(),
                "--default-threshold", defaultThreshold,
                "--min-samples-per-segment", minSamples
        ));
    }

    private static void runBuildDeepfakeCalibration(Path root, String python) throws Exception {
        Path script = Paths.get(envOrDefault("DEEPFAKE_CALIBRATION_SCRIPT",
                root.resolve("scripts/recommender/build_deepfake_calibration.py").toString()));
        String inputJsonl = requireEnv("DEEPFAKE_CALIBRATION_INPUT_JSONL");
        Path outputJson = Paths.get(envOrDefault("DEEPFAKE_CALIBRATION_OUTPUT",
                root.resolve("configs/moderation/deepfake_thresholds.json").toString()));
        String minSamples = envOrDefault("DEEPFAKE_CALIBRATION_MIN_SAMPLES", "30");
        String targetPrecision = envOrDefault("DEEPFAKE_CALIBRATION_TARGET_PRECISION", "0.90");

        Files.createDirectories(outputJson.getParent());
        runProcess(List.of(
                python,
                script.toString(),
                "--input-jsonl", inputJsonl,
                "--output-json", outputJson.toString(),
                "--min-samples-per-segment", minSamples,
                "--target-precision", targetPrecision
        ));
    }

    private static void runEvaluateRerankerOffline(Path root, String python) throws Exception {
        Path script = Paths.get(envOrDefault("RERANKER_EVAL_SCRIPT",
                root.resolve("scripts/recommender/evaluate_reranker_offline.py").toString()));
        String inputCsv = requireEnv("RERANKER_EVAL_INPUT_CSV");
        Path outputJson = Paths.get(envOrDefault("RERANKER_EVAL_OUTPUT_JSON",
                root.resolve("logs/reranker-eval/latest_report.json").toString()));
        String k = envOrDefault("RERANKER_EVAL_K", "20");
        String controlCol = envOrDefault("RERANKER_EVAL_CONTROL_COL", "score_control");
        String treatmentCol = envOrDefault("RERANKER_EVAL_TREATMENT_COL", "score_treatment");
        String labelCol = envOrDefault("RERANKER_EVAL_LABEL_COL", "label");
        String userCol = envOrDefault("RERANKER_EVAL_USER_COL", "user_id");
        String itemCol = envOrDefault("RERANKER_EVAL_ITEM_COL", "candidate_id");
        String groupCol = envOrDefault("RERANKER_EVAL_GROUP_COL", "segment_key");

        Files.createDirectories(outputJson.getParent());
        runProcess(List.of(
                python,
                script.toString(),
                "--input-csv", inputCsv,
                "--output-json", outputJson.toString(),
                "--k", k,
                "--control-col", controlCol,
                "--treatment-col", treatmentCol,
                "--label-col", labelCol,
                "--user-col", userCol,
                "--item-col", itemCol,
                "--group-col", groupCol
        ));
    }

    private static void runManageRerankerRollout(Path root, String python) throws Exception {
        Path script = Paths.get(envOrDefault("RERANKER_ROLLOUT_SCRIPT",
                root.resolve("scripts/recommender/manage_reranker_rollout.py").toString()));
        String dbUrl = requireEnv("ROLLOUT_DB_URL");

        List<String> command = new ArrayList<>(List.of(
                python,
                script.toString(),
                "--db-url", dbUrl,
                "--flag-name", envOrDefault("ROLLOUT_FLAG_NAME", "MATCH_RERANKER"),
                "--segment-key", envOrDefault("ROLLOUT_SEGMENT_KEY", "*"),
                "--control-variant", envOrDefault("ROLLOUT_CONTROL_VARIANT", "control"),
                "--treatment-variant", envOrDefault("ROLLOUT_TREATMENT_VARIANT", "treatment"),
                "--window-hours", envOrDefault("ROLLOUT_WINDOW_HOURS", "24"),
                "--min-treatment-impressions", envOrDefault("ROLLOUT_MIN_TREATMENT_IMPRESSIONS", "500"),
                "--max-conv-drop", envOrDefault("ROLLOUT_MAX_CONV_DROP", "0.05"),
                "--max-report-rate-increase", envOrDefault("ROLLOUT_MAX_REPORT_RATE_INCREASE", "0.20"),
                "--max-block-rate-increase", envOrDefault("ROLLOUT_MAX_BLOCK_RATE_INCREASE", "0.20"),
                "--max-top-decile-share-increase", envOrDefault("ROLLOUT_MAX_TOP_DECILE_SHARE_INCREASE", "0.05"),
                "--rollout-stages", envOrDefault("ROLLOUT_STAGES", "1,10,50,100")
        ));
        if (parseBool(System.getenv("ROLLOUT_APPLY"))) {
            command.add("--apply");
        }
        runProcess(command);
    }

    private static void runTrainCollaborativePriors(Path root, String python) throws Exception {
        Path script = Paths.get(envOrDefault("CF_TRAINER_SCRIPT",
                root.resolve("scripts/recommender/train_cf_priors.py").toString()));
        String dbUrl = requireEnv("CF_DB_URL");

        Path artifactDir = Paths.get(envOrDefault("CF_ARTIFACT_DIR", root.resolve("logs/cf-priors").toString()));
        int retentionDays = Math.max(1, parseInt(envOrDefault("CF_RETENTION_DAYS", "30"), 30));
        int keepMinFiles = Math.max(1, parseInt(envOrDefault("CF_KEEP_MIN_FILES", "20"), 20));
        String sinceDays = envOrDefault("CF_SINCE_DAYS", "90");
        String factors = envOrDefault("CF_FACTORS", "32");
        String regularization = envOrDefault("CF_REGULARIZATION", "0.01");
        String iterations = envOrDefault("CF_ITERATIONS", "20");
        String epochs = envOrDefault("CF_EPOCHS", "20");
        String n0 = envOrDefault("CF_N0", "200");

        List<String> models = parseModels(System.getenv("CF_MODEL"), System.getenv("CF_MODELS"));
        List<String> segments = parseList(System.getenv("CF_SEGMENT_KEYS"),
                List.of(envOrDefault("CF_SEGMENT_KEY", "*")));

        Files.createDirectories(artifactDir);
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(":", "-").replace(".", "-");
        List<Map<String, Object>> outputs = new ArrayList<>();

        for (String model : models) {
            for (String segment : segments) {
                String fileName = timestamp + "__" + toSafeToken(model) + "__" + toSafeToken(segment) + ".sql";
                Path outputSql = artifactDir.resolve(fileName);
                runProcess(List.of(
                        python,
                        script.toString(),
                        "--db-url", dbUrl,
                        "--model", model,
                        "--segment-key", segment,
                        "--since-days", sinceDays,
                        "--factors", factors,
                        "--regularization", regularization,
                        "--iterations", iterations,
                        "--epochs", epochs,
                        "--n0", n0,
                        "--output-sql", outputSql.toString()
                ));
                outputs.add(Map.of(
                        "model", model,
                        "segmentKey", segment,
                        "sqlPath", outputSql.toString()
                ));
            }
        }

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("generatedAt", Instant.now().toString());
        manifest.put("models", models);
        manifest.put("segments", segments);
        manifest.put("outputs", outputs);

        Path manifestPath = artifactDir.resolve(timestamp + "__manifest.json");
        Files.writeString(manifestPath, OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(manifest),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        purgeExpiredArtifacts(artifactDir, retentionDays, keepMinFiles);
    }

    private static void runProcess(List<String> command) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        processBuilder.inheritIO();
        Process process = processBuilder.start();
        int code = process.waitFor();
        if (code != 0) {
            throw new IllegalStateException("Command failed with exit code " + code + ": " + String.join(" ", command));
        }
    }

    private static void purgeExpiredArtifacts(Path artifactDir, int retentionDays, int keepMinFiles) throws IOException {
        if (!Files.exists(artifactDir)) {
            return;
        }
        List<Path> files;
        try (Stream<Path> stream = Files.list(artifactDir)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .sorted((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .toList();
        }

        if (files.size() <= keepMinFiles) {
            return;
        }

        Instant cutoff = Instant.now().minusSeconds((long) retentionDays * 24 * 60 * 60);
        int kept = 0;
        for (Path file : files) {
            kept++;
            if (kept <= keepMinFiles) {
                continue;
            }
            if (Files.getLastModifiedTime(file).toInstant().isAfter(cutoff)) {
                continue;
            }
            Files.deleteIfExists(file);
        }
    }

    private static List<String> parseModels(String model, String modelsCsv) {
        String normalizedModel = (model == null ? "" : model.trim().toLowerCase(Locale.ROOT));
        if ("both".equals(normalizedModel)) {
            return List.of("implicit", "lightfm");
        }

        List<String> explicit = parseList(modelsCsv, List.of());
        List<String> out = explicit.stream()
                .map(x -> x.toLowerCase(Locale.ROOT))
                .filter(x -> x.equals("implicit") || x.equals("lightfm"))
                .toList();
        if (!out.isEmpty()) {
            return out;
        }

        if ("implicit".equals(normalizedModel) || "lightfm".equals(normalizedModel)) {
            return List.of(normalizedModel);
        }
        return List.of("implicit");
    }

    private static List<String> parseList(String raw, List<String> fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        List<String> out = new ArrayList<>();
        for (String part : raw.split(",")) {
            String token = part.trim();
            if (!token.isEmpty()) {
                out.add(token);
            }
        }
        return out.isEmpty() ? fallback : out;
    }

    private static String toSafeToken(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "_").replaceAll("^_+|_+$", "");
        if (normalized.isEmpty()) {
            return "default";
        }
        return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static boolean parseBool(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("1")
                || normalized.equals("true")
                || normalized.equals("yes")
                || normalized.equals("on");
    }

    private static String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required env var: " + key);
        }
        return value;
    }

    private static String envOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private static void printUsage() {
        System.err.println("Usage: MlJobsRunner <job>");
        System.err.println("Jobs:");
        System.err.println("  train-cf-priors");
        System.err.println("  build-scut-calibration");
        System.err.println("  build-nsfw-calibration");
        System.err.println("  build-deepfake-calibration");
        System.err.println("  evaluate-reranker-offline");
        System.err.println("  manage-reranker-rollout");
    }
}
