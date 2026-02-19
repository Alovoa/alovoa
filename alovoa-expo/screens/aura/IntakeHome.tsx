import React from "react";
import {
  View,
  useWindowDimensions,
  Pressable,
  ScrollView,
} from "react-native";
import {
  Text,
  Card,
  Button,
  ActivityIndicator,
  useTheme,
  ProgressBar,
  Surface,
  Avatar,
} from "react-native-paper";
import { MaterialCommunityIcons } from "@expo/vector-icons";
import * as Global from "../../Global";
import * as URL from "../../URL";
import * as I18N from "../../i18n";
import { IntakeProgressDto, IntakeStep } from "../../myTypes";
import { STATUS_BAR_HEIGHT } from "../../assets/styles";

const i18n = I18N.getI18n();

interface IntakeStepConfig {
  id: IntakeStep;
  title: string;
  description: string;
  icon: string;
  route?: string;
  estimatedMinutes: number;
}

const INTAKE_STEPS: IntakeStepConfig[] = [
  {
    id: IntakeStep.BASIC_PROFILE,
    title: "Basic Profile",
    description: "Add your photos and write a bio",
    icon: "account-circle",
    estimatedMinutes: 5,
  },
  {
    id: IntakeStep.VIDEO_INTRO,
    title: "Video Introduction",
    description: "Record a 1-2 minute video telling us about yourself",
    icon: "video",
    route: "VideoIntro",
    estimatedMinutes: 5,
  },
  {
    id: IntakeStep.ASSESSMENT,
    title: "Personality Assessment",
    description: "Answer questions to help us find your best matches",
    icon: "head-question",
    route: "Assessment.Home",
    estimatedMinutes: 15,
  },
  {
    id: IntakeStep.POLITICAL,
    title: "Values & Politics",
    description: "Share your views on important topics",
    icon: "scale-balance",
    route: "Assessment.Question",
    estimatedMinutes: 10,
  },
  {
    id: IntakeStep.VERIFICATION,
    title: "Video Verification",
    description: "Verify you're real with a quick video selfie",
    icon: "check-decagram",
    route: "VideoVerification",
    estimatedMinutes: 2,
  },
];

const IntakeHome = ({ navigation }: any) => {
  const { colors } = useTheme();
  const { height, width } = useWindowDimensions();

  const [loading, setLoading] = React.useState(true);
  const [progress, setProgress] = React.useState<IntakeProgressDto | null>(null);
  const [encouragement, setEncouragement] = React.useState("");

  React.useEffect(() => {
    load();
  }, []);

  async function load() {
    setLoading(true);
    try {
      const response = await Global.Fetch(URL.API_INTAKE_PROGRESS);
      const raw = response.data?.progress || response.data || {};
      const stepsCompleted: IntakeStep[] = [];
      if (raw.questionsComplete) stepsCompleted.push(IntakeStep.ASSESSMENT);
      if (raw.videoIntroComplete) stepsCompleted.push(IntakeStep.VIDEO_INTRO);
      if (raw.picturesComplete) {
        stepsCompleted.push(IntakeStep.PHOTOS);
        stepsCompleted.push(IntakeStep.BASIC_PROFILE);
      }
      if (raw.intakeComplete) stepsCompleted.push(IntakeStep.COMPLETE);

      const mappedProgress: IntakeProgressDto = {
        userId: raw.userId || 0,
        currentStep: raw.currentStep || IntakeStep.VIDEO_INTRO,
        stepsCompleted,
        basicProfileComplete: raw.picturesComplete || false,
        photoUploaded: raw.picturesCount > 0,
        videoIntroComplete: raw.videoIntroComplete || false,
        assessmentStarted: (raw.questionsAnswered || 0) > 0,
        assessmentComplete: raw.questionsComplete || false,
        politicalAssessmentComplete: false,
        verificationComplete: false,
        questionsAnswered: raw.questionsAnswered || 0,
        totalQuestionsRequired: 10,
        estimatedTimeRemaining: raw.intakeComplete ? 0 : 20,
      };

      setProgress(mappedProgress);
      const encouragementText =
        response.data?.encouragement?.stepEncouragement ||
        response.data?.encouragement?.message ||
        mappedProgress.lastEncouragement ||
        getDefaultEncouragement(mappedProgress);
      setEncouragement(encouragementText);
    } catch (e) {
      console.error(e);
      // Use default progress if API fails
      setProgress({
        userId: 0,
        currentStep: IntakeStep.VIDEO_INTRO,
        stepsCompleted: [IntakeStep.BASIC_PROFILE],
        basicProfileComplete: true,
        photoUploaded: true,
        videoIntroComplete: false,
        assessmentStarted: false,
        assessmentComplete: false,
        politicalAssessmentComplete: false,
        verificationComplete: false,
        questionsAnswered: 0,
        totalQuestionsRequired: 50,
        estimatedTimeRemaining: 30,
      });
    }
    setLoading(false);
  }

  function getDefaultEncouragement(prog: IntakeProgressDto): string {
    if (!prog.videoIntroComplete) {
      return "Let's start with a video intro! It helps matches get to know the real you.";
    }
    if (!prog.assessmentStarted) {
      return "Great video! Now let's discover what makes you tick.";
    }
    if (!prog.assessmentComplete) {
      return "You're making great progress! Keep going with the assessment.";
    }
    if (!prog.verificationComplete) {
      return "Almost there! Just verify your identity and you're ready to match.";
    }
    return "You're all set! Time to find your perfect match.";
  }

  function isStepComplete(step: IntakeStep): boolean {
    if (!progress) return false;
    return progress.stepsCompleted.includes(step);
  }

  function isStepAvailable(step: IntakeStep): boolean {
    if (!progress) return false;
    const stepIndex = INTAKE_STEPS.findIndex(s => s.id === step);
    if (stepIndex === 0) return true;

    // Check if all previous steps are complete
    for (let i = 0; i < stepIndex; i++) {
      if (!isStepComplete(INTAKE_STEPS[i].id)) {
        return false;
      }
    }
    return true;
  }

  function getCurrentStep(): IntakeStepConfig | undefined {
    return INTAKE_STEPS.find(s => !isStepComplete(s.id) && isStepAvailable(s.id));
  }

  function navigateToStep(step: IntakeStepConfig) {
    if (step.route) {
      if (step.id === IntakeStep.POLITICAL) {
        // Navigate to political category specifically
        Global.navigate(step.route, false, { category: 'POLITICAL', random: false });
      } else {
        Global.navigate(step.route, false, {});
      }
    }
  }

  function getProgressPercentage(): number {
    if (!progress) return 0;
    const completedSteps = progress.stepsCompleted.length;
    return completedSteps / INTAKE_STEPS.length;
  }

  if (loading) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: colors.background }}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  const currentStep = getCurrentStep();
  const progressPct = getProgressPercentage();

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      <ScrollView contentContainerStyle={{ padding: 16, paddingTop: STATUS_BAR_HEIGHT + 16 }}>
        {/* Header */}
        <View style={{ marginBottom: 24 }}>
          <Text style={{ fontSize: 28, fontWeight: '700' }}>
            Complete Your Profile
          </Text>
          <Text style={{ color: colors.onSurfaceVariant, marginTop: 4 }}>
            Finish these steps to start matching
          </Text>
        </View>

        {/* Overall Progress */}
        <Surface style={{ padding: 16, borderRadius: 12, marginBottom: 24 }} elevation={1}>
          <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginBottom: 8 }}>
            <Text style={{ fontWeight: '600' }}>Your Progress</Text>
            <Text style={{ fontWeight: '600', color: colors.primary }}>
              {Math.round(progressPct * 100)}%
            </Text>
          </View>
          <ProgressBar
            progress={progressPct}
            color={colors.primary}
            style={{ height: 8, borderRadius: 4 }}
          />
          <Text style={{ color: colors.onSurfaceVariant, marginTop: 8, fontSize: 13 }}>
            {progress?.estimatedTimeRemaining || 30} minutes remaining
          </Text>
        </Surface>

        {/* Encouragement */}
        {encouragement && (
          <Card style={{ marginBottom: 24, backgroundColor: colors.primaryContainer }}>
            <Card.Content>
              <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                <MaterialCommunityIcons name="lightbulb-on" size={24} color={colors.primary} />
                <Text style={{ marginLeft: 12, flex: 1, color: colors.onPrimaryContainer }}>
                  {encouragement}
                </Text>
              </View>
            </Card.Content>
          </Card>
        )}

        {/* Current Step Highlight */}
        {currentStep && (
          <Card style={{ marginBottom: 24, borderWidth: 2, borderColor: colors.primary }}>
            <Card.Content>
              <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                <View style={{
                  width: 56,
                  height: 56,
                  borderRadius: 28,
                  backgroundColor: colors.primary,
                  justifyContent: 'center',
                  alignItems: 'center',
                }}>
                  <MaterialCommunityIcons
                    name={currentStep.icon as any}
                    size={28}
                    color="white"
                  />
                </View>
                <View style={{ marginLeft: 16, flex: 1 }}>
                  <Text style={{ fontSize: 12, color: colors.primary, fontWeight: '600' }}>
                    UP NEXT
                  </Text>
                  <Text style={{ fontSize: 18, fontWeight: '600', marginTop: 2 }}>
                    {currentStep.title}
                  </Text>
                  <Text style={{ color: colors.onSurfaceVariant, marginTop: 2 }}>
                    {currentStep.description}
                  </Text>
                </View>
              </View>

              <Button
                mode="contained"
                onPress={() => navigateToStep(currentStep)}
                style={{ marginTop: 16 }}
                icon="arrow-right"
                contentStyle={{ flexDirection: 'row-reverse' }}
              >
                Start ({currentStep.estimatedMinutes} min)
              </Button>
            </Card.Content>
          </Card>
        )}

        {/* All Steps */}
        <Text style={{ fontSize: 16, fontWeight: '600', marginBottom: 12 }}>
          All Steps
        </Text>

        {INTAKE_STEPS.map((step, index) => {
          const isComplete = isStepComplete(step.id);
          const isAvailable = isStepAvailable(step.id);
          const isCurrent = currentStep?.id === step.id;

          return (
            <Pressable
              key={step.id}
              onPress={() => isAvailable && !isComplete && navigateToStep(step)}
              disabled={!isAvailable || isComplete}
              style={{ marginBottom: 12 }}
            >
              <Surface
                style={{
                  padding: 16,
                  borderRadius: 12,
                  opacity: !isAvailable ? 0.5 : 1,
                  borderWidth: isCurrent ? 2 : 0,
                  borderColor: colors.primary,
                }}
                elevation={1}
              >
                <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                  {/* Step Number / Check */}
                  <View style={{
                    width: 40,
                    height: 40,
                    borderRadius: 20,
                    backgroundColor: isComplete ? '#10B981' : isAvailable ? colors.primaryContainer : colors.surfaceVariant,
                    justifyContent: 'center',
                    alignItems: 'center',
                  }}>
                    {isComplete ? (
                      <MaterialCommunityIcons name="check" size={24} color="white" />
                    ) : (
                      <Text style={{
                        fontWeight: '700',
                        color: isAvailable ? colors.primary : colors.onSurfaceVariant,
                      }}>
                        {index + 1}
                      </Text>
                    )}
                  </View>

                  {/* Step Info */}
                  <View style={{ marginLeft: 12, flex: 1 }}>
                    <Text style={{
                      fontWeight: '600',
                      color: isComplete ? '#10B981' : colors.onSurface,
                    }}>
                      {step.title}
                    </Text>
                    <Text style={{
                      fontSize: 13,
                      color: colors.onSurfaceVariant,
                      marginTop: 2,
                    }}>
                      {isComplete ? 'Completed' : `~${step.estimatedMinutes} min`}
                    </Text>
                  </View>

                  {/* Icon */}
                  <MaterialCommunityIcons
                    name={isComplete ? "check-circle" : step.icon as any}
                    size={24}
                    color={isComplete ? '#10B981' : isAvailable ? colors.primary : colors.onSurfaceVariant}
                  />
                </View>
              </Surface>
            </Pressable>
          );
        })}

        {/* Skip for now option */}
        <Button
          mode="text"
          onPress={() => Global.navigate("Main", true, {})}
          style={{ marginTop: 8 }}
        >
          Skip for now
        </Button>

        <View style={{ height: 50 }} />
      </ScrollView>
    </View>
  );
};

export default IntakeHome;
