import { useState, useRef, useCallback } from "react";
import { Platform } from "react-native";
import { TranscriptSegment } from "./useTranscription";

export type WhisperModel = "tiny" | "base" | "small" | "medium" | "large";

interface WhisperState {
  isLoading: boolean;
  isTranscribing: boolean;
  progress: number; // 0-100
  modelLoaded: boolean;
  error: string | null;
  backendType: "local" | "api" | null; // Which backend is being used
}

export interface WhisperConfig {
  apiKey?: string; // OpenAI API key for fallback
  apiEndpoint?: string; // Custom API endpoint (defaults to OpenAI)
  preferLocal?: boolean; // Prefer local whisper.rn over API (default: true)
}

interface UseWhisperTranscriptionReturn extends WhisperState {
  loadModel: (model?: WhisperModel) => Promise<void>;
  transcribe: (audioUri: string) => Promise<TranscriptSegment[]>;
  transcribeBlob: (blob: Blob) => Promise<TranscriptSegment[]>;
  unloadModel: () => void;
  configure: (config: WhisperConfig) => void;
}

// Transformers.js types (loaded dynamically)
interface Pipeline {
  (audio: Float32Array | string, options?: object): Promise<TranscriberOutput>;
}

interface TranscriberOutput {
  text: string;
  chunks?: Array<{
    text: string;
    timestamp: [number, number];
  }>;
}

/**
 * Client-side Whisper transcription hook.
 * Uses @xenova/transformers on web (FREE, runs in browser).
 * Uses whisper.rn on native (FREE, runs on device) with API fallback.
 *
 * Models (size → accuracy tradeoff):
 * - tiny: ~75MB, fastest, least accurate
 * - base: ~150MB, good balance
 * - small: ~500MB, better accuracy
 * - medium: ~1.5GB, high accuracy
 * - large: ~3GB, best accuracy (not recommended for browser)
 *
 * Native fallback:
 * If whisper.rn is not installed, falls back to OpenAI Whisper API.
 * Configure with: configure({ apiKey: "sk-..." })
 */
export function useWhisperTranscription(): UseWhisperTranscriptionReturn {
  const [state, setState] = useState<WhisperState>({
    isLoading: false,
    isTranscribing: false,
    progress: 0,
    modelLoaded: false,
    error: null,
    backendType: null,
  });

  const pipelineRef = useRef<Pipeline | null>(null);
  const whisperContextRef = useRef<any>(null); // For whisper.rn
  const configRef = useRef<WhisperConfig>({
    preferLocal: true,
  });
  const importOptionalModule = useCallback(async <T = any>(moduleName: string): Promise<T> => {
    const dynamicImport = new Function("m", "return import(m)");
    return (await dynamicImport(moduleName)) as T;
  }, []);

  const configure = useCallback((config: WhisperConfig) => {
    configRef.current = { ...configRef.current, ...config };
  }, []);

  const loadModel = useCallback(async (model: WhisperModel = "base") => {
    if (state.modelLoaded) return;

    setState((s) => ({ ...s, isLoading: true, error: null, progress: 0 }));

    try {
      if (Platform.OS === "web") {
        await loadWebModel(model);
      } else {
        await loadNativeModel(model);
      }
      setState((s) => ({ ...s, isLoading: false, modelLoaded: true, progress: 100 }));
    } catch (error) {
      const message = error instanceof Error ? error.message : "Failed to load model";
      setState((s) => ({ ...s, isLoading: false, error: message }));
      throw error;
    }
  }, [state.modelLoaded, importOptionalModule]);

  const loadWebModel = async (model: WhisperModel) => {
    // Dynamically import transformers.js
    const transformers = await importOptionalModule<{ pipeline: any }>("@xenova/transformers");
    const { pipeline } = transformers;

    const modelId = `Xenova/whisper-${model}`;

    pipelineRef.current = await pipeline(
      "automatic-speech-recognition",
      modelId,
      {
        progress_callback: (progress: { progress: number }) => {
          setState((s) => ({ ...s, progress: Math.round(progress.progress) }));
        },
      }
    );

    setState((s) => ({ ...s, backendType: "local" }));
  };

  const loadNativeModel = async (model: WhisperModel) => {
    const config = configRef.current;

    // Try whisper.rn first if preferLocal is true
    if (config.preferLocal) {
      try {
        const whisperRn = await importOptionalModule<any>("whisper.rn");
        const { initWhisper } = whisperRn;

        // Model files need to be bundled or downloaded
        // See: https://github.com/mybigday/whisper.rn#usage
        const modelPath = await getModelPath(model);

        whisperContextRef.current = await initWhisper({
          filePath: modelPath,
        });

        setState((s) => ({ ...s, backendType: "local" }));
        return;
      } catch {
        // whisper.rn not installed, try API fallback
        console.log("whisper.rn not available, trying API fallback");
      }
    }

    // Fallback to API-based transcription
    if (config.apiKey) {
      setState((s) => ({ ...s, backendType: "api" }));
      // API doesn't need model loading, mark as ready
      return;
    }

    // No fallback available
    throw new Error(
      "Native transcription requires either:\n" +
        "1. whisper.rn: npm install whisper.rn && cd ios && pod install\n" +
        "2. API key: configure({ apiKey: 'sk-...' }) for OpenAI Whisper API"
    );
  };

  const getModelPath = async (model: WhisperModel): Promise<string> => {
    // In production, you'd download the model or bundle it
    // Models available at: https://huggingface.co/ggerganov/whisper.cpp
    const FileSystem = await import("expo-file-system");
    const fsAny = FileSystem as any;
    const documentDirectory = fsAny.documentDirectory || fsAny.Paths?.document?.uri || "";
    const modelDir = `${documentDirectory}whisper-models/`;
    const modelFile = `ggml-${model}.bin`;
    const modelPath = `${modelDir}${modelFile}`;

    // Check if model exists
    const info = await FileSystem.getInfoAsync(modelPath);
    if (info.exists) {
      return modelPath;
    }

    // Download model
    setState((s) => ({ ...s, progress: 0 }));

    const modelUrl = `https://huggingface.co/ggerganov/whisper.cpp/resolve/main/${modelFile}`;

    await FileSystem.makeDirectoryAsync(modelDir, { intermediates: true });

    const download = FileSystem.createDownloadResumable(
      modelUrl,
      modelPath,
      {},
      (progress) => {
        const pct = (progress.totalBytesWritten / progress.totalBytesExpectedToWrite) * 100;
        setState((s) => ({ ...s, progress: Math.round(pct) }));
      }
    );

    const result = await download.downloadAsync();
    if (!result?.uri) {
      throw new Error("Failed to download model");
    }

    return modelPath;
  };

  const transcribe = useCallback(
    async (audioUri: string): Promise<TranscriptSegment[]> => {
      if (!state.modelLoaded) {
        await loadModel();
      }

      setState((s) => ({ ...s, isTranscribing: true, error: null }));

      try {
        if (Platform.OS === "web") {
          return await transcribeWeb(audioUri);
        } else {
          return await transcribeNative(audioUri);
        }
      } catch (error) {
        const message = error instanceof Error ? error.message : "Transcription failed";
        setState((s) => ({ ...s, error: message }));
        throw error;
      } finally {
        setState((s) => ({ ...s, isTranscribing: false }));
      }
    },
    [state.modelLoaded, loadModel]
  );

  const transcribeBlob = useCallback(
    async (blob: Blob): Promise<TranscriptSegment[]> => {
      if (Platform.OS !== "web") {
        throw new Error("transcribeBlob only available on web");
      }

      if (!state.modelLoaded) {
        await loadModel();
      }

      setState((s) => ({ ...s, isTranscribing: true, error: null }));

      try {
        // Convert blob to audio data
        const audioData = await blobToFloat32Array(blob);
        return await runWebInference(audioData);
      } catch (error) {
        const message = error instanceof Error ? error.message : "Transcription failed";
        setState((s) => ({ ...s, error: message }));
        throw error;
      } finally {
        setState((s) => ({ ...s, isTranscribing: false }));
      }
    },
    [state.modelLoaded, loadModel]
  );

  const transcribeWeb = async (audioUri: string): Promise<TranscriptSegment[]> => {
    // Fetch audio file
    const response = await fetch(audioUri);
    const blob = await response.blob();
    const audioData = await blobToFloat32Array(blob);

    return runWebInference(audioData);
  };

  const runWebInference = async (audioData: Float32Array): Promise<TranscriptSegment[]> => {
    if (!pipelineRef.current) {
      throw new Error("Model not loaded");
    }

    const result = await pipelineRef.current(audioData, {
      chunk_length_s: 30,
      stride_length_s: 5,
      return_timestamps: true,
      language: "english",
      task: "transcribe",
    });

    // Convert to segments
    if (result.chunks && result.chunks.length > 0) {
      return result.chunks.map((chunk, i) => ({
        text: chunk.text.trim(),
        startMs: chunk.timestamp[0] * 1000,
        endMs: chunk.timestamp[1] * 1000,
        confidence: 0.95, // Whisper doesn't provide confidence
        isFinal: true,
      }));
    }

    // Fallback: single segment
    return [
      {
        text: result.text.trim(),
        startMs: 0,
        endMs: 0,
        confidence: 0.95,
        isFinal: true,
      },
    ];
  };

  const transcribeNative = async (audioUri: string): Promise<TranscriptSegment[]> => {
    // Use API fallback if configured
    if (state.backendType === "api") {
      return transcribeWithApi(audioUri);
    }

    // Use local whisper.rn
    if (!whisperContextRef.current) {
      throw new Error("Model not loaded");
    }

    const whisperRn = await importOptionalModule<any>("whisper.rn");
    const whisperTranscribe = whisperRn.transcribe;

    const result = await whisperTranscribe(whisperContextRef.current, audioUri, {
      language: "en",
      translate: false,
    });

    // Convert whisper.rn result to segments
    if (result.segments && result.segments.length > 0) {
      return result.segments.map((seg: any) => ({
        text: seg.text.trim(),
        startMs: seg.t0,
        endMs: seg.t1,
        confidence: 0.95,
        isFinal: true,
      }));
    }

    return [
      {
        text: result.result.trim(),
        startMs: 0,
        endMs: 0,
        confidence: 0.95,
        isFinal: true,
      },
    ];
  };

  /**
   * Transcribe using OpenAI Whisper API (native fallback)
   */
  const transcribeWithApi = async (audioUri: string): Promise<TranscriptSegment[]> => {
    const config = configRef.current;
    if (!config.apiKey) {
      throw new Error("API key required for API-based transcription. Call configure({ apiKey: 'sk-...' })");
    }

    const FileSystem = await import("expo-file-system");

    // Read the audio file
    const fileInfo = await FileSystem.getInfoAsync(audioUri);
    if (!fileInfo.exists) {
      throw new Error("Audio file not found");
    }

    // Determine file extension and mime type
    const extension = audioUri.split(".").pop()?.toLowerCase() || "m4a";
    const mimeTypes: Record<string, string> = {
      m4a: "audio/m4a",
      mp3: "audio/mpeg",
      mp4: "audio/mp4",
      wav: "audio/wav",
      webm: "audio/webm",
      ogg: "audio/ogg",
      flac: "audio/flac",
    };
    const mimeType = mimeTypes[extension] || "audio/m4a";

    // Read file as base64
    const base64Audio = await FileSystem.readAsStringAsync(audioUri, {
      encoding: ((FileSystem as any).EncodingType?.Base64 || "base64") as any,
    });

    // Convert base64 to blob for FormData
    const byteCharacters = atob(base64Audio);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
      byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);
    const blob = new Blob([byteArray], { type: mimeType });

    // Create FormData for API request
    const formData = new FormData();
    formData.append("file", blob, `audio.${extension}`);
    formData.append("model", "whisper-1");
    formData.append("response_format", "verbose_json");
    formData.append("timestamp_granularities[]", "segment");

    const endpoint = config.apiEndpoint || "https://api.openai.com/v1/audio/transcriptions";

    const response = await fetch(endpoint, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${config.apiKey}`,
      },
      body: formData,
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Transcription API error: ${response.status} - ${errorText}`);
    }

    const result = await response.json();

    // Convert API response to segments
    if (result.segments && result.segments.length > 0) {
      return result.segments.map((seg: any) => ({
        text: seg.text.trim(),
        startMs: seg.start * 1000,
        endMs: seg.end * 1000,
        confidence: seg.avg_logprob ? Math.exp(seg.avg_logprob) : 0.9,
        isFinal: true,
      }));
    }

    // Fallback: single segment with full text
    return [
      {
        text: result.text?.trim() || "",
        startMs: 0,
        endMs: (result.duration || 0) * 1000,
        confidence: 0.9,
        isFinal: true,
      },
    ];
  };

  const unloadModel = useCallback(() => {
    pipelineRef.current = null;

    if (whisperContextRef.current?.release) {
      whisperContextRef.current.release();
    }
    whisperContextRef.current = null;

    setState({
      isLoading: false,
      isTranscribing: false,
      progress: 0,
      modelLoaded: false,
      error: null,
      backendType: null,
    });
  }, []);

  return {
    ...state,
    loadModel,
    transcribe,
    transcribeBlob,
    unloadModel,
    configure,
  };
}

/**
 * Convert audio Blob to Float32Array for Whisper
 */
async function blobToFloat32Array(blob: Blob): Promise<Float32Array> {
  const arrayBuffer = await blob.arrayBuffer();
  const audioContext = new (window.AudioContext || (window as any).webkitAudioContext)({
    sampleRate: 16000, // Whisper expects 16kHz
  });

  const audioBuffer = await audioContext.decodeAudioData(arrayBuffer);

  // Get mono audio (Whisper expects single channel)
  let audioData: Float32Array;
  if (audioBuffer.numberOfChannels > 1) {
    // Mix to mono
    const left = audioBuffer.getChannelData(0);
    const right = audioBuffer.getChannelData(1);
    audioData = new Float32Array(left.length);
    for (let i = 0; i < left.length; i++) {
      audioData[i] = (left[i] + right[i]) / 2;
    }
  } else {
    audioData = audioBuffer.getChannelData(0);
  }

  // Resample to 16kHz if needed
  if (audioBuffer.sampleRate !== 16000) {
    audioData = resample(audioData, audioBuffer.sampleRate, 16000);
  }

  return audioData;
}

/**
 * Simple linear resampling
 */
function resample(
  audioData: Float32Array,
  fromSampleRate: number,
  toSampleRate: number
): Float32Array {
  const ratio = fromSampleRate / toSampleRate;
  const newLength = Math.round(audioData.length / ratio);
  const result = new Float32Array(newLength);

  for (let i = 0; i < newLength; i++) {
    const srcIndex = i * ratio;
    const srcIndexFloor = Math.floor(srcIndex);
    const srcIndexCeil = Math.min(srcIndexFloor + 1, audioData.length - 1);
    const t = srcIndex - srcIndexFloor;

    result[i] = audioData[srcIndexFloor] * (1 - t) + audioData[srcIndexCeil] * t;
  }

  return result;
}

/**
 * Estimate model download size
 */
export function getModelSize(model: WhisperModel): string {
  const sizes: Record<WhisperModel, string> = {
    tiny: "~75 MB",
    base: "~150 MB",
    small: "~500 MB",
    medium: "~1.5 GB",
    large: "~3 GB",
  };
  return sizes[model];
}
