import React from "react";
import {
  View,
  useWindowDimensions,
  ScrollView,
  Pressable,
} from "react-native";
import {
  Text,
  Card,
  ProgressBar,
  Button,
  ActivityIndicator,
  Chip,
  useTheme,
  Surface,
} from "react-native-paper";
import { MaterialCommunityIcons } from "@expo/vector-icons";
import * as Global from "../../Global";
import * as URL from "../../URL";
import * as I18N from "../../i18n";
import {
  AssessmentCategory,
  AssessmentResource,
  UserAssessmentProfile,
} from "../../myTypes";
import { STATUS_BAR_HEIGHT } from "../../assets/styles";
import VerticalView from "../../components/VerticalView";

const i18n = I18N.getI18n();

// Category metadata with icons and colors
const CATEGORY_META: Record<string, { icon: string; color: string; label: string }> = {
  [AssessmentCategory.PERSONALITY]: { icon: "head-cog", color: "#8B5CF6", label: "Personality" },
  [AssessmentCategory.VALUES]: { icon: "heart-pulse", color: "#EC4899", label: "Values" },
  [AssessmentCategory.LIFESTYLE]: { icon: "home-heart", color: "#10B981", label: "Lifestyle" },
  [AssessmentCategory.DATING]: { icon: "heart", color: "#F43F5E", label: "Dating" },
  [AssessmentCategory.SEX_INTIMACY]: { icon: "fire", color: "#F97316", label: "Intimacy" },
  [AssessmentCategory.RELATIONSHIP_STYLE]: { icon: "account-heart", color: "#6366F1", label: "Relationship Style" },
  [AssessmentCategory.SOCIAL]: { icon: "account-group", color: "#14B8A6", label: "Social" },
  [AssessmentCategory.LIFESTYLE_HABITS]: { icon: "leaf", color: "#22C55E", label: "Habits" },
  [AssessmentCategory.FUTURE_GOALS]: { icon: "flag-checkered", color: "#3B82F6", label: "Future Goals" },
  [AssessmentCategory.COMMUNICATION]: { icon: "message-text", color: "#A855F7", label: "Communication" },
  [AssessmentCategory.ATTACHMENT]: { icon: "link-variant", color: "#EF4444", label: "Attachment" },
  [AssessmentCategory.BIG_FIVE]: { icon: "star-five-points", color: "#F59E0B", label: "Big Five" },
};

const AssessmentHome = ({ navigation }: any) => {
  const { colors } = useTheme();
  const { height, width } = useWindowDimensions();

  const [loading, setLoading] = React.useState(true);
  const [resource, setResource] = React.useState<AssessmentResource | null>(null);
  const [profile, setProfile] = React.useState<UserAssessmentProfile | null>(null);
  const [availableCategories, setAvailableCategories] = React.useState<string[]>([]);

  React.useEffect(() => {
    load();
  }, []);

  async function load() {
    setLoading(true);
    try {
      const [progressRes, profileRes] = await Promise.all([
        Global.Fetch(URL.API_ASSESSMENT_PROGRESS),
        Global.Fetch(URL.API_ASSESSMENT_PROFILE),
      ]);
      const progressData = progressRes.data || {};
      const categoryKeys = Object.keys(progressData).filter((key) => {
        const value = (progressData as any)[key];
        return value && typeof value === "object" && value.answered !== undefined && value.total !== undefined;
      });

      let answeredCount = 0;
      let totalRequired = 0;
      const categoryProgress: Record<string, number> = {};

      categoryKeys.forEach((category) => {
        const item = (progressData as any)[category];
        const answered = Number(item?.answered || 0);
        const total = Number(item?.total || 0);
        answeredCount += answered;
        totalRequired += total;
        categoryProgress[category] = total > 0 ? answered / total : 0;
      });

      setResource({
        questions: [],
        categories: categoryKeys as any,
        userProgress: {
          answeredCount,
          totalRequired,
          categoryProgress,
        },
        profile: profileRes.data,
      });
      setAvailableCategories(categoryKeys);
      setProfile(profileRes.data);
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t('error.generic'));
    }
    setLoading(false);
  }

  function getOverallProgress(): number {
    if (!resource?.userProgress) return 0;
    const { answeredCount, totalRequired } = resource.userProgress;
    return totalRequired > 0 ? answeredCount / totalRequired : 0;
  }

  function getCategoryProgress(category: string): number {
    if (!resource?.userProgress?.categoryProgress) return 0;
    return resource.userProgress.categoryProgress[category] || 0;
  }

  function navigateToCategory(category: string) {
    Global.navigate("Assessment.Question", false, { category });
  }

  function navigateToResults() {
    Global.navigate("Assessment.Results", false, {});
  }

  function startRandomQuestions() {
    Global.navigate("Assessment.Question", false, { random: true, count: 10 });
  }

  const overallProgress = getOverallProgress();
  const answeredCount = resource?.userProgress?.answeredCount || 0;
  const totalRequired = resource?.userProgress?.totalRequired || 100;

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      {loading && (
        <View style={{ height, width, zIndex: 1, justifyContent: 'center', alignItems: 'center', position: "absolute" }}>
          <ActivityIndicator animating={loading} size="large" />
        </View>
      )}

      <VerticalView onRefresh={load} style={{ padding: 0 }}>
        <View style={{ paddingTop: STATUS_BAR_HEIGHT + 16, paddingHorizontal: 16 }}>
          {/* Header */}
          <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 24 }}>
            <Pressable onPress={() => navigation.goBack()} style={{ marginRight: 12 }}>
              <MaterialCommunityIcons name="arrow-left" size={24} color={colors.onSurface} />
            </Pressable>
            <Text style={{ fontSize: 24, fontWeight: '600' }}>
              {i18n.t('assessment.title') || 'Personality Assessment'}
            </Text>
          </View>

          {/* Overall Progress Card */}
          <Card style={{ marginBottom: 20, backgroundColor: colors.elevation.level2 }}>
            <Card.Content>
              <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                <Text style={{ fontSize: 18, fontWeight: '600' }}>
                  {i18n.t('assessment.progress') || 'Your Progress'}
                </Text>
                <Chip icon="check-circle" mode="flat">
                  {answeredCount} / {totalRequired}
                </Chip>
              </View>

              <ProgressBar
                progress={overallProgress}
                color={colors.primary}
                style={{ height: 8, borderRadius: 4, marginBottom: 8 }}
              />

              <Text style={{ color: colors.onSurfaceVariant, fontSize: 14 }}>
                {Math.round(overallProgress * 100)}% complete - {totalRequired - answeredCount} questions remaining
              </Text>

              {profile?.profileComplete && (
                <View style={{ flexDirection: 'row', alignItems: 'center', marginTop: 12 }}>
                  <MaterialCommunityIcons name="check-decagram" size={20} color="#10B981" />
                  <Text style={{ color: "#10B981", marginLeft: 8, fontWeight: '500' }}>
                    Profile Complete!
                  </Text>
                </View>
              )}
            </Card.Content>
          </Card>

          {/* Quick Actions */}
          <View style={{ flexDirection: 'row', gap: 12, marginBottom: 24 }}>
            <Button
              mode="contained"
              icon="shuffle"
              onPress={startRandomQuestions}
              style={{ flex: 1 }}
            >
              Quick 10
            </Button>
            <Button
              mode="contained-tonal"
              icon="chart-bar"
              onPress={navigateToResults}
              style={{ flex: 1 }}
              disabled={answeredCount < 20}
            >
              Results
            </Button>
          </View>

          {/* Categories Grid */}
          <Text style={{ fontSize: 18, fontWeight: '600', marginBottom: 16 }}>
            {i18n.t('assessment.categories') || 'Categories'}
          </Text>

          <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 12 }}>
            {availableCategories.map((category) => {
              const meta = CATEGORY_META[category] || {
                icon: "help-circle",
                color: colors.primary,
                label: category
              };
              const progress = getCategoryProgress(category);

              return (
                <Pressable
                  key={category}
                  onPress={() => navigateToCategory(category)}
                  style={{ width: (width - 44) / 2 }}
                >
                  <Surface
                    style={{
                      padding: 16,
                      borderRadius: 12,
                      borderLeftWidth: 4,
                      borderLeftColor: meta.color,
                    }}
                    elevation={1}
                  >
                    <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 8 }}>
                      <MaterialCommunityIcons
                        name={meta.icon as any}
                        size={24}
                        color={meta.color}
                      />
                      <Text style={{ marginLeft: 8, fontWeight: '500', flex: 1 }} numberOfLines={1}>
                        {meta.label}
                      </Text>
                    </View>

                    <ProgressBar
                      progress={progress}
                      color={meta.color}
                      style={{ height: 4, borderRadius: 2, marginBottom: 4 }}
                    />

                    <Text style={{ fontSize: 12, color: colors.onSurfaceVariant }}>
                      {Math.round(progress * 100)}%
                    </Text>
                  </Surface>
                </Pressable>
              );
            })}
          </View>

          {/* Personality Traits Preview (if profile exists) */}
          {profile && answeredCount >= 20 && (
            <View style={{ marginTop: 24 }}>
              <Text style={{ fontSize: 18, fontWeight: '600', marginBottom: 16 }}>
                Your Traits
              </Text>

              <Card style={{ backgroundColor: colors.elevation.level2 }}>
                <Card.Content>
                  {/* Big Five Preview */}
                  <TraitBar label="Openness" value={profile.openness} color="#8B5CF6" />
                  <TraitBar label="Conscientiousness" value={profile.conscientiousness} color="#10B981" />
                  <TraitBar label="Extraversion" value={profile.extraversion} color="#F59E0B" />
                  <TraitBar label="Agreeableness" value={profile.agreeableness} color="#EC4899" />
                  <TraitBar label="Neuroticism" value={profile.neuroticism} color="#EF4444" />
                </Card.Content>
              </Card>
            </View>
          )}

          <View style={{ height: 100 }} />
        </View>
      </VerticalView>
    </View>
  );
};

// Helper component for trait visualization
const TraitBar = ({ label, value, color }: { label: string; value: number; color: string }) => {
  const { colors } = useTheme();

  return (
    <View style={{ marginBottom: 12 }}>
      <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginBottom: 4 }}>
        <Text style={{ fontSize: 14 }}>{label}</Text>
        <Text style={{ fontSize: 14, color: colors.onSurfaceVariant }}>{value}%</Text>
      </View>
      <View style={{ height: 8, backgroundColor: colors.surfaceVariant, borderRadius: 4, overflow: 'hidden' }}>
        <View
          style={{
            height: '100%',
            width: `${value}%`,
            backgroundColor: color,
            borderRadius: 4,
          }}
        />
      </View>
    </View>
  );
};

export default AssessmentHome;
