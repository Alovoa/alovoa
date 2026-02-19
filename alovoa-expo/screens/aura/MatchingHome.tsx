import React from "react";
import {
  View,
  useWindowDimensions,
  Pressable,
  Image,
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
  Avatar,
  FAB,
} from "react-native-paper";
import { MaterialCommunityIcons } from "@expo/vector-icons";
import * as Global from "../../Global";
import * as URL from "../../URL";
import * as I18N from "../../i18n";
import {
  CompatibilityScore,
  UserDailyMatchLimit,
  UserDto,
} from "../../myTypes";
import { STATUS_BAR_HEIGHT } from "../../assets/styles";
import VerticalView from "../../components/VerticalView";

const i18n = I18N.getI18n();

interface MatchWithScore {
  user: UserDto;
  compatibilityScore: CompatibilityScore;
}

const MatchingHome = ({ navigation }: any) => {
  const { colors } = useTheme();
  const { height, width } = useWindowDimensions();

  const [loading, setLoading] = React.useState(true);
  const [refreshing, setRefreshing] = React.useState(false);
  const [matches, setMatches] = React.useState<MatchWithScore[]>([]);
  const [dailyLimit, setDailyLimit] = React.useState<UserDailyMatchLimit | null>(null);

  React.useEffect(() => {
    load();
  }, []);

  async function load() {
    setLoading(true);
    try {
      const [matchesRes, limitRes] = await Promise.all([
        Global.Fetch(URL.API_MATCHING_MATCHES),
        Global.Fetch(URL.API_MATCHING_DAILY_LIMIT),
      ]);
      const rawMatches = Array.isArray(matchesRes.data)
        ? matchesRes.data
        : (matchesRes.data?.matches || []);
      const normalizedMatches = (rawMatches as any[]).map((match) => ({
        ...match,
        compatibilityScore: match.compatibilityScore || match.score || { overallScore: 0 },
      }));
      setMatches(normalizedMatches as MatchWithScore[]);

      const limitPayload = limitRes.data || matchesRes.data?.dailyLimit || null;
      setDailyLimit(limitPayload);
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t('error.generic'));
    }
    setLoading(false);
  }

  async function refreshMatches() {
    setRefreshing(true);
    try {
      await Global.Fetch(URL.API_MATCHING_REFRESH, 'post');
      await load();
      Global.ShowToast("Matches refreshed!");
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t('error.generic'));
    }
    setRefreshing(false);
  }

  function viewProfile(userId: string) {
    Global.navigate("Profile", false, { uuid: userId });
  }

  function viewCompatibility(userId: string) {
    Global.navigate("Matching.Compatibility", false, { userId });
  }

  function getCompatibilityColor(score: number): string {
    if (score >= 90) return "#10B981";
    if (score >= 75) return "#22C55E";
    if (score >= 60) return "#F59E0B";
    if (score >= 40) return "#F97316";
    return "#EF4444";
  }

  function getCompatibilityLabel(score: number): string {
    if (score >= 90) return "Excellent Match";
    if (score >= 75) return "Great Match";
    if (score >= 60) return "Good Match";
    if (score >= 40) return "Fair Match";
    return "Low Match";
  }

  if (loading) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: colors.background }}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      <VerticalView onRefresh={load} style={{ padding: 0 }}>
        <View style={{ paddingTop: STATUS_BAR_HEIGHT + 16, paddingHorizontal: 16 }}>
          {/* Header */}
          <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
            <View style={{ flexDirection: 'row', alignItems: 'center' }}>
              <Pressable onPress={() => navigation.goBack()} style={{ marginRight: 12 }}>
                <MaterialCommunityIcons name="arrow-left" size={24} color={colors.onSurface} />
              </Pressable>
              <Text style={{ fontSize: 24, fontWeight: '600' }}>
                Your Matches
              </Text>
            </View>

            <Button
              mode="text"
              icon="cog"
              onPress={() => Global.navigate("Matching.Filter", false, {})}
            >
              Filters
            </Button>
          </View>

          {/* Daily Limit Card */}
          {dailyLimit && (
            <Card style={{ marginBottom: 20, backgroundColor: colors.primaryContainer }}>
              <Card.Content>
                <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
                  <View>
                    <Text style={{ fontWeight: '600', color: colors.onPrimaryContainer }}>
                      Daily Matches
                    </Text>
                    <Text style={{ color: colors.onPrimaryContainer, fontSize: 12 }}>
                      Resets at midnight
                    </Text>
                  </View>
                  <View style={{ alignItems: 'center' }}>
                    <Text style={{ fontSize: 24, fontWeight: '700', color: colors.primary }}>
                      {dailyLimit.remaining} / {dailyLimit.limit}
                    </Text>
                    <Text style={{ fontSize: 12, color: colors.onPrimaryContainer }}>
                      remaining
                    </Text>
                  </View>
                </View>

                <ProgressBar
                  progress={dailyLimit.remaining / dailyLimit.limit}
                  color={colors.primary}
                  style={{ height: 6, borderRadius: 3, marginTop: 12 }}
                />
              </Card.Content>
            </Card>
          )}

          {/* Growth / Journey Quick Actions */}
          <Card style={{ marginBottom: 20 }}>
            <Card.Content>
              <Text style={{ fontWeight: '600', marginBottom: 10 }}>Build Better Compatibility Data</Text>
              <Text style={{ color: colors.onSurfaceVariant, marginBottom: 12 }}>
                Add your current chapter, pace, and value tradeoffs to improve match quality and explanations.
              </Text>
              <View style={{ flexDirection: 'row', gap: 8, marginBottom: 8 }}>
                <Button
                  mode="contained-tonal"
                  style={{ flex: 1 }}
                  icon="chart-timeline-variant"
                  onPress={() => Global.navigate("Growth.Context", false, {})}
                >
                  Growth Context
                </Button>
                <Button
                  mode="outlined"
                  style={{ flex: 1 }}
                  icon="sort"
                  onPress={() => Global.navigate("Values.Hierarchy", false, {})}
                >
                  Values Rank
                </Button>
              </View>
              <Button
                mode="text"
                icon="bridge"
                onPress={() => Global.navigate("Bridge.Journey", false, {})}
              >
                Relationship Journey
              </Button>
              <Button
                mode="text"
                icon="window-open"
                onPress={() => Global.navigate("MatchWindow.List", false, {})}
              >
                Match Windows
              </Button>
            </Card.Content>
          </Card>

          {/* Matches List */}
          {matches.length === 0 ? (
            <Card style={{ marginBottom: 20 }}>
              <Card.Content style={{ alignItems: 'center', paddingVertical: 32 }}>
                <MaterialCommunityIcons name="heart-off" size={64} color={colors.onSurfaceVariant} />
                <Text style={{ fontSize: 18, marginTop: 16 }}>No Matches Yet</Text>
                <Text style={{ color: colors.onSurfaceVariant, marginTop: 8, textAlign: 'center' }}>
                  Answer more assessment questions to improve your matches, or try adjusting your filters.
                </Text>
                <Button mode="contained" onPress={() => Global.navigate("Assessment.Home", false, {})} style={{ marginTop: 16 }}>
                  Take Assessment
                </Button>
              </Card.Content>
            </Card>
          ) : (
            <>
              <Text style={{ fontWeight: '600', marginBottom: 12 }}>
                Top Matches ({matches.length})
              </Text>

              {matches.map((match, index) => {
                const score = match.compatibilityScore.overallScore;
                const compatColor = getCompatibilityColor(score);

                return (
                  <Card key={match.user.idEncoded || index} style={{ marginBottom: 12 }}>
                    <Card.Content>
                      <Pressable
                        onPress={() => viewProfile(match.user.uuid)}
                        style={{ flexDirection: 'row', alignItems: 'center' }}
                      >
                        {/* Profile Image */}
                        {match.user.profilePicture ? (
                          <Image
                            source={{ uri: match.user.profilePicture }}
                            style={{ width: 80, height: 80, borderRadius: 40 }}
                          />
                        ) : (
                          <Avatar.Text
                            size={80}
                            label={match.user.firstName?.substring(0, 2).toUpperCase() || "?"}
                          />
                        )}

                        {/* Info */}
                        <View style={{ marginLeft: 16, flex: 1 }}>
                          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                            <Text style={{ fontSize: 18, fontWeight: '600' }}>
                              {match.user.firstName}, {match.user.age}
                            </Text>
                            {match.user.verified && (
                              <MaterialCommunityIcons
                                name="check-decagram"
                                size={18}
                                color="#10B981"
                                style={{ marginLeft: 6 }}
                              />
                            )}
                          </View>

                          {match.user.locationName && (
                            <View style={{ flexDirection: 'row', alignItems: 'center', marginTop: 4 }}>
                              <MaterialCommunityIcons name="map-marker" size={14} color={colors.onSurfaceVariant} />
                              <Text style={{ fontSize: 13, color: colors.onSurfaceVariant, marginLeft: 4 }}>
                                {match.user.locationName}
                              </Text>
                            </View>
                          )}

                          {/* Compatibility Score - Using category labels instead of percentages */}
                          <View style={{ flexDirection: 'row', alignItems: 'center', marginTop: 8 }}>
                            <View style={{
                              backgroundColor: compatColor + '20',
                              paddingHorizontal: 12,
                              paddingVertical: 4,
                              borderRadius: 16,
                              flexDirection: 'row',
                              alignItems: 'center',
                            }}>
                              <MaterialCommunityIcons name="heart" size={16} color={compatColor} />
                              <Text style={{ marginLeft: 6, fontWeight: '700', color: compatColor }}>
                                {match.compatibilityScore.matchCategory || getCompatibilityLabel(score)}
                              </Text>
                            </View>
                          </View>
                        </View>
                      </Pressable>

                      {/* Actions */}
                      <View style={{ flexDirection: 'row', gap: 8, marginTop: 12 }}>
                        <Button
                          mode="contained-tonal"
                          onPress={() => viewCompatibility(match.user.uuid)}
                          style={{ flex: 1 }}
                          icon="chart-bar"
                        >
                          See Why
                        </Button>
                        <Button
                          mode="contained"
                          onPress={() => viewProfile(match.user.uuid)}
                          style={{ flex: 1 }}
                          icon="account"
                        >
                          View Profile
                        </Button>
                      </View>
                    </Card.Content>
                  </Card>
                );
              })}
            </>
          )}

          <View style={{ height: 100 }} />
        </View>
      </VerticalView>

      {/* Refresh FAB */}
      <FAB
        icon="refresh"
        style={{
          position: 'absolute',
          right: 16,
          bottom: 32,
          backgroundColor: colors.primary,
        }}
        onPress={refreshMatches}
        loading={refreshing}
      />
    </View>
  );
};

export default MatchingHome;
