import { OrchestratorConfig } from "../config";
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
import { postJson } from "../utils/httpClient";
import { MediaProvider } from "./interfaces";

export class PythonMediaProxyProvider implements MediaProvider {
  private readonly baseUrl: string;
  private readonly timeoutMs: number;

  constructor(config: OrchestratorConfig) {
    this.baseUrl = config.pythonMediaServiceUrl.replace(/\/$/, "");
    this.timeoutMs = config.requestTimeoutMs;
  }

  getLivenessChallenges(request: LivenessRequest): Promise<LivenessResponse> {
    return postJson<LivenessResponse>(`${this.baseUrl}/verify/liveness/challenges`, request, this.timeoutMs);
  }

  verifyFace(request: VerificationRequest): Promise<VerificationResponse> {
    return postJson<VerificationResponse>(`${this.baseUrl}/verify/face`, request, this.timeoutMs);
  }

  analyzeVideo(request: VideoAnalysisRequest): Promise<VideoAnalysisResponse> {
    return postJson<VideoAnalysisResponse>(`${this.baseUrl}/video/analyze`, request, this.timeoutMs);
  }

  scoreAttractiveness(request: AttractivenessRequest): Promise<AttractivenessResponse> {
    return postJson<AttractivenessResponse>(`${this.baseUrl}/attractiveness/score`, request, this.timeoutMs);
  }

  moderateImage(request: ModerationRequest): Promise<ModerationResponse> {
    return postJson<ModerationResponse>(`${this.baseUrl}/moderation/image`, request, this.timeoutMs);
  }

  moderateText(request: TextModerationRequest): Promise<TextModerationResponse> {
    return postJson<TextModerationResponse>(`${this.baseUrl}/moderation/text`, request, this.timeoutMs);
  }

  scoreFaceQuality(request: FaceQualityRequest): Promise<FaceQualityResponse> {
    return postJson<FaceQualityResponse>(`${this.baseUrl}/quality/face`, request, this.timeoutMs);
  }

  transcribeVideo(request: Record<string, unknown>): Promise<Record<string, unknown>> {
    return postJson<Record<string, unknown>>(`${this.baseUrl}/video/transcribe`, request, this.timeoutMs);
  }

  analyzeTranscript(request: Record<string, unknown>): Promise<Record<string, unknown>> {
    return postJson<Record<string, unknown>>(`${this.baseUrl}/video/analyze-transcript`, request, this.timeoutMs);
  }
}
