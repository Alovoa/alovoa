import React from "react";
import {
  View,
  useWindowDimensions,
  Pressable,
  StyleSheet,
  ScrollView,
  Platform,
} from "react-native";
import {
  Text,
  Button,
  ActivityIndicator,
  useTheme,
  Card,
  Chip,
  ProgressBar,
  Surface,
  IconButton,
} from "react-native-paper";
import { MaterialCommunityIcons } from "@expo/vector-icons";
import * as Global from "../../Global";
import * as URL from "../../URL";
import * as I18N from "../../i18n";
import {
  UserVideoIntroduction,
  VideoIntroStatus,
  VideoAnalysisResult,
} from "../../myTypes";
import { STATUS_BAR_HEIGHT } from "../../assets/styles";

// Platform-specific imports
import WebCamera, { WebCameraRef, requestWebCameraPermissions } from "../../components/WebCamera";

// Native imports (conditional)
let Camera: any;
let CameraView: any;
let Audio: any;

if (Platform.OS !== "web") {
  Camera = require("expo-camera").Camera;
  CameraView = require("expo-camera").CameraView;
  Audio = require("expo-av").Audio;
}

type CameraType = "front" | "back";

const i18n = I18N.getI18n();

const STATUS_CONFIG: Record<VideoIntroStatus, { icon: string; color: string; label: string }> = {
  [VideoIntroStatus.NONE]: { icon: "video-plus", color: "#6B7280", label: "No Video" },
  [VideoIntroStatus.PENDING]: { icon: "clock-outline", color: "#F59E0B", label: "Pending" },
  [VideoIntroStatus.UPLOADING]: { icon: "cloud-upload", color: "#3B82F6", label: "Uploading" },
  [VideoIntroStatus.PROCESSING]: { icon: "cog", color: "#F59E0B", label: "Processing" },
  [VideoIntroStatus.ANALYZING]: { icon: "brain", color: "#8B5CF6", label: "AI Analyzing" },
  [VideoIntroStatus.READY]: { icon: "check-circle", color: "#10B981", label: "Ready" },
  [VideoIntroStatus.COMPLETE]: { icon: "check-all", color: "#10B981", label: "Complete" },
  [VideoIntroStatus.FAILED]: { icon: "alert-circle", color: "#EF4444", label: "Failed" },
};

enum ScreenStep {
  STATUS,
  TIPS,
  RECORDING,
  UPLOADING,
  ANALYZING,
  RESULTS,
}

const PROMPTS = [
  "Tell us about yourself - what makes you, you?",
  "What are you passionate about?",
  "What does your ideal weekend look like?",
  "What are you looking for in a partner?",
  "Share a fun fact about yourself!",
];

const VideoIntro = ({ navigation }: any) => {
  const { colors } = useTheme();
  const { height, width } = useWindowDimensions();

  // Use different refs for web vs native
  const nativeCameraRef = React.useRef<any>(null);
  const webCameraRef = React.useRef<WebCameraRef>(null);
  const cameraRef = Platform.OS === "web" ? webCameraRef : nativeCameraRef;

  const [loading, setLoading] = React.useState(true);
  const [step, setStep] = React.useState<ScreenStep>(ScreenStep.STATUS);
  const [intro, setIntro] = React.useState<UserVideoIntroduction | null>(null);
  const [analysis, setAnalysis] = React.useState<VideoAnalysisResult | null>(null);
  const [hasPermission, setHasPermission] = React.useState<boolean | null>(null);
  const [isRecording, setIsRecording] = React.useState(false);
  const [recordingProgress, setRecordingProgress] = React.useState(0);
  const [uploadProgress, setUploadProgress] = React.useState(0);
  const [currentPrompt, setCurrentPrompt] = React.useState(0);
  const [cameraType, setCameraType] = React.useState<CameraType>("front");

  const MAX_DURATION = 120; // 2 minutes max
  const MIN_DURATION = 30; // 30 seconds min

  React.useEffect(() => {
    loadStatus();
    requestPermissions();
  }, []);

  async function requestPermissions() {
    if (Platform.OS === "web") {
      const result = await requestWebCameraPermissions();
      setHasPermission(result.status === "granted");
    } else {
      const [cameraStatus, audioStatus] = await Promise.all([
        Camera.requestCameraPermissionsAsync(),
        Audio.requestPermissionsAsync(),
      ]);
      setHasPermission(
        cameraStatus.status === "granted" && audioStatus.status === "granted"
      );
    }
  }

  async function loadStatus() {
    setLoading(true);
    try {
      const response = await Global.Fetch(URL.API_VIDEO_INTRO_STATUS);
      setIntro(response.data);

      if (response.data?.status === VideoIntroStatus.READY || response.data?.status === VideoIntroStatus.COMPLETE) {
        // Load analysis if ready
        try {
          const analysisRes = await Global.Fetch(URL.API_VIDEO_INTRO_ANALYSIS);
          setAnalysis(analysisRes.data);
        } catch (e) {
          console.error("Failed to load analysis:", e);
        }
      }
    } catch (e) {
      console.error(e);
    }
    setLoading(false);
  }

  function startRecording() {
    setCurrentPrompt(Math.floor(Math.random() * PROMPTS.length));
    setStep(ScreenStep.TIPS);
  }

  async function beginRecording() {
    if (!cameraRef.current || isRecording) return;

    setStep(ScreenStep.RECORDING);
    setIsRecording(true);
    setRecordingProgress(0);

    // Progress animation
    const progressInterval = setInterval(() => {
      setRecordingProgress((prev) => {
        const newProgress = prev + (1 / MAX_DURATION);
        return Math.min(newProgress, 1);
      });
    }, 1000);

    try {
      const video = await cameraRef.current.recordAsync({
        maxDuration: MAX_DURATION,
      });

      clearInterval(progressInterval);
      setIsRecording(false);

      if (video?.uri) {
        await uploadVideo(video.uri);
      }
    } catch (e) {
      clearInterval(progressInterval);
      console.error(e);
      setIsRecording(false);
      Global.ShowToast("Recording failed. Please try again.");
      setStep(ScreenStep.TIPS);
    }
  }

  async function stopRecording() {
    if (cameraRef.current && isRecording) {
      const recordedDuration = recordingProgress * MAX_DURATION;
      if (recordedDuration < MIN_DURATION) {
        Global.ShowToast(`Please record at least ${MIN_DURATION} seconds`);
        return;
      }
      cameraRef.current.stopRecording();
    }
  }

  async function uploadVideo(uri: string) {
    setStep(ScreenStep.UPLOADING);
    setUploadProgress(0);

    try {
      const formData = new FormData();
      if (Platform.OS === "web") {
        const blob = await fetch(uri).then((r) => r.blob());
        formData.append("video", blob as any, "intro.webm");
      } else {
        formData.append("video", {
          uri,
          name: "intro.mp4",
          type: "video/mp4",
        } as any);
      }

      setUploadProgress(0.5);
      await Global.Fetch(URL.API_VIDEO_INTRO_UPLOAD, 'post', formData, 'multipart/form-data');
      setUploadProgress(1);

      setStep(ScreenStep.ANALYZING);

      // Poll for analysis completion
      pollForAnalysis();
    } catch (e) {
      console.error(e);
      Global.ShowToast("Upload failed. Please try again.");
      setStep(ScreenStep.STATUS);
    }
  }

  async function pollForAnalysis() {
    const maxAttempts = 60; // 5 minutes max
    let attempts = 0;

    const poll = async () => {
      try {
        const response = await Global.Fetch(URL.API_VIDEO_INTRO_STATUS);
        setIntro(response.data);

        if (response.data?.status === VideoIntroStatus.READY || response.data?.status === VideoIntroStatus.COMPLETE) {
          const analysisRes = await Global.Fetch(URL.API_VIDEO_INTRO_ANALYSIS);
          setAnalysis(analysisRes.data);
          setStep(ScreenStep.RESULTS);
          return;
        }

        if (response.data?.status === VideoIntroStatus.FAILED) {
          Global.ShowToast("Analysis failed. Please try recording again.");
          setStep(ScreenStep.STATUS);
          return;
        }

        attempts++;
        if (attempts < maxAttempts) {
          setTimeout(poll, 5000);
        } else {
          Global.ShowToast("Analysis is taking longer than expected. Check back later.");
          setStep(ScreenStep.STATUS);
        }
      } catch (e) {
        console.error(e);
        setStep(ScreenStep.STATUS);
      }
    };

    poll();
  }

  async function deleteIntro() {
    try {
      await Global.Fetch(URL.API_VIDEO_INTRO_DELETE, 'delete');
      setIntro(null);
      setAnalysis(null);
      Global.ShowToast("Video deleted");
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t('error.generic'));
    }
  }

  function viewScaffoldedProfile() {
    Global.navigate("Intake.ScaffoldedProfile", false, {});
  }

  function renderCameraOverlay() {
    return (
      <View style={styles.cameraOverlay}>
        {/* Prompt at top */}
        <Surface style={styles.promptContainer}>
          <Text style={styles.promptText}>
            {PROMPTS[currentPrompt]}
          </Text>
        </Surface>

        {/* Timer and controls at bottom */}
        <View style={styles.controlsContainer}>
          {/* Timer */}
          <View style={styles.timerContainer}>
            <Text style={styles.timerText}>
              {Math.floor(recordingProgress * MAX_DURATION)}s / {MAX_DURATION}s
            </Text>
            <ProgressBar
              progress={recordingProgress}
              color={recordingProgress < (MIN_DURATION / MAX_DURATION) ? "#F59E0B" : "#10B981"}
              style={{ height: 4, borderRadius: 2, marginTop: 8 }}
            />
            {recordingProgress < (MIN_DURATION / MAX_DURATION) && (
              <Text style={{ color: '#F59E0B', fontSize: 12, marginTop: 4 }}>
                Record at least {MIN_DURATION} seconds
              </Text>
            )}
          </View>

          {/* Recording indicator */}
          <View style={styles.recordingIndicator}>
            <View style={styles.recordingDot} />
            <Text style={styles.recordingLabel}>Recording</Text>
          </View>

          {/* Stop button */}
          <Pressable
            onPress={stopRecording}
            style={[
              styles.stopButton,
              recordingProgress < (MIN_DURATION / MAX_DURATION) && styles.stopButtonDisabled
            ]}
          >
            <MaterialCommunityIcons name="stop" size={32} color="white" />
          </Pressable>
        </View>
      </View>
    );
  }

  if (loading) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: colors.background }}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  // Permission denied
  if (hasPermission === false) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: colors.background, padding: 24 }}>
        <MaterialCommunityIcons name="camera-off" size={64} color={colors.error} />
        <Text style={{ fontSize: 18, marginTop: 16, textAlign: 'center' }}>
          Camera and microphone permissions are required for video intros.
        </Text>
        <Button mode="contained" onPress={requestPermissions} style={{ marginTop: 24 }}>
          Grant Permissions
        </Button>
      </View>
    );
  }

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      {/* Header */}
      <View style={{ paddingTop: STATUS_BAR_HEIGHT + 8, paddingHorizontal: 16, paddingBottom: 8 }}>
        <View style={{ flexDirection: 'row', alignItems: 'center' }}>
          <Pressable onPress={() => navigation.goBack()} style={{ marginRight: 12 }}>
            <MaterialCommunityIcons name="arrow-left" size={24} color={colors.onSurface} />
          </Pressable>
          <Text style={{ fontSize: 20, fontWeight: '600' }}>
            Video Introduction
          </Text>
        </View>
      </View>

      {/* Status Screen */}
      {step === ScreenStep.STATUS && (
        <ScrollView style={{ flex: 1, padding: 16 }}>
          {intro?.status === VideoIntroStatus.READY || intro?.status === VideoIntroStatus.COMPLETE ? (
            <>
              <Card style={{ marginBottom: 20, backgroundColor: '#D1FAE5' }}>
                <Card.Content>
                  <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                    <MaterialCommunityIcons name="check-decagram" size={32} color="#10B981" />
                    <View style={{ marginLeft: 12, flex: 1 }}>
                      <Text style={{ fontSize: 18, fontWeight: '600', color: '#065F46' }}>
                        Video Ready!
                      </Text>
                      <Text style={{ color: '#047857' }}>
                        Your intro has been analyzed and is visible to matches.
                      </Text>
                    </View>
                  </View>
                </Card.Content>
              </Card>

              {/* Analysis Summary */}
              {analysis && (
                <Card style={{ marginBottom: 20 }}>
                  <Card.Content>
                    <Text style={{ fontSize: 16, fontWeight: '600', marginBottom: 12 }}>
                      AI Analysis Summary
                    </Text>

                    {analysis.worldviewSummary && (
                      <View style={{ marginBottom: 12 }}>
                        <Text style={{ fontWeight: '500', color: colors.primary }}>Worldview</Text>
                        <Text style={{ color: colors.onSurfaceVariant }}>{analysis.worldviewSummary}</Text>
                      </View>
                    )}

                    {analysis.backgroundSummary && (
                      <View style={{ marginBottom: 12 }}>
                        <Text style={{ fontWeight: '500', color: colors.primary }}>Background</Text>
                        <Text style={{ color: colors.onSurfaceVariant }}>{analysis.backgroundSummary}</Text>
                      </View>
                    )}

                    {analysis.lifeStorySummary && (
                      <View style={{ marginBottom: 12 }}>
                        <Text style={{ fontWeight: '500', color: colors.primary }}>Life Story</Text>
                        <Text style={{ color: colors.onSurfaceVariant }}>{analysis.lifeStorySummary}</Text>
                      </View>
                    )}

                    {analysis.overallInferenceConfidence && (
                      <View style={{ flexDirection: 'row', alignItems: 'center', marginTop: 8 }}>
                        <MaterialCommunityIcons name="brain" size={20} color="#8B5CF6" />
                        <Text style={{ marginLeft: 8, color: colors.onSurfaceVariant }}>
                          Confidence: {Math.round(analysis.overallInferenceConfidence * 100)}%
                        </Text>
                      </View>
                    )}
                  </Card.Content>
                </Card>
              )}

              <Button
                mode="contained"
                onPress={viewScaffoldedProfile}
                style={{ marginBottom: 12 }}
                icon="account-check"
              >
                View Scaffolded Profile
              </Button>

              <Button
                mode="outlined"
                onPress={startRecording}
                style={{ marginBottom: 12 }}
                icon="video-plus"
              >
                Record New Video
              </Button>

              <Button
                mode="text"
                onPress={deleteIntro}
                textColor={colors.error}
                icon="delete"
              >
                Delete Video
              </Button>
            </>
          ) : intro?.status === VideoIntroStatus.PROCESSING || intro?.status === VideoIntroStatus.ANALYZING ? (
            <Card style={{ marginBottom: 20 }}>
              <Card.Content style={{ alignItems: 'center', paddingVertical: 32 }}>
                <ActivityIndicator size="large" style={{ marginBottom: 16 }} />
                <Text style={{ fontSize: 18, fontWeight: '600' }}>
                  {STATUS_CONFIG[intro.status]?.label}
                </Text>
                <Text style={{ color: colors.onSurfaceVariant, marginTop: 8, textAlign: 'center' }}>
                  {intro.status === VideoIntroStatus.ANALYZING
                    ? "Our AI is analyzing your video to understand your personality..."
                    : "Your video is being processed. This usually takes a few minutes."}
                </Text>
                <Button mode="text" onPress={loadStatus} style={{ marginTop: 16 }}>
                  Refresh Status
                </Button>
              </Card.Content>
            </Card>
          ) : (
            <>
              <Card style={{ marginBottom: 20 }}>
                <Card.Content style={{ alignItems: 'center', paddingVertical: 24 }}>
                  <MaterialCommunityIcons name="video-vintage" size={64} color={colors.primary} />
                  <Text style={{ fontSize: 20, fontWeight: '600', marginTop: 16, textAlign: 'center' }}>
                    Create Your Video Introduction
                  </Text>
                  <Text style={{ color: colors.onSurfaceVariant, marginTop: 8, textAlign: 'center' }}>
                    Record a 30 second to 2 minute video telling potential matches about yourself.
                  </Text>
                </Card.Content>
              </Card>

              {/* Benefits */}
              <Text style={{ fontSize: 16, fontWeight: '600', marginBottom: 12 }}>
                Why Record a Video?
              </Text>

              <View style={{ gap: 12, marginBottom: 24 }}>
                <BenefitItem icon="account-voice" text="Show your authentic personality" />
                <BenefitItem icon="brain" text="AI analyzes to improve your matches" />
                <BenefitItem icon="shield-check" text="Builds trust with potential matches" />
                <BenefitItem icon="clock-fast" text="Skip hours of questions - get matched faster" />
              </View>

              <Button
                mode="contained"
                onPress={startRecording}
                icon="video"
                style={{ marginBottom: 16 }}
              >
                Start Recording
              </Button>
            </>
          )}
        </ScrollView>
      )}

      {/* Tips Screen */}
      {step === ScreenStep.TIPS && (
        <View style={{ flex: 1, padding: 16, justifyContent: 'center' }}>
          <Card>
            <Card.Content style={{ paddingVertical: 24 }}>
              <MaterialCommunityIcons
                name="lightbulb-on"
                size={48}
                color="#F59E0B"
                style={{ alignSelf: 'center' }}
              />
              <Text style={{ fontSize: 20, fontWeight: '600', textAlign: 'center', marginTop: 16 }}>
                Tips for a Great Video
              </Text>

              <View style={{ marginTop: 20, gap: 12 }}>
                <TipItem icon="lightbulb" text="Find good lighting - face a window if possible" />
                <TipItem icon="volume-high" text="Record in a quiet place" />
                <TipItem icon="eye" text="Look at the camera, not your reflection" />
                <TipItem icon="emoticon-happy" text="Be yourself - authenticity is attractive!" />
                <TipItem icon="clock" text="Aim for 30 seconds to 2 minutes" />
              </View>

              <Surface style={{ marginTop: 20, padding: 16, borderRadius: 12, backgroundColor: colors.primaryContainer }}>
                <Text style={{ fontWeight: '600', color: colors.onPrimaryContainer, marginBottom: 8 }}>
                  Prompt to get you started:
                </Text>
                <Text style={{ fontSize: 16, fontStyle: 'italic', color: colors.onPrimaryContainer }}>
                  "{PROMPTS[currentPrompt]}"
                </Text>
              </Surface>
            </Card.Content>
          </Card>

          <Button mode="contained" onPress={beginRecording} style={{ marginTop: 24 }} icon="video">
            I'm Ready - Start Recording
          </Button>
          <Button mode="text" onPress={() => setStep(ScreenStep.STATUS)} style={{ marginTop: 8 }}>
            Cancel
          </Button>
        </View>
      )}

      {/* Recording Screen */}
      {step === ScreenStep.RECORDING && (
        <View style={{ flex: 1 }}>
          {Platform.OS === "web" ? (
            <WebCamera
              ref={webCameraRef}
              style={{ flex: 1 }}
              facing={cameraType}
              mode="video"
            >
              {renderCameraOverlay()}
            </WebCamera>
          ) : (
            <CameraView
              ref={nativeCameraRef}
              style={{ flex: 1 }}
              facing={cameraType}
              mode="video"
            >
              {renderCameraOverlay()}
            </CameraView>
          )}
        </View>
      )}

      {/* Uploading Screen */}
      {step === ScreenStep.UPLOADING && (
        <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', padding: 24 }}>
          <ActivityIndicator size="large" />
          <Text style={{ fontSize: 18, marginTop: 16 }}>Uploading...</Text>
          <ProgressBar
            progress={uploadProgress}
            color={colors.primary}
            style={{ width: 200, height: 8, borderRadius: 4, marginTop: 16 }}
          />
          <Text style={{ color: colors.onSurfaceVariant, marginTop: 8 }}>
            {Math.round(uploadProgress * 100)}%
          </Text>
        </View>
      )}

      {/* Analyzing Screen */}
      {step === ScreenStep.ANALYZING && (
        <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', padding: 24 }}>
          <MaterialCommunityIcons name="brain" size={64} color="#8B5CF6" />
          <Text style={{ fontSize: 20, fontWeight: '600', marginTop: 16 }}>
            AI is Analyzing Your Video
          </Text>
          <Text style={{ color: colors.onSurfaceVariant, marginTop: 8, textAlign: 'center' }}>
            Our AI is watching your video to understand your personality, values, and what makes you unique.
          </Text>
          <ActivityIndicator style={{ marginTop: 24 }} />
          <Text style={{ color: colors.onSurfaceVariant, marginTop: 16, fontSize: 12 }}>
            This usually takes 2-5 minutes...
          </Text>
        </View>
      )}

      {/* Results Screen */}
      {step === ScreenStep.RESULTS && analysis && (
        <ScrollView style={{ flex: 1, padding: 16 }}>
          <Card style={{ marginBottom: 20, backgroundColor: '#D1FAE5' }}>
            <Card.Content>
              <View style={{ alignItems: 'center', paddingVertical: 16 }}>
                <MaterialCommunityIcons name="check-circle" size={64} color="#10B981" />
                <Text style={{ fontSize: 20, fontWeight: '600', marginTop: 12, color: '#065F46' }}>
                  Video Analyzed!
                </Text>
              </View>
            </Card.Content>
          </Card>

          {/* Analysis Results */}
          <Card style={{ marginBottom: 20 }}>
            <Card.Content>
              <Text style={{ fontSize: 18, fontWeight: '600', marginBottom: 16 }}>
                What We Learned About You
              </Text>

              {analysis.worldviewSummary && (
                <AnalysisSection
                  icon="earth"
                  title="Your Worldview"
                  content={analysis.worldviewSummary}
                  color="#3B82F6"
                />
              )}

              {analysis.backgroundSummary && (
                <AnalysisSection
                  icon="book-open-variant"
                  title="Your Background"
                  content={analysis.backgroundSummary}
                  color="#8B5CF6"
                />
              )}

              {analysis.lifeStorySummary && (
                <AnalysisSection
                  icon="timeline"
                  title="Your Story"
                  content={analysis.lifeStorySummary}
                  color="#EC4899"
                />
              )}
            </Card.Content>
          </Card>

          {/* Inferred Traits Preview */}
          {analysis.inferredBigFive && (
            <Card style={{ marginBottom: 20 }}>
              <Card.Content>
                <Text style={{ fontSize: 16, fontWeight: '600', marginBottom: 12 }}>
                  Inferred Personality
                </Text>
                <Text style={{ color: colors.onSurfaceVariant, marginBottom: 16 }}>
                  Based on your video, here's what we think about your personality:
                </Text>

                <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 8 }}>
                  {Object.entries(analysis.inferredBigFive).map(([trait, value]) => (
                    <Chip key={trait} style={{ backgroundColor: colors.primaryContainer }}>
                      {trait}: {Math.round(value as number)}%
                    </Chip>
                  ))}
                </View>
              </Card.Content>
            </Card>
          )}

          <Button
            mode="contained"
            onPress={viewScaffoldedProfile}
            style={{ marginBottom: 12 }}
            icon="account-check"
          >
            Review & Confirm Profile
          </Button>

          <Button
            mode="outlined"
            onPress={() => setStep(ScreenStep.STATUS)}
            style={{ marginBottom: 24 }}
          >
            Done
          </Button>
        </ScrollView>
      )}
    </View>
  );
};

// Helper Components
const BenefitItem = ({ icon, text }: { icon: string; text: string }) => {
  const { colors } = useTheme();
  return (
    <View style={{ flexDirection: 'row', alignItems: 'center' }}>
      <MaterialCommunityIcons name={icon as any} size={24} color={colors.primary} />
      <Text style={{ marginLeft: 12, flex: 1 }}>{text}</Text>
    </View>
  );
};

const TipItem = ({ icon, text }: { icon: string; text: string }) => {
  const { colors } = useTheme();
  return (
    <View style={{ flexDirection: 'row', alignItems: 'center' }}>
      <MaterialCommunityIcons name={icon as any} size={20} color="#F59E0B" />
      <Text style={{ marginLeft: 12, flex: 1, color: colors.onSurfaceVariant }}>{text}</Text>
    </View>
  );
};

const AnalysisSection = ({ icon, title, content, color }: { icon: string; title: string; content: string; color: string }) => {
  return (
    <View style={{ marginBottom: 16 }}>
      <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 8 }}>
        <MaterialCommunityIcons name={icon as any} size={24} color={color} />
        <Text style={{ marginLeft: 8, fontWeight: '600', fontSize: 15 }}>{title}</Text>
      </View>
      <Text style={{ lineHeight: 22, color: '#4B5563' }}>{content}</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  cameraOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.2)',
    justifyContent: 'space-between',
    padding: 20,
  },
  promptContainer: {
    backgroundColor: 'rgba(0,0,0,0.7)',
    padding: 16,
    borderRadius: 12,
    marginTop: 40,
  },
  promptText: {
    color: 'white',
    fontSize: 18,
    textAlign: 'center',
    fontStyle: 'italic',
  },
  controlsContainer: {
    alignItems: 'center',
    marginBottom: 40,
  },
  timerContainer: {
    width: '100%',
    marginBottom: 20,
  },
  timerText: {
    color: 'white',
    fontSize: 18,
    textAlign: 'center',
    fontWeight: '600',
  },
  recordingIndicator: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 16,
  },
  recordingDot: {
    width: 12,
    height: 12,
    borderRadius: 6,
    backgroundColor: '#EF4444',
    marginRight: 8,
  },
  recordingLabel: {
    color: 'white',
    fontWeight: '600',
  },
  stopButton: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: '#EF4444',
    justifyContent: 'center',
    alignItems: 'center',
  },
  stopButtonDisabled: {
    backgroundColor: '#9CA3AF',
  },
});

export default VideoIntro;
