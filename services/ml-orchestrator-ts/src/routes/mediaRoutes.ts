import { FastifyInstance } from "fastify";
import {
  attractivenessRequestSchema,
  faceQualityRequestSchema,
  livenessRequestSchema,
  moderationRequestSchema,
  textModerationRequestSchema,
  verificationRequestSchema,
  videoAnalysisRequestSchema,
} from "../types/contracts";
import { OrchestratorService } from "../providers/orchestratorService";

function parseOrThrow<T>(schema: { parse: (value: unknown) => T }, payload: unknown): T {
  return schema.parse(payload);
}

export async function registerMediaRoutes(app: FastifyInstance, service: OrchestratorService): Promise<void> {
  app.post("/verify/liveness/challenges", async (request, reply) => {
    try {
      const payload = parseOrThrow(livenessRequestSchema, request.body);
      return await service.getLivenessChallenges(payload);
    } catch (error) {
      request.log.warn({ err: error }, "Invalid liveness request");
      return reply.code(400).send({ detail: "Invalid request payload" });
    }
  });

  app.post("/verify/face", async (request, reply) => {
    try {
      const payload = parseOrThrow(verificationRequestSchema, request.body);
      return await service.verifyFace(payload);
    } catch (error) {
      request.log.warn({ err: error }, "Face verification failed");
      return reply.code(400).send({ detail: "Invalid request payload" });
    }
  });

  app.post("/video/analyze", async (request, reply) => {
    try {
      const payload = parseOrThrow(videoAnalysisRequestSchema, request.body);
      return await service.analyzeVideo(payload);
    } catch (error) {
      request.log.warn({ err: error }, "Video analysis failed");
      return reply.code(400).send({ detail: "Invalid request payload" });
    }
  });

  app.post("/attractiveness/score", async (request, reply) => {
    try {
      const payload = parseOrThrow(attractivenessRequestSchema, request.body);
      return await service.scoreAttractiveness(payload);
    } catch (error) {
      request.log.warn({ err: error }, "Attractiveness scoring failed");
      return reply.code(400).send({ detail: "Front image is required or payload invalid" });
    }
  });

  app.post("/moderation/image", async (request, reply) => {
    try {
      const payload = parseOrThrow(moderationRequestSchema, request.body);
      return await service.moderateImage(payload);
    } catch (error) {
      request.log.warn({ err: error }, "Image moderation failed");
      return reply.code(400).send({ detail: "Image is required or payload invalid" });
    }
  });

  app.post("/moderation/text", async (request, reply) => {
    try {
      const payload = parseOrThrow(textModerationRequestSchema, request.body);
      return await service.moderateText(payload);
    } catch (error) {
      request.log.warn({ err: error }, "Text moderation failed");
      return reply.code(400).send({ detail: "Text is required or payload invalid" });
    }
  });

  app.post("/quality/face", async (request, reply) => {
    try {
      const payload = parseOrThrow(faceQualityRequestSchema, request.body);
      return await service.scoreFaceQuality(payload);
    } catch (error) {
      request.log.warn({ err: error }, "Face quality scoring failed");
      return reply.code(400).send({ detail: "Image is required or payload invalid" });
    }
  });

  app.post("/video/transcribe", async (request, reply) => {
    try {
      return await service.transcribeVideo((request.body ?? {}) as Record<string, unknown>);
    } catch (error) {
      request.log.warn({ err: error }, "Video transcription failed");
      return reply.code(502).send({ detail: "Transcription provider unavailable" });
    }
  });

  app.post("/video/analyze-transcript", async (request, reply) => {
    try {
      return await service.analyzeTranscript((request.body ?? {}) as Record<string, unknown>);
    } catch (error) {
      request.log.warn({ err: error }, "Transcript analysis failed");
      return reply.code(502).send({ detail: "Transcript analysis provider unavailable" });
    }
  });

  app.get("/integrations/status", async () => {
    return service.integrationStatus();
  });

  app.get("/integrations/qdrant/candidate-enrichment", async (request, reply) => {
    const query = (request.query ?? {}) as Record<string, unknown>;
    const userId = Number(query.user_id ?? NaN);
    if (!Number.isInteger(userId) || userId <= 0) {
      return reply.code(400).send({ detail: "user_id query param is required and must be positive integer" });
    }

    const segmentKeyRaw = query.segment_key;
    const limitRaw = query.limit;
    const segmentKey = typeof segmentKeyRaw === "string" && segmentKeyRaw.trim() ? segmentKeyRaw.trim() : undefined;
    const limit = Number.isFinite(Number(limitRaw)) ? Number(limitRaw) : undefined;
    return service.getCandidateEnrichment({
      userId,
      segmentKey,
      limit,
    });
  });
}
