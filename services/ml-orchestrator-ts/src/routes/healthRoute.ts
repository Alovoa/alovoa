import { FastifyInstance } from "fastify";
import { OrchestratorConfig } from "../config";

export async function registerHealthRoute(app: FastifyInstance, config: OrchestratorConfig): Promise<void> {
  app.get("/health", async () => ({
    status: "healthy",
    service: "ml-orchestrator-ts",
    timestamp: new Date().toISOString(),
    providerMode: config.enableLocalModeration || config.enableLocalAttractiveness ? "hybrid" : "proxy",
  }));
}
