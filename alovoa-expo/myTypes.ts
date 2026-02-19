import * as Global from "./Global"; 

export type RootStackParamList = {
  [Global.SCREEN_CHAT]: undefined;
  [Global.SCREEN_DONATE]: undefined;
  [Global.SCREEN_LIKES]: undefined;
  Login: undefined;
  Main: object;
  MessageDetail: {
    conversation: ConversationDto;
  };
  Onboarding: undefined;
  PasswordReset: undefined;
  Profile: {
    user?: UserDto;
    uuid?: string;
    odId?: string;
  };
  [Global.SCREEN_PROFILE_ADVANCED_SETTINGS]: {
    user: UserDto
  };
  [Global.SCREEN_PROFILE_PICTURES]: {
    changed: boolean;
    user: UserDto;
  };
  'Profile.Prompts': {
    prompts: UserPrompt[];
    updatePrompts: (prompts: UserPrompt[]) => void
  };
  [Global.SCREEN_PROFILE_PROFILESETTINGS]: {
    data: YourProfileResource;
    user: UserDto;
  };
  [Global.SCREEN_PROFILE_SEARCHSETTINGS]: {
    data: YourProfileResource
  }
  [Global.SCREEN_PROFILE_SETTINGS]: {
    data: YourProfileResource
  };
  Register: {
    registerEmail: boolean
  };
  [Global.SCREEN_SEARCH]: {
    changed: boolean
  };
  [Global.SCREEN_YOURPROFILE]: {
    changed: boolean
  };
  // AURA Routes
  'Intake.Home': undefined;
  'Assessment.Home': undefined;
  'Assessment.Question': {
    category?: AssessmentCategory;
    random?: boolean;
    count?: number;
  };
  'Assessment.Category': {
    category: AssessmentCategory;
  };
  'Assessment.Results': undefined;
  'VideoVerification': undefined;
  'VideoIntro': undefined;
  'VideoDate.List': undefined;
  'VideoDate.Schedule': {
    videoDateId: string;
    partnerId: string;
  };
  'VideoDate.Call': {
    videoDateId: string;
  };
  'VideoDate.Feedback': {
    videoDateId: string;
  };
  'Matching.Home': undefined;
  'Matching.Compatibility': {
    userId: string;
  };
  'Matching.Filter': undefined;
  'Growth.Context': undefined;
  'Values.Hierarchy': undefined;
  'Bridge.Journey': {
    conversationId?: number;
  } | undefined;
  'MatchWindow.List': undefined;
  'Calendar.Availability': undefined;
  'Intake.ScaffoldedProfile': undefined;
  'Report.User': {
    userId: string;
    userName?: string;
  };
  'Reputation.Score': undefined;
};

export type CardItemT = {
  user: UserDto;
  donation?: number;
  unitsImperial?: boolean;
  swiper?: any;
  message?: string;
  index?: number;
  onLikePressed?: () => void;
  onMessagePressed?: (result: LikeResultT) => void;
};

export type IconT = {
  name: any;
  size: number;
  color: string;
  style?: any;
};

export type MessageT = {
  conversation: ConversationDto;
};

export type TabBarIconT = {
  focused: boolean;
  iconName: any;
  text: string;
};

export type SelectModalT = {
  disabled: boolean,
  multi: boolean,
  minItems: number,
  title: string;
  data: [number, string | undefined][];
  selected: number[];
  onValueChanged: (id: number, checked: boolean) => void
};

export type ComplimentModalT = {
  visible: boolean,
  setVisible: (state: boolean) => void,
  name: string,
  age: number,
  profilePicture: string,
  onSend: (message: string, pop?: boolean) => void,
  onDismiss?: () => void;
};

export type RangeSliderModalT = {
  title: string;
  titleLower: string,
  titleUpper: string,
  valueLower: number,
  valueUpper: number,
  onValueLowerChanged: (value: number) => void,
  onValueUpperChanged: (value: number) => void
};

export type InterestModalT = {
  data: UserInterest[];
  user?: UserDto;
  updateButtonText?: (interests: UserInterest[]) => void;
  setInterestsExternal?: (interests: UserInterest[]) => void;
};

export type DonationDtoListModel = {
  list: DonationDto[];
}

export type RegisterBody = {
  email?: string
  password?: string
  firstName: string;
  dateOfBirth: Date;
  referrerCode?: string
  gender: number
  termsConditions: boolean
  privacy: boolean;
}

export type UserInterestAutocomplete = {
  count: number
  countString: string
  name: string
}

export type UserOnboarding = {
  intention: number;
  preferredGenders: number[];
  profilePictureMime: string;
  description: string;
  interests: string[];
  notificationLike: boolean;
  notificationChat: boolean;
}

export type Gender = {
  id: number;
  text: string
}

export type UserMiscInfo = {
  id: number;
  value: number;
}

export type UserIntention = {
  id: number;
  text: string
}

export type UserInterest = {
  text: string
}

export type UserImage = {
  id: number;
  content: string;
}

export type DataT = {
    id: number
    name: string
    isOnline: boolean
    match: string
    description: string
    age?: string
    location?: string
    info1?: string
    info2?: string
    info3?: string
    info4?: string
    message: string
    image: string
}

export type UserDto = {
  uuid: string
  idEncoded?: string // Alias for uuid in some contexts
  email?: string //is null when not current user
  firstName: string
  age: number
  donationAmount: number
  gender: Gender;
  hasAudio: boolean;
  audio: string;
  units: number;
  preferedMinAge: number;
  preferedMaxAge: number;
  miscInfos: UserMiscInfo[]
  preferedGenders: Gender[];
  intention: UserIntention;
  interests: UserInterest[]
  profilePicture: string;
  images: UserImage[];
  description: string;
  country: string;
  distanceToUser: number;
  commonInterests: UserInterest[];
  totalDonations: number;
  numBlockedByUsers: number;
  numReports: number;
  blockedByCurrentUser: boolean;
  reportedByCurrentUser: boolean;
  likesCurrentUser: boolean;
  likedByCurrentUser: boolean;
  hiddenByCurrentUser: boolean;
  numberReferred: number;
  compatible: boolean
  hasLocation: boolean;
  locationLatitude: number;
  locationLongitude: number;
  lastActiveState: number;
  userSettings: UserSettings;
  prompts: UserPrompt[];
  verificationPicture: UserDtoVerificationPicture;

  // AURA fields (aligned with Java User entity)
  verified?: boolean;
  locationName?: string;

  // Profile details (OKCupid-style) - matches UserProfileDetails.java
  profileDetails?: UserProfileDetails;

  // Personality & Assessment - matches UserPersonalityProfile.java
  personalityProfile?: UserPersonalityProfile;

  // Political Assessment - matches UserPoliticalAssessment.java
  politicalAssessment?: UserPoliticalAssessmentFull;

  // Reputation - matches UserReputationScore.java
  reputationScore?: UserReputationScoreFull;

  // Video Verification - matches UserVideoVerification.java
  videoVerification?: UserVideoVerificationFull;

  // Location Preferences - matches UserLocationPreferences.java
  locationPreferences?: UserLocationPreferences;

  // Relationship status
  relationship?: UserRelationship;

  // Donation tier - matches User.java donationTier field
  donationTier?: DonationTier;
  lastDonationDate?: Date;
  donationStreakMonths?: number;

  // Show zodiac preference
  showZodiac?: boolean;

  // Video-First Display (AURA authenticity feature)
  hasVideoIntro?: boolean;
  videoIntroUrl?: string;
  videoIntroThumbnail?: string;
  videoIntroDuration?: number;
  videoWatchRequired?: boolean;
  videoWatched?: boolean;
}

export type UserSettings = {
  emailLike: boolean;
  emailChat: boolean;
  shareGrowthProfile?: boolean;
  allowBehaviorSignals?: boolean;
  monthlyGrowthCheckins?: boolean;
}

export type UserInterestDto = {
  id: string,
  number: string
}

export type UserPrompt = {
  promptId: number,
  text: string
}

export type DonationDto = {
  id: number;
  date: Date;
  user: UserDto;
  amount: number;
}

export type MessageDto = {
  id: number;
  content: string;
  date: Date;
  from: boolean;
  allowedFormatting: boolean;
}

export type ConversationDto = {
  id: number;
  lastUpdated: Date;
  userName: string;
  userProfilePicture: string;
  lastMessage: MessageDto;
  uuid: string;
  read: boolean;
}

export type NotificationDto = {
  id: number;
  date: Date;
  message: string;
  userFromDto: UserDto;
}

export enum UnitsEnum {
  SI = 0,
  IMPERIAL = 1
}

export enum SettingsEmailEnum {
  LIKE = 1,
  CHAT = 2
}

export enum SearchStageEnum {
  NORMAL,
  INCREASED_RADIUS_1,
  INCREASED_RADIUS_2,
  WORLD,
  IGNORE_1,
  IGNORE_2,
  IGNORE_ALL
}

export enum UserMiscInfoEnum {
  DRUGS_TOBACCO = 1,
  DRUGS_ALCOHOL = 2,
  DRUGS_CANNABIS = 3,
  DRUGS_OTHER = 4,
  RELATIONSHIP_SINGLE = 11,
  RELATIONSHIP_TAKEN = 12,
  RELATIONSHIP_OPEN = 13,
  RELATIONSHIP_OTHER = 14,
  KIDS_NO = 21,
  KIDS_YES = 22,
  DRUGS_TOBACCO_NO = 31,
  DRUGS_ALCOHOL_NO = 32,
  DRUGS_CANNABIS_NO = 33,
  DRUGS_OTHER_NO = 34,
  DRUGS_TOBACCO_SOMETIMES = 41,
  DRUGS_ALCOHOL_SOMETIMES = 42,
  DRUGS_CANNABIS_SOMETIMES = 43,
  DRUGS_OTHER_SOMETIMES = 44,
  RELATIONSHIP_TYPE_MONOGAMOUS = 51,
  RELATIONSHIP_TYPE_POLYAMOROUS = 52,
  GENDER_IDENTITY_CIS = 61,
  GENDER_IDENTITY_TRANS = 62,
  POLITICS_MODERATE = 71,
  POLITICS_LEFT = 72,
  POLITICS_RIGHT = 73,
  RELIGION_NO = 81,
  RELIGION_YES = 82,
  FAMILY_WANT = 91,
  FAMILY_NOT_WANT = 92,
  FAMILY_NOT_SURE = 93,
}

export enum GenderEnum {
  MALE = 1,
  FEMALE = 2,
  OTHER = 3
}

export enum IntentionEnum {
  MEET = 1,
  DATE = 2,
  SEX = 3,
}

export type SearchDto = {
  users: UserDto[];
  message: string;
  stage: SearchStageEnum;
  global: boolean;
  incompatible: boolean;
}

export type YourProfileResource = {
  user: UserDto;
  genders: Gender[];
  intentions: UserIntention[];
  imageMax: number,
  isLegal: boolean,
  mediaMaxSize: number,
  interestMaxSize: number,
  referralsLeft: number;
  showIntention: boolean
  "settings.ignoreIntention": boolean;
}

export type DonateResource = {
  user: UserDto;
}

export type DonateSearchFilterResource = {
  currUser: UserDto;
  donations: DonationDto[];
  filter: number;
}

export type ChatDetailResource = {
  user: UserDto;
  convoId: number;
  partner: UserDto;
}

export type ChatMessageUpdateResource = {
  messages: MessageDto[];
}

export type AlertsResource = {
  notifications: NotificationDto[];
  user: UserDto;
}

export type ProfileResource = {
  compatible: boolean;
  user: UserDto;
  currUserDto: UserDto;
  isLegal: boolean,
}

export type SearchResource = {
  user: UserDto;
}

export type SearchUsersResource = {
  dto: SearchDto;
  currUser: UserDto;
}

export type ChatsResource = {
  user: UserDto;
  conversations: ConversationDto[];
}

export type MessageDtoListModel = {
  list: MessageDto[];
}

export type UserOnboardingResource = {
  genders: Gender[];
  intentions: UserIntention[];
  isLegal: boolean;
  mediaMaxSize: number
  interestMaxSize: number;
}

export type UserUsersResource = {
  users: UserDto[];
  user: UserDto;
}

export type UserDtoVerificationPicture = {
  verifiedByAdmin: boolean;
  verifiedByUsers: boolean;
  votedByCurrentUser: boolean;
  hasPicture: boolean;
  data: string;
  text: string;
  uuid: string;
  userYes: number;
  userNo: number;
}

export type AlertModel = {
  visible: boolean;
  message: string;
  buttons: AlertButtonModel[];
  setVisible: (bool: boolean) => void;
}

export type AlertButtonModel = {
  text: string;
  onPress: () => void;
}

export type Captcha = {
  id: number;
  image: string;
}

export type PasswordResetDto = {
  captchaId: number;
  captchaText: string;
  email: string;
}

export type LikeResultT = {
  user: UserDto;
  message?: string;
}

export const GenderMap = new Map<number, string>([
  [GenderEnum.MALE, "male"],
  [GenderEnum.FEMALE, "female"],
  [GenderEnum.OTHER, "other"],
]);

export enum IntentionE {
  MEET = 1,
  DATE = 2,
  SEX = 3
}

export type SearchParams = {
    distance?: number;
    preferredGenderIds?: number[]
    preferredMinAge?: number;
    preferredMaxAge?: number;
    showOutsideParameters?: boolean;
    sort?: SearchParamsSortE;
    latitude?: number;
    longitude?: number;
    miscInfos?: number[];
    intentions?: number[];
    interests?: string[];
}

export enum SearchParamsSortE {
  DISTANCE = 1,
  ACTIVE_DATE = 2,
  INTEREST = 3,
  DEFAULT = 4,
  DONATION_TOTAL = 5,
  NEWEST_USER = 6,
}

export const MiscInfoNameMap = new Map<number, string>([
  [UserMiscInfoEnum.RELATIONSHIP_SINGLE, 'profile.misc-info.relationship.single'],
  [UserMiscInfoEnum.RELATIONSHIP_TAKEN, 'profile.misc-info.relationship.taken'],
  [UserMiscInfoEnum.RELATIONSHIP_OPEN, 'profile.misc-info.relationship.open'],
  [UserMiscInfoEnum.RELATIONSHIP_OTHER, 'profile.misc-info.relationship.other'],
  [UserMiscInfoEnum.KIDS_NO, 'profile.misc-info.kids.no'],
  [UserMiscInfoEnum.KIDS_YES, 'profile.misc-info.kids.yes'],
  [UserMiscInfoEnum.FAMILY_WANT, 'profile.misc-info.family.yes'],
  [UserMiscInfoEnum.FAMILY_NOT_WANT, 'profile.misc-info.family.no'],
  [UserMiscInfoEnum.FAMILY_NOT_SURE, 'profile.misc-info.family.not-sure'],
  [UserMiscInfoEnum.RELATIONSHIP_TYPE_MONOGAMOUS, 'profile.misc-info.relationship-type.monogamous'],
  [UserMiscInfoEnum.RELATIONSHIP_TYPE_POLYAMOROUS, 'profile.misc-info.relationship-type.polyamorous'],
  [UserMiscInfoEnum.POLITICS_LEFT, 'profile.misc-info.politics.left'],
  [UserMiscInfoEnum.POLITICS_MODERATE, 'profile.misc-info.politics.moderate'],
  [UserMiscInfoEnum.POLITICS_RIGHT, 'profile.misc-info.politics.right'],
  [UserMiscInfoEnum.GENDER_IDENTITY_CIS, 'profile.misc-info.gender-identity.cis'],
  [UserMiscInfoEnum.GENDER_IDENTITY_TRANS, 'profile.misc-info.gender-identity.trans'],
  [UserMiscInfoEnum.RELIGION_YES, 'profile.misc-info.religion.yes'],
  [UserMiscInfoEnum.RELIGION_NO, 'profile.misc-info.religion.no'],
  [UserMiscInfoEnum.DRUGS_ALCOHOL, 'profile.misc-info.yes'],
  [UserMiscInfoEnum.DRUGS_ALCOHOL_SOMETIMES, 'profile.misc-info.sometimes'],
  [UserMiscInfoEnum.DRUGS_ALCOHOL_NO, 'profile.misc-info.no'],
  [UserMiscInfoEnum.DRUGS_TOBACCO, 'profile.misc-info.yes'],
  [UserMiscInfoEnum.DRUGS_TOBACCO_SOMETIMES, 'profile.misc-info.sometimes'],
  [UserMiscInfoEnum.DRUGS_TOBACCO_NO, 'profile.misc-info.no'],
  [UserMiscInfoEnum.DRUGS_CANNABIS, 'profile.misc-info.yes'],
  [UserMiscInfoEnum.DRUGS_CANNABIS_SOMETIMES, 'profile.misc-info.sometimes'],
  [UserMiscInfoEnum.DRUGS_CANNABIS_NO, 'profile.misc-info.no'],
  [UserMiscInfoEnum.DRUGS_OTHER, 'profile.misc-info.yes'],
  [UserMiscInfoEnum.DRUGS_OTHER_SOMETIMES, 'profile.misc-info.sometimes'],
  [UserMiscInfoEnum.DRUGS_OTHER_NO, 'profile.misc-info.no'],
]); 

export const MiscInfoRelationshipNameMap = new Map<number, string>([
  [UserMiscInfoEnum.RELATIONSHIP_SINGLE, 'profile.misc-info.relationship.single'],
  [UserMiscInfoEnum.RELATIONSHIP_TAKEN, 'profile.misc-info.relationship.taken'],
  [UserMiscInfoEnum.RELATIONSHIP_OPEN, 'profile.misc-info.relationship.open'],
  [UserMiscInfoEnum.RELATIONSHIP_OTHER, 'profile.misc-info.relationship.other'],
]);

export const MiscInfoKidsNameMap = new Map<number, string>([
  [UserMiscInfoEnum.KIDS_NO, 'profile.misc-info.kids.no'],
  [UserMiscInfoEnum.KIDS_YES, 'profile.misc-info.kids.yes'],
]); 

export const MiscInfoFamilyNameMap = new Map<number, string>([
  [UserMiscInfoEnum.FAMILY_WANT, 'profile.misc-info.family.yes'],
  [UserMiscInfoEnum.FAMILY_NOT_WANT, 'profile.misc-info.family.no'],
  [UserMiscInfoEnum.FAMILY_NOT_SURE, 'profile.misc-info.family.not-sure'],
]); 

export const MiscInfoRelationshipTypeNameMap = new Map<number, string>([
  [UserMiscInfoEnum.RELATIONSHIP_TYPE_MONOGAMOUS, 'profile.misc-info.relationship-type.monogamous'],
  [UserMiscInfoEnum.RELATIONSHIP_TYPE_POLYAMOROUS, 'profile.misc-info.relationship-type.polyamorous'],
]); 

export const MiscInfoPoliticsNameMap = new Map<number, string>([
  [UserMiscInfoEnum.POLITICS_LEFT, 'profile.misc-info.politics.left'],
  [UserMiscInfoEnum.POLITICS_MODERATE, 'profile.misc-info.politics.moderate'],
  [UserMiscInfoEnum.POLITICS_RIGHT, 'profile.misc-info.politics.right'],
]); 

export const MiscInfoGenderIdentityNameMap = new Map<number, string>([
  [UserMiscInfoEnum.GENDER_IDENTITY_CIS, 'profile.misc-info.gender-identity.cis'],
  [UserMiscInfoEnum.GENDER_IDENTITY_TRANS, 'profile.misc-info.gender-identity.trans'],
]); 

export const MiscInfoReligionNameMap = new Map<number, string>([
  [UserMiscInfoEnum.RELIGION_YES, 'profile.misc-info.religion.yes'],
  [UserMiscInfoEnum.RELIGION_NO, 'profile.misc-info.religion.no'],
]); 

export const MiscInfoDrugsAlcoholNameMap = new Map<number, string>([
  [UserMiscInfoEnum.DRUGS_ALCOHOL, 'profile.misc-info.yes'],
  [UserMiscInfoEnum.DRUGS_ALCOHOL_SOMETIMES, 'profile.misc-info.sometimes'],
  [UserMiscInfoEnum.DRUGS_ALCOHOL_NO, 'profile.misc-info.no'],
]); 

export const MiscInfoDrugsTobaccoNameMap = new Map<number, string>([
  [UserMiscInfoEnum.DRUGS_TOBACCO, 'profile.misc-info.yes'],
  [UserMiscInfoEnum.DRUGS_TOBACCO_SOMETIMES, 'profile.misc-info.sometimes'],
  [UserMiscInfoEnum.DRUGS_TOBACCO_NO, 'profile.misc-info.no'],
]); 

export const MiscInfoDrugsCannabisNameMap = new Map<number, string>([
  [UserMiscInfoEnum.DRUGS_CANNABIS, 'profile.misc-info.yes'],
  [UserMiscInfoEnum.DRUGS_CANNABIS_SOMETIMES, 'profile.misc-info.sometimes'],
  [UserMiscInfoEnum.DRUGS_CANNABIS_NO, 'profile.misc-info.no'],
]); 

export const MiscInfoDrugsOtherNameMap = new Map<number, string>([
  [UserMiscInfoEnum.DRUGS_OTHER, 'profile.misc-info.yes'],
  [UserMiscInfoEnum.DRUGS_OTHER_SOMETIMES, 'profile.misc-info.sometimes'],
  [UserMiscInfoEnum.DRUGS_OTHER_NO, 'profile.misc-info.no'],
]); 

export const IntentionNameMap = new Map<number, string>([
  [IntentionE.MEET, 'profile.intention.meet'],
  [IntentionE.DATE, 'profile.intention.date'],
  [IntentionE.SEX, 'profile.intention.sex'],
]); 

export const GenderNameMap = new Map<number, string>([
  [GenderEnum.MALE, 'gender.male'],
  [GenderEnum.FEMALE, 'gender.female'],
  [GenderEnum.OTHER, 'gender.other'],
]); 

export const UnitsNameMap = new Map<number, string>([
  [UnitsEnum.SI, 'profile.units.si'],
  [UnitsEnum.IMPERIAL, 'profile.units.imperial'],
]); 

export const SettingsEmailNameMap = new Map<number, string>([
  [SettingsEmailEnum.LIKE, 'profile.settings.email.like'],
  [SettingsEmailEnum.CHAT, 'profile.settings.email.chat'],
]);

// ============================================
// AURA Platform Types
// ============================================

// Assessment Question Types
export interface AssessmentQuestion {
  id: string;
  text: string;
  category: AssessmentCategory;
  subcategory?: string;
  options: AssessmentOption[];
  weight?: number;
  importance?: QuestionImportance;
  dealbreaker?: boolean;
  tags?: string[];
}

export interface AssessmentOption {
  id: string;
  text: string;
  value: number;
  traits?: { [key: string]: number };
}

export enum AssessmentCategory {
  PERSONALITY = 'PERSONALITY',
  VALUES = 'VALUES',
  LIFESTYLE = 'LIFESTYLE',
  DATING = 'DATING',
  SEX_INTIMACY = 'SEX_INTIMACY',
  RELATIONSHIP_STYLE = 'RELATIONSHIP_STYLE',
  SOCIAL = 'SOCIAL',
  LIFESTYLE_HABITS = 'LIFESTYLE_HABITS',
  FUTURE_GOALS = 'FUTURE_GOALS',
  COMMUNICATION = 'COMMUNICATION',
  POLITICAL = 'POLITICAL',
  ATTACHMENT = 'ATTACHMENT',
  BIG_FIVE = 'BIG_FIVE',
}

export enum QuestionImportance {
  IRRELEVANT = 0,
  A_LITTLE = 1,
  SOMEWHAT = 2,
  VERY = 3,
  MANDATORY = 4,
}

// User Assessment Profile
export interface UserAssessmentProfile {
  id: number;
  userId: number;

  // Big Five Personality (0-100)
  openness: number;
  conscientiousness: number;
  extraversion: number;
  agreeableness: number;
  neuroticism: number;

  // Attachment Style (0-100)
  attachmentAnxiety: number;
  attachmentAvoidance: number;
  attachmentStyle: AttachmentStyle;

  // Values (0-100)
  progressive: number;
  egalitarian: number;

  // Lifestyle (0-100)
  socialOrientation: number;
  healthFocus: number;
  workLifeBalance: number;
  financialAmbition: number;

  // Assessment Stats
  questionsAnswered: number;
  lastUpdated: Date;
  profileComplete: boolean;

  // Answers
  answers?: UserQuestionAnswer[];
}

export enum AttachmentStyle {
  SECURE = 'SECURE',
  ANXIOUS = 'ANXIOUS',
  AVOIDANT = 'AVOIDANT',
  FEARFUL_AVOIDANT = 'FEARFUL_AVOIDANT',
}

export interface UserQuestionAnswer {
  questionId: string;
  selectedOptionId: string;
  acceptableOptionIds: string[];
  importance: QuestionImportance;
  dealbreaker: boolean;
  answeredAt: Date;
}

// Political Assessment
export interface UserPoliticalAssessment {
  id: number;
  userId: number;

  // Political Compass (-100 to 100)
  economicAxis: number;  // -100 = left, 100 = right
  socialAxis: number;    // -100 = libertarian, 100 = authoritarian

  // Individual dimensions
  fiscalPolicy: number;
  socialPolicy: number;
  foreignPolicy: number;
  environmentalPolicy: number;

  // Political identity
  partyAffiliation?: string;
  politicalLabel?: string;

  assessmentComplete: boolean;
  completedAt?: Date;
}

// Compatibility Scoring
export interface CompatibilityScore {
  id: number;
  user1Id: number;
  user2Id: number;

  // Overall score (0-100)
  overallScore: number;

  // Component scores (0-100)
  personalityScore: number;
  valuesScore: number;
  lifestyleScore: number;
  attachmentScore: number;
  interestsScore: number;
  dealbreakersScore: number;
  politicalScore: number;

  // Question match stats
  questionsCompared: number;
  matchPercentage: number;
  conflictCount: number;
  matchCategory?: string;

  // Status
  calculatedAt: Date;
  stale: boolean;
}

export interface CompatibilityDimension {
  dimension: string;
  score: number;
  weight?: number;
  details?: Array<{ label: string; compatible: boolean }>;
}

export interface CompatibilityDealbreaker {
  category: string;
  description: string;
}

export interface SharedQuestion {
  questionText: string;
  yourAnswer: string;
  theirAnswer: string;
}

export interface CompatibilityBreakdown {
  category?: string;
  score?: number;
  weight?: number;
  details?: string[];
  conflicts?: string[];
  strengths?: string[];
  // Full breakdown fields
  overallScore: number;
  matchCategoryLabel?: string;
  summary?: string;
  enemyScore?: number;
  hasDealbreaker?: boolean;
  questionsCompared?: number;
  yourSatisfaction?: number;
  theirSatisfaction?: number;
  topCompatibilities?: string[];
  areasToDiscuss?: string[];
  matchInsight?: {
    topAreas?: string[];
    areasToDiscuss?: string[];
    summary?: string;
  };
  growthContext?: {
    valuesAlignment?: number;
    paceAlignment?: number;
    intentionAlignment?: number;
    trajectoryTypeLabel?: string;
    guidedConversationPrompts?: string[];
    paceAgreementTemplate?: { [key: string]: { you: string | number; them: string | number } };
  };
  dimensionLabels?: { [key: string]: string };
  dimensions: CompatibilityDimension[];
  dealbreakers: CompatibilityDealbreaker[];
  sharedQuestions: SharedQuestion[];
  questionMatches?: Array<{
    questionText: string;
    yourAnswer: string;
    theirAnswer: string;
    yourImportance?: string;
    theirImportance?: string;
    importance?: string;
    isMatch?: boolean;
    isPartialMatch?: boolean;
  }>;
}

// Video Verification
export interface UserVideoVerification {
  id: number;
  userId: number;
  s3Key: string;
  status: VerificationStatus;

  // Liveness detection
  livenessScore?: number;
  livenessChecked: boolean;

  // Deepfake detection
  deepfakeScore?: number;
  deepfakeChecked: boolean;

  // Face matching
  faceMatchScore?: number;
  faceMatchChecked: boolean;

  // Human review
  humanReviewed: boolean;
  humanApproved?: boolean;
  reviewedAt?: Date;
  reviewerNotes?: string;

  createdAt: Date;
  verifiedAt?: Date;
  expiresAt?: Date;
}

export enum VerificationStatus {
  PENDING = 'PENDING',
  PROCESSING = 'PROCESSING',
  VERIFIED = 'VERIFIED',
  FAILED = 'FAILED',
  EXPIRED = 'EXPIRED',
  NEEDS_REVIEW = 'NEEDS_REVIEW',
}

// Video Introduction
export enum VideoIntroStatus {
  NONE = 'NONE',
  PENDING = 'PENDING',
  UPLOADING = 'UPLOADING',
  PROCESSING = 'PROCESSING',
  ANALYZING = 'ANALYZING',
  READY = 'READY',
  COMPLETE = 'COMPLETE',
  FAILED = 'FAILED',
}

export interface VideoAnalysisResult {
  transcription?: string;
  worldview?: string;
  worldviewSummary?: string;
  background?: string;
  backgroundSummary?: string;
  lifeStory?: string;
  lifeStorySummary?: string;
  analysisScore?: number;
  overallInferenceConfidence?: number;
  inferredBigFive?: { [key: string]: number };
  inferredValues?: { [key: string]: number };
  inferredLifestyle?: { [key: string]: number };
  inferredAttachment?: { anxiety: number; avoidance: number };
  inferredAttachmentStyle?: AttachmentStyle;
  suggestedDealbreakers?: string[];
  confidenceScores?: { [key: string]: number };
  lowConfidenceAreas?: string[];
}

export interface UserVideoIntroduction {
  id: number;
  userId: number;
  s3Key: string;
  thumbnailS3Key?: string;
  durationSeconds: number;
  status: VideoIntroStatus;

  // AI Analysis
  transcription?: string;
  worldview?: string;
  background?: string;
  lifeStory?: string;
  analysisScore?: number;
  analysisComplete: boolean;
  analysisResult?: VideoAnalysisResult;

  // Inferred Assessment (Profile Scaffolding)
  inferredAssessmentJson?: string;
  inferredOpenness?: number;
  inferredConscientiousness?: number;
  inferredExtraversion?: number;
  inferredAgreeableness?: number;
  inferredNeuroticism?: number;
  inferredAttachmentAnxiety?: number;
  inferredAttachmentAvoidance?: number;
  inferredAttachmentStyle?: AttachmentStyle;
  overallInferenceConfidence?: number;
  inferenceReviewed: boolean;
  inferenceConfirmed: boolean;

  createdAt: Date;
  analyzedAt?: Date;
}

// Video Dating
export interface VideoDate {
  id: number;
  date1UserId: number;
  date2UserId: number;

  // Partner info (computed based on current user)
  partnerId?: number;
  partnerName?: string;
  partnerProfilePicture?: string;
  isInitiator?: boolean;

  // Scheduling
  scheduledAt?: Date;
  durationMinutes: number;
  status: VideoDateStatus;

  // Room info
  roomId?: string;
  roomToken?: string;

  // Timing
  startedAt?: Date;
  endedAt?: Date;
  actualDurationSeconds?: number;

  // Feedback
  date1Feedback?: VideoDateFeedback;
  date2Feedback?: VideoDateFeedback;
  feedbackGiven?: boolean;

  createdAt: Date;
}

export enum VideoDateStatus {
  PROPOSED = 'PROPOSED',
  ACCEPTED = 'ACCEPTED',
  DECLINED = 'DECLINED',
  SCHEDULED = 'SCHEDULED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
  NO_SHOW = 'NO_SHOW',
}

export interface VideoDateFeedback {
  rating: number; // 1-5
  overallRating?: number; // Alias for rating
  wouldMeetAgain: boolean;
  chemistry: number; // 1-5
  conversation: number; // 1-5
  connectionFelt?: string;
  notes?: string;
  privateNotes?: string;
  hasIssue?: boolean;
  issueType?: string;
  reportIssue?: boolean;
  submittedAt: Date;
}

// Match Window / Calendar
export interface MatchWindow {
  id: string | number;
  userId: number;

  // Window timing
  windowStart: Date;
  windowEnd: Date;

  // Match details
  matchedUserId: number;
  matchedUser?: UserDto;
  compatibilityScore?: number;
  matchPercentage?: number;
  hasMandatoryConflict?: boolean;
  matchCategory?: string;
  matchReason?: string;
  conversationId?: number;
  canSendIntroMessage?: boolean;
  hasSentIntroMessage?: boolean;
  hasReceivedIntroMessage?: boolean;
  myIntroMessage?: string;
  theirIntroMessage?: string;

  // Status
  status: MatchWindowStatus;
  userResponded: boolean;
  matchResponded: boolean;

  // Outcome
  conversationStarted: boolean;
  dateScheduled: boolean;

  createdAt: Date;
  expiresAt: string;
}

export interface GrowthContextProfile {
  traits: {
    purposeStatement: string;
    valuesHierarchy: string[];
    valueTradeoffs: Array<{ left: string; right: string; choice: string }>;
    growthArchetypes: { primary?: string; secondary?: string };
    relationshipIntentions: { relationshipType?: string; priorities?: string[]; minimumViableRelationship?: string };
    boundaries: string[];
    shadowPatterns: string[];
  };
  state: {
    currentChapter: string;
    pacePreferences: {
      messagesPerDay?: number;
      datesPerWeek?: number;
      aloneTimePerWeek?: number;
      emotionalAvailability?: number;
      timeAvailability?: number;
    };
    context: {
      stressLevel?: number;
      capacityLoad?: number;
      focusAreas?: string[];
    };
  };
  purposeStatement?: string;
  currentChapter?: string;
  valuesHierarchy?: string[];
  valueTradeoffs?: Array<{ left: string; right: string; choice: string }>;
  growthArchetypes?: { primary?: string; secondary?: string };
  pacePreferences?: {
    messagesPerDay?: number;
    datesPerWeek?: number;
    aloneTimePerWeek?: number;
    emotionalAvailability?: number;
    timeAvailability?: number;
  };
  relationshipIntentions?: { relationshipType?: string; priorities?: string[]; minimumViableRelationship?: string };
  boundaries?: string[];
  shadowPatterns?: string[];
  stateContext?: { stressLevel?: number; capacityLoad?: number; focusAreas?: string[] };
  updatedAt?: Date | string;
}

export interface BridgeMilestone {
  uuid: string;
  type: string;
  date: string;
  checkInSent?: boolean;
  relationshipStatus?: string;
  stillTogether?: boolean;
  leftPlatformTogether?: boolean;
}

export interface BridgeJourneySummary {
  totalMilestones: number;
  activeSuggestions: number;
  acceptedSuggestions: number;
  latestMilestone?: BridgeMilestone;
}

export enum MatchWindowStatus {
  PENDING = 'PENDING',
  ACTIVE = 'ACTIVE',
  ACCEPTED = 'ACCEPTED',
  DECLINED = 'DECLINED',
  EXPIRED = 'EXPIRED',
  MATCHED = 'MATCHED',
  SKIPPED = 'SKIPPED',
}

export enum MatchWindowResponse {
  ACCEPT = 'ACCEPT',
  DECLINE = 'DECLINE',
  SKIP = 'SKIP',
}

export interface TimeSlot {
  start: string;
  end: string;
}

export interface DayAvailability {
  enabled: boolean;
  slots: TimeSlot[];
}

export interface CalendarSlot {
  startTime: string;
  endTime: string;
  available: boolean;
}

export interface UserCalendarAvailability {
  userId?: number;
  timezone: string;
  days: Record<string, DayAvailability>;
  videoDatesEnabled: boolean;
  minimumNoticeHours: number;
}

// Accountability / Reporting
export interface UserAccountabilityReport {
  id: number;
  reporterId: number;
  reportedUserId: number;

  // Report details
  category: ReportCategory;
  subcategory?: string;
  description: string;

  // Evidence
  evidence: ReportEvidence[];

  // Status
  status: ReportStatus;
  priority: ReportPriority;

  // Resolution
  reviewedAt?: Date;
  reviewerId?: number;
  resolution?: string;
  actionTaken?: ReportAction;

  createdAt: Date;
  updatedAt: Date;

  // Public accountability
  isPublic: boolean;
  publicSummary?: string;
}

export enum ReportCategory {
  HARASSMENT = 'HARASSMENT',
  CATFISHING = 'CATFISHING',
  INAPPROPRIATE_CONTENT = 'INAPPROPRIATE_CONTENT',
  SCAM = 'SCAM',
  UNDERAGE = 'UNDERAGE',
  VIOLENCE = 'VIOLENCE',
  GHOSTING = 'GHOSTING',
  MISLEADING_PROFILE = 'MISLEADING_PROFILE',
  OTHER = 'OTHER',
}

// Aliases for simpler component use
export enum ReportType {
  HARASSMENT = 'HARASSMENT',
  FAKE_PROFILE = 'FAKE_PROFILE',
  INAPPROPRIATE_CONTENT = 'INAPPROPRIATE_CONTENT',
  SCAM = 'SCAM',
  UNDERAGE = 'UNDERAGE',
  THREATS = 'THREATS',
  SPAM = 'SPAM',
  OTHER = 'OTHER',
}

export enum ReportSeverity {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL',
}

export enum ReportStatus {
  PENDING = 'PENDING',
  UNDER_REVIEW = 'UNDER_REVIEW',
  RESOLVED = 'RESOLVED',
  DISMISSED = 'DISMISSED',
  ESCALATED = 'ESCALATED',
}

export enum ReportPriority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL',
}

export enum ReportAction {
  NO_ACTION = 'NO_ACTION',
  WARNING = 'WARNING',
  TEMPORARY_BAN = 'TEMPORARY_BAN',
  PERMANENT_BAN = 'PERMANENT_BAN',
  PROFILE_HIDDEN = 'PROFILE_HIDDEN',
}

export interface ReportEvidence {
  id: number;
  type: EvidenceType;
  s3Key?: string;
  content?: string;
  metadata?: string;
  createdAt: Date;
}

export enum EvidenceType {
  SCREENSHOT = 'SCREENSHOT',
  MESSAGE_LOG = 'MESSAGE_LOG',
  VIDEO = 'VIDEO',
  AUDIO = 'AUDIO',
  TEXT = 'TEXT',
}

// Reputation Scoring
export interface UserReputationScore {
  id: number;
  userId: number;

  // Overall score (0-100)
  overallScore: number;
  totalScore: number; // Alias for UI

  // Component scores
  responseRate: number;
  messageQuality: number;
  dateHonor: number; // Shows up to dates
  profileAccuracy: number;
  communityStanding: number;

  // Point breakdown for UI
  verificationPoints: number;
  responsePoints: number;
  reliabilityPoints: number;
  feedbackPoints: number;
  tenurePoints: number;

  // Counts
  totalInteractions: number;
  positiveInteractions: number;
  negativeInteractions: number;
  reportsReceived: number;
  reportsUpheld: number;

  // Badges
  badges: ReputationBadge[];

  // Status
  trustLevel: TrustLevel;
  lastUpdated: Date;
}

export interface ReputationHistoryItem {
  id: number;
  type: string;
  points: number;
  description: string;
  createdAt: string;
}

export enum TrustLevel {
  NEW = 'NEW',
  BASIC = 'BASIC',
  MEMBER = 'MEMBER',
  REGULAR = 'REGULAR',
  TRUSTED = 'TRUSTED',
  LEADER = 'LEADER',
}

export interface ReputationBadge {
  type: BadgeType | string;
  name: string;
  earnedAt?: string;
  description: string;
}

export enum BadgeType {
  VERIFIED = 'VERIFIED',
  RESPONSIVE = 'RESPONSIVE',
  GREAT_CONVERSATIONALIST = 'GREAT_CONVERSATIONALIST',
  DATE_KEEPER = 'DATE_KEEPER',
  COMMUNITY_HELPER = 'COMMUNITY_HELPER',
  LONG_TERM_MEMBER = 'LONG_TERM_MEMBER',
}

// Profile Scaffolding
export interface ScaffoldedProfileDto {
  bigFive: { [key: string]: ScoreWithConfidence };
  values: { [key: string]: ScoreWithConfidence };
  lifestyle: { [key: string]: ScoreWithConfidence };
  attachment: AttachmentInference;
  suggestedDealbreakers: DealbreakeSuggestion[];
  lowConfidenceAreas: string[];
  reviewed: boolean;
  confirmed: boolean;
}

export interface ScoreWithConfidence {
  score: number;
  confidence: number;
}

export interface AttachmentInference {
  anxiety: ScoreWithConfidence;
  avoidance: ScoreWithConfidence;
  style: AttachmentStyle;
  styleConfidence: number;
}

export interface DealbreakeSuggestion {
  category: string;
  item: string;
  confidence: number;
  reason: string;
}

// Intake Flow
export interface IntakeProgressDto {
  userId: number;
  currentStep: IntakeStep;
  stepsCompleted: IntakeStep[];

  // Step completion status
  basicProfileComplete: boolean;
  photoUploaded: boolean;
  videoIntroComplete: boolean;
  assessmentStarted: boolean;
  assessmentComplete: boolean;
  politicalAssessmentComplete: boolean;
  verificationComplete: boolean;

  // Progress stats
  questionsAnswered: number;
  totalQuestionsRequired: number;
  estimatedTimeRemaining: number; // minutes

  // Encouragement
  lastEncouragement?: string;
  nextMilestone?: string;
}

export enum IntakeStep {
  WELCOME = 'WELCOME',
  BASIC_PROFILE = 'BASIC_PROFILE',
  PHOTOS = 'PHOTOS',
  VIDEO_INTRO = 'VIDEO_INTRO',
  ASSESSMENT = 'ASSESSMENT',
  POLITICAL = 'POLITICAL',
  VERIFICATION = 'VERIFICATION',
  COMPLETE = 'COMPLETE',
}

// Match Daily Limits
export interface UserDailyMatchLimit {
  userId: number;
  matchesRemaining: number;
  likesRemaining: number;
  messagesRemaining: number;
  resetAt: Date;
  isPremium: boolean;
  // Aliases for simpler UI access
  remaining: number;
  limit: number;
}

// Waitlist & Stripe
export interface WaitlistEntry {
  id: number;
  email: string;
  referralCode?: string;
  position: number;
  status: WaitlistStatus;
  invitedAt?: Date;
  joinedAt?: Date;
  createdAt: Date;
}

export enum WaitlistStatus {
  WAITING = 'WAITING',
  INVITED = 'INVITED',
  JOINED = 'JOINED',
  EXPIRED = 'EXPIRED',
}

export interface StripeSubscription {
  id: string;
  userId: number;
  status: SubscriptionStatus;
  planId: string;
  currentPeriodStart: Date;
  currentPeriodEnd: Date;
  cancelAtPeriodEnd: boolean;
}

export enum SubscriptionStatus {
  ACTIVE = 'ACTIVE',
  PAST_DUE = 'PAST_DUE',
  CANCELED = 'CANCELED',
  INCOMPLETE = 'INCOMPLETE',
  TRIALING = 'TRIALING',
}

// Privacy-Safe Location
export interface PrivacySafeLocation {
  centroidId: string;
  displayName: string;
  travelTimeMinutes?: number;
  approximateDistance?: number;
}

// Profile Visitors
export interface ProfileVisit {
  id: number;
  visitorId: number;
  visitedUserId: number;
  visitor?: UserDto;
  visitedAt: Date;
  viewDurationSeconds?: number;
}

// Essay Prompts (Extended Prompts)
export interface UserEssay {
  id: number;
  userId: number;
  promptId: string;
  promptText: string;
  response: string;
  isPublic: boolean;
  createdAt: Date;
  updatedAt: Date;
}

// WebSocket Message Types
export interface WebSocketMessage {
  type: WebSocketMessageType;
  payload: any;
  timestamp: Date;
}

export enum WebSocketMessageType {
  CHAT_MESSAGE = 'CHAT_MESSAGE',
  TYPING_INDICATOR = 'TYPING_INDICATOR',
  READ_RECEIPT = 'READ_RECEIPT',
  PRESENCE_UPDATE = 'PRESENCE_UPDATE',
  MATCH_NOTIFICATION = 'MATCH_NOTIFICATION',
  VIDEO_DATE_SIGNAL = 'VIDEO_DATE_SIGNAL',
}

// AURA API Resources
export interface AssessmentResource {
  questions: AssessmentQuestion[];
  categories: AssessmentCategory[];
  userProgress: {
    answeredCount: number;
    totalRequired: number;
    categoryProgress: { [key: string]: number };
  };
  profile?: UserAssessmentProfile;
}

export interface CompatibilityResource {
  matches: CompatibilityMatch[];
  dailyLimit: UserDailyMatchLimit;
  filterSettings: CompatibilityFilterSettings;
}

export interface CompatibilityMatch {
  user: UserDto;
  score: CompatibilityScore;
  breakdown: CompatibilityBreakdown[];
  commonAnswers: number;
  sharedInterests: string[];
}

export interface CompatibilityFilterSettings {
  minCompatibility: number;
  maxDistance: number;
  ageRange: [number, number];
  mustHaveVideo: boolean;
  mustBeVerified: boolean;
  dealbreakersStrict: boolean;
}

export interface VideoDateResource {
  upcomingDates: VideoDate[];
  pastDates: VideoDate[];
  availableSlots: Date[];
  pendingRequests: VideoDate[];
}

export interface IntakeResource {
  progress: IntakeProgressDto;
  currentStepData: any;
  encouragement: string;
  estimatedCompletion: Date;
}

export interface ReputationResource {
  score: UserReputationScore;
  history: ReputationEvent[];
  leaderboard?: UserReputationScore[];
}

export interface ReputationEvent {
  type: string;
  points: number;
  description: string;
  occurredAt: Date;
}

export interface ScaffoldedProfileResource {
  scaffolded: ScaffoldedProfileDto;
  videoIntro: UserVideoIntroduction;
  clarificationQuestions?: AssessmentQuestion[];
}

// AURA Navigation Types
export type AuraStackParamList = {
  // Assessment
  'Assessment.Home': undefined;
  'Assessment.Category': { category: AssessmentCategory };
  'Assessment.Question': { questionId: string };
  'Assessment.Results': undefined;

  // Video
  'Video.Verification': undefined;
  'Video.Introduction': undefined;
  'Video.Recording': { type: 'verification' | 'introduction' };
  'Video.Preview': { videoUri: string; type: 'verification' | 'introduction' };

  // Video Dating
  'VideoDate.Home': undefined;
  'VideoDate.Schedule': { matchUserId: number };
  'VideoDate.Call': { dateId: number };
  'VideoDate.Feedback': { dateId: number };

  // Compatibility
  'Compatibility.Home': undefined;
  'Compatibility.Detail': { userId: number };
  'Compatibility.Breakdown': { userId: number };

  // Match Windows
  'MatchWindow.Home': undefined;
  'MatchWindow.Calendar': undefined;
  'MatchWindow.Detail': { windowId: number };

  // Profile Scaffolding
  'Scaffolding.Review': undefined;
  'Scaffolding.Adjust': { category: string };
  'Scaffolding.Confirm': undefined;

  // Intake
  'Intake.Home': undefined;
  'Intake.Step': { step: IntakeStep };

  // Accountability
  'Report.Create': { userId: number };
  'Report.Evidence': { reportId: number };
  'Report.History': undefined;

  // Reputation
  'Reputation.Home': undefined;
  'Reputation.Badges': undefined;
  'Reputation.Leaderboard': undefined;

  // Political Assessment
  'Political.Home': undefined;
  'Political.Question': { questionIndex: number };
  'Political.Results': undefined;
};

// ================================
// ALIGNED ENTITY TYPES (Java Backend)
// ================================

// UserProfileDetails - OKCupid 2016 style extended profile
// Matches: src/main/java/com/nonononoki/alovoa/entity/user/UserProfileDetails.java
export interface UserProfileDetails {
  id?: number;
  userId?: number;

  // Physical attributes
  heightCm?: number;
  bodyType?: BodyType;
  ethnicity?: Ethnicity;

  // Lifestyle
  diet?: Diet;
  pets?: PetStatus;
  petDetails?: string;

  // Background
  education?: EducationLevel;
  occupation?: string;
  employer?: string;
  languages?: string; // comma-separated or JSON array

  // Zodiac
  zodiacSign?: ZodiacSign;

  // Response behavior (calculated)
  responseRate?: ResponseRate;

  // Income (optional)
  income?: IncomeLevel;
}

// Enums matching UserProfileDetails.java
export enum BodyType {
  THIN = 'THIN',
  FIT = 'FIT',
  AVERAGE = 'AVERAGE',
  CURVY = 'CURVY',
  FULL_FIGURED = 'FULL_FIGURED',
  OVERWEIGHT = 'OVERWEIGHT',
  JACKED = 'JACKED',
  RATHER_NOT_SAY = 'RATHER_NOT_SAY',
}

export enum Ethnicity {
  ASIAN = 'ASIAN',
  BLACK = 'BLACK',
  HISPANIC_LATINO = 'HISPANIC_LATINO',
  INDIAN = 'INDIAN',
  MIDDLE_EASTERN = 'MIDDLE_EASTERN',
  NATIVE_AMERICAN = 'NATIVE_AMERICAN',
  PACIFIC_ISLANDER = 'PACIFIC_ISLANDER',
  WHITE = 'WHITE',
  MIXED = 'MIXED',
  OTHER = 'OTHER',
  RATHER_NOT_SAY = 'RATHER_NOT_SAY',
}

export enum Diet {
  OMNIVORE = 'OMNIVORE',
  VEGETARIAN = 'VEGETARIAN',
  VEGAN = 'VEGAN',
  PESCATARIAN = 'PESCATARIAN',
  KOSHER = 'KOSHER',
  HALAL = 'HALAL',
  GLUTEN_FREE = 'GLUTEN_FREE',
  OTHER = 'OTHER',
  RATHER_NOT_SAY = 'RATHER_NOT_SAY',
}

export enum PetStatus {
  HAS_PETS = 'HAS_PETS',
  HAS_DOGS = 'HAS_DOGS',
  HAS_CATS = 'HAS_CATS',
  HAS_OTHER_PETS = 'HAS_OTHER_PETS',
  NO_PETS_LIKES_THEM = 'NO_PETS_LIKES_THEM',
  NO_PETS_DOESNT_LIKE = 'NO_PETS_DOESNT_LIKE',
  ALLERGIC = 'ALLERGIC',
  RATHER_NOT_SAY = 'RATHER_NOT_SAY',
}

export enum EducationLevel {
  HIGH_SCHOOL = 'HIGH_SCHOOL',
  SOME_COLLEGE = 'SOME_COLLEGE',
  TRADE_SCHOOL = 'TRADE_SCHOOL',
  ASSOCIATES = 'ASSOCIATES',
  BACHELORS = 'BACHELORS',
  MASTERS = 'MASTERS',
  DOCTORATE = 'DOCTORATE',
  OTHER = 'OTHER',
  RATHER_NOT_SAY = 'RATHER_NOT_SAY',
}

export enum ZodiacSign {
  ARIES = 'ARIES',
  TAURUS = 'TAURUS',
  GEMINI = 'GEMINI',
  CANCER = 'CANCER',
  LEO = 'LEO',
  VIRGO = 'VIRGO',
  LIBRA = 'LIBRA',
  SCORPIO = 'SCORPIO',
  SAGITTARIUS = 'SAGITTARIUS',
  CAPRICORN = 'CAPRICORN',
  AQUARIUS = 'AQUARIUS',
  PISCES = 'PISCES',
}

export enum ResponseRate {
  REPLIES_OFTEN = 'REPLIES_OFTEN',
  REPLIES_SELECTIVELY = 'REPLIES_SELECTIVELY',
  REPLIES_VERY_SELECTIVELY = 'REPLIES_VERY_SELECTIVELY',
}

export enum IncomeLevel {
  LESS_THAN_20K = 'LESS_THAN_20K',
  INCOME_20K_40K = 'INCOME_20K_40K',
  INCOME_40K_60K = 'INCOME_40K_60K',
  INCOME_60K_80K = 'INCOME_60K_80K',
  INCOME_80K_100K = 'INCOME_80K_100K',
  INCOME_100K_150K = 'INCOME_100K_150K',
  INCOME_150K_250K = 'INCOME_150K_250K',
  INCOME_250K_500K = 'INCOME_250K_500K',
  INCOME_500K_PLUS = 'INCOME_500K_PLUS',
  RATHER_NOT_SAY = 'RATHER_NOT_SAY',
}

// UserPoliticalAssessment - Economic/Political Gating System
// Matches: src/main/java/com/nonononoki/alovoa/entity/user/UserPoliticalAssessment.java
export interface UserPoliticalAssessmentFull {
  id?: number;
  userId?: number;

  // Economic Classification
  incomeBracket?: IncomeBracket;
  primaryIncomeSource?: IncomeSource;
  wealthBracket?: WealthBracket;
  ownsRentalProperties?: boolean;
  employsOthers?: boolean;
  livesOffCapital?: boolean;
  economicClass?: EconomicClass;

  // Political/Economic Beliefs (1-5 scale)
  politicalOrientation?: PoliticalOrientation;
  wealthRedistributionView?: number;
  workerOwnershipView?: number;
  universalServicesView?: number;
  housingRightsView?: number;
  billionaireExistenceView?: number;
  meritocracyBeliefView?: number;
  wealthContributionView?: WealthContributionView;
  economicValuesScore?: number; // 0-100

  // Reproductive Rights (Male users)
  reproductiveRightsView?: ReproductiveRightsView;
  vasectomyStatus?: VasectomyStatus;
  vasectomyVerificationUrl?: string;
  vasectomyVerifiedAt?: Date;
  acknowledgedVasectomyRequirement?: boolean;
  frozenSpermStatus?: FrozenSpermStatus;
  frozenSpermVerificationUrl?: string;
  frozenSpermVerifiedAt?: Date;
  wantsKids?: boolean;

  // Gating & Class Consciousness
  gateStatus?: GateStatus;
  rejectionReason?: GateRejectionReason;
  conservativeExplanation?: string;
  explanationReviewed?: boolean;
  reviewNotes?: string;
  classConsciousnessScore?: number; // 0-100
  policyClassAnalysisScore?: number;
  laborHistoryScore?: number;

  // Timestamps
  createdAt?: Date;
  assessmentCompletedAt?: Date;
  lastUpdatedAt?: Date;
  assessmentVersion?: number;
}

// Political Assessment Enums
export enum IncomeBracket {
  LESS_THAN_25K = 'LESS_THAN_25K',
  INCOME_25K_50K = 'INCOME_25K_50K',
  INCOME_50K_75K = 'INCOME_50K_75K',
  INCOME_75K_100K = 'INCOME_75K_100K',
  INCOME_100K_150K = 'INCOME_100K_150K',
  INCOME_150K_250K = 'INCOME_150K_250K',
  INCOME_250K_500K = 'INCOME_250K_500K',
  INCOME_500K_1M = 'INCOME_500K_1M',
  INCOME_1M_PLUS = 'INCOME_1M_PLUS',
}

export enum IncomeSource {
  WAGES_SALARY = 'WAGES_SALARY',
  SELF_EMPLOYED_SOLO = 'SELF_EMPLOYED_SOLO',
  BUSINESS_OWNER = 'BUSINESS_OWNER',
  INVESTMENTS_DIVIDENDS = 'INVESTMENTS_DIVIDENDS',
  RENTAL_INCOME = 'RENTAL_INCOME',
  INHERITANCE_TRUST = 'INHERITANCE_TRUST',
  MULTIPLE_SOURCES = 'MULTIPLE_SOURCES',
  UNEMPLOYED_STUDENT = 'UNEMPLOYED_STUDENT',
  RETIRED = 'RETIRED',
}

export enum WealthBracket {
  NEGATIVE = 'NEGATIVE',
  LESS_THAN_10K = 'LESS_THAN_10K',
  WEALTH_10K_50K = 'WEALTH_10K_50K',
  WEALTH_50K_100K = 'WEALTH_50K_100K',
  WEALTH_100K_250K = 'WEALTH_100K_250K',
  WEALTH_250K_500K = 'WEALTH_250K_500K',
  WEALTH_500K_1M = 'WEALTH_500K_1M',
  WEALTH_1M_5M = 'WEALTH_1M_5M',
  WEALTH_5M_10M = 'WEALTH_5M_10M',
  WEALTH_10M_PLUS = 'WEALTH_10M_PLUS',
}

export enum EconomicClass {
  WORKING_CLASS = 'WORKING_CLASS',
  PROFESSIONAL_CLASS = 'PROFESSIONAL_CLASS',
  SMALL_BUSINESS_OWNER = 'SMALL_BUSINESS_OWNER',
  PETITE_BOURGEOISIE = 'PETITE_BOURGEOISIE',
  CAPITAL_CLASS = 'CAPITAL_CLASS',
}

export enum PoliticalOrientation {
  SOCIALIST = 'SOCIALIST',
  PROGRESSIVE = 'PROGRESSIVE',
  LIBERAL = 'LIBERAL',
  MODERATE = 'MODERATE',
  CONSERVATIVE = 'CONSERVATIVE',
  LIBERTARIAN = 'LIBERTARIAN',
  APOLITICAL = 'APOLITICAL',
  OTHER = 'OTHER',
}

export enum WealthContributionView {
  LABOR_CREATES_ALL = 'LABOR_CREATES_ALL',
  MOSTLY_LABOR = 'MOSTLY_LABOR',
  SHARED = 'SHARED',
  MOSTLY_CAPITAL = 'MOSTLY_CAPITAL',
  CAPITAL_CREATES_ALL = 'CAPITAL_CREATES_ALL',
}

export enum ReproductiveRightsView {
  FULL_BODILY_AUTONOMY = 'FULL_BODILY_AUTONOMY',
  SENTIENCE_BASED = 'SENTIENCE_BASED',
  SOME_RESTRICTIONS_OK = 'SOME_RESTRICTIONS_OK',
  FORCED_BIRTH = 'FORCED_BIRTH',
  UNDECIDED = 'UNDECIDED',
  PREFER_NOT_TO_SAY = 'PREFER_NOT_TO_SAY',
}

export enum VasectomyStatus {
  NOT_APPLICABLE = 'NOT_APPLICABLE',
  NOT_VERIFIED = 'NOT_VERIFIED',
  VERIFICATION_PENDING = 'VERIFICATION_PENDING',
  VERIFIED = 'VERIFIED',
  DECLINED = 'DECLINED',
}

export enum FrozenSpermStatus {
  NOT_APPLICABLE = 'NOT_APPLICABLE',
  NOT_PROVIDED = 'NOT_PROVIDED',
  VERIFICATION_PENDING = 'VERIFICATION_PENDING',
  VERIFIED = 'VERIFIED',
  NOT_NEEDED = 'NOT_NEEDED',
}

export enum GateStatus {
  PENDING_ASSESSMENT = 'PENDING_ASSESSMENT',
  APPROVED = 'APPROVED',
  PENDING_EXPLANATION = 'PENDING_EXPLANATION',
  PENDING_VASECTOMY = 'PENDING_VASECTOMY',
  REJECTED = 'REJECTED',
  REDIRECT_RAYA = 'REDIRECT_RAYA',
  UNDER_REVIEW = 'UNDER_REVIEW',
}

export enum GateRejectionReason {
  CAPITAL_CLASS = 'CAPITAL_CLASS',
  ANTI_WORKER = 'ANTI_WORKER',
  FORCED_BIRTH = 'FORCED_BIRTH',
  INADEQUATE_EXPLANATION = 'INADEQUATE_EXPLANATION',
  OTHER = 'OTHER',
}

// UserReputationScore - Trust & Behavior Metrics
// Matches: src/main/java/com/nonononoki/alovoa/entity/user/UserReputationScore.java
export interface UserReputationScoreFull {
  id?: number;
  userId?: number;

  // Reputation Scores (0-100, default 50)
  responseQuality?: number;
  respectScore?: number;
  authenticityScore?: number;
  investmentScore?: number;

  // Behavior Tracking
  ghostingCount?: number;
  reportsReceived?: number;
  reportsUpheld?: number;
  datesCompleted?: number;
  positiveFeedbackCount?: number;

  // Trust Level
  trustLevel?: TrustLevelJava;
  updatedAt?: Date;
}

export enum TrustLevelJava {
  NEW_MEMBER = 'NEW_MEMBER',
  VERIFIED = 'VERIFIED',
  TRUSTED = 'TRUSTED',
  HIGHLY_TRUSTED = 'HIGHLY_TRUSTED',
  UNDER_REVIEW = 'UNDER_REVIEW',
  RESTRICTED = 'RESTRICTED',
}

// UserPersonalityProfile - Big Five & Attachment Assessment
// Matches: src/main/java/com/nonononoki/alovoa/entity/user/UserPersonalityProfile.java
export interface UserPersonalityProfile {
  id?: number;
  userId?: number;

  // Big Five Scores (0-100)
  openness?: number;
  conscientiousness?: number;
  extraversion?: number;
  agreeableness?: number;
  neuroticism?: number;

  // Attachment & Communication
  attachmentStyle?: AttachmentStyleJava;
  attachmentConfidence?: number;
  communicationDirectness?: number; // 0-100
  communicationEmotional?: number; // 0-100

  // Embeddings & Assessment
  personalityEmbeddingId?: string;
  valuesEmbeddingId?: string;
  interestsEmbeddingId?: string;
  valuesAnswers?: string; // JSON of raw answers
  assessmentCompletedAt?: Date;
  updatedAt?: Date;
  assessmentVersion?: number;
}

export enum AttachmentStyleJava {
  SECURE = 'SECURE',
  ANXIOUS = 'ANXIOUS',
  AVOIDANT = 'AVOIDANT',
  DISORGANIZED = 'DISORGANIZED',
}

// UserVideoVerification - Liveness & Deepfake Detection
// Matches: src/main/java/com/nonononoki/alovoa/entity/user/UserVideoVerification.java
export interface UserVideoVerificationFull {
  id?: number;
  userId?: number;
  uuid?: string;
  videoUrl?: string;
  status?: VerificationStatusJava;
  faceMatchScore?: number;
  livenessScore?: number;
  deepfakeScore?: number;
  verifiedAt?: Date;
  createdAt?: Date;
  failureReason?: string;
  sessionId?: string;
  captureMetadata?: string; // JSON (MIME type, duration, resolution, timestamps)
}

export enum VerificationStatusJava {
  PENDING = 'PENDING',
  PROCESSING = 'PROCESSING',
  VERIFIED = 'VERIFIED',
  FAILED = 'FAILED',
  EXPIRED = 'EXPIRED',
}

// UserDates - Birth Date & Activity Tracking
// Matches: src/main/java/com/nonononoki/alovoa/entity/user/UserDates.java
export interface UserDates {
  dateOfBirth?: Date;
  activeDate?: Date;
  creationDate?: Date;
  intentionChangeDate?: Date;
  notificationDate?: Date;
  notificationCheckedDate?: Date;
  messageDate?: Date;
  messageCheckedDate?: Date;
  latestDonationDate?: Date;
}

// UserRelationship - Facebook-style Relationship Links
// Matches: src/main/java/com/nonononoki/alovoa/entity/UserRelationship.java
export interface UserRelationship {
  id?: number;
  uuid?: string;
  user1Id?: number;
  user2Id?: number;
  type?: RelationshipType;
  status?: RelationshipStatus;
  createdAt?: Date;
  confirmedAt?: Date;
  anniversaryDate?: Date;
  isPublic?: boolean;
}

export enum RelationshipType {
  DATING = 'DATING',
  IN_A_RELATIONSHIP = 'IN_A_RELATIONSHIP',
  ENGAGED = 'ENGAGED',
  MARRIED = 'MARRIED',
  DOMESTIC_PARTNERSHIP = 'DOMESTIC_PARTNERSHIP',
  CIVIL_UNION = 'CIVIL_UNION',
  ITS_COMPLICATED = 'ITS_COMPLICATED',
  OPEN_RELATIONSHIP = 'OPEN_RELATIONSHIP',
}

export enum RelationshipStatus {
  PENDING = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  DECLINED = 'DECLINED',
  ENDED = 'ENDED',
}

// UserLocationPreferences - Privacy-Safe Location Matching
// Matches: src/main/java/com/nonononoki/alovoa/entity/user/UserLocationPreferences.java
export interface UserLocationPreferences {
  id?: number;
  userId?: number;
  maxTravelMinutes?: number; // default 30
  requireAreaOverlap?: boolean;
  showExceptionalMatches?: boolean;
  exceptionalMatchThreshold?: number; // default 0.90
  movingToCity?: string;
  movingToState?: string;
  movingDate?: Date;
  createdAt?: Date;
  updatedAt?: Date;
}

// UserVideo - Profile Videos
// Matches: src/main/java/com/nonononoki/alovoa/entity/user/UserVideo.java
export interface UserVideo {
  id?: number;
  uuid?: string;
  userId?: number;
  videoType?: VideoType;
  videoUrl?: string;
  thumbnailUrl?: string;
  durationSeconds?: number;
  transcript?: string;
  sentimentScores?: string; // JSON
  isIntro?: boolean;
  isVerified?: boolean;
  createdAt?: Date;
}

export enum VideoType {
  INTRO = 'INTRO',
  DAY_IN_LIFE = 'DAY_IN_LIFE',
  HOBBY = 'HOBBY',
  RESPONSE = 'RESPONSE',
  VERIFICATION = 'VERIFICATION',
}

// Donation Tier from User.java
export enum DonationTier {
  NONE = 'NONE',
  SUPPORTER = 'SUPPORTER',
  BELIEVER = 'BELIEVER',
  BUILDER = 'BUILDER',
  FOUNDING_MEMBER = 'FOUNDING_MEMBER',
}

export {}
