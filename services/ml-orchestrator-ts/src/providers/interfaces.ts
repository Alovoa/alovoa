import {
  AttractivenessRequest,
  AttractivenessResponse,
  FaceQualityRequest,
  FaceQualityResponse,
  LivenessRequest,
  LivenessResponse,
  ModerationRequest,
  ModerationResponse,
  TextModerationRequest,
  TextModerationResponse,
  VerificationRequest,
  VerificationResponse,
  VideoAnalysisRequest,
  VideoAnalysisResponse,
} from "../types/contracts";

export interface MediaProvider {
  getLivenessChallenges(request: LivenessRequest): Promise<LivenessResponse>;
  verifyFace(request: VerificationRequest): Promise<VerificationResponse>;
  analyzeVideo(request: VideoAnalysisRequest): Promise<VideoAnalysisResponse>;
  scoreAttractiveness(request: AttractivenessRequest): Promise<AttractivenessResponse>;
  moderateImage(request: ModerationRequest): Promise<ModerationResponse>;
  moderateText?(request: TextModerationRequest): Promise<TextModerationResponse>;
  scoreFaceQuality?(request: FaceQualityRequest): Promise<FaceQualityResponse>;
  transcribeVideo?(request: Record<string, unknown>): Promise<Record<string, unknown>>;
  analyzeTranscript?(request: Record<string, unknown>): Promise<Record<string, unknown>>;
}

export interface NsfwScriptPayload {
  nsfw_score: number;
  confidence: number;
  categories?: Record<string, number>;
  signals?: Record<string, number>;
}

export interface AttractivenessScriptPayload {
  score: number;
  confidence: number;
  provider?: string;
  model_version?: string;
  signals?: Record<string, number>;
}

export interface TextModerationScriptPayload {
  labels?: Record<string, number>;
  toxicity_score?: number;
  max_label?: string;
  provider?: string;
  model_version?: string;
  signals?: Record<string, number>;
}

export interface FaceQualityScriptPayload {
  quality_score?: number;
  score?: number;
  confidence?: number;
  provider?: string;
  model_version?: string;
  signals?: Record<string, number>;
}
