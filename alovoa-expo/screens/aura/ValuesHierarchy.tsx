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
} from "react-native-paper";
import { MaterialCommunityIcons } from "@expo/vector-icons";
import * as Global from "../../Global";
import * as URL from "../../URL";
import * as I18N from "../../i18n";
import { GrowthContextProfile } from "../../myTypes";
import { STATUS_BAR_HEIGHT } from "../../assets/styles";

const i18n = I18N.getI18n();

const VALUE_OPTIONS = [
  "freedom",
  "stability",
  "ambition",
  "presence",
  "novelty",
  "tradition",
  "family",
  "service",
  "creativity",
  "security",
  "autonomy",
  "community",
];

const TRADEOFFS = [
  { left: "freedom", right: "stability" },
  { left: "ambition", right: "presence" },
  { left: "novelty", right: "tradition" },
  { left: "autonomy", right: "community" },
  { left: "family", right: "career" },
];

interface ValueTradeoffChoice {
  left: string;
  right: string;
  choice: string;
}

const ValuesHierarchy = ({ navigation }: any) => {
  const { colors } = useTheme();

  const [loading, setLoading] = React.useState(true);
  const [saving, setSaving] = React.useState(false);
  const [selectedValues, setSelectedValues] = React.useState<string[]>([]);
  const [tradeoffs, setTradeoffs] = React.useState<ValueTradeoffChoice[]>([]);

  React.useEffect(() => {
    load();
  }, []);

  async function load() {
    setLoading(true);
    try {
      const response = await Global.Fetch(URL.API_ASSESSMENT_GROWTH_CONTEXT);
      const data = (response.data || {}) as GrowthContextProfile;

      const initialValues = (data.valuesHierarchy || data.traits?.valuesHierarchy || []) as string[];
      setSelectedValues(initialValues);

      const initialTradeoffs = (data.valueTradeoffs || data.traits?.valueTradeoffs || []) as ValueTradeoffChoice[];
      setTradeoffs(initialTradeoffs);
    } catch (e) {
      console.error(e);
      Global.ShowToast(i18n.t("error.generic"));
    }
    setLoading(false);
  }

  function toggleValue(value: string) {
    if (selectedValues.includes(value)) {
      setSelectedValues(selectedValues.filter((v) => v !== value));
      return;
    }
    if (selectedValues.length >= 5) {
      Global.ShowToast("Select up to 5 values");
      return;
    }
    setSelectedValues([...selectedValues, value]);
  }

  function move(index: number, direction: -1 | 1) {
    const target = index + direction;
    if (target < 0 || target >= selectedValues.length) {
      return;
    }
    const copy = [...selectedValues];
    const tmp = copy[index];
    copy[index] = copy[target];
    copy[target] = tmp;
    setSelectedValues(copy);
  }

  function chooseTradeoff(left: string, right: string, choice: string) {
    const next = [...tradeoffs];
    const idx = next.findIndex((item) => item.left === left && item.right === right);
    const payload = { left, right, choice };
    if (idx >= 0) {
      next[idx] = payload;
    } else {
      next.push(payload);
    }
    setTradeoffs(next);
  }

  function selectedChoice(left: string, right: string): string {
    return tradeoffs.find((item) => item.left === left && item.right === right)?.choice || "";
  }

  async function save() {
    setSaving(true);
    try {
      await Global.Fetch(URL.API_ASSESSMENT_GROWTH_CONTEXT, "post", {
        valuesHierarchy: selectedValues,
        valueTradeoffs: tradeoffs,
      });
      Global.ShowToast("Values hierarchy saved");
      navigation.goBack();
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
            <Text style={{ fontSize: 24, fontWeight: "600" }}>Values Hierarchy</Text>
          </View>

          <Card style={{ marginBottom: 16, backgroundColor: colors.primaryContainer }}>
            <Card.Content>
              <Text style={{ color: colors.onPrimaryContainer, fontWeight: "600" }}>
                Pick and rank top 5 values
              </Text>
              <Text style={{ color: colors.onPrimaryContainer, fontSize: 12, marginTop: 4 }}>
                Forced choice and tradeoffs are more reliable than generic self-ratings.
              </Text>
            </Card.Content>
          </Card>

          <Card style={{ marginBottom: 16 }}>
            <Card.Content>
              <Text style={{ fontWeight: "600", marginBottom: 10 }}>Choose up to 5</Text>
              <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 8 }}>
                {VALUE_OPTIONS.map((value) => (
                  <Chip key={value} selected={selectedValues.includes(value)} onPress={() => toggleValue(value)}>
                    {value}
                  </Chip>
                ))}
              </View>
            </Card.Content>
          </Card>

          <Card style={{ marginBottom: 16 }}>
            <Card.Content>
              <Text style={{ fontWeight: "600", marginBottom: 10 }}>Rank order</Text>
              {selectedValues.length === 0 && (
                <Text style={{ color: colors.onSurfaceVariant }}>Select values to rank them.</Text>
              )}
              {selectedValues.map((value, index) => (
                <View key={value} style={{
                  flexDirection: "row",
                  alignItems: "center",
                  justifyContent: "space-between",
                  paddingVertical: 8,
                  borderBottomWidth: index === selectedValues.length - 1 ? 0 : 1,
                  borderBottomColor: colors.surfaceVariant,
                }}>
                  <Text>{index + 1}. {value}</Text>
                  <View style={{ flexDirection: "row" }}>
                    <Button compact onPress={() => move(index, -1)} disabled={index === 0}>Up</Button>
                    <Button compact onPress={() => move(index, 1)} disabled={index === selectedValues.length - 1}>Down</Button>
                  </View>
                </View>
              ))}
            </Card.Content>
          </Card>

          <Card style={{ marginBottom: 16 }}>
            <Card.Content>
              <Text style={{ fontWeight: "600", marginBottom: 10 }}>Value tradeoffs</Text>
              {TRADEOFFS.map((pair) => {
                const choice = selectedChoice(pair.left, pair.right);
                return (
                  <View key={`${pair.left}-${pair.right}`} style={{ marginBottom: 14 }}>
                    <Text style={{ marginBottom: 8 }}>If forced, what matters more now?</Text>
                    <View style={{ flexDirection: "row", gap: 8 }}>
                      <Chip selected={choice === pair.left} onPress={() => chooseTradeoff(pair.left, pair.right, pair.left)}>
                        {pair.left}
                      </Chip>
                      <Chip selected={choice === pair.right} onPress={() => chooseTradeoff(pair.left, pair.right, pair.right)}>
                        {pair.right}
                      </Chip>
                    </View>
                  </View>
                );
              })}
            </Card.Content>
          </Card>

          <Button mode="contained" onPress={save} loading={saving} disabled={saving} icon="content-save">
            Save Values Hierarchy
          </Button>
        </View>
      </ScrollView>
    </View>
  );
};

export default ValuesHierarchy;
