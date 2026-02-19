import React from "react";
import {
  View,
  useWindowDimensions,
  Pressable,
  Animated,
} from "react-native";
import {
  Text,
  Button,
  ActivityIndicator,
  RadioButton,
  Chip,
  useTheme,
  ProgressBar,
  Surface,
  IconButton,
} from "react-native-paper";
import { MaterialCommunityIcons } from "@expo/vector-icons";
import * as Global from "../../Global";
import * as URL from "../../URL";
import * as I18N from "../../i18n";
import {
  AssessmentQuestion as QuestionType,
  AssessmentCategory,
  QuestionImportance,
  UserQuestionAnswer,
} from "../../myTypes";
import { STATUS_BAR_HEIGHT } from "../../assets/styles";

const i18n = I18N.getI18n();

const IMPORTANCE_LABELS: Record<QuestionImportance, string> = {
  [QuestionImportance.IRRELEVANT]: "Irrelevant",
  [QuestionImportance.A_LITTLE]: "A Little Important",
  [QuestionImportance.SOMEWHAT]: "Somewhat Important",
  [QuestionImportance.VERY]: "Very Important",
  [QuestionImportance.MANDATORY]: "Mandatory",
};

const AssessmentQuestionScreen = ({ route, navigation }: any) => {
  const { colors } = useTheme();
  const { height, width } = useWindowDimensions();

  const category: AssessmentCategory | undefined = route.params?.category;
  const random: boolean = route.params?.random || false;
  const count: number = route.params?.count || 10;

  const [loading, setLoading] = React.useState(true);
  const [submitting, setSubmitting] = React.useState(false);
  const [questions, setQuestions] = React.useState<QuestionType[]>([]);
  const [currentIndex, setCurrentIndex] = React.useState(0);
  const [selectedOption, setSelectedOption] = React.useState<string | null>(null);
  const [acceptableOptions, setAcceptableOptions] = React.useState<string[]>([]);
  const [importance, setImportance] = React.useState<QuestionImportance>(QuestionImportance.SOMEWHAT);
  const [isDealbreaker, setIsDealbreaker] = React.useState(false);
  const [showImportance, setShowImportance] = React.useState(false);

  const fadeAnim = React.useRef(new Animated.Value(1)).current;

  React.useEffect(() => {
    loadQuestions();
  }, []);

  async function loadQuestions() {
    setLoading(true);
    try {
      let url = URL.API_ASSESSMENT_QUESTIONS;
      if (random) {
        url = Global.format(URL.API_ASSESSMENT_QUESTIONS_RANDOM, String(count));
      } else if (category) {
        url = Global.format(URL.API_ASSESSMENT_QUESTIONS_CATEGORY, category);
      }

      const response = await Global.Fetch(url);
      const payload = response.data;
      const rawQuestions = Array.isArray(payload) ? payload : (payload?.questions || []);
      const normalized = rawQuestions.map((q: any) => normalizeQuestion(q));
      setQuestions(normalized);
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t('error.generic'));
    }
    setLoading(false);
  }

  function resetAnswerState() {
    setSelectedOption(null);
    setAcceptableOptions([]);
    setImportance(QuestionImportance.SOMEWHAT);
    setIsDealbreaker(false);
    setShowImportance(false);
  }

  async function submitAnswer() {
    if (!selectedOption || !questions[currentIndex]) return;

    setSubmitting(true);
    try {
      const answer: UserQuestionAnswer = {
        questionId: String((questions[currentIndex] as any).externalId || questions[currentIndex].id),
        selectedOptionId: selectedOption,
        acceptableOptionIds: acceptableOptions.length > 0 ? acceptableOptions : [selectedOption],
        importance,
        dealbreaker: isDealbreaker,
        answeredAt: new Date(),
      };

      await Global.Fetch(URL.API_ASSESSMENT_ANSWER + "/mobile", 'post', answer);

      // Animate transition to next question
      Animated.timing(fadeAnim, {
        toValue: 0,
        duration: 150,
        useNativeDriver: true,
      }).start(() => {
        if (currentIndex < questions.length - 1) {
          resetAnswerState();
          setCurrentIndex(currentIndex + 1);
          Animated.timing(fadeAnim, {
            toValue: 1,
            duration: 150,
            useNativeDriver: true,
          }).start();
        } else {
          // All questions answered
          Global.ShowToast("Great job! Questions completed.");
          navigation.goBack();
        }
      });
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t('error.generic'));
    }
    setSubmitting(false);
  }

  function normalizeQuestion(question: any): QuestionType {
    const options = Array.isArray(question?.options) && question.options.length > 0
      ? question.options.map((o: any, index: number) => ({
          id: String(o?.id ?? index + 1),
          text: String(o?.text ?? o?.label ?? `Option ${index + 1}`),
          value: Number(o?.value ?? index + 1),
        }))
      : buildFallbackOptions(question?.responseScale);

    return {
      ...(question || {}),
      id: String(question?.id ?? ""),
      text: String(question?.text ?? ""),
      category: (question?.category || category || AssessmentCategory.VALUES),
      options,
    } as QuestionType;
  }

  function buildFallbackOptions(responseScale?: string) {
    if (responseScale === "BINARY") {
      return [
        { id: "0", text: "No", value: 0 },
        { id: "1", text: "Yes", value: 1 },
      ];
    }
    return [
      { id: "1", text: "Strongly Disagree", value: 1 },
      { id: "2", text: "Disagree", value: 2 },
      { id: "3", text: "Neutral", value: 3 },
      { id: "4", text: "Agree", value: 4 },
      { id: "5", text: "Strongly Agree", value: 5 },
    ];
  }

  function toggleAcceptable(optionId: string) {
    if (acceptableOptions.includes(optionId)) {
      setAcceptableOptions(acceptableOptions.filter(id => id !== optionId));
    } else {
      setAcceptableOptions([...acceptableOptions, optionId]);
    }
  }

  function skipQuestion() {
    if (currentIndex < questions.length - 1) {
      resetAnswerState();
      setCurrentIndex(currentIndex + 1);
    } else {
      navigation.goBack();
    }
  }

  if (loading) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: colors.background }}>
        <ActivityIndicator size="large" />
        <Text style={{ marginTop: 16 }}>Loading questions...</Text>
      </View>
    );
  }

  if (questions.length === 0) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: colors.background, padding: 24 }}>
        <MaterialCommunityIcons name="check-all" size={64} color={colors.primary} />
        <Text style={{ fontSize: 20, marginTop: 16, textAlign: 'center' }}>
          No more questions in this category!
        </Text>
        <Button mode="contained" onPress={() => navigation.goBack()} style={{ marginTop: 24 }}>
          Go Back
        </Button>
      </View>
    );
  }

  const currentQuestion = questions[currentIndex];
  const progress = (currentIndex + 1) / questions.length;

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      {/* Header */}
      <View style={{ paddingTop: STATUS_BAR_HEIGHT + 8, paddingHorizontal: 16, paddingBottom: 8 }}>
        <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
          <Pressable onPress={() => navigation.goBack()}>
            <MaterialCommunityIcons name="close" size={24} color={colors.onSurface} />
          </Pressable>

          <Text style={{ fontSize: 16, color: colors.onSurfaceVariant }}>
            {currentIndex + 1} / {questions.length}
          </Text>

          <Pressable onPress={skipQuestion}>
            <Text style={{ color: colors.primary, fontWeight: '500' }}>Skip</Text>
          </Pressable>
        </View>

        <ProgressBar
          progress={progress}
          color={colors.primary}
          style={{ height: 4, borderRadius: 2, marginTop: 12 }}
        />
      </View>

      {/* Question Content */}
      <Animated.ScrollView
        style={{ flex: 1, opacity: fadeAnim }}
        contentContainerStyle={{ padding: 16 }}
      >
        {/* Category Badge */}
        <Chip
          style={{ alignSelf: 'flex-start', marginBottom: 16 }}
          textStyle={{ fontSize: 12 }}
        >
          {currentQuestion.category}
        </Chip>

        {/* Question Text */}
        <Text style={{ fontSize: 22, fontWeight: '600', lineHeight: 30, marginBottom: 24 }}>
          {currentQuestion.text}
        </Text>

        {/* Your Answer */}
        <Text style={{ fontSize: 14, color: colors.onSurfaceVariant, marginBottom: 12, fontWeight: '500' }}>
          YOUR ANSWER
        </Text>

        <RadioButton.Group
          value={selectedOption || ""}
          onValueChange={(value) => setSelectedOption(value)}
        >
          {currentQuestion.options.map((option) => (
            <Surface
              key={option.id}
              style={{
                marginBottom: 8,
                borderRadius: 12,
                overflow: 'hidden',
                borderWidth: selectedOption === option.id ? 2 : 0,
                borderColor: colors.primary,
              }}
              elevation={1}
            >
              <Pressable
                onPress={() => setSelectedOption(option.id)}
                style={{ flexDirection: 'row', alignItems: 'center', padding: 16 }}
              >
                <RadioButton value={option.id} />
                <Text style={{ flex: 1, marginLeft: 8, fontSize: 16 }}>
                  {option.text}
                </Text>
              </Pressable>
            </Surface>
          ))}
        </RadioButton.Group>

        {/* Acceptable Answers (shown after selecting) */}
        {selectedOption && (
          <View style={{ marginTop: 24 }}>
            <Text style={{ fontSize: 14, color: colors.onSurfaceVariant, marginBottom: 12, fontWeight: '500' }}>
              ANSWERS YOU'D ACCEPT FROM A MATCH
            </Text>

            {currentQuestion.options.map((option) => (
              <Pressable
                key={`accept-${option.id}`}
                onPress={() => toggleAcceptable(option.id)}
                style={{
                  flexDirection: 'row',
                  alignItems: 'center',
                  padding: 12,
                  marginBottom: 8,
                  backgroundColor: acceptableOptions.includes(option.id)
                    ? colors.primaryContainer
                    : colors.surfaceVariant,
                  borderRadius: 8,
                }}
              >
                <MaterialCommunityIcons
                  name={acceptableOptions.includes(option.id) ? "checkbox-marked" : "checkbox-blank-outline"}
                  size={24}
                  color={acceptableOptions.includes(option.id) ? colors.primary : colors.onSurfaceVariant}
                />
                <Text style={{ flex: 1, marginLeft: 12 }}>{option.text}</Text>
              </Pressable>
            ))}
          </View>
        )}

        {/* Importance Section */}
        {selectedOption && (
          <View style={{ marginTop: 24 }}>
            <Pressable
              onPress={() => setShowImportance(!showImportance)}
              style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}
            >
              <Text style={{ fontSize: 14, color: colors.onSurfaceVariant, fontWeight: '500' }}>
                HOW IMPORTANT IS THIS?
              </Text>
              <MaterialCommunityIcons
                name={showImportance ? "chevron-up" : "chevron-down"}
                size={24}
                color={colors.onSurfaceVariant}
              />
            </Pressable>

            {showImportance && (
              <View style={{ marginTop: 12 }}>
                {Object.entries(IMPORTANCE_LABELS).map(([key, label]) => (
                  <Pressable
                    key={key}
                    onPress={() => setImportance(Number(key) as QuestionImportance)}
                    style={{
                      flexDirection: 'row',
                      alignItems: 'center',
                      padding: 12,
                      marginBottom: 4,
                      backgroundColor: importance === Number(key) ? colors.primaryContainer : 'transparent',
                      borderRadius: 8,
                    }}
                  >
                    <RadioButton
                      value={key}
                      status={importance === Number(key) ? 'checked' : 'unchecked'}
                      onPress={() => setImportance(Number(key) as QuestionImportance)}
                    />
                    <Text style={{ marginLeft: 8 }}>{label}</Text>
                  </Pressable>
                ))}

                {/* Dealbreaker Toggle */}
                <Pressable
                  onPress={() => setIsDealbreaker(!isDealbreaker)}
                  style={{
                    flexDirection: 'row',
                    alignItems: 'center',
                    padding: 12,
                    marginTop: 8,
                    backgroundColor: isDealbreaker ? '#FEE2E2' : colors.surfaceVariant,
                    borderRadius: 8,
                  }}
                >
                  <MaterialCommunityIcons
                    name={isDealbreaker ? "checkbox-marked" : "checkbox-blank-outline"}
                    size={24}
                    color={isDealbreaker ? '#EF4444' : colors.onSurfaceVariant}
                  />
                  <View style={{ marginLeft: 12, flex: 1 }}>
                    <Text style={{ fontWeight: '500', color: isDealbreaker ? '#EF4444' : colors.onSurface }}>
                      This is a dealbreaker
                    </Text>
                    <Text style={{ fontSize: 12, color: colors.onSurfaceVariant }}>
                      Filter out people who don't match
                    </Text>
                  </View>
                </Pressable>
              </View>
            )}
          </View>
        )}

        <View style={{ height: 100 }} />
      </Animated.ScrollView>

      {/* Submit Button */}
      <View style={{ padding: 16, paddingBottom: 32, backgroundColor: colors.background }}>
        <Button
          mode="contained"
          onPress={submitAnswer}
          disabled={!selectedOption || submitting}
          loading={submitting}
          style={{ borderRadius: 12 }}
          contentStyle={{ paddingVertical: 8 }}
        >
          {currentIndex < questions.length - 1 ? "Next Question" : "Finish"}
        </Button>
      </View>
    </View>
  );
};

export default AssessmentQuestionScreen;
