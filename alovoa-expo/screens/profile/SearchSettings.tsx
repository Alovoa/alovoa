import React from "react";
import {
  View,
  useWindowDimensions
} from "react-native";
import { ActivityIndicator, Checkbox, Divider, Text, useTheme } from "react-native-paper";
import { YourProfileResource, GenderEnum, UserIntention, Gender, IntentionE, SearchParams, IntentionNameMap, GenderNameMap } from "../../myTypes";
import * as I18N from "../../i18n";
import * as Global from "../../Global";
import * as URL from "../../URL";
import SelectModal from "../../components/SelectModal";
import AgeRangeSliderModal from "../../components/AgeRangeSliderModal";
import VerticalView from "../../components/VerticalView";
import { useHeaderHeight } from '@react-navigation/elements';
import Slider from "@react-native-community/slider";
import { GRAY } from "../../assets/styles";

const i18n = I18N.getI18n()
const MIN_AGE = 18;
const MAX_AGE = 100;

type Props = {
  route: {
    params?: {
      data?: YourProfileResource;
    };
  };
}

const SearchSettings = ({ route }: Props) => {

  //var data: YourProfileResource = route.params.data;
  const { colors } = useTheme();
  const { height, width } = useWindowDimensions();
  const headerHeight = useHeaderHeight();

  const [data, setData] = React.useState<YourProfileResource | undefined>(route?.params?.data);
  const [isLegal, setIsLegal] = React.useState(false);
  const [intention, setIntention] = React.useState(IntentionE.MEET);
  const [showIntention, setShowIntention] = React.useState(false);
  const [minAge, setMinAge] = React.useState(MIN_AGE)
  const [maxAge, setMaxAge] = React.useState(MAX_AGE)
  const [preferredGenders, setPreferredGenders] = React.useState(Array<number>);
  const [settingsIgnoreIntention, setSettingsIgnoreIntention] = React.useState(false);
  const [loading, setLoading] = React.useState(false);
  const [changed, setChanged] = React.useState(false);

  const minDistance = 1;
  const [maxDistance] = React.useState(Global.MAX_DISTANCE); // todo: setMaxDistance
  const [distance, setDistance] = React.useState(Global.DEFAULT_DISTANCE);
  const [distanceText, setDistanceText] = React.useState(distance);
  const [distanceUnit] = React.useState("km"); // todo: setDistanceUnit
  const [params, setParams] = React.useState<SearchParams>();
  const [showOutsideParams, setShowOutsideParams] = React.useState(true);

  async function load() {
    setLoading(true);
    let response = await Global.Fetch(URL.API_RESOURCE_YOUR_PROFILE);
    let data: YourProfileResource = response.data;
    setData(data)
    loadUser(data);
  }

  async function loadUser(data: YourProfileResource) {
    setLoading(true);
    setShowIntention(data.showIntention);
    setIsLegal(data.user.age >= MIN_AGE);
    setMinAge(data.user.preferedMinAge);
    setMaxAge(data.user.preferedMaxAge);
    setIntention(data.user.intention.id);
    setPreferredGenders(data.user.preferedGenders.map(item => item.id));
    setSettingsIgnoreIntention(data["settings.ignoreIntention"]);
    let paramsStorage = await Global.GetStorage(Global.STORAGE_ADV_SEARCH_PARAMS);
    setParams(paramsStorage ? JSON.parse(paramsStorage) : {});
    setLoading(false);
  }

  async function onDistanceChanged(value: number) {
    let params: SearchParams = await getStoredParams();
    params.distance = value;
    setParams(params);
  }

  async function toggleShowOutsideParams() {
    let newState = !showOutsideParams;
    setShowOutsideParams(newState);
    let params: SearchParams = await getStoredParams();
    params.showOutsideParameters = newState;
    setParams(params);
  }

  async function getStoredParams(): Promise<SearchParams> {
    let paramsStorage = await Global.GetStorage(Global.STORAGE_ADV_SEARCH_PARAMS);
    let params: SearchParams = paramsStorage ? JSON.parse(paramsStorage) : {};
    return params;
  }

  React.useEffect(() => {
    Global.SetStorage(Global.STORAGE_RELOAD_SEARCH, Global.STORAGE_FALSE);
    if (data) {
      loadUser(data);
    } else {
      load();
    }
  }, []);

  React.useEffect(() => {
    if (changed) {
      Global.SetStorage(Global.STORAGE_RELOAD_SEARCH, Global.STORAGE_TRUE);
      setChanged(false)
    }
  }, [changed]);

  React.useEffect(() => {
    //TODO
    //let isIS = data.user.units == UnitsEnum.SI;
    let saveParam = false;
    if(params?.distance) {
      setDistance(params.distance);
      saveParam = true;
    }
    if (params?.showOutsideParameters !== undefined) {
      setShowOutsideParams(params.showOutsideParameters);
      saveParam = true;
    }
    if(saveParam) {
      saveParams()
    }
  }, [params]);

  async function saveParams() {
    if (params) {
      await Global.SetStorage(Global.STORAGE_ADV_SEARCH_PARAMS, JSON.stringify(params));
      setChanged(true);
    }
  }

  async function updateIntention(num: number) {
    await Global.Fetch(Global.format(URL.USER_UPDATE_INTENTION, String(num)), 'post');
    Global.ShowToast(i18n.t('profile.intention-toast'));
    setIntention(num);
    setShowIntention(false);

    if (!data) {
      return;
    }
    let intention: UserIntention = { id: num, text: "" };
    data.user.intention = intention;
    setChanged(true);
  }

  async function updateGenders(genderId: number, state: boolean) {
    await Global.Fetch(Global.format(URL.USER_UPDATE_PREFERED_GENDER, genderId, state ? "1" : "0"), 'post');
    if (!data) {
      return;
    }
    if (state) {
      let gender: Gender = {
        id: genderId,
        text: ""
      };
      data.user.preferedGenders.push(gender);
    } else {
      data.user.preferedGenders.forEach((item, index) => {
        if (item.id === genderId) data.user.preferedGenders.splice(index, 1);
      });
    }
    setChanged(true);
  }

  async function updateMinAge(num: number) {
    await Global.Fetch(Global.format(URL.USER_UPDATE_MIN_AGE, String(num)), 'post');
    setMinAge(num);
    if (!data) {
      return;
    }
    data.user.preferedMinAge = num;
    setChanged(true);
  }

  async function updateMaxAge(num: number) {
    await Global.Fetch(Global.format(URL.USER_UPDATE_MAX_AGE, String(num)), 'post');
    setMaxAge(num);
    if (!data) {
      return;
    }
    data.user.preferedMaxAge = num;
    setChanged(true);
  }

  return (
    <View style={{ height: height - headerHeight }}>
      {loading &&
        <View style={{ height: height, width: width, zIndex: 1, justifyContent: 'center', alignItems: 'center', position: "absolute" }}>
          <ActivityIndicator animating={loading} size="large" />
        </View>
      }

      <VerticalView onRefresh={load}>

        <View style={{ gap: 12 }}>

          {!settingsIgnoreIntention &&
            <View>
              <SelectModal disabled={!showIntention} multi={false} minItems={1} title={i18n.t('profile.intention.title')}
                data={[
                  [IntentionE.MEET, IntentionNameMap.get(IntentionE.MEET)],
                  [IntentionE.DATE, IntentionNameMap.get(IntentionE.DATE)],
                  [IntentionE.SEX, IntentionNameMap.get(IntentionE.SEX)],
                ]}
                selected={[intention]} onValueChanged={function (id: number, checked: boolean): void {
                  updateIntention(id);
                }}></SelectModal>
            </View>
          }

          <View>
            <SelectModal disabled={false} multi={true} minItems={1} title={i18n.t('profile.gender')}
              data={[
                [GenderEnum.MALE, GenderNameMap.get(GenderEnum.MALE)],
                [GenderEnum.FEMALE, GenderNameMap.get(GenderEnum.FEMALE)],
                [GenderEnum.OTHER, GenderNameMap.get(GenderEnum.OTHER)],
              ]}
              selected={preferredGenders} onValueChanged={function (id: number, checked: boolean): void {
                updateGenders(id, checked);
              }}></SelectModal>
          </View>

          {isLegal &&
            <View>
              <AgeRangeSliderModal title={i18n.t('profile.preferred-age-range')} titleLower={i18n.t('profile.age.min')} titleUpper={i18n.t('profile.age.max')}
                valueLower={minAge} valueUpper={maxAge} onValueLowerChanged={updateMinAge} onValueUpperChanged={updateMaxAge}></AgeRangeSliderModal>
            </View>
          }

          <Divider style={{ marginVertical: 16 }} />

          <View style={{ gap: 4 }}>
            <Text>{i18n.t('profile.search.settings.distance') + ": " + distanceText + " " + distanceUnit}</Text>
            <View style={{ flexDirection: 'row', gap: 4 }}>
              <Slider
                style={{ flex: 1 }}
                value={distance}
                minimumValue={minDistance}
                maximumValue={maxDistance}
                minimumTrackTintColor={colors.secondary}
                maximumTrackTintColor={GRAY}
                thumbTintColor={colors.primary}
                step={1}
                onValueChange={(value: number) => {
                  setDistanceText(value);
                }}
                onSlidingComplete={(value: number) => {
                  onDistanceChanged(value);
                }}
              />
            </View>
          </View>

          <View style={{ flexDirection: "row" }}>
            <Checkbox.Item onPress={toggleShowOutsideParams}
              status={showOutsideParams ? 'checked' : 'unchecked'} label={i18n.t('profile.search.settings.show-outside-parameters')} />
          </View>

        </View>
      </VerticalView>
    </View>
  );
};

export default SearchSettings;
