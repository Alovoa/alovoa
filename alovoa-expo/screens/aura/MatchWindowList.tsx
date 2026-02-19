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
  Avatar,
  IconButton,
  TextInput,
} from "react-native-paper";
import { MaterialCommunityIcons } from "@expo/vector-icons";
import * as Global from "../../Global";
import * as URL from "../../URL";
import * as I18N from "../../i18n";
import { MatchWindow, MatchWindowStatus, MatchWindowResponse } from "../../myTypes";
import { STATUS_BAR_HEIGHT } from "../../assets/styles";
import VerticalView from "../../components/VerticalView";

const i18n = I18N.getI18n();

const STATUS_CONFIG: Record<MatchWindowStatus, { icon: string; color: string; label: string }> = {
  [MatchWindowStatus.PENDING]: { icon: "clock-outline", color: "#F59E0B", label: "Pending" },
  [MatchWindowStatus.ACTIVE]: { icon: "play-circle", color: "#3B82F6", label: "Active" },
  [MatchWindowStatus.ACCEPTED]: { icon: "check", color: "#10B981", label: "Accepted" },
  [MatchWindowStatus.DECLINED]: { icon: "close", color: "#EF4444", label: "Declined" },
  [MatchWindowStatus.EXPIRED]: { icon: "clock-alert", color: "#6B7280", label: "Expired" },
  [MatchWindowStatus.MATCHED]: { icon: "heart", color: "#EC4899", label: "Matched" },
  [MatchWindowStatus.SKIPPED]: { icon: "skip-next", color: "#6B7280", label: "Skipped" },
};

const MatchWindowList = ({ navigation }: any) => {
  const { colors } = useTheme();
  const { height, width } = useWindowDimensions();

  const [loading, setLoading] = React.useState(true);
  const [currentWindow, setCurrentWindow] = React.useState<MatchWindow | null>(null);
  const [windows, setWindows] = React.useState<MatchWindow[]>([]);
  const [responding, setResponding] = React.useState(false);
  const [introMessage, setIntroMessage] = React.useState("");
  const [sendingIntro, setSendingIntro] = React.useState(false);

  React.useEffect(() => {
    load();
  }, []);

  async function load() {
    setLoading(true);
    try {
      const [currentRes, listRes] = await Promise.all([
        Global.Fetch(URL.API_MATCH_WINDOW_CURRENT),
        Global.Fetch(URL.API_MATCH_WINDOW_LIST),
      ]);
      const currentPayload = currentRes.data && typeof currentRes.data === "object" ? currentRes.data : null;
      const listPayload = Array.isArray(listRes.data)
        ? listRes.data
        : (listRes.data?.windows || []);
      setCurrentWindow(currentPayload as MatchWindow | null);
      setIntroMessage("");
      setWindows(listPayload as MatchWindow[]);
    } catch (e) {
      console.error(e);
    }
    setLoading(false);
  }

  async function respond(windowId: string | number, response: MatchWindowResponse) {
    setResponding(true);
    try {
      await Global.Fetch(Global.format(URL.API_MATCH_WINDOW_RESPOND, String(windowId)), 'post', { response });

      if (response === MatchWindowResponse.ACCEPT) {
        Global.ShowToast("Match accepted! Start a conversation.");
      } else {
        Global.ShowToast("Match declined");
      }

      await load();
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t('error.generic'));
    }
    setResponding(false);
  }

  async function skip(windowId: string | number) {
    setResponding(true);
    try {
      await Global.Fetch(Global.format(URL.API_MATCH_WINDOW_SKIP, String(windowId)), 'post');
      Global.ShowToast("Skipped for now");
      await load();
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t('error.generic'));
    }
    setResponding(false);
  }

  async function sendIntro(windowId: string | number) {
    const trimmed = introMessage.trim();
    if (!trimmed) {
      Global.ShowToast("Intro message cannot be empty");
      return;
    }
    setSendingIntro(true);
    try {
      await Global.Fetch(Global.format(URL.API_MATCH_WINDOW_INTRO_MESSAGE, String(windowId)), 'post', {
        message: trimmed,
      });
      Global.ShowToast("Intro sent");
      setIntroMessage("");
      await load();
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t('error.generic'));
    }
    setSendingIntro(false);
  }

  function formatTimeRemaining(expiresAt: string): string {
    const now = new Date();
    const expires = new Date(expiresAt);
    const diffMs = expires.getTime() - now.getTime();

    if (diffMs <= 0) return "Expired";

    const hours = Math.floor(diffMs / (1000 * 60 * 60));
    const minutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));

    if (hours > 24) {
      const days = Math.floor(hours / 24);
      return `${days}d ${hours % 24}h left`;
    }
    if (hours > 0) {
      return `${hours}h ${minutes}m left`;
    }
    return `${minutes}m left`;
  }

  function getCompatibilityColor(score: number): string {
    if (score >= 80) return "#10B981";
    if (score >= 60) return "#22C55E";
    if (score >= 40) return "#F59E0B";
    return "#EF4444";
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
                Match Windows
              </Text>
            </View>

            <View style={{ flexDirection: 'row' }}>
              <IconButton
                icon="bridge"
                onPress={() => Global.navigate("Bridge.Journey", false, {})}
              />
              <IconButton
                icon="calendar"
                onPress={() => Global.navigate("Calendar.Availability", false, {})}
              />
            </View>
          </View>

          {/* Explanation Card */}
          <Card style={{ marginBottom: 20, backgroundColor: colors.primaryContainer }}>
            <Card.Content>
              <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                <MaterialCommunityIcons name="window-open" size={32} color={colors.primary} />
                <View style={{ marginLeft: 12, flex: 1 }}>
                  <Text style={{ fontWeight: '600', color: colors.onPrimaryContainer }}>
                    How It Works
                  </Text>
                  <Text style={{ color: colors.onPrimaryContainer, fontSize: 12 }}>
                    You receive one curated match at a time. Respond before the window expires to connect!
                  </Text>
                </View>
              </View>
            </Card.Content>
          </Card>

          {/* Current Window */}
          {currentWindow && (
            currentWindow.status === MatchWindowStatus.PENDING
            || currentWindow.status === MatchWindowStatus.ACTIVE
          ) && (
            <>
              <Text style={{ fontSize: 18, fontWeight: '600', marginBottom: 16 }}>
                ⏰ Current Match
              </Text>

              <Card style={{ marginBottom: 24, borderWidth: 2, borderColor: colors.primary }}>
                <Card.Content>
                  {/* Timer */}
                  <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'center', marginBottom: 16 }}>
                    <MaterialCommunityIcons name="clock-alert" size={20} color="#F59E0B" />
                    <Text style={{ marginLeft: 8, color: "#F59E0B", fontWeight: '600' }}>
                      {formatTimeRemaining(currentWindow.expiresAt)}
                    </Text>
                  </View>

                  {/* Match Info */}
                  {currentWindow.matchedUser && (
                  <Pressable
                    onPress={() => currentWindow.matchedUser && Global.navigate("Profile", false, { uuid: currentWindow.matchedUser.uuid })}
                    style={{ alignItems: 'center' }}
                  >
                    {currentWindow.matchedUser.profilePicture ? (
                      <Image
                        source={{ uri: currentWindow.matchedUser.profilePicture }}
                        style={{ width: 120, height: 120, borderRadius: 60 }}
                      />
                    ) : (
                      <Avatar.Text
                        size={120}
                        label={currentWindow.matchedUser.firstName?.substring(0, 2).toUpperCase() || "?"}
                      />
                    )}

                    <View style={{ flexDirection: 'row', alignItems: 'center', marginTop: 16 }}>
                      <Text style={{ fontSize: 24, fontWeight: '600' }}>
                        {currentWindow.matchedUser.firstName}, {currentWindow.matchedUser.age}
                      </Text>
                      {currentWindow.matchedUser.verified && (
                        <MaterialCommunityIcons
                          name="check-decagram"
                          size={20}
                          color="#10B981"
                          style={{ marginLeft: 6 }}
                        />
                      )}
                    </View>

                    {currentWindow.matchedUser.locationName && (
                      <View style={{ flexDirection: 'row', alignItems: 'center', marginTop: 4 }}>
                        <MaterialCommunityIcons name="map-marker" size={16} color={colors.onSurfaceVariant} />
                        <Text style={{ marginLeft: 4, color: colors.onSurfaceVariant }}>
                          {currentWindow.matchedUser.locationName}
                        </Text>
                      </View>
                    )}

                    {/* Compatibility Score - Using category labels instead of percentages */}
                    {currentWindow.compatibilityScore && (
                      <View style={{
                        flexDirection: 'row',
                        alignItems: 'center',
                        marginTop: 12,
                        backgroundColor: getCompatibilityColor(currentWindow.compatibilityScore) + '20',
                        paddingHorizontal: 16,
                        paddingVertical: 8,
                        borderRadius: 20,
                      }}>
                        <MaterialCommunityIcons
                          name="heart"
                          size={20}
                          color={getCompatibilityColor(currentWindow.compatibilityScore)}
                        />
                        <Text style={{
                          marginLeft: 8,
                          fontWeight: '700',
                          fontSize: 16,
                          color: getCompatibilityColor(currentWindow.compatibilityScore),
                        }}>
                          {currentWindow.matchCategory || (currentWindow.compatibilityScore >= 80 ? "Strong Match" : currentWindow.compatibilityScore >= 60 ? "Good Match" : "Fair Match")}
                        </Text>
                      </View>
                    )}
                  </Pressable>
                  )}

                  {/* Prompt/Reason */}
                  {currentWindow.matchReason && (
                    <Surface style={{ marginTop: 16, padding: 12, borderRadius: 8 }} elevation={1}>
                      <Text style={{ color: colors.onSurfaceVariant, fontStyle: 'italic', textAlign: 'center' }}>
                        "{currentWindow.matchReason}"
                      </Text>
                    </Surface>
                  )}

                  {currentWindow.theirIntroMessage && (
                    <Surface style={{ marginTop: 12, padding: 12, borderRadius: 8, backgroundColor: colors.primaryContainer }} elevation={1}>
                      <Text style={{ color: colors.onPrimaryContainer, fontWeight: '600', marginBottom: 4 }}>
                        Their intro message
                      </Text>
                      <Text style={{ color: colors.onPrimaryContainer }}>
                        {currentWindow.theirIntroMessage}
                      </Text>
                    </Surface>
                  )}

                  {currentWindow.myIntroMessage && (
                    <Surface style={{ marginTop: 12, padding: 12, borderRadius: 8 }} elevation={1}>
                      <Text style={{ color: colors.onSurfaceVariant, fontWeight: '600', marginBottom: 4 }}>
                        Your intro message
                      </Text>
                      <Text style={{ color: colors.onSurface }}>
                        {currentWindow.myIntroMessage}
                      </Text>
                    </Surface>
                  )}

                  {currentWindow.canSendIntroMessage && !currentWindow.myIntroMessage && (
                    <View style={{ marginTop: 14 }}>
                      <Text style={{ color: colors.onSurfaceVariant, marginBottom: 8 }}>
                        Send one intro message before deciding
                      </Text>
                      <TextInput
                        mode="outlined"
                        value={introMessage}
                        onChangeText={setIntroMessage}
                        placeholder="Say hi with something meaningful..."
                        maxLength={500}
                        multiline
                      />
                      <Button
                        mode="contained-tonal"
                        onPress={() => sendIntro(currentWindow.id)}
                        loading={sendingIntro}
                        disabled={sendingIntro}
                        style={{ marginTop: 8 }}
                        icon="message-text"
                      >
                        Send Intro
                      </Button>
                    </View>
                  )}

                  {/* Actions */}
                  <View style={{ flexDirection: 'row', gap: 12, marginTop: 20 }}>
                    <Button
                      mode="outlined"
                      onPress={() => respond(currentWindow.id, MatchWindowResponse.DECLINE)}
                      style={{ flex: 1 }}
                      loading={responding}
                      icon="close"
                    >
                      Pass
                    </Button>
                    <Button
                      mode="contained"
                      onPress={() => respond(currentWindow.id, MatchWindowResponse.ACCEPT)}
                      style={{ flex: 1 }}
                      loading={responding}
                      icon="heart"
                    >
                      Connect
                    </Button>
                  </View>

                  <Button
                    mode="text"
                    onPress={() => skip(currentWindow.id)}
                    style={{ marginTop: 8 }}
                    disabled={responding}
                  >
                    Skip for Now
                  </Button>
                </Card.Content>
              </Card>
            </>
          )}

          {/* No Current Window */}
          {!currentWindow && (
            <Card style={{ marginBottom: 24 }}>
              <Card.Content style={{ alignItems: 'center', paddingVertical: 32 }}>
                <MaterialCommunityIcons name="window-closed-variant" size={64} color={colors.onSurfaceVariant} />
                <Text style={{ fontSize: 18, marginTop: 16 }}>No Active Match Window</Text>
                <Text style={{ color: colors.onSurfaceVariant, marginTop: 8, textAlign: 'center' }}>
                  Your next match will appear here. Check back soon!
                </Text>
              </Card.Content>
            </Card>
          )}

          {/* Previous Windows */}
          {windows.filter(w =>
            w.status !== MatchWindowStatus.PENDING
            && w.status !== MatchWindowStatus.ACTIVE
          ).length > 0 && (
            <>
              <Text style={{ fontSize: 18, fontWeight: '600', marginBottom: 16 }}>
                Previous Matches
              </Text>

              {windows.filter(w =>
                w.status !== MatchWindowStatus.PENDING
                && w.status !== MatchWindowStatus.ACTIVE
                && w.matchedUser
              ).map((window) => {
                const statusConfig = STATUS_CONFIG[window.status];
                const matchedUser = window.matchedUser!;

                return (
                  <Card key={window.id} style={{ marginBottom: 12, opacity: 0.8 }}>
                    <Card.Content>
                      <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                        {matchedUser.profilePicture ? (
                          <Image
                            source={{ uri: matchedUser.profilePicture }}
                            style={{ width: 56, height: 56, borderRadius: 28 }}
                          />
                        ) : (
                          <Avatar.Text
                            size={56}
                            label={matchedUser.firstName?.substring(0, 2).toUpperCase() || "?"}
                          />
                        )}

                        <View style={{ marginLeft: 12, flex: 1 }}>
                          <Text style={{ fontWeight: '600' }}>
                            {matchedUser.firstName}, {matchedUser.age}
                          </Text>
                          <Chip
                            icon={statusConfig.icon}
                            textStyle={{ fontSize: 11 }}
                            style={{
                              alignSelf: 'flex-start',
                              marginTop: 4,
                              backgroundColor: statusConfig.color + '20',
                            }}
                          >
                            {statusConfig.label}
                          </Chip>
                        </View>

                        {window.status === MatchWindowStatus.ACCEPTED && (
                          <View style={{ flexDirection: 'row' }}>
                            <Button
                              mode="text"
                              onPress={() => Global.navigate("Main", false, { screen: Global.SCREEN_CHAT })}
                            >
                              Open Chats
                            </Button>
                            {window.conversationId && (
                              <Button
                                mode="text"
                                onPress={() => Global.navigate("Bridge.Journey", false, { conversationId: window.conversationId })}
                              >
                                Journey
                              </Button>
                            )}
                          </View>
                        )}
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
    </View>
  );
};

export default MatchWindowList;
