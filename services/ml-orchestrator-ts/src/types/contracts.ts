import { z } from "zod";

export const healthResponseSchema = z.object({
  status: z.string(),
  service: z.string(),
  timestamp: z.string().optional(),
  providerMode: z.string().optional(),
});

export const livenessRequestSchema = z.object({
  user_id: z.number().int().positive(),
});

export const livenessChallengeSchema = z.object({
  type: z.string(),
  instruction: z.string(),
});

export const livenessResponseSchema = z.object({
  session_id: z.string(),
  challenges: z.array(livenessChallengeSchema),
  timeout: z.number(),
  total_timeout: z.number(),
});

export const verificationRequestSchema = z.object({
  user_id: z.number().int().positive(),
  profile_image_url: z.string().min(1),
  verification_video_url: z.string().min(1),
  session_id: z.string().min(1),
});

export const verificationResponseSchema = z.object({
  verified: z.boolean(),
  face_match_score: z.number(),
  liveness_score: z.number(),
  deepfake_score: z.number(),
  issues: z.array(z.string()),
});

export const videoAnalysisRequestSchema = z.object({
  video_url: z.string().min(1),
  user_id: z.number().int().positive(),
});

export const videoAnalysisResponseSchema = z.object({
  duration: z.number().optional(),
  sentiment: z.record(z.number()).optional(),
  thumbnail_url: z.string().nullable().optional(),
  transcript: z.string().nullable().optional(),
  frame_count: z.number().optional(),
}).passthrough();

export const attractivenessRequestSchema = z.object({
  user_id: z.number().int().positive().optional(),
  front_image_base64: z.string().optional(),
  side_image_base64: z.string().optional(),
  front_image_url: z.string().optional(),
  side_image_url: z.string().optional(),
  segment_key: z.string().optional(),
});

export const attractivenessResponseSchema = z.object({
  score: z.number(),
  confidence: z.number(),
  provider: z.string(),
  model_version: z.string(),
  provider_versions: z.record(z.string()).optional(),
  signals: z.record(z.number()),
  repo_refs: z.array(z.string()),
});

export const moderationRequestSchema = z.object({
  user_id: z.number().int().positive().optional(),
  image_base64: z.string().optional(),
  image_url: z.string().optional(),
  image_type: z.string().optional(),
  segment_key: z.string().optional(),
});

export const moderationResponseSchema = z.object({
  is_safe: z.boolean(),
  nsfw_score: z.number(),
  confidence: z.number(),
  action: z.enum(["ALLOW", "BLOCK"]),
  provider: z.string(),
  categories: z.record(z.number()),
  signals: z.record(z.number()),
  repo_refs: z.array(z.string()),
});

export const textModerationRequestSchema = z.object({
  user_id: z.number().int().positive().optional(),
  text: z.string().min(1),
  content_type: z.string().optional(),
  segment_key: z.string().optional(),
});

export const textModerationResponseSchema = z.object({
  is_allowed: z.boolean(),
  decision: z.enum(["ALLOW", "WARN", "BLOCK"]),
  toxicity_score: z.number(),
  max_label: z.string().nullable().optional(),
  blocked_categories: z.array(z.string()),
  labels: z.record(z.number()),
  reason: z.string().nullable().optional(),
  provider: z.string(),
  model_version: z.string(),
  signals: z.record(z.number()),
  repo_refs: z.array(z.string()),
});

export const faceQualityRequestSchema = z.object({
  user_id: z.number().int().positive().optional(),
  image_base64: z.string().optional(),
  image_url: z.string().optional(),
  surface: z.string().optional(),
  segment_key: z.string().optional(),
});

export const faceQualityResponseSchema = z.object({
  quality_score: z.number(),
  confidence: z.number(),
  provider: z.string(),
  model_version: z.string(),
  signals: z.record(z.number()),
  repo_refs: z.array(z.string()),
});

export type HealthResponse = z.infer<typeof healthResponseSchema>;
export type LivenessRequest = z.infer<typeof livenessRequestSchema>;
export type LivenessResponse = z.infer<typeof livenessResponseSchema>;
export type VerificationRequest = z.infer<typeof verificationRequestSchema>;
export type VerificationResponse = z.infer<typeof verificationResponseSchema>;
export type VideoAnalysisRequest = z.infer<typeof videoAnalysisRequestSchema>;
export type VideoAnalysisResponse = z.infer<typeof videoAnalysisResponseSchema>;
export type AttractivenessRequest = z.infer<typeof attractivenessRequestSchema>;
export type AttractivenessResponse = z.infer<typeof attractivenessResponseSchema>;
export type ModerationRequest = z.infer<typeof moderationRequestSchema>;
export type ModerationResponse = z.infer<typeof moderationResponseSchema>;
export type TextModerationRequest = z.infer<typeof textModerationRequestSchema>;
export type TextModerationResponse = z.infer<typeof textModerationResponseSchema>;
export type FaceQualityRequest = z.infer<typeof faceQualityRequestSchema>;
export type FaceQualityResponse = z.infer<typeof faceQualityResponseSchema>;
