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
  UserReputationScore,
  ReputationBadge,
  ReputationHistoryItem,
} from "../../myTypes";
import { STATUS_BAR_HEIGHT } from "../../assets/styles";
import VerticalView from "../../components/VerticalView";

const i18n = I18N.getI18n();

// Standing labels (categorical instead of numerical) to prevent gaming/optimization behavior
const STANDING_CONFIG: Record<string, { color: string; label: string; icon: string; minScore: number }> = {
  building: { color: "#6B7280", label: "Building", icon: "account-clock", minScore: 0 },
  good: { color: "#22C55E", label: "Good Standing", icon: "account-check", minScore: 200 },
  excellent: { color: "#3B82F6", label: "Excellent", icon: "account-star", minScore: 500 },
  outstanding: { color: "#8B5CF6", label: "Outstanding", icon: "star-circle", minScore: 1000 },
  exemplary: { color: "#EC4899", label: "Exemplary", icon: "crown", minScore: 2000 },
};

const BADGE_ICONS: Record<string, { icon: string; color: string }> = {
  verified: { icon: "check-decagram", color: "#10B981" },
  early_adopter: { icon: "clock-fast", color: "#8B5CF6" },
  active_dater: { icon: "heart-multiple", color: "#EC4899" },
  good_communicator: { icon: "message-text", color: "#3B82F6" },
  community_builder: { icon: "account-group", color: "#F59E0B" },
  respectful: { icon: "handshake", color: "#14B8A6" },
  reliable: { icon: "calendar-check", color: "#22C55E" },
  photo_verified: { icon: "camera-account", color: "#6366F1" },
  video_verified: { icon: "video-check", color: "#A855F7" },
  assessment_complete: { icon: "clipboard-check", color: "#EF4444" },
};

const ReputationScore = ({ navigation }: any) => {
  const { colors } = useTheme();
  const { height, width } = useWindowDimensions();

  const [loading, setLoading] = React.useState(true);
  const [score, setScore] = React.useState<UserReputationScore | null>(null);
  const [badges, setBadges] = React.useState<ReputationBadge[]>([]);
  const [history, setHistory] = React.useState<ReputationHistoryItem[]>([]);

  React.useEffect(() => {
    load();
  }, []);

  async function load() {
    setLoading(true);
    try {
      const [scoreRes, badgesRes, historyRes] = await Promise.all([
        Global.Fetch(URL.API_REPUTATION_SCORE),
        Global.Fetch(URL.API_REPUTATION_BADGES),
        Global.Fetch(URL.API_REPUTATION_HISTORY),
      ]);
      setScore(scoreRes.data);
      setBadges((badgesRes.data?.badges || badgesRes.data || []) as ReputationBadge[]);
      setHistory((historyRes.data?.history || historyRes.data || []) as ReputationHistoryItem[]);
    } catch (e) {
      console.error(e);
    }
    setLoading(false);
  }

  function getStanding(totalScore: number): { color: string; label: string; icon: string; minScore: number; nextStanding?: typeof STANDING_CONFIG[string] } {
    const standings = Object.entries(STANDING_CONFIG).sort((a, b) => b[1].minScore - a[1].minScore);

    for (let i = 0; i < standings.length; i++) {
      if (totalScore >= standings[i][1].minScore) {
        return {
          ...standings[i][1],
          nextStanding: i > 0 ? standings[i - 1][1] : undefined,
        };
      }
    }

    return { ...STANDING_CONFIG.building, nextStanding: STANDING_CONFIG.good };
  }

  function formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString(undefined, {
      month: 'short',
      day: 'numeric',
    });
  }

  if (loading) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: colors.background }}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  const totalScore = score?.totalScore || 0;
  const standing = getStanding(totalScore);
  const progressToNext = standing.nextStanding
    ? (totalScore - standing.minScore) / (standing.nextStanding.minScore - standing.minScore)
    : 1;

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      <VerticalView onRefresh={load} style={{ padding: 0 }}>
        <View style={{ paddingTop: STATUS_BAR_HEIGHT + 16, paddingHorizontal: 16 }}>
          {/* Header */}
          <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 20 }}>
            <Pressable onPress={() => navigation.goBack()} style={{ marginRight: 12 }}>
              <MaterialCommunityIcons name="arrow-left" size={24} color={colors.onSurface} />
            </Pressable>
            <Text style={{ fontSize: 24, fontWeight: '600' }}>
              Reputation
            </Text>
          </View>

          {/* Standing Card - Uses categorical labels instead of numerical scores */}
          <Card style={{ marginBottom: 24, overflow: 'hidden' }}>
            <View style={{ backgroundColor: standing.color, padding: 24, alignItems: 'center' }}>
              <MaterialCommunityIcons name={standing.icon as any} size={48} color="white" />
              <Text style={{ color: 'white', fontSize: 32, fontWeight: '700', marginTop: 8, textAlign: 'center' }}>
                {standing.label}
              </Text>
              <Text style={{ color: 'white', fontSize: 14, opacity: 0.9, marginTop: 4 }}>
                Your community standing
              </Text>
            </View>

            {standing.nextStanding && (
              <Card.Content style={{ paddingTop: 16 }}>
                <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginBottom: 8 }}>
                  <Text style={{ color: colors.onSurfaceVariant }}>
                    Progress to {standing.nextStanding.label}
                  </Text>
                  <Text style={{ fontWeight: '600', color: colors.onSurfaceVariant }}>
                    Keep it up!
                  </Text>
                </View>
                <ProgressBar
                  progress={progressToNext}
                  color={standing.nextStanding.color}
                  style={{ height: 8, borderRadius: 4 }}
                />
              </Card.Content>
            )}
          </Card>

          {/* Standing Factors - Uses categorical labels */}
          {score && (
            <>
              <Text style={{ fontSize: 18, fontWeight: '600', marginBottom: 16 }}>
                Standing Factors
              </Text>

              <Card style={{ marginBottom: 24 }}>
                <Card.Content>
                  <StandingFactorRow
                    icon="check-decagram"
                    label="Verification"
                    value={score.verificationPoints}
                    color="#10B981"
                  />
                  <StandingFactorRow
                    icon="message-text"
                    label="Response Rate"
                    value={score.responsePoints}
                    color="#3B82F6"
                  />
                  <StandingFactorRow
                    icon="calendar-check"
                    label="Reliability"
                    value={score.reliabilityPoints}
                    color="#22C55E"
                  />
                  <StandingFactorRow
                    icon="heart"
                    label="Positive Feedback"
                    value={score.feedbackPoints}
                    color="#EC4899"
                  />
                  <StandingFactorRow
                    icon="clock"
                    label="Tenure"
                    value={score.tenurePoints}
                    color="#F59E0B"
                    isLast
                  />
                </Card.Content>
              </Card>
            </>
          )}

          {/* Badges */}
          <Text style={{ fontSize: 18, fontWeight: '600', marginBottom: 16 }}>
            Badges ({badges.length})
          </Text>

          {badges.length > 0 ? (
            <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 12, marginBottom: 24 }}>
              {badges.map((badge, index) => {
                const badgeInfo = BADGE_ICONS[badge.type] || { icon: "star", color: colors.primary };

                return (
                  <Surface key={index} style={{ padding: 16, borderRadius: 12, alignItems: 'center', minWidth: 100 }} elevation={1}>
                    <MaterialCommunityIcons
                      name={badgeInfo.icon as any}
                      size={32}
                      color={badgeInfo.color}
                    />
                    <Text style={{ marginTop: 8, fontWeight: '500', textAlign: 'center', fontSize: 12 }}>
                      {badge.name}
                    </Text>
                    {badge.earnedAt && (
                      <Text style={{ fontSize: 10, color: colors.onSurfaceVariant }}>
                        {formatDate(badge.earnedAt)}
                      </Text>
                    )}
                  </Surface>
                );
              })}
            </View>
          ) : (
            <Card style={{ marginBottom: 24 }}>
              <Card.Content style={{ alignItems: 'center', paddingVertical: 24 }}>
                <MaterialCommunityIcons name="trophy-outline" size={48} color={colors.onSurfaceVariant} />
                <Text style={{ marginTop: 12, color: colors.onSurfaceVariant }}>
                  Complete activities to earn badges!
                </Text>
              </Card.Content>
            </Card>
          )}

          {/* Recent Activity - Shows impact type instead of numerical points */}
          {history.length > 0 && (
            <>
              <Text style={{ fontSize: 18, fontWeight: '600', marginBottom: 16 }}>
                Recent Activity
              </Text>

              <Card style={{ marginBottom: 24 }}>
                <Card.Content>
                  {history.slice(0, 10).map((item, index) => (
                    <View key={index}>
                      <View style={{ flexDirection: 'row', alignItems: 'center', paddingVertical: 12 }}>
                        <MaterialCommunityIcons
                          name={item.points >= 0 ? "arrow-up-circle" : "arrow-down-circle"}
                          size={24}
                          color={item.points >= 0 ? "#10B981" : "#EF4444"}
                        />
                        <View style={{ marginLeft: 12, flex: 1 }}>
                          <Text style={{ fontWeight: '500' }}>{item.description}</Text>
                          <Text style={{ fontSize: 12, color: colors.onSurfaceVariant }}>
                            {formatDate(item.createdAt)}
                          </Text>
                        </View>
                        <Chip
                          mode="flat"
                          textStyle={{ fontSize: 11 }}
                          style={{
                            backgroundColor: item.points >= 0 ? "#D1FAE5" : "#FEE2E2",
                          }}
                        >
                          {item.points >= 0 ? "Positive" : "Needs work"}
                        </Chip>
                      </View>
                      {index < history.slice(0, 10).length - 1 && <Divider />}
                    </View>
                  ))}
                </Card.Content>
              </Card>
            </>
          )}

          {/* How to Improve */}
          <Text style={{ fontSize: 18, fontWeight: '600', marginBottom: 16 }}>
            How to Improve
          </Text>

          <Card style={{ marginBottom: 32 }}>
            <Card.Content>
              <TipRow
                icon="check-decagram"
                title="Verify your profile"
                description="Complete video verification for +100 points"
              />
              <TipRow
                icon="message-reply"
                title="Respond to messages"
                description="Quick, thoughtful responses boost your score"
              />
              <TipRow
                icon="calendar"
                title="Show up for dates"
                description="Be reliable and on-time for scheduled calls"
              />
              <TipRow
                icon="thumb-up"
                title="Get positive feedback"
                description="Great conversations lead to reputation points"
                isLast
              />
            </Card.Content>
          </Card>

          <View style={{ height: 50 }} />
        </View>
      </VerticalView>
    </View>
  );
};

// Helper function to convert score to categorical label
function getFactorLabel(value: number): string {
  if (value >= 80) return "Excellent";
  if (value >= 60) return "Good";
  if (value >= 40) return "Fair";
  if (value >= 20) return "Building";
  return "New";
}

// Helper Components - Uses categorical labels instead of numerical scores
const StandingFactorRow = ({
  icon,
  label,
  value,
  color,
  isLast = false,
}: {
  icon: string;
  label: string;
  value: number;
  color: string;
  isLast?: boolean;
}) => {
  const { colors } = useTheme();
  const factorLabel = getFactorLabel(value);

  return (
    <View style={{ marginBottom: isLast ? 0 : 16 }}>
      <View style={{ flexDirection: 'row', alignItems: 'center' }}>
        <MaterialCommunityIcons name={icon as any} size={20} color={color} />
        <Text style={{ marginLeft: 12, flex: 1 }}>{label}</Text>
        <Text style={{ fontWeight: '700', color }}>{factorLabel}</Text>
      </View>
      <ProgressBar
        progress={Math.min(value / 100, 1)}
        color={color}
        style={{ height: 4, borderRadius: 2, marginTop: 8 }}
      />
    </View>
  );
};

const TipRow = ({
  icon,
  title,
  description,
  isLast = false,
}: {
  icon: string;
  title: string;
  description: string;
  isLast?: boolean;
}) => {
  const { colors } = useTheme();

  return (
    <>
      <View style={{ flexDirection: 'row', alignItems: 'flex-start', paddingVertical: 12 }}>
        <MaterialCommunityIcons name={icon as any} size={24} color={colors.primary} />
        <View style={{ marginLeft: 12, flex: 1 }}>
          <Text style={{ fontWeight: '600' }}>{title}</Text>
          <Text style={{ fontSize: 13, color: colors.onSurfaceVariant }}>{description}</Text>
        </View>
      </View>
      {!isLast && <Divider />}
    </>
  );
};

export default ReputationScore;
