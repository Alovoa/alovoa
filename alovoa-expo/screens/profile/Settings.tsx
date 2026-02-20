import React from "react";
import {
  ActivityIndicator,
  Text,
  TouchableOpacity,
  View,
  Platform,
  useWindowDimensions
} from "react-native";
import * as WebBrowser from "expo-web-browser";
import styles from "../../assets/styles";
import { RootStackParamList, SettingsEmailEnum, SettingsEmailNameMap, UnitsEnum, UnitsNameMap, YourProfileResource } from "../../myTypes";
import * as I18N from "../../i18n";
import * as Global from "../../Global";
import * as URL from "../../URL";
import SelectModal from "../../components/SelectModal";
import VerticalView from "../../components/VerticalView";
import ColorModal from "../../components/ColorModal";
import { BottomTabScreenProps } from "@react-navigation/bottom-tabs";

const i18n = I18N.getI18n();
const GrowthSettingEnum = {
  SHARE_PROFILE: 101,
  BEHAVIOR_SIGNALS: 102,
  MONTHLY_CHECKINS: 103,
} as const;

const SOCIAL_PROVIDERS: Array<{ id: string; label: string }> = [
  { id: "instagram", label: "Instagram" },
  { id: "tiktok", label: "TikTok" },
  { id: "youtube", label: "YouTube" },
  { id: "x", label: "X" },
];

type LinkedSocialAccount = {
  id: string;
  provider: string;
  providerUserId: string;
  providerUsername?: string;
  profileUrl?: string;
  linkedAt?: string;
};

type Props = BottomTabScreenProps<RootStackParamList, 'Profile.Settings'>

const Settings = ({ route }: Props) => {

  const data: YourProfileResource = route.params.data;

  const { height } = useWindowDimensions();
  const [units, setUnits] = React.useState(UnitsEnum.SI);
  const [emailSettings, setEmailSettings] = React.useState<Map<number, boolean>>(new Map());
  const [growthSettings, setGrowthSettings] = React.useState<Map<number, boolean>>(new Map());
  const [socialAccounts, setSocialAccounts] = React.useState<LinkedSocialAccount[]>([]);
  const [connectingProvider, setConnectingProvider] = React.useState<string | null>(null);
  const [socialMessage, setSocialMessage] = React.useState<string | null>(null);

  React.useEffect(() => {
    load();
  }, []);

  async function load() {
    let unitEnum: UnitsEnum = Number(await Global.GetStorage(Global.STORAGE_SETTINGS_UNIT));
    if (unitEnum) {
      setUnits(unitEnum);
    }
    let emailSettings = new Map<number, boolean>();
    if(data.user.userSettings.emailLike) {
      emailSettings.set(SettingsEmailEnum.LIKE, true);
    } else {
      emailSettings.set(SettingsEmailEnum.LIKE, false);
    }
    if(data.user.userSettings.emailChat) {
      emailSettings.set(SettingsEmailEnum.CHAT, true);
    } else {
      emailSettings.set(SettingsEmailEnum.CHAT, false);
    }
    setEmailSettings(emailSettings);

    try {
      const growthResponse = await Global.Fetch(URL.USER_SETTING_GROWTH_PRIVACY);
      const payload = growthResponse?.data || {};
      const growthMap = new Map<number, boolean>();
      growthMap.set(GrowthSettingEnum.SHARE_PROFILE, Boolean(payload.shareGrowthProfile));
      growthMap.set(GrowthSettingEnum.BEHAVIOR_SIGNALS, Boolean(payload.allowBehaviorSignals));
      growthMap.set(GrowthSettingEnum.MONTHLY_CHECKINS, Boolean(payload.monthlyGrowthCheckins));
      setGrowthSettings(growthMap);
    } catch (e) {
      console.error(e);
    }
    await loadSocialAccounts();
  }

  async function updateUnits(num: number) {
    setUnits(num);
    await Global.Fetch(Global.format(URL.USER_UPDATE_UNITS, String(num)), 'post');
    await Global.SetStorage(Global.STORAGE_SETTINGS_UNIT, String(num));
  }

  async function updateEmailSettings(id: number, checked: boolean) {
    emailSettings.set(id, checked);
    setEmailSettings(emailSettings);
    let value = checked ? URL.PATH_BOOLEAN_TRUE : URL.PATH_BOOLEAN_FALSE;
    if (id === SettingsEmailEnum.LIKE) {
      Global.Fetch(Global.format(URL.USER_SETTING_EMAIL_LIKE, value), 'post');
      data.user.userSettings.emailLike = checked;
    } else if (id === SettingsEmailEnum.CHAT) {
      Global.Fetch(Global.format(URL.USER_SETTING_EMAIL_CHAT, value), 'post');
      data.user.userSettings.emailChat = checked;
    }
  }

  async function updateGrowthSettings(id: number, checked: boolean) {
    growthSettings.set(id, checked);
    setGrowthSettings(new Map(growthSettings));
    let value = checked ? URL.PATH_BOOLEAN_TRUE : URL.PATH_BOOLEAN_FALSE;
    if (id === GrowthSettingEnum.SHARE_PROFILE) {
      await Global.Fetch(Global.format(URL.USER_SETTING_SHARE_GROWTH_PROFILE, value), 'post');
    } else if (id === GrowthSettingEnum.BEHAVIOR_SIGNALS) {
      await Global.Fetch(Global.format(URL.USER_SETTING_ALLOW_BEHAVIOR_SIGNALS, value), 'post');
    } else if (id === GrowthSettingEnum.MONTHLY_CHECKINS) {
      await Global.Fetch(Global.format(URL.USER_SETTING_MONTHLY_GROWTH_CHECKINS, value), 'post');
    }
  }

  async function loadSocialAccounts() {
    try {
      const response = await Global.Fetch(URL.USER_SOCIAL_CONNECT_ACCOUNTS);
      setSocialAccounts(Array.isArray(response?.data) ? response.data : []);
    } catch (e) {
      console.error(e);
      setSocialAccounts([]);
    }
  }

  async function connectSocialProvider(provider: string) {
    try {
      setSocialMessage(null);
      setConnectingProvider(provider);
      const response = await Global.Fetch(Global.format(URL.USER_SOCIAL_CONNECT_START, provider), "post");
      const authorizationUrl = response?.data?.authorizationUrl;
      const sessionId = response?.data?.sessionId;
      if (!authorizationUrl || !sessionId) {
        throw new Error("Invalid social connect response");
      }

      if (Platform.OS === "web" && typeof window !== "undefined") {
        window.open(authorizationUrl, "_blank", "noopener,noreferrer");
      } else {
        await WebBrowser.openBrowserAsync(authorizationUrl);
      }

      await pollSocialLinkStatus(String(sessionId), provider);
      await loadSocialAccounts();
      setSocialMessage(`${provider} connected`);
    } catch (e: any) {
      console.error(e);
      setSocialMessage(extractErrorMessage(e, `${provider} connection failed`));
    } finally {
      setConnectingProvider(null);
    }
  }

  async function disconnectSocialProvider(provider: string) {
    try {
      setSocialMessage(null);
      await Global.Fetch(Global.format(URL.USER_SOCIAL_CONNECT_UNLINK, provider), "delete");
      await loadSocialAccounts();
      setSocialMessage(`${provider} disconnected`);
    } catch (e: any) {
      console.error(e);
      setSocialMessage(extractErrorMessage(e, `${provider} disconnect failed`));
    }
  }

  async function pollSocialLinkStatus(sessionId: string, provider: string) {
    const maxAttempts = 45;
    for (let attempt = 0; attempt < maxAttempts; attempt++) {
      await sleep(2000);
      const response = await Global.Fetch(Global.format(URL.USER_SOCIAL_CONNECT_STATUS, sessionId));
      const status = response?.data?.status;
      if (status === "LINKED") {
        return;
      }
      if (status === "FAILED" || status === "EXPIRED") {
        const message = response?.data?.errorMessage || `${provider} connection failed`;
        throw new Error(message);
      }
    }
    throw new Error("Connection timed out");
  }

  function linkedAccount(provider: string): LinkedSocialAccount | undefined {
    return socialAccounts.find((account) => account.provider === provider);
  }

  function extractErrorMessage(error: any, fallback: string): string {
    return error?.response?.data || error?.message || fallback;
  }

  function sleep(ms: number) {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  return (
    <View style={{ height: height, width: '100%' }}>
      <VerticalView onRefresh={load} style={{ padding: 0 }}>
        <View style={[styles.containerProfileItem, { marginTop: 32 }]}>
          <View style={{ marginTop: 12 }}>
            <ColorModal
              title={i18n.t('profile.settings.colors.title')}>
            </ColorModal>
          </View>
          <View style={{ marginTop: 12 }}>
            <SelectModal disabled={false} multi={false} minItems={1} title={i18n.t('profile.units.title')}
              data={[
                [UnitsEnum.SI, UnitsNameMap.get(UnitsEnum.SI)],
                [UnitsEnum.IMPERIAL, UnitsNameMap.get(UnitsEnum.IMPERIAL)],
              ]}
              selected={[units]} onValueChanged={function (id: number, checked: boolean): void {
                if (checked) {
                  updateUnits(id);
                }
              }}></SelectModal>
          </View>
          <View style={{ marginTop: 12 }}>
            <SelectModal disabled={false} multi={true} minItems={0} title={i18n.t('profile.settings.notification')}
              data={[
                [SettingsEmailEnum.LIKE, SettingsEmailNameMap.get(SettingsEmailEnum.LIKE)],
                [SettingsEmailEnum.CHAT, SettingsEmailNameMap.get(SettingsEmailEnum.CHAT)],
              ]}
              selected={[...emailSettings.entries()].filter((item) => item[1]).map((item) => item[0])}
              onValueChanged={updateEmailSettings}>
            </SelectModal>
          </View>
          <View style={{ marginTop: 12 }}>
            <SelectModal disabled={false} multi={true} minItems={0} title={"Growth data controls"}
              data={[
                [GrowthSettingEnum.SHARE_PROFILE, "Use growth profile for matching"],
                [GrowthSettingEnum.BEHAVIOR_SIGNALS, "Use behavior consistency signals"],
                [GrowthSettingEnum.MONTHLY_CHECKINS, "Enable monthly growth check-ins"],
              ]}
              selected={[...growthSettings.entries()].filter((item) => item[1]).map((item) => item[0])}
              onValueChanged={updateGrowthSettings}>
            </SelectModal>
          </View>
          <View style={{ marginTop: 12 }}>
            <View style={{
              borderWidth: 1,
              borderColor: "#e5e7eb",
              borderRadius: 12,
              padding: 12
            }}>
              <Text style={{ fontSize: 16, fontWeight: "600", marginBottom: 4 }}>
                Connected social accounts
              </Text>
              <Text style={{ fontSize: 12, color: "#6b7280", marginBottom: 8 }}>
                A social account can only be linked to one ALOVOA account.
              </Text>
              {SOCIAL_PROVIDERS.map((provider, index) => {
                const account = linkedAccount(provider.id);
                const isConnecting = connectingProvider === provider.id;
                return (
                  <View
                    key={provider.id}
                    style={{
                      flexDirection: "row",
                      alignItems: "center",
                      justifyContent: "space-between",
                      paddingVertical: 10,
                      borderTopWidth: index === 0 ? 0 : 1,
                      borderTopColor: "#f1f5f9"
                    }}
                  >
                    <View style={{ flex: 1, paddingRight: 8 }}>
                      <Text style={{ fontSize: 14, fontWeight: "500" }}>{provider.label}</Text>
                      <Text style={{ fontSize: 12, color: "#6b7280" }}>
                        {account
                          ? `Connected as ${account.providerUsername || account.providerUserId}`
                          : "Not connected"}
                      </Text>
                    </View>
                    {isConnecting ? (
                      <ActivityIndicator size="small" color="#ec407a" />
                    ) : account ? (
                      <TouchableOpacity
                        onPress={() => disconnectSocialProvider(provider.id)}
                        style={{
                          paddingVertical: 8,
                          paddingHorizontal: 12,
                          borderRadius: 8,
                          borderWidth: 1,
                          borderColor: "#ef4444"
                        }}
                      >
                        <Text style={{ color: "#ef4444", fontWeight: "600" }}>Disconnect</Text>
                      </TouchableOpacity>
                    ) : (
                      <TouchableOpacity
                        onPress={() => connectSocialProvider(provider.id)}
                        style={{
                          paddingVertical: 8,
                          paddingHorizontal: 12,
                          borderRadius: 8,
                          backgroundColor: "#ec407a"
                        }}
                      >
                        <Text style={{ color: "#fff", fontWeight: "600" }}>Connect</Text>
                      </TouchableOpacity>
                    )}
                  </View>
                );
              })}
              {socialMessage ? (
                <Text style={{ marginTop: 8, fontSize: 12, color: "#6b7280" }}>{socialMessage}</Text>
              ) : null}
            </View>
          </View>
        </View>
      </VerticalView>
    </View>
  );
};

export default Settings;
