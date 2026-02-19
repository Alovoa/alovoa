import { loadConfig } from "./config";
import { buildServer } from "./server";

async function main(): Promise<void> {
  const config = loadConfig();
  const app = buildServer(config);

  try {
    await app.listen({
      host: config.host,
      port: config.port,
    });
    app.log.info({ host: config.host, port: config.port }, "ml-orchestrator-ts started");
  } catch (error) {
    app.log.error({ err: error }, "Failed to start ml-orchestrator-ts");
    process.exit(1);
  }
}

main();
