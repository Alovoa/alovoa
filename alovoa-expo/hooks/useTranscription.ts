import { useState, useRef, useCallback, useEffect } from "react";
import { Platform } from "react-native";

export interface TranscriptSegment {
  text: string;
  startMs: number;
  endMs: number;
  confidence: number;
  isFinal: boolean;
}

interface TranscriptionState {
  isListening: boolean;
  transcript: string;
  segments: TranscriptSegment[];
  interimText: string; // Current non-final text
  error: string | null;
}

interface UseTranscriptionReturn extends TranscriptionState {
  startListening: () => Promise<void>;
  stopListening: () => void;
  reset: () => void;
  // For file-based transcription (post-recording)
  transcribeAudio: (uri: string) => Promise<TranscriptSegment[]>;
}

// Web Speech API types
interface SpeechRecognitionEvent {
  resultIndex: number;
  results: SpeechRecognitionResultList;
}

interface SpeechRecognitionResultList {
  length: number;
  item(index: number): SpeechRecognitionResult;
  [index: number]: SpeechRecognitionResult;
}

interface SpeechRecognitionResult {
  isFinal: boolean;
  length: number;
  item(index: number): SpeechRecognitionAlternative;
  [index: number]: SpeechRecognitionAlternative;
}

interface SpeechRecognitionAlternative {
  transcript: string;
  confidence: number;
}

interface SpeechRecognition extends EventTarget {
  continuous: boolean;
  interimResults: boolean;
  lang: string;
  start(): void;
  stop(): void;
  abort(): void;
  onresult: ((event: SpeechRecognitionEvent) => void) | null;
  onerror: ((event: { error: string }) => void) | null;
  onend: (() => void) | null;
  onstart: (() => void) | null;
}

declare global {
  interface Window {
    SpeechRecognition: new () => SpeechRecognition;
    webkitSpeechRecognition: new () => SpeechRecognition;
  }
}

/**
 * Cross-platform transcription hook.
 * Uses Web Speech API on web (FREE, runs in browser).
 * On native, supports file-based transcription via Whisper API.
 *
 * Pattern: Transcribe → returns timestamped segments
 */
export function useTranscription(language = "en-US"): UseTranscriptionReturn {
  const [state, setState] = useState<TranscriptionState>({
    isListening: false,
    transcript: "",
    segments: [],
    interimText: "",
    error: null,
  });

  const recognitionRef = useRef<SpeechRecognition | null>(null);
  const segmentStartRef = useRef<number>(0);
  const sessionStartRef = useRef<number>(0);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      recognitionRef.current?.stop();
    };
  }, []);

  const startListening = useCallback(async () => {
    if (Platform.OS !== "web") {
      setState((s) => ({
        ...s,
        error: "Live transcription only available on web. Use transcribeAudio for recordings.",
      }));
      return;
    }

    const SpeechRecognition =
      window.SpeechRecognition || window.webkitSpeechRecognition;

    if (!SpeechRecognition) {
      setState((s) => ({
        ...s,
        error: "Speech recognition not supported in this browser",
      }));
      return;
    }

    try {
      const recognition = new SpeechRecognition();
      recognition.continuous = true;
      recognition.interimResults = true;
      recognition.lang = language;

      sessionStartRef.current = Date.now();
      segmentStartRef.current = Date.now();

      recognition.onstart = () => {
        setState((s) => ({ ...s, isListening: true, error: null }));
      };

      recognition.onresult = (event: SpeechRecognitionEvent) => {
        let interimTranscript = "";
        let finalTranscript = "";
        const newSegments: TranscriptSegment[] = [];

        for (let i = event.resultIndex; i < event.results.length; i++) {
          const result = event.results[i];
          const text = result[0].transcript;
          const confidence = result[0].confidence;

          if (result.isFinal) {
            finalTranscript += text;
            const now = Date.now();
            newSegments.push({
              text: text.trim(),
              startMs: segmentStartRef.current - sessionStartRef.current,
              endMs: now - sessionStartRef.current,
              confidence,
              isFinal: true,
            });
            segmentStartRef.current = now;
          } else {
            interimTranscript += text;
          }
        }

        setState((s) => ({
          ...s,
          transcript: s.transcript + finalTranscript,
          segments: [...s.segments, ...newSegments],
          interimText: interimTranscript,
        }));
      };

      recognition.onerror = (event) => {
        // "no-speech" and "aborted" are not real errors
        if (event.error !== "no-speech" && event.error !== "aborted") {
          setState((s) => ({ ...s, error: event.error }));
        }
      };

      recognition.onend = () => {
        setState((s) => ({ ...s, isListening: false, interimText: "" }));
      };

      recognition.start();
      recognitionRef.current = recognition;
    } catch (error) {
      setState((s) => ({
        ...s,
        error: error instanceof Error ? error.message : "Failed to start",
      }));
    }
  }, [language]);

  const stopListening = useCallback(() => {
    recognitionRef.current?.stop();
    recognitionRef.current = null;
    setState((s) => ({ ...s, isListening: false, interimText: "" }));
  }, []);

  const reset = useCallback(() => {
    recognitionRef.current?.stop();
    recognitionRef.current = null;
    setState({
      isListening: false,
      transcript: "",
      segments: [],
      interimText: "",
      error: null,
    });
  }, []);

  // Whisper transcription instance (lazy loaded)
  const whisperInstanceRef = useRef<{
    loadModel: (model?: string) => Promise<void>;
    transcribe: (uri: string) => Promise<TranscriptSegment[]>;
    transcribeBlob: (blob: Blob) => Promise<TranscriptSegment[]>;
    configure: (config: { apiKey?: string; apiEndpoint?: string; preferLocal?: boolean }) => void;
    modelLoaded: boolean;
    backendType: "local" | "api" | null;
  } | null>(null);

  /**
   * Transcribe an audio file using Whisper.
   *
   * On web: Uses @xenova/transformers (FREE, runs in browser)
   * On native: Uses whisper.rn if installed, or falls back to OpenAI API
   *
   * First call loads the model (~150MB for 'base'), subsequent calls are fast.
   */
  const transcribeAudio = useCallback(
    async (uri: string): Promise<TranscriptSegment[]> => {
      // Lazy initialize Whisper wrapper
      if (!whisperInstanceRef.current) {
        whisperInstanceRef.current = await createWhisperInstance();
      }

      return whisperInstanceRef.current.transcribe(uri);
    },
    []
  );

  return {
    ...state,
    startListening,
    stopListening,
    reset,
    transcribeAudio,
  };
}

/**
 * Create a lazy-loaded Whisper transcription instance.
 * Can't use the hook directly since this is called dynamically.
 */
async function createWhisperInstance() {
  const { Platform } = await import("react-native");
  const importOptionalModule = async <T = any>(moduleName: string): Promise<T> => {
    const dynamicImport = new Function("m", "return import(m)");
    return (await dynamicImport(moduleName)) as T;
  };

  const getDocumentDirectory = (fileSystemModule: any): string => {
    return fileSystemModule.documentDirectory || fileSystemModule.Paths?.document?.uri || "";
  };

  // State for the instance
  let modelLoaded = false;
  let backendType: "local" | "api" | null = null;
  let pipeline: any = null;
  let whisperContext: any = null;
  let config = { apiKey: "", apiEndpoint: "", preferLocal: true };

  const configure = (newConfig: { apiKey?: string; apiEndpoint?: string; preferLocal?: boolean }) => {
    config = { ...config, ...newConfig };
  };

  const loadModel = async (model = "base") => {
    if (modelLoaded) return;

    if (Platform.OS === "web") {
      // Web: Use transformers.js
      const transformers = await importOptionalModule<{ pipeline: (...args: any[]) => Promise<any> }>("@xenova/transformers");
      const modelId = `Xenova/whisper-${model}`;
      pipeline = await transformers.pipeline("automatic-speech-recognition", modelId);
      backendType = "local";
      modelLoaded = true;
    } else {
      // Native: Try whisper.rn first, then API fallback
      if (config.preferLocal) {
        try {
          const whisperRn = await importOptionalModule<any>("whisper.rn");
          const FileSystem = await import("expo-file-system");

          // Get or download model
          const documentDirectory = getDocumentDirectory(FileSystem);
          const modelDir = `${documentDirectory}whisper-models/`;
          const modelFile = `ggml-${model}.bin`;
          const modelPath = `${modelDir}${modelFile}`;

          const info = await FileSystem.getInfoAsync(modelPath);
          if (!info.exists) {
            // Download model
            await FileSystem.makeDirectoryAsync(modelDir, { intermediates: true });
            const modelUrl = `https://huggingface.co/ggerganov/whisper.cpp/resolve/main/${modelFile}`;
            const download = FileSystem.createDownloadResumable(modelUrl, modelPath);
            await download.downloadAsync();
          }

          whisperContext = await whisperRn.initWhisper({ filePath: modelPath });
          backendType = "local";
          modelLoaded = true;
          return;
        } catch {
          console.log("whisper.rn not available, trying API fallback");
        }
      }

      // API fallback
      if (config.apiKey) {
        backendType = "api";
        modelLoaded = true;
        return;
      }

      throw new Error(
        "Native transcription requires either:\n" +
          "1. whisper.rn: npm install whisper.rn && cd ios && pod install\n" +
          "2. API key: Call configure({ apiKey: 'sk-...' }) for OpenAI Whisper API"
      );
    }
  };

  const transcribe = async (audioUri: string): Promise<TranscriptSegment[]> => {
    if (!modelLoaded) {
      await loadModel();
    }

    if (Platform.OS === "web") {
      return transcribeWeb(audioUri);
    } else {
      return transcribeNative(audioUri);
    }
  };

  const transcribeWeb = async (audioUri: string): Promise<TranscriptSegment[]> => {
    const response = await fetch(audioUri);
    const blob = await response.blob();
    const audioData = await blobToFloat32Array(blob);

    const result = await pipeline(audioData, {
      chunk_length_s: 30,
      stride_length_s: 5,
      return_timestamps: true,
      language: "english",
      task: "transcribe",
    });

    if (result.chunks && result.chunks.length > 0) {
      return result.chunks.map((chunk: any) => ({
        text: chunk.text.trim(),
        startMs: chunk.timestamp[0] * 1000,
        endMs: chunk.timestamp[1] * 1000,
        confidence: 0.95,
        isFinal: true,
      }));
    }

    return [{
      text: result.text.trim(),
      startMs: 0,
      endMs: 0,
      confidence: 0.95,
      isFinal: true,
    }];
  };

  const transcribeNative = async (audioUri: string): Promise<TranscriptSegment[]> => {
    if (backendType === "api") {
      return transcribeWithApi(audioUri);
    }

    // Local whisper.rn
    const whisperRn = await importOptionalModule<any>("whisper.rn");
    const whisperTranscribe = whisperRn.transcribe;
    const result = await whisperTranscribe(whisperContext, audioUri, {
      language: "en",
      translate: false,
    });

    if (result.segments && result.segments.length > 0) {
      return result.segments.map((seg: any) => ({
        text: seg.text.trim(),
        startMs: seg.t0,
        endMs: seg.t1,
        confidence: 0.95,
        isFinal: true,
      }));
    }

    return [{
      text: result.result.trim(),
      startMs: 0,
      endMs: 0,
      confidence: 0.95,
      isFinal: true,
    }];
  };

  const transcribeWithApi = async (audioUri: string): Promise<TranscriptSegment[]> => {
    if (!config.apiKey) {
      throw new Error("API key required. Call configure({ apiKey: 'sk-...' })");
    }

    const FileSystem = await import("expo-file-system");

    const fileInfo = await FileSystem.getInfoAsync(audioUri);
    if (!fileInfo.exists) {
      throw new Error("Audio file not found");
    }

    const extension = audioUri.split(".").pop()?.toLowerCase() || "m4a";
    const mimeTypes: Record<string, string> = {
      m4a: "audio/m4a",
      mp3: "audio/mpeg",
      wav: "audio/wav",
      webm: "audio/webm",
    };
    const mimeType = mimeTypes[extension] || "audio/m4a";

    const base64Audio = await FileSystem.readAsStringAsync(audioUri, {
      encoding: ((FileSystem as any).EncodingType?.Base64 || "base64") as any,
    });

    const byteCharacters = atob(base64Audio);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
      byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);
    const blob = new Blob([byteArray], { type: mimeType });

    const formData = new FormData();
    formData.append("file", blob, `audio.${extension}`);
    formData.append("model", "whisper-1");
    formData.append("response_format", "verbose_json");
    formData.append("timestamp_granularities[]", "segment");

    const endpoint = config.apiEndpoint || "https://api.openai.com/v1/audio/transcriptions";

    const response = await fetch(endpoint, {
      method: "POST",
      headers: { Authorization: `Bearer ${config.apiKey}` },
      body: formData,
    });

    if (!response.ok) {
      throw new Error(`API error: ${response.status}`);
    }

    const result = await response.json();

    if (result.segments && result.segments.length > 0) {
      return result.segments.map((seg: any) => ({
        text: seg.text.trim(),
        startMs: seg.start * 1000,
        endMs: seg.end * 1000,
        confidence: 0.9,
        isFinal: true,
      }));
    }

    return [{
      text: result.text?.trim() || "",
      startMs: 0,
      endMs: (result.duration || 0) * 1000,
      confidence: 0.9,
      isFinal: true,
    }];
  };

  const transcribeBlob = async (blob: Blob): Promise<TranscriptSegment[]> => {
    if (Platform.OS !== "web") {
      throw new Error("transcribeBlob only available on web");
    }

    if (!modelLoaded) {
      await loadModel();
    }

    const audioData = await blobToFloat32Array(blob);
    const result = await pipeline(audioData, {
      chunk_length_s: 30,
      stride_length_s: 5,
      return_timestamps: true,
      language: "english",
      task: "transcribe",
    });

    if (result.chunks && result.chunks.length > 0) {
      return result.chunks.map((chunk: any) => ({
        text: chunk.text.trim(),
        startMs: chunk.timestamp[0] * 1000,
        endMs: chunk.timestamp[1] * 1000,
        confidence: 0.95,
        isFinal: true,
      }));
    }

    return [{
      text: result.text.trim(),
      startMs: 0,
      endMs: 0,
      confidence: 0.95,
      isFinal: true,
    }];
  };

  return {
    loadModel,
    transcribe,
    transcribeBlob,
    configure,
    get modelLoaded() { return modelLoaded; },
    get backendType() { return backendType; },
  };
}

/**
 * Convert audio Blob to Float32Array for Whisper
 */
async function blobToFloat32Array(blob: Blob): Promise<Float32Array> {
  const arrayBuffer = await blob.arrayBuffer();
  const audioContext = new (window.AudioContext || (window as any).webkitAudioContext)({
    sampleRate: 16000,
  });

  const audioBuffer = await audioContext.decodeAudioData(arrayBuffer);

  let audioData: Float32Array;
  if (audioBuffer.numberOfChannels > 1) {
    const left = audioBuffer.getChannelData(0);
    const right = audioBuffer.getChannelData(1);
    audioData = new Float32Array(left.length);
    for (let i = 0; i < left.length; i++) {
      audioData[i] = (left[i] + right[i]) / 2;
    }
  } else {
    audioData = audioBuffer.getChannelData(0);
  }

  if (audioBuffer.sampleRate !== 16000) {
    audioData = resample(audioData, audioBuffer.sampleRate, 16000);
  }

  return audioData;
}

/**
 * Simple linear resampling to 16kHz
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
 * Helper: Format milliseconds to MM:SS
 */
export function formatTimestamp(ms: number): string {
  const totalSeconds = Math.floor(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${seconds.toString().padStart(2, "0")}`;
}

/**
 * Helper: Group segments into paragraphs by pause duration
 */
export function groupSegments(
  segments: TranscriptSegment[],
  pauseThresholdMs = 2000
): string[] {
  const paragraphs: string[] = [];
  let currentParagraph: string[] = [];

  for (let i = 0; i < segments.length; i++) {
    currentParagraph.push(segments[i].text);

    const nextSegment = segments[i + 1];
    if (nextSegment) {
      const pause = nextSegment.startMs - segments[i].endMs;
      if (pause > pauseThresholdMs) {
        paragraphs.push(currentParagraph.join(" "));
        currentParagraph = [];
      }
    }
  }

  if (currentParagraph.length > 0) {
    paragraphs.push(currentParagraph.join(" "));
  }

  return paragraphs;
}
