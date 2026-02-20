import React from "react";
import {
  View,
  TouchableOpacity,
  Image,
  StyleSheet,
  useWindowDimensions,
  FlatList,
} from "react-native";
import styles, { WIDESCREEN_HORIZONTAL_MAX } from "../../assets/styles";
import * as I18N from "../../i18n";
import * as Global from "../../Global";
import * as URL from "../../URL";
import { RootStackParamList, UserDto, UserImage, YourProfileResource } from "../../myTypes";
import { Badge, Button, Modal, Portal, Text, TextInput, useTheme } from 'react-native-paper';
import Alert from "../../components/Alert";
import VerticalView from "../../components/VerticalView";
import { useHeaderHeight } from '@react-navigation/elements';
import { BottomTabScreenProps } from "@react-navigation/bottom-tabs";

type Props = BottomTabScreenProps<RootStackParamList, 'Profile.Pictures'>

const Pictures = ({ route, navigation }: Props) => {

  const user: UserDto = route.params.user;
  const { colors } = useTheme();

  const { height } = useWindowDimensions();
  const headerHeight = useHeaderHeight();
  const i18n = I18N.getI18n()
  const MAX_IMAGES = 4;

  const [alertVisible, setAlertVisible] = React.useState(false);
  const [profilePic, setProfilePic] = React.useState("");
  const [images, setImages] = React.useState(Array<UserImage>);
  const [changedProfilePic, setChangedProfilePic] = React.useState(false);
  const [imageIdToBeRemoved, setImageIdToBeRemoved] = React.useState(0);
  const [importModalVisible, setImportModalVisible] = React.useState(false);
  const [importUrl, setImportUrl] = React.useState("");
  const [importLoading, setImportLoading] = React.useState(false);
  const [linkedSocialAccounts, setLinkedSocialAccounts] = React.useState<Array<{ provider: string; providerUsername?: string }>>([]);
  const [selectedImportProvider, setSelectedImportProvider] = React.useState<string | null>(null);

  const alertButtons = [
    {
      text: i18n.t('cancel'),
      onPress: () => {
        setAlertVisible(false);
        setImageIdToBeRemoved(0);
      }
    },
    {
      text: i18n.t('ok'),
      onPress: async () => {
        await Global.Fetch(Global.format(URL.USER_DELETE_IMAGE, String(imageIdToBeRemoved)), 'post');
        let imagesCopy = [...images];
        let newImages = imagesCopy.filter(item => item.id !== imageIdToBeRemoved);
        setImages(newImages);
        setImageIdToBeRemoved(0);
        setAlertVisible(false);
        user.images = newImages;
      }
    }
  ]

  React.useEffect(() => {
    if (imageIdToBeRemoved) {
      setAlertVisible(true);
    }
  }, [imageIdToBeRemoved]);

  React.useEffect(
    () =>
      navigation.addListener('beforeRemove', (e: any) => {
        e.preventDefault();
        goBack();
      }),
    [navigation]
  );

  React.useEffect(() => {
    setImages(user.images);
    setProfilePic(user.profilePicture);
  }, []);

  async function load() {
    let response = await Global.Fetch(URL.API_RESOURCE_YOUR_PROFILE);
    let data: YourProfileResource = response.data;
    let dto: UserDto = data.user;
    setImages(dto.images);
    setProfilePic(dto.profilePicture);
  }

  async function updateProfilePicture() {
    let imageData: string | null | undefined = await Global.pickImage();
    if (imageData) {
      const bodyFormData = Global.buildFormData(imageData);
      await Global.Fetch(URL.USER_UPDATE_PROFILE_PICTURE, 'post', bodyFormData, 'multipart/form-data');
      load();
      setChangedProfilePic(true);
      navigation.setParams({ changed: false });
    }
  }

  async function addImage() {
    let imageData: string | null | undefined = await Global.pickImage();
    if (imageData != null) {
      const bodyFormData = Global.buildFormData(imageData);
      const response = await Global.Fetch(URL.USER_ADD_IMAGE, 'post', bodyFormData, 'multipart/form-data');
      const responseImages: UserImage[] = response.data;
      setImages(responseImages);
      user.images = responseImages;
    }
  }

  async function removeImage(id: number) {
    setImageIdToBeRemoved(id);
  }

  async function importSocialImage(useMediaUrl: boolean) {
    if (!importUrl?.trim()) {
      return;
    }

    setImportLoading(true);
    try {
      const provider = selectedImportProvider || undefined;
      const payload = useMediaUrl
        ? { provider, mediaUrl: importUrl.trim() }
        : { provider, postUrl: importUrl.trim() };
      const response = await Global.Fetch(URL.USER_IMPORT_SOCIAL_IMAGE, 'post', payload);
      const responseImages: UserImage[] = response.data;
      setImages(responseImages);
      user.images = responseImages;
      setImportModalVisible(false);
      setImportUrl("");
      setSelectedImportProvider(null);
      Global.ShowToast(i18n.t('profile.photos.import-social-success'));
    } catch (error: any) {
      console.error(error);
      const backendMessage = error?.response?.data;
      if (typeof backendMessage === 'string' && backendMessage.includes('social_media_import_requires_linked_account')) {
        Global.ShowToast('Link that social account in Settings first');
      } else if (typeof backendMessage === 'string' && backendMessage.includes('social_media_import_ownership_unverified')) {
        Global.ShowToast('URL does not match your linked social account');
      } else {
        Global.ShowToast('Could not import social image');
      }
    } finally {
      setImportLoading(false);
    }
  }

  async function openImportModal() {
    setImportModalVisible(true);
    setImportUrl("");
    setSelectedImportProvider(null);
    try {
      const response = await Global.Fetch(URL.USER_SOCIAL_CONNECT_ACCOUNTS);
      const accounts = Array.isArray(response?.data) ? response.data : [];
      setLinkedSocialAccounts(accounts);
      if (accounts.length === 1) {
        setSelectedImportProvider(accounts[0].provider);
      }
    } catch (e) {
      console.error(e);
      setLinkedSocialAccounts([]);
    }
  }

  async function goBack() {
    navigation.navigate('Main', {
      screen: Global.SCREEN_YOURPROFILE,
      params: { changed: changedProfilePic },
      merge: true,
    });
  }

  const style = StyleSheet.create({
    image: {
      width: '100%',
      height: 'auto',
      maxWidth: WIDESCREEN_HORIZONTAL_MAX,
      aspectRatio: 1,
    },
    imageSmall: {
      width: '50%',
      maxWidth: '50%',
    },
    modalContainer: {
      backgroundColor: colors.surface,
      borderRadius: 12,
      padding: 20,
      marginHorizontal: 16,
      maxWidth: WIDESCREEN_HORIZONTAL_MAX,
      alignSelf: 'center',
      width: '90%',
    },
  });

  return (
    <View style={{ height: height - headerHeight }}>
      <View style={{
        zIndex: 1, position: 'absolute', marginBottom: 16,
        marginRight: 16, width: '100%', right: 0, bottom: 0
      }}>
        <View style={{
          position: 'absolute',
          bottom: 10,
          right: 10
        }}>
          <Badge size={12} visible={images.length === 0} style={styles.badge} />
          {images.length < MAX_IMAGES &&
            <>
              <Button icon="image-plus" mode="elevated" onPress={() => addImage()}>
                {i18n.t('profile.photos.add')}
              </Button>
              <Button icon="link-variant" mode="contained-tonal" style={{ marginTop: 8 }}
                onPress={() => openImportModal()}>
                {i18n.t('profile.photos.import-social')}
              </Button>
            </>
          }
        </View>
      </View>
      <VerticalView onRefresh={load}>
        <TouchableOpacity
          onPress={() => { updateProfilePicture() }}>
          <Image source={{ uri: profilePic ? profilePic : undefined }} style={[style.image, { width: '70%', alignSelf: 'center', borderRadius: 12 }]} />
        </TouchableOpacity>
        <View style={{ alignItems: 'center', justifyContent: 'center', marginTop: -54, marginBottom: 24 }}>
          <Button mode="contained-tonal" style={{ width: 240 }} onPress={() => updateProfilePicture()}>{i18n.t('profile.photos.change-profile-pic')}</Button>
        </View>
        <View style={{ flexDirection: 'row', width: '100%' }}>
          <FlatList
            scrollEnabled={false}
            style={{ marginBottom: 4 }}
            numColumns={2}
            data={images}
            keyExtractor={(item, index) => index.toString()}
            renderItem={({ item }) => (
              <TouchableOpacity style={[style.image, style.imageSmall, { padding: 12 }]} onPress={() => removeImage(item.id)}>
                <Image source={{ uri: item.content }} style={[style.image, { borderRadius: 8 }]} />
              </TouchableOpacity>
            )}
          />
        </View>
        <Alert visible={alertVisible} setVisible={setAlertVisible} message={i18n.t('profile.photos.delete')} buttons={alertButtons} />
      </VerticalView>
      <Portal>
        <Modal visible={importModalVisible} onDismiss={() => setImportModalVisible(false)} contentContainerStyle={style.modalContainer}>
          <Text variant="titleMedium">{i18n.t('profile.photos.import-social-title')}</Text>
          <Text style={{ marginTop: 8, marginBottom: 10 }}>
            {i18n.t('profile.photos.import-social-subtitle')}
          </Text>
          <Text style={{ marginBottom: 8, fontSize: 12 }}>
            Connected account:
          </Text>
          <View style={{ flexDirection: 'row', flexWrap: 'wrap', marginBottom: 10 }}>
            {linkedSocialAccounts.length === 0 && (
              <Text style={{ fontSize: 12, opacity: 0.7 }}>
                No linked social accounts found. Link one in Settings first.
              </Text>
            )}
            {linkedSocialAccounts.map((account) => (
              <Button
                key={`${account.provider}-${account.providerUsername || ''}`}
                compact
                mode={selectedImportProvider === account.provider ? "contained" : "outlined"}
                style={{ marginRight: 6, marginBottom: 6 }}
                onPress={() => setSelectedImportProvider(account.provider)}
              >
                {account.providerUsername ? `${account.provider} @${account.providerUsername}` : account.provider}
              </Button>
            ))}
          </View>
          <TextInput
            mode="outlined"
            label={i18n.t('profile.photos.import-social-url')}
            value={importUrl}
            onChangeText={setImportUrl}
            autoCapitalize="none"
            autoCorrect={false}
          />
          <View style={{ marginTop: 12, flexDirection: 'row', justifyContent: 'space-between', flexWrap: 'wrap' }}>
            <Button onPress={() => setImportModalVisible(false)}>
              {i18n.t('cancel')}
            </Button>
            <Button loading={importLoading} disabled={importLoading || !importUrl.trim()}
              onPress={() => importSocialImage(false)}>
              {i18n.t('profile.photos.import-social-post')}
            </Button>
            <Button loading={importLoading} disabled={importLoading || !importUrl.trim()}
              onPress={() => importSocialImage(true)}>
              {i18n.t('profile.photos.import-social-media')}
            </Button>
          </View>
        </Modal>
      </Portal>
    </View>
  )
};

export default Pictures;
