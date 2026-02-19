import React from "react";
import {
  View,
  useWindowDimensions
} from "react-native";
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

type Props = BottomTabScreenProps<RootStackParamList, 'Profile.Settings'>

const Settings = ({ route }: Props) => {

  const data: YourProfileResource = route.params.data;

  const { height } = useWindowDimensions();
  const [units, setUnits] = React.useState(UnitsEnum.SI);
  const [emailSettings, setEmailSettings] = React.useState<Map<number, boolean>>(new Map());
  const [growthSettings, setGrowthSettings] = React.useState<Map<number, boolean>>(new Map());

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
        </View>
      </VerticalView>
    </View>
  );
};

export default Settings;
