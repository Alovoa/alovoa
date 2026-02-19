import React from "react";
import {
  View,
  ScrollView,
  Pressable,
} from "react-native";
import {
  Text,
  Card,
  Button,
  ActivityIndicator,
  useTheme,
  Chip,
  TextInput,
} from "react-native-paper";
import { MaterialCommunityIcons } from "@expo/vector-icons";
import * as Global from "../../Global";
import * as URL from "../../URL";
import * as I18N from "../../i18n";
import { BridgeJourneySummary, BridgeMilestone } from "../../myTypes";
import { STATUS_BAR_HEIGHT } from "../../assets/styles";

const i18n = I18N.getI18n();

const RELATIONSHIP_STATUSES = [
  "IN_RELATIONSHIP",
  "DATING",
  "TAKING_A_BREAK",
  "ENDED",
];

const BridgeJourney = ({ route, navigation }: any) => {
  const { colors } = useTheme();

  const conversationId = route?.params?.conversationId;

  const [loading, setLoading] = React.useState(true);
  const [submitting, setSubmitting] = React.useState(false);
  const [journey, setJourney] = React.useState<BridgeJourneySummary | null>(null);
  const [milestones, setMilestones] = React.useState<BridgeMilestone[]>([]);
  const [suggestions, setSuggestions] = React.useState<any[]>([]);

  const [responseText, setResponseText] = React.useState("");
  const [relationshipStatus, setRelationshipStatus] = React.useState("IN_RELATIONSHIP");
  const [stillTogether, setStillTogether] = React.useState(true);

  React.useEffect(() => {
    load();
  }, []);

  async function load() {
    setLoading(true);
    try {
      const journeyRes = await Global.Fetch(URL.API_BRIDGE_JOURNEY);
      setJourney(journeyRes.data as BridgeJourneySummary);

      if (conversationId) {
        const [milestonesRes, suggestionsRes] = await Promise.all([
          Global.Fetch(Global.format(URL.API_BRIDGE_MILESTONES, String(conversationId))),
          Global.Fetch(Global.format(URL.API_BRIDGE_SUGGESTIONS, String(conversationId))),
        ]);
        setMilestones(Array.isArray(milestonesRes.data) ? milestonesRes.data : []);
        setSuggestions(Array.isArray(suggestionsRes.data) ? suggestionsRes.data : []);
      } else {
        setMilestones([]);
        setSuggestions([]);
      }
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t("error.generic"));
    }
    setLoading(false);
  }

  async function submitMilestoneResponse(milestoneUuid: string) {
    setSubmitting(true);
    try {
      await Global.Fetch(Global.format(URL.API_BRIDGE_MILESTONE_RESPOND, milestoneUuid), "post", {
        response: responseText,
        relationshipStatus,
        stillTogether,
      });
      Global.ShowToast("Check-in submitted");
      setResponseText("");
      await load();
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t("error.generic"));
    }
    setSubmitting(false);
  }

  async function generateSuggestions() {
    if (!conversationId) {
      return;
    }
    setSubmitting(true);
    try {
      const response = await Global.Fetch(Global.format(URL.API_BRIDGE_SUGGESTIONS_GENERATE, String(conversationId)), "post");
      setSuggestions(Array.isArray(response.data) ? response.data : []);
      Global.ShowToast("New suggestions generated");
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t("error.generic"));
    }
    setSubmitting(false);
  }

  async function updateSuggestion(id: number, action: "accept" | "dismiss") {
    setSubmitting(true);
    try {
      const endpoint = action === "accept" ? URL.API_BRIDGE_SUGGESTION_ACCEPT : URL.API_BRIDGE_SUGGESTION_DISMISS;
      await Global.Fetch(Global.format(endpoint, String(id)), "post");
      await load();
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t("error.generic"));
    }
    setSubmitting(false);
  }

  if (loading) {
    return (
      <View style={{ flex: 1, justifyContent: "center", alignItems: "center", backgroundColor: colors.background }}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  const latestMilestone = journey?.latestMilestone;

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      <ScrollView style={{ flex: 1 }}>
        <View style={{ paddingTop: STATUS_BAR_HEIGHT + 16, paddingHorizontal: 16, paddingBottom: 24 }}>
          <View style={{ flexDirection: "row", alignItems: "center", marginBottom: 20 }}>
            <Pressable onPress={() => navigation.goBack()} style={{ marginRight: 12 }}>
              <MaterialCommunityIcons name="arrow-left" size={24} color={colors.onSurface} />
            </Pressable>
            <Text style={{ fontSize: 24, fontWeight: "600" }}>Relationship Journey</Text>
          </View>

          <Card style={{ marginBottom: 16, backgroundColor: colors.primaryContainer }}>
            <Card.Content>
              <Text style={{ color: colors.onPrimaryContainer, fontWeight: "600" }}>Bridge to Real World</Text>
              <Text style={{ color: colors.onPrimaryContainer, fontSize: 12, marginTop: 4 }}>
                Track milestones, run check-ins, and keep relationship progress intentional.
              </Text>
            </Card.Content>
          </Card>

          <Card style={{ marginBottom: 16 }}>
            <Card.Content>
              <Text style={{ fontWeight: "600", marginBottom: 10 }}>Journey Summary</Text>
              <Text>Total milestones: {journey?.totalMilestones ?? 0}</Text>
              <Text>Active date suggestions: {journey?.activeSuggestions ?? 0}</Text>
              <Text>Accepted suggestions: {journey?.acceptedSuggestions ?? 0}</Text>

              {latestMilestone && (
                <View style={{ marginTop: 12 }}>
                  <Text style={{ fontWeight: "600" }}>Latest milestone</Text>
                  <Text>Type: {latestMilestone.type}</Text>
                  <Text>Date: {latestMilestone.date}</Text>
                  {latestMilestone.relationshipStatus && <Text>Status: {latestMilestone.relationshipStatus}</Text>}
                </View>
              )}
            </Card.Content>
          </Card>

          {latestMilestone?.checkInSent && (
            <Card style={{ marginBottom: 16 }}>
              <Card.Content>
                <Text style={{ fontWeight: "600", marginBottom: 10 }}>Respond to Check-in</Text>

                <View style={{ flexDirection: "row", gap: 8, marginBottom: 10 }}>
                  <Chip selected={stillTogether} onPress={() => setStillTogether(true)}>Still together</Chip>
                  <Chip selected={!stillTogether} onPress={() => setStillTogether(false)}>Not together</Chip>
                </View>

                <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 8, marginBottom: 10 }}>
                  {RELATIONSHIP_STATUSES.map((status) => (
                    <Chip
                      key={status}
                      selected={relationshipStatus === status}
                      onPress={() => setRelationshipStatus(status)}
                    >
                      {status}
                    </Chip>
                  ))}
                </View>

                <TextInput
                  mode="outlined"
                  label="How is the relationship going?"
                  value={responseText}
                  onChangeText={setResponseText}
                  multiline
                />

                <Button
                  mode="contained"
                  onPress={() => submitMilestoneResponse(latestMilestone.uuid)}
                  loading={submitting}
                  disabled={submitting}
                  style={{ marginTop: 12 }}
                >
                  Submit Check-in
                </Button>
              </Card.Content>
            </Card>
          )}

          {conversationId ? (
            <>
              <Card style={{ marginBottom: 16 }}>
                <Card.Content>
                  <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center", marginBottom: 10 }}>
                    <Text style={{ fontWeight: "600" }}>Date Suggestions</Text>
                    <Button mode="text" onPress={generateSuggestions} loading={submitting} disabled={submitting}>Refresh</Button>
                  </View>

                  {suggestions.length === 0 && (
                    <Text style={{ color: colors.onSurfaceVariant }}>No suggestions yet for this conversation.</Text>
                  )}

                  {suggestions.map((suggestion) => (
                    <Card key={suggestion.id} style={{ marginBottom: 10 }}>
                      <Card.Content>
                        <Text style={{ fontWeight: "600" }}>{suggestion.venueName || suggestion.venueCategory}</Text>
                        <Text style={{ color: colors.onSurfaceVariant, marginTop: 4 }}>
                          {suggestion.reason || suggestion.venueDescription || "Suggested based on your shared profile."}
                        </Text>
                        <View style={{ flexDirection: "row", gap: 8, marginTop: 10 }}>
                          <Button mode="contained-tonal" onPress={() => updateSuggestion(suggestion.id, "accept")}>Accept</Button>
                          <Button mode="text" onPress={() => updateSuggestion(suggestion.id, "dismiss")}>Dismiss</Button>
                        </View>
                      </Card.Content>
                    </Card>
                  ))}
                </Card.Content>
              </Card>

              <Card style={{ marginBottom: 16 }}>
                <Card.Content>
                  <Text style={{ fontWeight: "600", marginBottom: 10 }}>Milestones</Text>
                  {milestones.length === 0 && <Text style={{ color: colors.onSurfaceVariant }}>No milestones yet.</Text>}
                  {milestones.map((milestone) => (
                    <View key={milestone.uuid} style={{ marginBottom: 10 }}>
                      <Text>{milestone.type} · {milestone.date}</Text>
                      <Text style={{ color: colors.onSurfaceVariant }}>
                        {milestone.relationshipStatus || (milestone.stillTogether ? "Still together" : "No status yet")}
                      </Text>
                    </View>
                  ))}
                </Card.Content>
              </Card>
            </>
          ) : (
            <Card>
              <Card.Content>
                <Text style={{ color: colors.onSurfaceVariant }}>
                  Open this screen from a specific chat to see date suggestions and milestone history for that conversation.
                </Text>
              </Card.Content>
            </Card>
          )}
        </View>
      </ScrollView>
    </View>
  );
};

export default BridgeJourney;
