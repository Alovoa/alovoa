import Fastify from "fastify";
import cors from "@fastify/cors";
import { OrchestratorConfig } from "./config";
import { PythonMediaProxyProvider } from "./providers/pythonMediaProxyProvider";
import { OrchestratorService } from "./providers/orchestratorService";
import { registerHealthRoute } from "./routes/healthRoute";
import { registerMediaRoutes } from "./routes/mediaRoutes";

export function buildServer(config: OrchestratorConfig) {
  const app = Fastify({
    logger: true,
    trustProxy: true,
  });

  app.register(cors, {
    origin: true,
    methods: ["GET", "POST", "OPTIONS"],
    allowedHeaders: ["Content-Type", "Authorization"],
  });

  const proxyProvider = new PythonMediaProxyProvider(config);
  const service = new OrchestratorService(config, proxyProvider);

  app.setErrorHandler((error, request, reply) => {
    request.log.error({ err: error }, "Unhandled orchestrator error");
    reply.code(500).send({ detail: "internal_error" });
  });

  registerHealthRoute(app, config);
  registerMediaRoutes(app, service);

  return app;
}
