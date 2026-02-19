/**
 * Mock Users for AURA Platform Testing
 * Includes comprehensive profiles with all AURA features
 */

import type {
  UserDto,
  Gender,
  UserIntention,
  UserInterest,
  UserImage,
  UserMiscInfo,
  UserSettings,
  UserPrompt,
  UserDtoVerificationPicture,
  UserProfileDetails,
  UserPersonalityProfile,
  UserPoliticalAssessmentFull,
  UserReputationScoreFull,
  UserVideoVerificationFull,
  UserLocationPreferences,
} from '../myTypes';

// Use string literals instead of importing enums to avoid circular dependency issues
// These match the enum values in myTypes.ts

// Helper to generate a mock profile picture placeholder URL
// Using a simple color-based placeholder that doesn't require base64
const generateMockProfilePicture = (seed: string): string => {
  // Use a placeholder service or simple colored image
  const color = seed.slice(0, 6).padStart(6, 'A').replace(/[^0-9A-Fa-f]/g, 'A');
  const initial = seed.charAt(0).toUpperCase();
  // Using ui-avatars.com for placeholder images (works without base64)
  return `https://ui-avatars.com/api/?name=${initial}&background=${color}&color=fff&size=200`;
};

// Base mock user template
const createMockUser = (overrides: Partial<UserDto>): UserDto => ({
  uuid: 'mock-user-' + Math.random().toString(36).substr(2, 9),
  idEncoded: overrides.uuid || 'mock-encoded-id',
  firstName: 'MockUser',
  age: 28,
  donationAmount: 0,
  gender: { id: 2, text: 'Female' }, // 2 = 2
  hasAudio: false,
  audio: '',
  units: 0,
  preferedMinAge: 21,
  preferedMaxAge: 40,
  miscInfos: [],
  preferedGenders: [{ id: 1, text: 'Male' }], // 1 = 1
  intention: { id: 2, text: 'Dating' }, // IntentionEnum.DATE = 2
  interests: [],
  profilePicture: generateMockProfilePicture('default'),
  images: [],
  description: 'A mock user for testing',
  country: 'US',
  distanceToUser: 5,
  commonInterests: [],
  totalDonations: 0,
  numBlockedByUsers: 0,
  numReports: 0,
  blockedByCurrentUser: false,
  reportedByCurrentUser: false,
  likesCurrentUser: false,
  likedByCurrentUser: false,
  hiddenByCurrentUser: false,
  numberReferred: 0,
  compatible: true,
  hasLocation: true,
  locationLatitude: 37.7749,
  locationLongitude: -122.4194,
  lastActiveState: 0,
  userSettings: { emailLike: true, emailChat: true },
  prompts: [],
  verificationPicture: {
    verifiedByAdmin: false,
    verifiedByUsers: false,
    votedByCurrentUser: false,
    hasPicture: false,
    data: '',
    text: '',
    uuid: '',
    userYes: 0,
    userNo: 0,
  },
  verified: false,
  locationName: 'San Francisco, CA',
  ...overrides,
});

// ============================================
// TEST USER - BYPASS LOGIN
// Email: test@aura.dev / Password: aura123
// ============================================
export const MOCK_CURRENT_USER: UserDto = createMockUser({
  uuid: 'test-user-current-001',
  idEncoded: 'test-user-current-001',
  email: 'test@aura.dev',
  firstName: 'TestUser',
  age: 30,
  gender: { id: 1, text: 'Male' }, // 1 = 1
  preferedGenders: [
    { id: 2, text: 'Female' }, // 2 = 2
    { id: 3, text: 'Non-binary' }, // 3 = 3
  ],
  profilePicture: generateMockProfilePicture('TestAA'),
  description: 'AURA test account with full access to all features. Use this to explore the platform!',
  interests: [
    { text: 'Technology' },
    { text: 'Music' },
    { text: 'Travel' },
    { text: 'Hiking' },
    { text: 'Coffee' },
  ],
  miscInfos: [
    { id: 11, value: 1 }, // RELATIONSHIP_SINGLE = 11
    { id: 21, value: 1 }, // KIDS_NO = 21
    { id: 42, value: 1 }, // DRUGS_ALCOHOL_SOMETIMES = 42
    { id: 72, value: 1 }, // POLITICS_LEFT = 72
  ],
  prompts: [
    { promptId: 1, text: "I'm testing all the AURA features!" },
    { promptId: 2, text: 'Looking for someone who loves adventure and deep conversations.' },
  ],
  verified: true,
  donationTier: 'BUILDER' as any,
  totalDonations: 50,

  // Full AURA Profile
  profileDetails: {
    heightCm: 180,
    bodyType: 'FIT' as any,
    ethnicity: 'MIXED' as any,
    diet: 'OMNIVORE' as any,
    pets: 'HAS_DOGS' as any,
    petDetails: 'Golden Retriever named Max',
    education: 'MASTERS' as any,
    occupation: 'Software Engineer',
    employer: 'Tech Startup',
    languages: 'English, Spanish',
    zodiacSign: 'AQUARIUS' as any,
    responseRate: 'REPLIES_OFTEN' as any,
    income: 'INCOME_100K_150K' as any,
  },

  personalityProfile: {
    openness: 85,
    conscientiousness: 70,
    extraversion: 65,
    agreeableness: 75,
    neuroticism: 35,
    attachmentStyle: 'SECURE' as any,
    attachmentConfidence: 0.85,
    communicationDirectness: 70,
    communicationEmotional: 60,
    assessmentCompletedAt: new Date(),
  },

  politicalAssessment: {
    incomeBracket: 'INCOME_100K_150K' as any,
    primaryIncomeSource: 'WAGES_SALARY' as any,
    wealthBracket: 'WEALTH_100K_250K' as any,
    ownsRentalProperties: false,
    employsOthers: false,
    livesOffCapital: false,
    economicClass: 'PROFESSIONAL_CLASS' as any,
    politicalOrientation: 'PROGRESSIVE' as any,
    wealthRedistributionView: 4,
    workerOwnershipView: 4,
    universalServicesView: 5,
    housingRightsView: 4,
    billionaireExistenceView: 2,
    meritocracyBeliefView: 2,
    wealthContributionView: 'MOSTLY_LABOR' as any,
    economicValuesScore: 78,
    reproductiveRightsView: 'FULL_BODILY_AUTONOMY' as any,
    vasectomyStatus: 'NOT_APPLICABLE' as any,
    wantsKids: true,
    gateStatus: 'APPROVED' as any,
    classConsciousnessScore: 82,
    assessmentCompletedAt: new Date(),
  },

  reputationScore: {
    responseQuality: 90,
    respectScore: 95,
    authenticityScore: 100,
    investmentScore: 85,
    ghostingCount: 0,
    reportsReceived: 0,
    reportsUpheld: 0,
    datesCompleted: 5,
    positiveFeedbackCount: 4,
    trustLevel: 'TRUSTED' as any,
  },

  videoVerification: {
    status: 'VERIFIED' as any,
    faceMatchScore: 0.98,
    livenessScore: 0.99,
    deepfakeScore: 0.02,
    verifiedAt: new Date(),
  },

  locationPreferences: {
    maxTravelMinutes: 45,
    requireAreaOverlap: false,
    showExceptionalMatches: true,
    exceptionalMatchThreshold: 0.85,
  },
});

// ============================================
// MOCK MATCHES - Diverse profiles for testing
// ============================================

export const MOCK_USERS: UserDto[] = [
  // High compatibility match
  createMockUser({
    uuid: 'match-emma-001',
    firstName: 'Emma',
    age: 28,
    gender: { id: 2, text: 'Female' }, // 2 = 2
    profilePicture: generateMockProfilePicture('EmmaBC'),
    description: 'Artist and coffee enthusiast. Love hiking on weekends and trying new restaurants. Looking for genuine connections.',
    distanceToUser: 3,
    verified: true,
    interests: [
      { text: 'Art' },
      { text: 'Coffee' },
      { text: 'Hiking' },
      { text: 'Travel' },
      { text: 'Photography' },
    ],
    profileDetails: {
      heightCm: 165,
      bodyType: "AVERAGE" as any,
      ethnicity: "WHITE" as any,
      diet: "VEGETARIAN" as any,
      pets: "HAS_CATS" as any,
      education: "BACHELORS" as any,
      occupation: 'Graphic Designer',
      zodiacSign: "PISCES" as any,
      responseRate: "REPLIES_OFTEN" as any,
    },
    personalityProfile: {
      openness: 90,
      conscientiousness: 65,
      extraversion: 55,
      agreeableness: 80,
      neuroticism: 40,
      attachmentStyle: "SECURE" as any,
      attachmentConfidence: 0.82,
    },
    politicalAssessment: {
      politicalOrientation: "PROGRESSIVE" as any,
      gateStatus: "APPROVED" as any,
      economicValuesScore: 75,
    },
    reputationScore: {
      trustLevel: "TRUSTED" as any,
      responseQuality: 88,
      respectScore: 92,
    },
  }),

  // Medium compatibility
  createMockUser({
    uuid: 'match-sophia-002',
    firstName: 'Sophia',
    age: 26,
    gender: { id: 2, text: 'Female' },
    profilePicture: generateMockProfilePicture('SophDE'),
    description: 'Yoga instructor and wellness advocate. Passionate about mindfulness and healthy living. Let\'s grab smoothies!',
    distanceToUser: 8,
    verified: true,
    interests: [
      { text: 'Yoga' },
      { text: 'Meditation' },
      { text: 'Cooking' },
      { text: 'Nature' },
      { text: 'Music' },
    ],
    profileDetails: {
      heightCm: 170,
      bodyType: "FIT" as any,
      diet: "VEGAN" as any,
      pets: "NO_PETS_LIKES_THEM" as any,
      education: "BACHELORS" as any,
      occupation: 'Yoga Instructor',
      zodiacSign: "VIRGO" as any,
      responseRate: "REPLIES_SELECTIVELY" as any,
    },
    personalityProfile: {
      openness: 75,
      conscientiousness: 80,
      extraversion: 70,
      agreeableness: 85,
      neuroticism: 25,
      attachmentStyle: "SECURE" as any,
    },
    politicalAssessment: {
      politicalOrientation: "LIBERAL" as any,
      gateStatus: "APPROVED" as any,
    },
  }),

  // Another high match
  createMockUser({
    uuid: 'match-olivia-003',
    firstName: 'Olivia',
    age: 31,
    gender: { id: 2, text: 'Female' },
    profilePicture: generateMockProfilePicture('OlivFG'),
    description: 'Software engineer by day, amateur chef by night. Love board games, sci-fi, and spontaneous road trips.',
    distanceToUser: 12,
    verified: true,
    interests: [
      { text: 'Technology' },
      { text: 'Cooking' },
      { text: 'Board Games' },
      { text: 'Sci-Fi' },
      { text: 'Travel' },
    ],
    commonInterests: [
      { text: 'Technology' },
      { text: 'Travel' },
    ],
    profileDetails: {
      heightCm: 168,
      bodyType: "AVERAGE" as any,
      ethnicity: "ASIAN" as any,
      diet: "OMNIVORE" as any,
      pets: "HAS_CATS" as any,
      education: "MASTERS" as any,
      occupation: 'Backend Engineer',
      employer: 'Google',
      zodiacSign: "SCORPIO" as any,
      responseRate: "REPLIES_OFTEN" as any,
      income: "INCOME_150K_250K" as any,
    },
    personalityProfile: {
      openness: 82,
      conscientiousness: 88,
      extraversion: 50,
      agreeableness: 70,
      neuroticism: 38,
      attachmentStyle: "SECURE" as any,
    },
    politicalAssessment: {
      politicalOrientation: "PROGRESSIVE" as any,
      gateStatus: "APPROVED" as any,
      economicValuesScore: 80,
    },
    reputationScore: {
      trustLevel: "HIGHLY_TRUSTED" as any,
      responseQuality: 95,
      respectScore: 98,
      datesCompleted: 8,
    },
  }),

  // Non-binary match
  createMockUser({
    uuid: 'match-alex-004',
    firstName: 'Alex',
    age: 27,
    gender: { id: 3, text: 'Non-binary' },
    profilePicture: generateMockProfilePicture('AlexHI'),
    description: 'Musician and barista. They/them. Looking for someone who appreciates live music and late-night conversations.',
    distanceToUser: 5,
    verified: true,
    interests: [
      { text: 'Music' },
      { text: 'Coffee' },
      { text: 'Poetry' },
      { text: 'Film' },
      { text: 'Art' },
    ],
    profileDetails: {
      heightCm: 175,
      bodyType: "THIN" as any,
      diet: "VEGETARIAN" as any,
      pets: "NO_PETS_LIKES_THEM" as any,
      education: "SOME_COLLEGE" as any,
      occupation: 'Barista / Musician',
      zodiacSign: "GEMINI" as any,
    },
    personalityProfile: {
      openness: 95,
      conscientiousness: 55,
      extraversion: 75,
      agreeableness: 80,
      neuroticism: 45,
      attachmentStyle: "ANXIOUS" as any,
    },
    politicalAssessment: {
      politicalOrientation: "SOCIALIST" as any,
      gateStatus: "APPROVED" as any,
      economicValuesScore: 92,
    },
  }),

  // New user (unverified)
  createMockUser({
    uuid: 'match-mia-005',
    firstName: 'Mia',
    age: 25,
    gender: { id: 2, text: 'Female' },
    profilePicture: generateMockProfilePicture('MiaJKL'),
    description: 'New to the app! Love dancing, trying new foods, and meeting interesting people.',
    distanceToUser: 15,
    verified: false,
    interests: [
      { text: 'Dancing' },
      { text: 'Food' },
      { text: 'Movies' },
    ],
    profileDetails: {
      heightCm: 162,
      bodyType: "CURVY" as any,
      diet: "OMNIVORE" as any,
      education: "BACHELORS" as any,
      occupation: 'Marketing Coordinator',
    },
    personalityProfile: {
      openness: 70,
      conscientiousness: 60,
      extraversion: 85,
      agreeableness: 75,
      neuroticism: 50,
      attachmentStyle: "ANXIOUS" as any,
    },
    reputationScore: {
      trustLevel: "NEW_MEMBER" as any,
    },
  }),

  // Someone who likes current user
  createMockUser({
    uuid: 'match-isabella-006',
    firstName: 'Isabella',
    age: 29,
    gender: { id: 2, text: 'Female' },
    profilePicture: generateMockProfilePicture('IsabMN'),
    description: 'Writer and bookworm. Always looking for my next adventure, whether in the pages of a book or exploring a new city.',
    distanceToUser: 7,
    verified: true,
    likesCurrentUser: true,
    interests: [
      { text: 'Reading' },
      { text: 'Writing' },
      { text: 'Travel' },
      { text: 'Coffee' },
      { text: 'Museums' },
    ],
    commonInterests: [
      { text: 'Travel' },
      { text: 'Coffee' },
    ],
    profileDetails: {
      heightCm: 167,
      bodyType: "AVERAGE" as any,
      ethnicity: "HISPANIC_LATINO" as any,
      diet: "PESCATARIAN" as any,
      pets: "HAS_CATS" as any,
      petDetails: 'Two cats named Luna and Sol',
      education: "MASTERS" as any,
      occupation: 'Content Writer',
      zodiacSign: "LIBRA" as any,
      responseRate: "REPLIES_OFTEN" as any,
    },
    personalityProfile: {
      openness: 88,
      conscientiousness: 72,
      extraversion: 48,
      agreeableness: 82,
      neuroticism: 42,
      attachmentStyle: "SECURE" as any,
    },
    politicalAssessment: {
      politicalOrientation: "PROGRESSIVE" as any,
      gateStatus: "APPROVED" as any,
      economicValuesScore: 76,
    },
    reputationScore: {
      trustLevel: "TRUSTED" as any,
      responseQuality: 90,
      respectScore: 94,
    },
  }),

  // User you've matched with (mutual like)
  createMockUser({
    uuid: 'match-ava-007',
    firstName: 'Ava',
    age: 27,
    gender: { id: 2, text: 'Female' },
    profilePicture: generateMockProfilePicture('AvaOPQ'),
    description: 'Product designer with a passion for UX. Love hiking, craft beer, and board game nights.',
    distanceToUser: 4,
    verified: true,
    likesCurrentUser: true,
    likedByCurrentUser: true,
    interests: [
      { text: 'Design' },
      { text: 'Hiking' },
      { text: 'Board Games' },
      { text: 'Beer' },
      { text: 'Technology' },
    ],
    commonInterests: [
      { text: 'Hiking' },
      { text: 'Technology' },
    ],
    profileDetails: {
      heightCm: 170,
      bodyType: "FIT" as any,
      ethnicity: "WHITE" as any,
      diet: "OMNIVORE" as any,
      pets: "HAS_DOGS" as any,
      education: "BACHELORS" as any,
      occupation: 'Product Designer',
      employer: 'Startup',
      zodiacSign: "LEO" as any,
      responseRate: "REPLIES_OFTEN" as any,
      income: "INCOME_80K_100K" as any,
    },
    personalityProfile: {
      openness: 80,
      conscientiousness: 78,
      extraversion: 68,
      agreeableness: 75,
      neuroticism: 30,
      attachmentStyle: "SECURE" as any,
    },
    politicalAssessment: {
      politicalOrientation: "LIBERAL" as any,
      gateStatus: "APPROVED" as any,
    },
    reputationScore: {
      trustLevel: "TRUSTED" as any,
      datesCompleted: 3,
      positiveFeedbackCount: 3,
    },
  }),

  // Diverse distance range
  createMockUser({
    uuid: 'match-luna-008',
    firstName: 'Luna',
    age: 32,
    gender: { id: 2, text: 'Female' },
    profilePicture: generateMockProfilePicture('LunaRS'),
    description: 'Scientist by profession, adventurer by heart. Looking for someone to share experiments and explorations with.',
    distanceToUser: 25,
    verified: true,
    interests: [
      { text: 'Science' },
      { text: 'Hiking' },
      { text: 'Photography' },
      { text: 'Cooking' },
    ],
    profileDetails: {
      heightCm: 165,
      bodyType: "AVERAGE" as any,
      diet: "OMNIVORE" as any,
      education: "DOCTORATE" as any,
      occupation: 'Research Scientist',
      zodiacSign: "CAPRICORN" as any,
    },
    personalityProfile: {
      openness: 92,
      conscientiousness: 90,
      extraversion: 45,
      agreeableness: 70,
      neuroticism: 35,
      attachmentStyle: "AVOIDANT" as any,
    },
    politicalAssessment: {
      politicalOrientation: "PROGRESSIVE" as any,
      gateStatus: "APPROVED" as any,
    },
  }),
];

// Export map for quick lookup
export const MOCK_USERS_MAP = new Map<string, UserDto>(
  [MOCK_CURRENT_USER, ...MOCK_USERS].map(u => [u.uuid, u])
);

// Bypass credentials
export const BYPASS_CREDENTIALS = {
  email: 'test@aura.dev',
  password: 'aura123',
};

export default {
  MOCK_CURRENT_USER,
  MOCK_USERS,
  MOCK_USERS_MAP,
  BYPASS_CREDENTIALS,
};
