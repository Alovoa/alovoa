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
import Slider from "@react-native-community/slider";
import { MaterialCommunityIcons } from "@expo/vector-icons";
import * as Global from "../../Global";
import * as URL from "../../URL";
import * as I18N from "../../i18n";
import { GrowthContextProfile } from "../../myTypes";
import { STATUS_BAR_HEIGHT } from "../../assets/styles";

const i18n = I18N.getI18n();

const CHAPTERS = ["rebuilding", "expanding", "healing", "mastering", "serving", "creating"];
const ARCHETYPES = ["Builder", "Seeker", "Healer", "Servant", "Artist"];
const INTENTION_TYPES = ["Long-term partnership", "Family building", "Exploration", "Monogamy", "Non-monogamy"];
const INTENTION_PRIORITIES = ["companionship", "family", "adventure", "stability", "spiritual growth", "power-couple"];
const BOUNDARY_OPTIONS = [
  "Respects 'no' immediately",
  "Privacy by default",
  "Financial transparency",
  "Independent friendships",
  "Clear affection pacing",
  "Direct consent conversations",
];
const SHADOW_OPTIONS = [
  "withdraw",
  "control",
  "appease",
  "numb",
  "explode",
  "overwork",
  "chase novelty",
];

const GrowthContext = ({ navigation }: any) => {
  const { colors } = useTheme();

  const [loading, setLoading] = React.useState(true);
  const [saving, setSaving] = React.useState(false);

  const [purposeStatement, setPurposeStatement] = React.useState("");
  const [currentChapter, setCurrentChapter] = React.useState("");
  const [primaryArchetype, setPrimaryArchetype] = React.useState("");
  const [secondaryArchetype, setSecondaryArchetype] = React.useState("");

  const [relationshipType, setRelationshipType] = React.useState("");
  const [priorities, setPriorities] = React.useState<string[]>([]);
  const [minimumViableRelationship, setMinimumViableRelationship] = React.useState("");

  const [messagesPerDay, setMessagesPerDay] = React.useState(4);
  const [datesPerWeek, setDatesPerWeek] = React.useState(2);
  const [aloneTimePerWeek, setAloneTimePerWeek] = React.useState(10);
  const [emotionalAvailability, setEmotionalAvailability] = React.useState(70);
  const [timeAvailability, setTimeAvailability] = React.useState(70);
  const [stressLevel, setStressLevel] = React.useState(40);
  const [capacityLoad, setCapacityLoad] = React.useState(45);
  const [focusAreasText, setFocusAreasText] = React.useState("");

  const [boundaries, setBoundaries] = React.useState<string[]>([]);
  const [shadowPatterns, setShadowPatterns] = React.useState<string[]>([]);

  React.useEffect(() => {
    load();
  }, []);

  async function load() {
    setLoading(true);
    try {
      const response = await Global.Fetch(URL.API_ASSESSMENT_GROWTH_CONTEXT);
      const data = (response.data || {}) as GrowthContextProfile;

      setPurposeStatement(data.purposeStatement || data.traits?.purposeStatement || "");
      setCurrentChapter(data.currentChapter || data.state?.currentChapter || "");
      setPrimaryArchetype(data.growthArchetypes?.primary || data.traits?.growthArchetypes?.primary || "");
      setSecondaryArchetype(data.growthArchetypes?.secondary || data.traits?.growthArchetypes?.secondary || "");

      setRelationshipType(data.relationshipIntentions?.relationshipType || data.traits?.relationshipIntentions?.relationshipType || "");
      setPriorities(data.relationshipIntentions?.priorities || data.traits?.relationshipIntentions?.priorities || []);
      setMinimumViableRelationship(data.relationshipIntentions?.minimumViableRelationship
        || data.traits?.relationshipIntentions?.minimumViableRelationship
        || "");

      setMessagesPerDay(Number(data.pacePreferences?.messagesPerDay || data.state?.pacePreferences?.messagesPerDay || 4));
      setDatesPerWeek(Number(data.pacePreferences?.datesPerWeek || data.state?.pacePreferences?.datesPerWeek || 2));
      setAloneTimePerWeek(Number(data.pacePreferences?.aloneTimePerWeek || data.state?.pacePreferences?.aloneTimePerWeek || 10));
      setEmotionalAvailability(Number(data.pacePreferences?.emotionalAvailability || data.state?.pacePreferences?.emotionalAvailability || 70));
      setTimeAvailability(Number(data.pacePreferences?.timeAvailability || data.state?.pacePreferences?.timeAvailability || 70));

      setStressLevel(Number(data.stateContext?.stressLevel || data.state?.context?.stressLevel || 40));
      setCapacityLoad(Number(data.stateContext?.capacityLoad || data.state?.context?.capacityLoad || 45));
      const focusAreas = data.stateContext?.focusAreas || data.state?.context?.focusAreas || [];
      setFocusAreasText(Array.isArray(focusAreas) ? focusAreas.join(", ") : "");

      setBoundaries(data.boundaries || data.traits?.boundaries || []);
      setShadowPatterns(data.shadowPatterns || data.traits?.shadowPatterns || []);
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t("error.generic"));
    }
    setLoading(false);
  }

  function toggle(values: string[], setValues: (value: string[]) => void, item: string) {
    if (values.includes(item)) {
      setValues(values.filter((v) => v !== item));
    } else {
      setValues([...values, item]);
    }
  }

  async function save() {
    setSaving(true);
    try {
      const focusAreas = focusAreasText
        .split(",")
        .map((item) => item.trim())
        .filter((item) => item.length > 0);

      const payload: GrowthContextProfile = {
        traits: {
          purposeStatement,
          valuesHierarchy: [],
          valueTradeoffs: [],
          growthArchetypes: {
            primary: primaryArchetype,
            secondary: secondaryArchetype,
          },
          relationshipIntentions: {
            relationshipType,
            priorities,
            minimumViableRelationship,
          },
          boundaries,
          shadowPatterns,
        },
        state: {
          currentChapter,
          pacePreferences: {
            messagesPerDay,
            datesPerWeek,
            aloneTimePerWeek,
            emotionalAvailability,
            timeAvailability,
          },
          context: {
            stressLevel,
            capacityLoad,
            focusAreas,
          },
        },
      };

      await Global.Fetch(URL.API_ASSESSMENT_GROWTH_CONTEXT, "post", payload);
      Global.ShowToast("Growth context saved");
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t("error.generic"));
    }
    setSaving(false);
  }

  if (loading) {
    return (
      <View style={{ flex: 1, justifyContent: "center", alignItems: "center", backgroundColor: colors.background }}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      <ScrollView style={{ flex: 1 }}>
        <View style={{ paddingTop: STATUS_BAR_HEIGHT + 16, paddingHorizontal: 16, paddingBottom: 24 }}>
          <View style={{ flexDirection: "row", alignItems: "center", marginBottom: 20 }}>
            <Pressable onPress={() => navigation.goBack()} style={{ marginRight: 12 }}>
              <MaterialCommunityIcons name="arrow-left" size={24} color={colors.onSurface} />
            </Pressable>
            <Text style={{ fontSize: 24, fontWeight: "600" }}>Growth Context</Text>
          </View>

          <Card style={{ marginBottom: 16, backgroundColor: colors.primaryContainer }}>
            <Card.Content>
              <Text style={{ color: colors.onPrimaryContainer, fontWeight: "600" }}>
                Traits vs State
              </Text>
              <Text style={{ color: colors.onPrimaryContainer, fontSize: 12, marginTop: 4 }}>
                Capture stable traits and your current chapter so matching can account for pace and capacity.
              </Text>
            </Card.Content>
          </Card>

          <Card style={{ marginBottom: 16 }}>
            <Card.Content>
              <Text style={{ fontWeight: "600", marginBottom: 12 }}>Purpose + Chapter</Text>
              <TextInput
                mode="outlined"
                label="I'm becoming the kind of person who..."
                value={purposeStatement}
                onChangeText={setPurposeStatement}
                multiline
              />

              <Text style={{ color: colors.onSurfaceVariant, marginTop: 14, marginBottom: 8 }}>Current chapter</Text>
              <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 8 }}>
                {CHAPTERS.map((chapter) => (
                  <Chip
                    key={chapter}
                    selected={currentChapter === chapter}
                    onPress={() => setCurrentChapter(chapter)}
                  >
                    {chapter}
                  </Chip>
                ))}
              </View>
            </Card.Content>
          </Card>

          <Card style={{ marginBottom: 16 }}>
            <Card.Content>
              <Text style={{ fontWeight: "600", marginBottom: 12 }}>Growth Orientation</Text>
              <Text style={{ color: colors.onSurfaceVariant, marginBottom: 8 }}>Primary</Text>
              <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 8, marginBottom: 12 }}>
                {ARCHETYPES.map((item) => (
                  <Chip key={`primary-${item}`} selected={primaryArchetype === item} onPress={() => setPrimaryArchetype(item)}>{item}</Chip>
                ))}
              </View>
              <Text style={{ color: colors.onSurfaceVariant, marginBottom: 8 }}>Secondary</Text>
              <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 8 }}>
                {ARCHETYPES.map((item) => (
                  <Chip key={`secondary-${item}`} selected={secondaryArchetype === item} onPress={() => setSecondaryArchetype(item)}>{item}</Chip>
                ))}
              </View>
            </Card.Content>
          </Card>

          <Card style={{ marginBottom: 16 }}>
            <Card.Content>
              <Text style={{ fontWeight: "600", marginBottom: 12 }}>Relationship Intention</Text>
              <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 8, marginBottom: 12 }}>
                {INTENTION_TYPES.map((item) => (
                  <Chip key={item} selected={relationshipType === item} onPress={() => setRelationshipType(item)}>{item}</Chip>
                ))}
              </View>

              <Text style={{ color: colors.onSurfaceVariant, marginBottom: 8 }}>Top priorities</Text>
              <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 8, marginBottom: 12 }}>
                {INTENTION_PRIORITIES.map((item) => (
                  <Chip
                    key={item}
                    selected={priorities.includes(item)}
                    onPress={() => toggle(priorities, setPriorities, item)}
                  >
                    {item}
                  </Chip>
                ))}
              </View>

              <TextInput
                mode="outlined"
                label="Minimum viable relationship"
                value={minimumViableRelationship}
                onChangeText={setMinimumViableRelationship}
                multiline
              />
            </Card.Content>
          </Card>

          <Card style={{ marginBottom: 16 }}>
            <Card.Content>
              <Text style={{ fontWeight: "600", marginBottom: 12 }}>Pace + Capacity</Text>

              <Text style={{ marginTop: 6 }}>Messages / day: {Math.round(messagesPerDay)}</Text>
              <Slider value={messagesPerDay} onValueChange={setMessagesPerDay} minimumValue={0} maximumValue={20} step={1} />

              <Text style={{ marginTop: 6 }}>Dates / week: {Math.round(datesPerWeek)}</Text>
              <Slider value={datesPerWeek} onValueChange={setDatesPerWeek} minimumValue={0} maximumValue={7} step={1} />

              <Text style={{ marginTop: 6 }}>Alone time / week (hours): {Math.round(aloneTimePerWeek)}</Text>
              <Slider value={aloneTimePerWeek} onValueChange={setAloneTimePerWeek} minimumValue={0} maximumValue={40} step={1} />

              <Text style={{ marginTop: 6 }}>Emotional availability: {Math.round(emotionalAvailability)}%</Text>
              <Slider value={emotionalAvailability} onValueChange={setEmotionalAvailability} minimumValue={0} maximumValue={100} step={1} />

              <Text style={{ marginTop: 6 }}>Time availability: {Math.round(timeAvailability)}%</Text>
              <Slider value={timeAvailability} onValueChange={setTimeAvailability} minimumValue={0} maximumValue={100} step={1} />

              <Text style={{ marginTop: 6 }}>Stress level: {Math.round(stressLevel)}%</Text>
              <Slider value={stressLevel} onValueChange={setStressLevel} minimumValue={0} maximumValue={100} step={1} />

              <Text style={{ marginTop: 6 }}>Current load: {Math.round(capacityLoad)}%</Text>
              <Slider value={capacityLoad} onValueChange={setCapacityLoad} minimumValue={0} maximumValue={100} step={1} />

              <TextInput
                mode="outlined"
                label="Current focus areas (comma separated)"
                value={focusAreasText}
                onChangeText={setFocusAreasText}
                style={{ marginTop: 12 }}
              />
            </Card.Content>
          </Card>

          <Card style={{ marginBottom: 16 }}>
            <Card.Content>
              <Text style={{ fontWeight: "600", marginBottom: 10 }}>Boundaries + Consent Culture</Text>
              <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 8 }}>
                {BOUNDARY_OPTIONS.map((item) => (
                  <Chip
                    key={item}
                    selected={boundaries.includes(item)}
                    onPress={() => toggle(boundaries, setBoundaries, item)}
                  >
                    {item}
                  </Chip>
                ))}
              </View>
            </Card.Content>
          </Card>

          <Card style={{ marginBottom: 16 }}>
            <Card.Content>
              <Text style={{ fontWeight: "600", marginBottom: 10 }}>Shadow Patterns</Text>
              <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 8 }}>
                {SHADOW_OPTIONS.map((item) => (
                  <Chip
                    key={item}
                    selected={shadowPatterns.includes(item)}
                    onPress={() => toggle(shadowPatterns, setShadowPatterns, item)}
                  >
                    {item}
                  </Chip>
                ))}
              </View>
            </Card.Content>
          </Card>

          <Button
            mode="outlined"
            onPress={() => Global.navigate("Values.Hierarchy", false, {})}
            style={{ marginBottom: 12 }}
            icon="sort"
          >
            Edit Values Hierarchy + Tradeoffs
          </Button>

          <Button
            mode="contained"
            onPress={save}
            loading={saving}
            disabled={saving}
            icon="content-save"
          >
            Save Growth Context
          </Button>
        </View>
      </ScrollView>
    </View>
  );
};

export default GrowthContext;
