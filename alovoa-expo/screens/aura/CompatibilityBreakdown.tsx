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
  Chip,
  Surface,
  ProgressBar,
  Divider,
} from "react-native-paper";
import { MaterialCommunityIcons } from "@expo/vector-icons";
import * as Global from "../../Global";
import * as URL from "../../URL";
import * as I18N from "../../i18n";
import {
  CompatibilityBreakdown as BreakdownType,
  CompatibilityDimension,
} from "../../myTypes";
import { STATUS_BAR_HEIGHT } from "../../assets/styles";

const i18n = I18N.getI18n();

const DIMENSION_META: Record<string, { icon: string; color: string; label: string; description: string }> = {
  personality: {
    icon: "head-cog",
    color: "#8B5CF6",
    label: "Personality",
    description: "How well your personalities complement each other",
  },
  values: {
    icon: "heart-pulse",
    color: "#EC4899",
    label: "Values",
    description: "Alignment on core beliefs and priorities",
  },
  lifestyle: {
    icon: "home-heart",
    color: "#10B981",
    label: "Lifestyle",
    description: "Compatibility in daily habits and preferences",
  },
  communication: {
    icon: "message-text",
    color: "#3B82F6",
    label: "Communication",
    description: "How well your communication styles match",
  },
  attachment: {
    icon: "link-variant",
    color: "#F59E0B",
    label: "Attachment",
    description: "Emotional connection compatibility",
  },
  goals: {
    icon: "flag-checkered",
    color: "#6366F1",
    label: "Future Goals",
    description: "Alignment on future plans and aspirations",
  },
  questions: {
    icon: "help-circle",
    color: "#14B8A6",
    label: "Questions",
    description: "Agreement on answered questions",
  },
  dealbreakers: {
    icon: "close-octagon",
    color: "#EF4444",
    label: "Dealbreakers",
    description: "Any incompatibilities that matter most",
  },
};

const CompatibilityBreakdown = ({ route, navigation }: any) => {
  const { colors } = useTheme();
  const { height, width } = useWindowDimensions();

  const { userId } = route.params || {};

  const [loading, setLoading] = React.useState(true);
  const [breakdown, setBreakdown] = React.useState<BreakdownType | null>(null);

  React.useEffect(() => {
    load();
  }, []);

  async function load() {
    setLoading(true);
    try {
      const response = await Global.Fetch(Global.format(URL.API_MATCHING_BREAKDOWN, userId));
      setBreakdown((response.data?.breakdown || response.data) as BreakdownType);
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t('error.generic'));
    }
    setLoading(false);
  }

  function getScoreColor(score: number): string {
    if (score >= 80) return "#10B981";
    if (score >= 60) return "#22C55E";
    if (score >= 40) return "#F59E0B";
    return "#EF4444";
  }

  function getScoreLabel(score: number): string {
    if (score >= 80) return "Excellent";
    if (score >= 60) return "Good";
    if (score >= 40) return "Fair";
    return "Low";
  }

  if (loading) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: colors.background }}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  if (!breakdown) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: colors.background, padding: 24 }}>
        <MaterialCommunityIcons name="alert-circle" size={64} color={colors.error} />
        <Text style={{ fontSize: 18, marginTop: 16 }}>Could not load compatibility data</Text>
        <Button mode="contained" onPress={() => navigation.goBack()} style={{ marginTop: 24 }}>
          Go Back
        </Button>
      </View>
    );
  }

  const overallColor = getScoreColor(breakdown.overallScore);
  const dimensions = breakdown.dimensions || [];

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      <ScrollView style={{ flex: 1 }}>
        <View style={{ paddingTop: STATUS_BAR_HEIGHT + 16, paddingHorizontal: 16 }}>
          {/* Header */}
          <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 20 }}>
            <Pressable onPress={() => navigation.goBack()} style={{ marginRight: 12 }}>
              <MaterialCommunityIcons name="arrow-left" size={24} color={colors.onSurface} />
            </Pressable>
            <Text style={{ fontSize: 24, fontWeight: '600' }}>
              Compatibility
            </Text>
          </View>

          {/* Overall Score Card - Using category labels instead of percentages */}
          <Card style={{ marginBottom: 24, overflow: 'hidden' }}>
            <View style={{ backgroundColor: overallColor, padding: 24, alignItems: 'center' }}>
              <Text style={{ color: 'white', fontSize: 32, fontWeight: '700', textAlign: 'center' }}>
                {breakdown.matchCategoryLabel || getScoreLabel(breakdown.overallScore) + " Match"}
              </Text>
            </View>

            <Card.Content style={{ paddingTop: 16 }}>
              <Text style={{ textAlign: 'center', color: colors.onSurfaceVariant }}>
                {breakdown.overallScore >= 80
                  ? "You two have excellent compatibility across most areas!"
                  : breakdown.overallScore >= 60
                  ? "You have good overall compatibility with some areas to explore."
                  : breakdown.overallScore >= 40
                  ? "You have moderate compatibility. Your differences could complement each other."
                  : "You may have some significant differences to work through."}
              </Text>
            </Card.Content>
          </Card>

          {/* Compatibility Dimensions */}
          <Text style={{ fontSize: 18, fontWeight: '600', marginBottom: 16 }}>
            Breakdown by Category
          </Text>

          {dimensions.map((dimension) => {
            const meta = DIMENSION_META[dimension.dimension] || {
              icon: "help-circle",
              color: colors.primary,
              label: dimension.dimension,
              description: "",
            };
            const scoreColor = getScoreColor(dimension.score);

            return (
              <Card key={dimension.dimension} style={{ marginBottom: 12 }}>
                <Card.Content>
                  <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                    <Surface
                      style={{
                        width: 48,
                        height: 48,
                        borderRadius: 24,
                        justifyContent: 'center',
                        alignItems: 'center',
                        backgroundColor: meta.color + '20',
                      }}
                      elevation={0}
                    >
                      <MaterialCommunityIcons name={meta.icon as any} size={24} color={meta.color} />
                    </Surface>

                    <View style={{ flex: 1, marginLeft: 12 }}>
                      <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
                        <Text style={{ fontWeight: '600' }}>{meta.label}</Text>
                        <Text style={{ fontWeight: '700', color: scoreColor, fontSize: 16 }}>
                          {breakdown.dimensionLabels?.[dimension.dimension]
                            || breakdown.dimensionLabels?.[meta.label]
                            || getScoreLabel(dimension.score)}
                        </Text>
                      </View>

                      <ProgressBar
                        progress={dimension.score / 100}
                        color={scoreColor}
                        style={{ height: 8, borderRadius: 4, marginTop: 8 }}
                      />

                      <Text style={{ fontSize: 12, color: colors.onSurfaceVariant, marginTop: 6 }}>
                        {meta.description}
                      </Text>
                    </View>
                  </View>

                  {/* Details if available */}
                  {dimension.details && dimension.details.length > 0 && (
                    <View style={{ marginTop: 12, paddingTop: 12, borderTopWidth: 1, borderTopColor: colors.surfaceVariant }}>
                      {dimension.details.map((detail, index) => (
                        <View key={index} style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 6 }}>
                          <MaterialCommunityIcons
                            name={detail.compatible ? "check-circle" : "close-circle"}
                            size={16}
                            color={detail.compatible ? "#10B981" : "#EF4444"}
                          />
                          <Text style={{ marginLeft: 8, fontSize: 13, flex: 1 }}>
                            {detail.label}
                          </Text>
                        </View>
                      ))}
                    </View>
                  )}
                </Card.Content>
              </Card>
            );
          })}

          {/* Dealbreakers Section */}
          {breakdown.dealbreakers && breakdown.dealbreakers.length > 0 && (
            <>
              <Text style={{ fontSize: 18, fontWeight: '600', marginTop: 12, marginBottom: 16 }}>
                ⚠️ Potential Dealbreakers
              </Text>

              <Card style={{ marginBottom: 24, borderLeftWidth: 4, borderLeftColor: "#EF4444" }}>
                <Card.Content>
                  {breakdown.dealbreakers.map((dealbreaker, index) => (
                    <View key={index} style={{ marginBottom: index < breakdown.dealbreakers.length - 1 ? 12 : 0 }}>
                      <Text style={{ fontWeight: '600', color: "#EF4444" }}>
                        {dealbreaker.category}
                      </Text>
                      <Text style={{ color: colors.onSurfaceVariant, marginTop: 4 }}>
                        {dealbreaker.description}
                      </Text>
                    </View>
                  ))}
                </Card.Content>
              </Card>
            </>
          )}

          {/* Shared Questions */}
          {breakdown.sharedQuestions && breakdown.sharedQuestions.length > 0 && (
            <>
              <Text style={{ fontSize: 18, fontWeight: '600', marginBottom: 16 }}>
                Questions You Both Answered
              </Text>

              {breakdown.sharedQuestions.slice(0, 5).map((question, index) => (
                <Card key={index} style={{ marginBottom: 12 }}>
                  <Card.Content>
                    <Text style={{ fontWeight: '500', marginBottom: 8 }}>
                      {question.questionText}
                    </Text>

                    <View style={{ flexDirection: 'row', gap: 8 }}>
                      <Chip
                        icon={question.yourAnswer === question.theirAnswer ? "check" : "close"}
                        style={{
                          backgroundColor: question.yourAnswer === question.theirAnswer
                            ? '#D1FAE5'
                            : '#FEE2E2',
                        }}
                      >
                        {question.yourAnswer === question.theirAnswer ? "Same Answer" : "Different"}
                      </Chip>
                    </View>

                    {question.yourAnswer !== question.theirAnswer && (
                      <View style={{ marginTop: 8, flexDirection: 'row', gap: 8 }}>
                        <View style={{ flex: 1, padding: 8, backgroundColor: colors.surfaceVariant, borderRadius: 8 }}>
                          <Text style={{ fontSize: 11, color: colors.onSurfaceVariant }}>You said:</Text>
                          <Text style={{ fontSize: 13 }}>{question.yourAnswer}</Text>
                        </View>
                        <View style={{ flex: 1, padding: 8, backgroundColor: colors.surfaceVariant, borderRadius: 8 }}>
                          <Text style={{ fontSize: 11, color: colors.onSurfaceVariant }}>They said:</Text>
                          <Text style={{ fontSize: 13 }}>{question.theirAnswer}</Text>
                        </View>
                      </View>
                    )}
                  </Card.Content>
                </Card>
              ))}

              {breakdown.sharedQuestions.length > 5 && (
                <Button mode="text" style={{ marginBottom: 12 }}>
                  See All {breakdown.sharedQuestions.length} Questions
                </Button>
              )}
            </>
          )}

          {/* Action Buttons */}
          <View style={{ marginTop: 12, marginBottom: 32 }}>
            <Button
              mode="contained"
              onPress={() => Global.navigate("Profile", false, { uuid: userId })}
              style={{ marginBottom: 12 }}
              icon="account"
            >
              View Full Profile
            </Button>

            <Button
              mode="outlined"
              onPress={() => navigation.goBack()}
            >
              Back to Matches
            </Button>
          </View>
        </View>
      </ScrollView>
    </View>
  );
};

export default CompatibilityBreakdown;
