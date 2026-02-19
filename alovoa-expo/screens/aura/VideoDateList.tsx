import React from "react";
import {
  View,
  useWindowDimensions,
  Pressable,
  FlatList,
} from "react-native";
import {
  Text,
  Card,
  Button,
  ActivityIndicator,
  useTheme,
  Chip,
  Avatar,
  Surface,
  FAB,
} from "react-native-paper";
import { MaterialCommunityIcons } from "@expo/vector-icons";
import * as Global from "../../Global";
import * as URL from "../../URL";
import * as I18N from "../../i18n";
import { VideoDate, VideoDateStatus } from "../../myTypes";
import { STATUS_BAR_HEIGHT } from "../../assets/styles";
import VerticalView from "../../components/VerticalView";

const i18n = I18N.getI18n();

const STATUS_CONFIG: Record<VideoDateStatus, { icon: string; color: string; label: string }> = {
  [VideoDateStatus.PROPOSED]: { icon: "clock-outline", color: "#F59E0B", label: "Proposed" },
  [VideoDateStatus.ACCEPTED]: { icon: "check", color: "#10B981", label: "Accepted" },
  [VideoDateStatus.SCHEDULED]: { icon: "calendar-check", color: "#3B82F6", label: "Scheduled" },
  [VideoDateStatus.IN_PROGRESS]: { icon: "video", color: "#8B5CF6", label: "In Progress" },
  [VideoDateStatus.COMPLETED]: { icon: "check-circle", color: "#10B981", label: "Completed" },
  [VideoDateStatus.CANCELLED]: { icon: "close-circle", color: "#6B7280", label: "Cancelled" },
  [VideoDateStatus.DECLINED]: { icon: "close", color: "#EF4444", label: "Declined" },
  [VideoDateStatus.NO_SHOW]: { icon: "account-off", color: "#EF4444", label: "No Show" },
};

const VideoDateList = ({ navigation }: any) => {
  const { colors } = useTheme();
  const { height, width } = useWindowDimensions();

  const [loading, setLoading] = React.useState(true);
  const [videoDates, setVideoDates] = React.useState<VideoDate[]>([]);

  React.useEffect(() => {
    load();
  }, []);

  async function load() {
    setLoading(true);
    try {
      const response = await Global.Fetch(URL.API_VIDEO_DATE_LIST);
      const payload = Array.isArray(response.data)
        ? response.data
        : (response.data?.dates
          || response.data?.videoDates
          || [
            ...(response.data?.pendingRequests || []),
            ...(response.data?.upcomingDates || []),
            ...(response.data?.pastDates || []),
          ]);
      setVideoDates(payload as VideoDate[]);
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t('error.generic'));
    }
    setLoading(false);
  }

  async function acceptDate(dateId: string | number) {
    try {
      await Global.Fetch(Global.format(URL.API_VIDEO_DATE_ACCEPT, String(dateId)), 'post');
      Global.ShowToast("Date accepted!");
      load();
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t('error.generic'));
    }
  }

  async function declineDate(dateId: string | number) {
    try {
      await Global.Fetch(Global.format(URL.API_VIDEO_DATE_DECLINE, String(dateId)), 'post');
      Global.ShowToast("Date declined");
      load();
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t('error.generic'));
    }
  }

  async function cancelDate(dateId: string | number) {
    try {
      await Global.Fetch(Global.format(URL.API_VIDEO_DATE_CANCEL, String(dateId)), 'post');
      Global.ShowToast("Date cancelled");
      load();
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t('error.generic'));
    }
  }

  function joinCall(videoDate: VideoDate) {
    Global.navigate("VideoDate.Call", false, { videoDateId: videoDate.id });
  }

  function scheduleDate(videoDate: VideoDate) {
    Global.navigate("VideoDate.Schedule", false, { videoDateId: videoDate.id, partnerId: videoDate.partnerId });
  }

  function formatDateTime(dateValue: string | Date): string {
    const date = typeof dateValue === 'string' ? new Date(dateValue) : dateValue;
    return date.toLocaleString(undefined, {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    });
  }

  function isUpcoming(videoDate: VideoDate): boolean {
    if (!videoDate.scheduledAt) return false;
    const scheduled = new Date(videoDate.scheduledAt);
    const now = new Date();
    return scheduled > now && videoDate.status === VideoDateStatus.SCHEDULED;
  }

  function canJoin(videoDate: VideoDate): boolean {
    if (!videoDate.scheduledAt) return false;
    const scheduled = new Date(videoDate.scheduledAt);
    const now = new Date();
    const diffMinutes = (scheduled.getTime() - now.getTime()) / (1000 * 60);
    // Can join 5 minutes before to 30 minutes after scheduled time
    return diffMinutes <= 5 && diffMinutes >= -30 &&
           (videoDate.status === VideoDateStatus.SCHEDULED || videoDate.status === VideoDateStatus.IN_PROGRESS);
  }

  const renderDateItem = ({ item }: { item: VideoDate }) => {
    const statusConfig = STATUS_CONFIG[item.status] || STATUS_CONFIG[VideoDateStatus.PROPOSED];
    const canJoinNow = canJoin(item);

    return (
      <Card style={{ marginBottom: 12 }}>
        <Card.Content>
          {/* Header */}
          <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 12 }}>
            <Avatar.Text
              size={48}
              label={item.partnerName?.substring(0, 2).toUpperCase() || "?"}
            />
            <View style={{ marginLeft: 12, flex: 1 }}>
              <Text style={{ fontSize: 16, fontWeight: '600' }}>{item.partnerName}</Text>
              <Chip
                icon={statusConfig.icon}
                textStyle={{ fontSize: 12 }}
                style={{ alignSelf: 'flex-start', marginTop: 4, backgroundColor: statusConfig.color + '20' }}
              >
                {statusConfig.label}
              </Chip>
            </View>
          </View>

          {/* Scheduled Time */}
          {item.scheduledAt && (
            <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 12, backgroundColor: colors.surfaceVariant, padding: 12, borderRadius: 8 }}>
              <MaterialCommunityIcons name="calendar-clock" size={24} color={colors.primary} />
              <View style={{ marginLeft: 12 }}>
                <Text style={{ fontWeight: '500' }}>{formatDateTime(item.scheduledAt)}</Text>
                <Text style={{ fontSize: 12, color: colors.onSurfaceVariant }}>
                  Duration: {item.durationMinutes || 30} minutes
                </Text>
              </View>
            </View>
          )}

          {/* Actions based on status */}
          <View style={{ flexDirection: 'row', gap: 8 }}>
            {item.status === VideoDateStatus.PROPOSED && !item.isInitiator && (
              <>
                <Button mode="contained" onPress={() => acceptDate(item.id)} style={{ flex: 1 }}>
                  Accept
                </Button>
                <Button mode="outlined" onPress={() => declineDate(item.id)} style={{ flex: 1 }}>
                  Decline
                </Button>
              </>
            )}

            {item.status === VideoDateStatus.PROPOSED && item.isInitiator && (
              <Button mode="outlined" onPress={() => cancelDate(item.id)} style={{ flex: 1 }}>
                Cancel Request
              </Button>
            )}

            {item.status === VideoDateStatus.ACCEPTED && (
              <Button mode="contained" onPress={() => scheduleDate(item)} style={{ flex: 1 }} icon="calendar">
                Schedule Time
              </Button>
            )}

            {canJoinNow && (
              <Button mode="contained" onPress={() => joinCall(item)} style={{ flex: 1 }} icon="video">
                Join Call
              </Button>
            )}

            {item.status === VideoDateStatus.SCHEDULED && !canJoinNow && (
              <>
                <Button mode="outlined" onPress={() => scheduleDate(item)} style={{ flex: 1 }}>
                  Reschedule
                </Button>
                <Button mode="text" onPress={() => cancelDate(item.id)} textColor={colors.error}>
                  Cancel
                </Button>
              </>
            )}

            {item.status === VideoDateStatus.COMPLETED && !item.feedbackGiven && (
              <Button
                mode="contained"
                onPress={() => Global.navigate("VideoDate.Feedback", false, { videoDateId: item.id })}
                style={{ flex: 1 }}
                icon="star"
              >
                Give Feedback
              </Button>
            )}
          </View>
        </Card.Content>
      </Card>
    );
  };

  // Separate upcoming and past dates
  const upcomingDates = videoDates.filter(d =>
    d.status === VideoDateStatus.PROPOSED ||
    d.status === VideoDateStatus.ACCEPTED ||
    d.status === VideoDateStatus.SCHEDULED ||
    d.status === VideoDateStatus.IN_PROGRESS
  );
  const pastDates = videoDates.filter(d =>
    d.status === VideoDateStatus.COMPLETED ||
    d.status === VideoDateStatus.CANCELLED ||
    d.status === VideoDateStatus.DECLINED ||
    d.status === VideoDateStatus.NO_SHOW
  );

  if (loading && videoDates.length === 0) {
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
          <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 24 }}>
            <Pressable onPress={() => navigation.goBack()} style={{ marginRight: 12 }}>
              <MaterialCommunityIcons name="arrow-left" size={24} color={colors.onSurface} />
            </Pressable>
            <Text style={{ fontSize: 24, fontWeight: '600' }}>
              Video Dates
            </Text>
          </View>

          {videoDates.length === 0 ? (
            <Card style={{ marginBottom: 20 }}>
              <Card.Content style={{ alignItems: 'center', paddingVertical: 32 }}>
                <MaterialCommunityIcons name="video-off" size={64} color={colors.onSurfaceVariant} />
                <Text style={{ fontSize: 18, marginTop: 16 }}>No Video Dates Yet</Text>
                <Text style={{ color: colors.onSurfaceVariant, marginTop: 8, textAlign: 'center' }}>
                  Propose a video date from a match's profile to get started!
                </Text>
              </Card.Content>
            </Card>
          ) : (
            <>
              {/* Upcoming Dates */}
              {upcomingDates.length > 0 && (
                <>
                  <Text style={{ fontSize: 18, fontWeight: '600', marginBottom: 12 }}>
                    Upcoming
                  </Text>
                  {upcomingDates.map(date => (
                    <View key={date.id}>{renderDateItem({ item: date })}</View>
                  ))}
                </>
              )}

              {/* Past Dates */}
              {pastDates.length > 0 && (
                <>
                  <Text style={{ fontSize: 18, fontWeight: '600', marginTop: 16, marginBottom: 12 }}>
                    Past
                  </Text>
                  {pastDates.map(date => (
                    <View key={date.id}>{renderDateItem({ item: date })}</View>
                  ))}
                </>
              )}
            </>
          )}

          <View style={{ height: 100 }} />
        </View>
      </VerticalView>
    </View>
  );
};

export default VideoDateList;
