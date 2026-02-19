import React from "react";
import {
  View,
  Platform,
  useWindowDimensions,
  Image,
  Linking,
  Pressable
} from "react-native";
import { Text, Button, Card, ActivityIndicator, IconButton, Badge } from "react-native-paper";
import styles, { STATUS_BAR_HEIGHT } from "../assets/styles";
import { YourProfileResource, UserDto, RootStackParamList } from "../myTypes";
import * as I18N from "../i18n";
import * as Global from "../Global";
import * as URL from "../URL";
import * as WebBrowser from 'expo-web-browser';
import * as FileSystem from 'expo-file-system/legacy'
import * as Sharing from 'expo-sharing';
import VerticalView from "../components/VerticalView";
import * as Clipboard from 'expo-clipboard';
import Alert from "../components/Alert";
import { BottomTabScreenProps } from "@react-navigation/bottom-tabs";

const userdataFileName = "userdata-alovoa.json"
const MIME_JSON = "application/json";

const i18n = I18N.getI18n()

type Props = BottomTabScreenProps<RootStackParamList, 'YourProfile'>
const YourProfile = ({ route, navigation }: Props) => {

  const { height, width } = useWindowDimensions();
  const MAX_REFERRALS = 10;

  const [requestingDeletion, setRequestingDeletion] = React.useState(false);
  const [alertVisible, setAlertVisible] = React.useState(false);
  const [data, setData] = React.useState<YourProfileResource>();
  const [user, setUser] = React.useState<UserDto>();
  const [profilePic, setProfilePic] = React.useState<string>();
  const [name, setName] = React.useState("");
  const [age, setAge] = React.useState(0);
  const [numReferred, setNumReferred] = React.useState(MAX_REFERRALS);
  const [uuid, setUuid] = React.useState("");
  const [loading, setLoading] = React.useState(false);
  const [incompleteProfile, setIncompleteProfile] = React.useState(false);
  const [imcompletePhotos, setImcompletePhotos] = React.useState(false);

  const alertButtons = [
    {
      text: i18n.t('ok'),
      onPress: async () => {
        setAlertVisible(false);
      }
    }
  ]

  React.useEffect(() => {
    load();
  }, []);

  React.useEffect(() => {
    if (route.params?.changed) {
      navigation.setParams({ changed: false });
      load();
    }
  }, [route.params?.changed]);

  async function load() {
    setLoading(true);
    setTimeout(() => setLoading(false), 5000);
    let response = await Global.Fetch(URL.API_RESOURCE_YOUR_PROFILE);
    let data: YourProfileResource = response.data;
    setData(data);
    setUser(data.user)
    setUuid(data.user.uuid);
    setProfilePic(data.user.profilePicture);
    setName(data.user.firstName);
    setAge(data.user.age);
    setNumReferred(data.user.numberReferred);
    setLoading(false);

    if(data.user.interests.length === 0 || data.user.prompts.length == 0) {
      setIncompleteProfile(true);
    }
    if(data.user.images.length == 0) {
      setImcompletePhotos(true);
    }
  }

  async function copyReferralCodeToClipboard() {
    await Clipboard.setStringAsync(uuid);
    Global.ShowToast(i18n.t('referral.copy'));
  };

  async function logout() {
    Global.Fetch(URL.AUTH_LOGOUT);
    Global.SetStorage(Global.STORAGE_PAGE, Global.INDEX_LOGIN);
    Global.navigate("Login");
  }

  async function downloadUserData() {
    if (Platform.OS === 'android') {
      const response = await Global.Fetch(Global.format(URL.USER_USERDATA, uuid));
      const userData = JSON.stringify(response.data);
      const permissions = await FileSystem.StorageAccessFramework.requestDirectoryPermissionsAsync();
      if (permissions.granted) {
        const uri = permissions.directoryUri;
        let newFile = await FileSystem.StorageAccessFramework.createFileAsync(uri, userdataFileName, MIME_JSON);
        await FileSystem.StorageAccessFramework.writeAsStringAsync(newFile, userData);
        Global.ShowToast(i18n.t('profile.download-userdata-success'));
      }
    } else if (Platform.OS === 'ios') {
      const response = await Global.Fetch(Global.format(URL.USER_USERDATA, uuid));
      const userData = JSON.stringify(response.data);
      let fileName = FileSystem.documentDirectory + '/alovoa.json';
      await FileSystem.writeAsStringAsync(fileName, userData, { encoding: FileSystem.EncodingType.UTF8 });
      Global.ShowToast(i18n.t('profile.download-userdata-success'));
      if (await Sharing.isAvailableAsync()) {
        Sharing.shareAsync(fileName);
      }
    } else {
      Linking.openURL(Global.format(URL.USER_USERDATA, uuid));
    }
  }

  async function deleteAccount() {
    if (!requestingDeletion) {
      setRequestingDeletion(true);
      await Global.Fetch(URL.USER_DELETE_ACCOUNT, 'post');
      Global.ShowToast(i18n.t('profile.delete-account-success'));
      setRequestingDeletion(false);
    }
  }

  return (
    <View style={{ flex: 1, height: height }}>
      {loading &&
        <View style={{ height: height, width: width, zIndex: 1, justifyContent: 'center', alignItems: 'center', position: "absolute" }} >
          <ActivityIndicator animating={loading} size="large" />
        </View>
      }

      <VerticalView onRefresh={load} style={{ padding: 0 }}>
        <View style={{ paddingTop: STATUS_BAR_HEIGHT }}></View>
        <View style={{ paddingTop: 32 }}></View>
        <Pressable onPress={() => Global.nagivateProfile(user)}>
          <Image source={{ uri: profilePic }}
            style={{ width: '50%', maxWidth: 500, borderRadius: 500, height: 'auto', aspectRatio: 1, alignSelf: 'center' }}>
          </Image>
        </Pressable>

        <View style={[styles.containerProfileItem, { marginTop: 12, minHeight: height }]}>
          <Text style={[styles.name]}>{name + ", " + age}</Text>
          <View style={{ marginBottom: 48, marginTop: 12 }}>
            <Card mode="contained" style={{ padding: 12 }}>
              <Text style={{ textAlign: 'center' }}>{i18n.t('profile.donated') + ": " + String(user ? user.totalDonations : 0) + ' €'}</Text>
            </Card>
          </View>

          <Badge size={12} visible={imcompletePhotos} style={styles.badge} />
          <Button icon="chevron-right" mode="elevated" contentStyle={{ flexDirection: 'row-reverse', justifyContent: 'space-between' }}
            style={{ alignSelf: 'stretch', marginBottom: 8 }} onPress={() => Global.navigate(Global.SCREEN_PROFILE_PICTURES, false, { user: user })}>{i18n.t('profile.screen.pictures')}</Button>
          <Badge size={12} visible={incompleteProfile} style={styles.badge} />
          <Button icon="chevron-right" mode="elevated" contentStyle={{ flexDirection: 'row-reverse', justifyContent: 'space-between' }}
            style={{ alignSelf: 'stretch', marginBottom: 8 }} onPress={() => Global.navigate(Global.SCREEN_PROFILE_PROFILESETTINGS, false, { data: data })}>{i18n.t('profile.screen.profile')}</Button>
          <Button icon="chevron-right" mode="elevated" contentStyle={{ flexDirection: 'row-reverse', justifyContent: 'space-between' }}
            style={{ alignSelf: 'stretch', marginBottom: 8 }} onPress={() => Global.navigate(Global.SCREEN_PROFILE_SEARCHSETTINGS, false, { data: data })}>{i18n.t('profile.screen.search')}</Button>
          <Button icon="chevron-right" mode="elevated" contentStyle={{ flexDirection: 'row-reverse', justifyContent: 'space-between' }}
            style={{ alignSelf: 'stretch', marginBottom: 8 }} onPress={() => Global.navigate(Global.SCREEN_PROFILE_SETTINGS, false, { data: data })}>{i18n.t('profile.screen.settings')}</Button>

          <Card mode="contained" style={{ padding: 12, marginBottom: 10 }}>
            <Text style={{ fontWeight: '600', marginBottom: 8 }}>AURA Hub</Text>
            <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 8 }}>
              <Button mode="contained-tonal" compact onPress={() => Global.navigate("Intake.Home", false, {})} icon="clipboard-check">
                Intake
              </Button>
              <Button mode="contained-tonal" compact onPress={() => Global.navigate("Matching.Home", false, {})} icon="star-four-points">
                Matches
              </Button>
              <Button mode="contained-tonal" compact onPress={() => Global.navigate("MatchWindow.List", false, {})} icon="window-open">
                Windows
              </Button>
              <Button mode="contained-tonal" compact onPress={() => Global.navigate("VideoDate.List", false, {})} icon="video">
                Video Dates
              </Button>
              <Button mode="contained-tonal" compact onPress={() => Global.navigate("Reputation.Score", false, {})} icon="shield-check">
                Reputation
              </Button>
            </View>
          </Card>

          {numReferred < MAX_REFERRALS && <View style={{ flexDirection: "row", marginBottom: 8 }}>
            <Button icon="content-copy" mode="elevated" contentStyle={{ flexDirection: 'row-reverse', justifyContent: 'space-between', flexGrow: 1 }}
              style={{ flexGrow: 1 }} onPress={copyReferralCodeToClipboard}>
              {i18n.t('referral.title')}
            </Button>
            <IconButton
              icon="help"
              mode="contained"
              size={14}
              style={{ margin: 0, marginLeft: 4, alignSelf: "center" }}
              onPress={() => setAlertVisible(true)}
            />
          </View>
          }
        </View>
        <View style={[styles.containerProfileItem, { marginTop: 32, marginBottom: 48 }]}>
          <View style={{ marginTop: 128, paddingBottom: STATUS_BAR_HEIGHT + 24 }}>
            <Button mode='contained' onPress={() => logout()}>
              <Text>{i18n.t('profile.logout')}</Text>
            </Button>
            <View style={{ marginTop: 24 }}>
              <Text style={[styles.link, { padding: 8 }]} onPress={() => {
                WebBrowser.openBrowserAsync(URL.PRIVACY);
              }}>{i18n.t('privacy-policy')}</Text>
              <Text style={[styles.link, { padding: 8 }]} onPress={() => {
                WebBrowser.openBrowserAsync(URL.TOS);
              }}>{i18n.t('tos')}</Text>
              <Text style={[styles.link, { padding: 8 }]} onPress={() => {
                WebBrowser.openBrowserAsync(URL.IMPRINT);
              }}>{i18n.t('imprint')}</Text>
              <Text style={[styles.link, { padding: 8 }]} onPress={() => {
                Global.navigate(Global.SCREEN_PROFILE_ADVANCED_SETTINGS, false, { user: user })
              }}>{i18n.t('profile.screen.advanced-settings')}</Text>
              <Text style={[styles.link, { padding: 8 }]} onPress={() => {
                downloadUserData();
              }}>{i18n.t('profile.download-userdata')}</Text>
              <Text style={[styles.link, { padding: 8, opacity: requestingDeletion ? 0.3 : 1 }]} onPress={() => {
                deleteAccount();
              }}>{i18n.t('profile.delete-account')}</Text>
            </View>
          </View>

        </View>
      </VerticalView>
      <Alert visible={alertVisible} setVisible={setAlertVisible} message={i18n.t('referral.hint')} buttons={alertButtons} />
    </View>
  );
};

export default YourProfile;
