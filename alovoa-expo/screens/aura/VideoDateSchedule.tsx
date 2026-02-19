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
  SegmentedButtons,
} from "react-native-paper";
import { MaterialCommunityIcons } from "@expo/vector-icons";
import * as Global from "../../Global";
import * as URL from "../../URL";
import * as I18N from "../../i18n";
import { CalendarSlot, VideoDate } from "../../myTypes";
import { STATUS_BAR_HEIGHT } from "../../assets/styles";

const i18n = I18N.getI18n();

const DURATIONS = [
  { value: '15', label: '15 min' },
  { value: '30', label: '30 min' },
  { value: '45', label: '45 min' },
  { value: '60', label: '1 hour' },
];

const VideoDateSchedule = ({ route, navigation }: any) => {
  const { colors } = useTheme();
  const { height, width } = useWindowDimensions();

  const { videoDateId, partnerId } = route.params || {};

  const [loading, setLoading] = React.useState(true);
  const [submitting, setSubmitting] = React.useState(false);
  const [availableSlots, setAvailableSlots] = React.useState<CalendarSlot[]>([]);
  const [selectedSlot, setSelectedSlot] = React.useState<CalendarSlot | null>(null);
  const [duration, setDuration] = React.useState('30');
  const [selectedDate, setSelectedDate] = React.useState<Date>(new Date());

  React.useEffect(() => {
    load();
  }, [selectedDate]);

  async function load() {
    setLoading(true);
    try {
      const dateStr = selectedDate.toISOString().split('T')[0];
      const response = await Global.Fetch(Global.format(URL.API_CALENDAR_SLOTS, partnerId) + `?date=${dateStr}`);
      const payload = Array.isArray(response.data)
        ? response.data
        : (response.data?.slots || []);
      setAvailableSlots((payload as CalendarSlot[]).filter((slot) => slot.available !== false));
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t('error.generic'));
    }
    setLoading(false);
  }

  async function scheduleDate() {
    if (!selectedSlot) return;

    setSubmitting(true);
    try {
      await Global.Fetch(Global.format(URL.API_VIDEO_DATE_SCHEDULE, videoDateId), 'post', {
        scheduledAt: selectedSlot.startTime,
        durationMinutes: parseInt(duration),
      });

      Global.ShowToast("Video date scheduled!");
      navigation.goBack();
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t('error.generic'));
    }
    setSubmitting(false);
  }

  function formatTime(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleTimeString(undefined, {
      hour: 'numeric',
      minute: '2-digit',
    });
  }

  function formatDate(date: Date): string {
    return date.toLocaleDateString(undefined, {
      weekday: 'long',
      month: 'long',
      day: 'numeric',
    });
  }

  function changeDate(days: number) {
    const newDate = new Date(selectedDate);
    newDate.setDate(newDate.getDate() + days);

    // Don't allow past dates
    if (newDate < new Date(new Date().setHours(0, 0, 0, 0))) return;

    // Don't allow more than 14 days ahead
    const maxDate = new Date();
    maxDate.setDate(maxDate.getDate() + 14);
    if (newDate > maxDate) return;

    setSelectedDate(newDate);
    setSelectedSlot(null);
  }

  // Group slots by time of day
  const morningSlots = availableSlots.filter(s => {
    const hour = new Date(s.startTime).getHours();
    return hour >= 6 && hour < 12;
  });
  const afternoonSlots = availableSlots.filter(s => {
    const hour = new Date(s.startTime).getHours();
    return hour >= 12 && hour < 17;
  });
  const eveningSlots = availableSlots.filter(s => {
    const hour = new Date(s.startTime).getHours();
    return hour >= 17 || hour < 6;
  });

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      {/* Header */}
      <View style={{ paddingTop: STATUS_BAR_HEIGHT + 8, paddingHorizontal: 16, paddingBottom: 8 }}>
        <View style={{ flexDirection: 'row', alignItems: 'center' }}>
          <Pressable onPress={() => navigation.goBack()} style={{ marginRight: 12 }}>
            <MaterialCommunityIcons name="arrow-left" size={24} color={colors.onSurface} />
          </Pressable>
          <Text style={{ fontSize: 20, fontWeight: '600' }}>
            Schedule Video Date
          </Text>
        </View>
      </View>

      <ScrollView style={{ flex: 1 }} contentContainerStyle={{ padding: 16 }}>
        {/* Duration Selection */}
        <Text style={{ fontSize: 16, fontWeight: '600', marginBottom: 12 }}>
          Call Duration
        </Text>
        <SegmentedButtons
          value={duration}
          onValueChange={setDuration}
          buttons={DURATIONS}
          style={{ marginBottom: 24 }}
        />

        {/* Date Selection */}
        <Text style={{ fontSize: 16, fontWeight: '600', marginBottom: 12 }}>
          Select Date
        </Text>
        <Surface style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: 16, borderRadius: 12, marginBottom: 24 }} elevation={1}>
          <Pressable onPress={() => changeDate(-1)}>
            <MaterialCommunityIcons name="chevron-left" size={32} color={colors.primary} />
          </Pressable>
          <View style={{ alignItems: 'center' }}>
            <Text style={{ fontSize: 18, fontWeight: '600' }}>{formatDate(selectedDate)}</Text>
            <Text style={{ color: colors.onSurfaceVariant, fontSize: 12 }}>
              {selectedDate.toDateString() === new Date().toDateString() ? 'Today' : ''}
            </Text>
          </View>
          <Pressable onPress={() => changeDate(1)}>
            <MaterialCommunityIcons name="chevron-right" size={32} color={colors.primary} />
          </Pressable>
        </Surface>

        {/* Time Slots */}
        <Text style={{ fontSize: 16, fontWeight: '600', marginBottom: 12 }}>
          Available Times
        </Text>

        {loading ? (
          <View style={{ alignItems: 'center', padding: 32 }}>
            <ActivityIndicator />
          </View>
        ) : availableSlots.length === 0 ? (
          <Card style={{ marginBottom: 20 }}>
            <Card.Content style={{ alignItems: 'center', paddingVertical: 24 }}>
              <MaterialCommunityIcons name="calendar-remove" size={48} color={colors.onSurfaceVariant} />
              <Text style={{ marginTop: 12, color: colors.onSurfaceVariant, textAlign: 'center' }}>
                No available slots on this day. Try another date.
              </Text>
            </Card.Content>
          </Card>
        ) : (
          <>
            {/* Morning */}
            {morningSlots.length > 0 && (
              <TimeSlotSection
                title="Morning"
                icon="weather-sunny"
                slots={morningSlots}
                selectedSlot={selectedSlot}
                onSelect={setSelectedSlot}
              />
            )}

            {/* Afternoon */}
            {afternoonSlots.length > 0 && (
              <TimeSlotSection
                title="Afternoon"
                icon="white-balance-sunny"
                slots={afternoonSlots}
                selectedSlot={selectedSlot}
                onSelect={setSelectedSlot}
              />
            )}

            {/* Evening */}
            {eveningSlots.length > 0 && (
              <TimeSlotSection
                title="Evening"
                icon="weather-night"
                slots={eveningSlots}
                selectedSlot={selectedSlot}
                onSelect={setSelectedSlot}
              />
            )}
          </>
        )}

        <View style={{ height: 100 }} />
      </ScrollView>

      {/* Bottom Action */}
      <View style={{ padding: 16, paddingBottom: 32, backgroundColor: colors.background, borderTopWidth: 1, borderTopColor: colors.surfaceVariant }}>
        {selectedSlot && (
          <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 12 }}>
            <MaterialCommunityIcons name="check-circle" size={24} color="#10B981" />
            <Text style={{ marginLeft: 8, flex: 1 }}>
              {formatDate(selectedDate)} at {formatTime(selectedSlot.startTime)} ({duration} min)
            </Text>
          </View>
        )}
        <Button
          mode="contained"
          onPress={scheduleDate}
          disabled={!selectedSlot || submitting}
          loading={submitting}
        >
          Confirm Schedule
        </Button>
      </View>
    </View>
  );
};

// Helper Component
const TimeSlotSection = ({
  title,
  icon,
  slots,
  selectedSlot,
  onSelect,
}: {
  title: string;
  icon: string;
  slots: CalendarSlot[];
  selectedSlot: CalendarSlot | null;
  onSelect: (slot: CalendarSlot) => void;
}) => {
  const { colors } = useTheme();

  function formatTime(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleTimeString(undefined, {
      hour: 'numeric',
      minute: '2-digit',
    });
  }

  return (
    <View style={{ marginBottom: 20 }}>
      <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 8 }}>
        <MaterialCommunityIcons name={icon as any} size={20} color={colors.onSurfaceVariant} />
        <Text style={{ marginLeft: 8, color: colors.onSurfaceVariant, fontWeight: '500' }}>
          {title}
        </Text>
      </View>
      <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 8 }}>
        {slots.map((slot) => {
          const isSelected = selectedSlot?.startTime === slot.startTime;
          return (
            <Pressable
              key={slot.startTime}
              onPress={() => onSelect(slot)}
              style={{
                paddingHorizontal: 16,
                paddingVertical: 10,
                borderRadius: 8,
                backgroundColor: isSelected ? colors.primary : colors.surfaceVariant,
              }}
            >
              <Text style={{ color: isSelected ? colors.onPrimary : colors.onSurface, fontWeight: '500' }}>
                {formatTime(slot.startTime)}
              </Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
};

export default VideoDateSchedule;
