import React from "react";
import {
  View,
  useWindowDimensions,
  Pressable,
  StyleSheet,
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
} from "react-native-paper";
import { MaterialCommunityIcons } from "@expo/vector-icons";
import * as Global from "../../Global";
import * as URL from "../../URL";
import * as I18N from "../../i18n";
import { UserVideoVerification, VerificationStatus } from "../../myTypes";
import { STATUS_BAR_HEIGHT } from "../../assets/styles";

// Platform-specific imports
import WebCamera, { WebCameraRef, requestWebCameraPermissions } from "../../components/WebCamera";

// Native imports (conditional)
let Camera: any;
let CameraView: any;

if (Platform.OS !== "web") {
  Camera = require("expo-camera").Camera;
  CameraView = require("expo-camera").CameraView;
}

type CameraType = "front" | "back";

const i18n = I18N.getI18n();

const STATUS_CONFIG: Record<VerificationStatus, { icon: string; color: string; label: string }> = {
  [VerificationStatus.PENDING]: { icon: "clock-outline", color: "#F59E0B", label: "Pending Review" },
  [VerificationStatus.PROCESSING]: { icon: "cog", color: "#3B82F6", label: "Processing" },
  [VerificationStatus.VERIFIED]: { icon: "check-decagram", color: "#10B981", label: "Verified" },
  [VerificationStatus.FAILED]: { icon: "close-circle", color: "#EF4444", label: "Failed" },
  [VerificationStatus.EXPIRED]: { icon: "clock-alert", color: "#F59E0B", label: "Expired" },
  [VerificationStatus.NEEDS_REVIEW]: { icon: "eye", color: "#6366F1", label: "Under Review" },
};

enum ScreenStep {
  STATUS,
  INSTRUCTIONS,
  RECORDING,
  UPLOADING,
  COMPLETE,
}

const VideoVerification = ({ navigation }: any) => {
  const { colors } = useTheme();
  const { height, width } = useWindowDimensions();

  // Use different refs for web vs native
  const nativeCameraRef = React.useRef<any>(null);
  const webCameraRef = React.useRef<WebCameraRef>(null);
  const cameraRef = Platform.OS === "web" ? webCameraRef : nativeCameraRef;

  const [loading, setLoading] = React.useState(true);
  const [step, setStep] = React.useState<ScreenStep>(ScreenStep.STATUS);
  const [verification, setVerification] = React.useState<UserVideoVerification | null>(null);
  const [hasPermission, setHasPermission] = React.useState<boolean | null>(null);
  const [isRecording, setIsRecording] = React.useState(false);
  const [recordingProgress, setRecordingProgress] = React.useState(0);
  const [uploadProgress, setUploadProgress] = React.useState(0);
  const [videoUri, setVideoUri] = React.useState<string | null>(null);
  const [gesture, setGesture] = React.useState<string>("smile");
  const [cameraType, setCameraType] = React.useState<CameraType>("front");

  const GESTURES = ["smile", "nod", "wave", "thumbs up"];
  const RECORDING_DURATION = 5000; // 5 seconds

  React.useEffect(() => {
    loadStatus();
    requestPermissions();
  }, []);

  async function requestPermissions() {
    if (Platform.OS === "web") {
      const result = await requestWebCameraPermissions();
      setHasPermission(result.status === "granted");
    } else {
      const { status } = await Camera.requestCameraPermissionsAsync();
      setHasPermission(status === "granted");
    }
  }

  async function loadStatus() {
    setLoading(true);
    try {
      const response = await Global.Fetch(URL.API_VIDEO_VERIFICATION_STATUS);
      setVerification(response.data);

      // If already verified, stay on status
      if (response.data?.status === VerificationStatus.VERIFIED) {
        setStep(ScreenStep.STATUS);
      }
    } catch (e) {
      console.error(e);
    }
    setLoading(false);
  }

  function startVerification() {
    // Pick a random gesture
    const randomGesture = GESTURES[Math.floor(Math.random() * GESTURES.length)];
    setGesture(randomGesture);
    setStep(ScreenStep.INSTRUCTIONS);
  }

  async function startRecording() {
    if (!cameraRef.current || isRecording) return;

    setStep(ScreenStep.RECORDING);
    setIsRecording(true);
    setRecordingProgress(0);

    // Progress animation
    const progressInterval = setInterval(() => {
      setRecordingProgress((prev) => Math.min(prev + 0.02, 1));
    }, RECORDING_DURATION / 50);

    try {
      const video = await cameraRef.current.recordAsync({
        maxDuration: RECORDING_DURATION / 1000,
      });

      clearInterval(progressInterval);
      setRecordingProgress(1);
      setIsRecording(false);

      if (video?.uri) {
        setVideoUri(video.uri);
        await uploadVideo(video.uri);
      }
    } catch (e) {
      clearInterval(progressInterval);
      console.error(e);
      setIsRecording(false);
      Global.ShowToast("Recording failed. Please try again.");
      setStep(ScreenStep.INSTRUCTIONS);
    }
  }

  async function stopRecording() {
    if (cameraRef.current && isRecording) {
      cameraRef.current.stopRecording();
    }
  }

  async function uploadVideo(uri: string) {
    setStep(ScreenStep.UPLOADING);
    setUploadProgress(0);

    try {
      // Start verification session
      const startResponse = await Global.Fetch(URL.API_VIDEO_VERIFICATION_START, 'post', { gesture });
      const sessionId = startResponse.data?.sessionId;
      if (!sessionId) {
        throw new Error("Missing verification session");
      }

      const formData = new FormData();
      if (Platform.OS === "web") {
        const blob = await fetch(uri).then((r) => r.blob());
        formData.append("video", blob as any, "verification.webm");
      } else {
        formData.append("video", {
          uri,
          name: "verification.mp4",
          type: "video/mp4",
        } as any);
      }
      formData.append("sessionId", sessionId);
      formData.append("metadata", JSON.stringify({ gesture }));

      setUploadProgress(0.5);
      await Global.Fetch(URL.API_VIDEO_VERIFICATION_UPLOAD, 'post', formData, 'multipart/form-data');
      setUploadProgress(1);

      setStep(ScreenStep.COMPLETE);
      await loadStatus();
    } catch (e) {
      console.error(e);
      Global.ShowToast("Upload failed. Please try again.");
      setStep(ScreenStep.INSTRUCTIONS);
    }
  }

  async function retry() {
    try {
      await Global.Fetch(URL.API_VIDEO_VERIFICATION_RETRY, 'post');
      setVerification(null);
      startVerification();
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t('error.generic'));
    }
  }

  function renderCameraOverlay() {
    return (
      <View style={styles.cameraOverlay}>
        {/* Face Guide */}
        <View style={styles.faceGuide} />

        {/* Gesture Prompt */}
        <View style={styles.gesturePrompt}>
          <Text style={styles.gestureText}>
            {gesture.toUpperCase()}
          </Text>
        </View>

        {/* Progress */}
        <View style={styles.progressContainer}>
          <View style={styles.recordingIndicator}>
            <View style={styles.recordingDot} />
            <Text style={styles.recordingText}>Recording...</Text>
          </View>
          <ProgressBar
            progress={recordingProgress}
            color="#EF4444"
            style={{ height: 4, borderRadius: 2 }}
          />
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
          Camera permission is required for video verification.
        </Text>
        <Button mode="contained" onPress={requestPermissions} style={{ marginTop: 24 }}>
          Grant Permission
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
            Video Verification
          </Text>
        </View>
      </View>

      {/* Status Screen */}
      {step === ScreenStep.STATUS && (
        <View style={{ flex: 1, padding: 16 }}>
          {verification ? (
            <Card style={{ marginBottom: 20 }}>
              <Card.Content>
                <View style={{ alignItems: 'center', paddingVertical: 24 }}>
                  <MaterialCommunityIcons
                    name={STATUS_CONFIG[verification.status]?.icon as any || "help-circle"}
                    size={64}
                    color={STATUS_CONFIG[verification.status]?.color}
                  />
                  <Text style={{ fontSize: 24, fontWeight: '600', marginTop: 16 }}>
                    {STATUS_CONFIG[verification.status]?.label}
                  </Text>

                  {verification.status === VerificationStatus.VERIFIED && (
                    <View style={{ marginTop: 16, alignItems: 'center' }}>
                      <Chip icon="check" style={{ backgroundColor: '#D1FAE5' }}>
                        Verified on {new Date(verification.verifiedAt!).toLocaleDateString()}
                      </Chip>
                      {verification.expiresAt && (
                        <Text style={{ marginTop: 8, color: colors.onSurfaceVariant }}>
                          Expires: {new Date(verification.expiresAt).toLocaleDateString()}
                        </Text>
                      )}
                    </View>
                  )}

                  {verification.status === VerificationStatus.FAILED && (
                    <View style={{ marginTop: 16 }}>
                      <Text style={{ color: colors.error, textAlign: 'center' }}>
                        Verification failed. Please try again.
                      </Text>
                      <Button mode="contained" onPress={retry} style={{ marginTop: 12 }}>
                        Try Again
                      </Button>
                    </View>
                  )}

                  {verification.status === VerificationStatus.PROCESSING && (
                    <View style={{ marginTop: 16, alignItems: 'center' }}>
                      <ActivityIndicator style={{ marginBottom: 8 }} />
                      <Text style={{ color: colors.onSurfaceVariant }}>
                        We're analyzing your video. This usually takes a few minutes.
                      </Text>
                    </View>
                  )}
                </View>
              </Card.Content>
            </Card>
          ) : (
            <Card style={{ marginBottom: 20 }}>
              <Card.Content>
                <View style={{ alignItems: 'center', paddingVertical: 24 }}>
                  <MaterialCommunityIcons name="shield-check" size={64} color={colors.primary} />
                  <Text style={{ fontSize: 20, fontWeight: '600', marginTop: 16, textAlign: 'center' }}>
                    Get Verified
                  </Text>
                  <Text style={{ color: colors.onSurfaceVariant, marginTop: 8, textAlign: 'center' }}>
                    Verify you're a real person with a quick video selfie. This builds trust with your matches.
                  </Text>
                </View>
              </Card.Content>
            </Card>
          )}

          {/* Benefits */}
          <Text style={{ fontSize: 16, fontWeight: '600', marginBottom: 12 }}>
            Why Verify?
          </Text>

          <View style={{ gap: 12 }}>
            <BenefitItem icon="shield-check" text="Get a verified badge on your profile" />
            <BenefitItem icon="trending-up" text="Appear higher in search results" />
            <BenefitItem icon="heart" text="More matches trust verified profiles" />
            <BenefitItem icon="robot" text="AI checks for deepfakes and catfishing" />
          </View>

          {(!verification || verification.status === VerificationStatus.EXPIRED) && (
            <Button
              mode="contained"
              onPress={startVerification}
              style={{ marginTop: 24 }}
              icon="camera"
            >
              Start Verification
            </Button>
          )}
        </View>
      )}

      {/* Instructions Screen */}
      {step === ScreenStep.INSTRUCTIONS && (
        <View style={{ flex: 1, padding: 16, justifyContent: 'center' }}>
          <Card>
            <Card.Content style={{ alignItems: 'center', paddingVertical: 32 }}>
              <MaterialCommunityIcons name="gesture-tap" size={64} color={colors.primary} />
              <Text style={{ fontSize: 24, fontWeight: '600', marginTop: 16 }}>
                Quick Gesture Check
              </Text>
              <Text style={{ color: colors.onSurfaceVariant, marginTop: 8, textAlign: 'center' }}>
                When recording starts, please:
              </Text>
              <Chip icon="emoticon" style={{ marginTop: 16, backgroundColor: colors.primaryContainer }}>
                <Text style={{ fontWeight: '600', fontSize: 18 }}>{gesture.toUpperCase()}</Text>
              </Chip>
              <Text style={{ color: colors.onSurfaceVariant, marginTop: 16, textAlign: 'center' }}>
                Hold for 5 seconds. Make sure your face is well-lit and clearly visible.
              </Text>
            </Card.Content>
          </Card>

          <Button mode="contained" onPress={startRecording} style={{ marginTop: 24 }} icon="video">
            Start Recording
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

      {/* Complete Screen */}
      {step === ScreenStep.COMPLETE && (
        <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', padding: 24 }}>
          <MaterialCommunityIcons name="check-circle" size={80} color="#10B981" />
          <Text style={{ fontSize: 24, fontWeight: '600', marginTop: 16 }}>
            Submitted!
          </Text>
          <Text style={{ color: colors.onSurfaceVariant, marginTop: 8, textAlign: 'center' }}>
            Your verification video has been submitted. We'll notify you once it's processed.
          </Text>
          <Button mode="contained" onPress={() => setStep(ScreenStep.STATUS)} style={{ marginTop: 24 }}>
            Done
          </Button>
        </View>
      )}
    </View>
  );
};

// Helper Component
const BenefitItem = ({ icon, text }: { icon: string; text: string }) => {
  const { colors } = useTheme();
  return (
    <View style={{ flexDirection: 'row', alignItems: 'center' }}>
      <MaterialCommunityIcons name={icon as any} size={24} color={colors.primary} />
      <Text style={{ marginLeft: 12, flex: 1 }}>{text}</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  cameraOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.3)',
    justifyContent: 'space-between',
    padding: 20,
  },
  faceGuide: {
    width: 250,
    height: 300,
    borderWidth: 3,
    borderColor: 'rgba(255,255,255,0.5)',
    borderRadius: 150,
    alignSelf: 'center',
    marginTop: 100,
  },
  gesturePrompt: {
    backgroundColor: 'rgba(0,0,0,0.7)',
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 8,
    alignSelf: 'center',
  },
  gestureText: {
    color: 'white',
    fontSize: 24,
    fontWeight: '600',
  },
  progressContainer: {
    marginBottom: 40,
  },
  recordingIndicator: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 12,
  },
  recordingDot: {
    width: 12,
    height: 12,
    borderRadius: 6,
    backgroundColor: '#EF4444',
    marginRight: 8,
  },
  recordingText: {
    color: 'white',
    fontWeight: '600',
  },
});

export default VideoVerification;
