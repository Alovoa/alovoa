//export const DOMAIN : string = "http://localhost:8080"
//export const DOMAIN : string = "https://beta.alovoa.com"
export const DOMAIN : string = "https://alovoa.com"

export const PATH_BOOLEAN_TRUE = "true"
export const PATH_BOOLEAN_FALSE = "false"

export const IMPRINT = DOMAIN + "/imprint"
export const PRIVACY = DOMAIN + "/privacy"
export const TOS = DOMAIN + "/tos"
export const DONATE_LIST = DOMAIN + "/donate-list"

export const AUTH_LOGIN = DOMAIN + "/login"
export const AUTH_LOGOUT = DOMAIN + "/logout"
export const AUTH_LOGIN_ERROR = DOMAIN + "/?auth-error"

export const AUTH_GOOGLE = DOMAIN + "/oauth2/authorization/google"
export const AUTH_FACEBOOK = DOMAIN + "/oauth2/authorization/facebook"
export const AUTH_COOKIE = DOMAIN + "/oauth2/remember-me-cookie/%s/%s"

export const API_RESOURCE_YOUR_PROFILE = DOMAIN + "/api/v1/resource/profile"
export const API_RESOURCE_PROFILE = DOMAIN + "/api/v1/resource/profile/view/%s"
export const API_RESOURCE_SEARCH = DOMAIN + "/api/v1/resource/search"
export const API_RESOURCE_ALERTS = DOMAIN + "/api/v1/resource/alerts"
export const API_RESOURCE_CHATS = DOMAIN + "/api/v1/resource/chats"
export const API_RESOURCE_CHATS_DETAIL = DOMAIN + "/api/v1/resource/chats/%s"
export const API_RESOURCE_DONATE = DOMAIN + "/api/v1/resource/donate"
export const API_RESOURCE_USER_ONBOARDING = DOMAIN + "/api/v1/resource/user/onboarding"
export const API_RESOURCE_USER_BLOCKED = DOMAIN + "/api/v1/resource/blocked-users"
export const API_RESOURCE_USER_LIKED = DOMAIN + "/api/v1/resource/liked-users"
export const API_RESOURCE_USER_HIDDEN = DOMAIN + "/api/v1/resource/disliked-users"
export const API_SEARCH = DOMAIN + "/api/v1/search/users"
export const API_DONATE_RECENT = DOMAIN + "/api/v1/donate/recent/%s";
export const API_MESSAGE_UPDATE = DOMAIN + "/api/v1/message/update/%s/%s";

export const CATPCHA_GENERATE = DOMAIN + "/captcha/generate";

export const PASSWORD_RESET = DOMAIN + "/password/reset"

export const REGISTER = DOMAIN + "/register";
export const REGISTER_OAUTH = DOMAIN + "/register-oauth";

export const USER_INTEREST_AUTOCOMPLETE = DOMAIN + "/user/interest/autocomplete/%s";
export const USER_ONBOARDING = DOMAIN + "/user/onboarding";
export const USER_STATUS_ALERT = DOMAIN + "/user/status/new-alert"
export const USER_STATUS_ALERT_LANG = DOMAIN + "/user/status/new-alert/%s"
export const USER_STATUS_MESSAGE = DOMAIN + "/user/status/new-message"

export const USER_UPDATE_PROFILE_PICTURE = DOMAIN + "/user/update/profile-picture"
export const USER_UPDATE_DESCRIPTION = DOMAIN + "/user/update/description"
export const USER_UPDATE_INTENTION = DOMAIN + "/user/update/intention/%s"
export const USER_UPDATE_MIN_AGE = DOMAIN + "/user/update/min-age/%s"
export const USER_UPDATE_MAX_AGE = DOMAIN + "/user/update/max-age/%s"
export const USER_UPDATE_PREFERED_GENDER = DOMAIN + "/user/update/preferedGender/%s/%s"
export const USER_UPDATE_MISC_INFO = DOMAIN + "/user/update/misc-info/%s/%s"
export const USER_ADD_INTEREST = DOMAIN + "/user/interest/add/%s"
export const USER_REMOVE_INTEREST = DOMAIN + "/user/interest/delete/%s"
export const USER_UPDATE_UNITS = DOMAIN + "/user/units/update/%s"
export const USER_USERDATA = DOMAIN + "/user/userdata/%s";
export const USER_DELETE_ACCOUNT = DOMAIN + "/user/delete-account";

export const USER_LIKE = DOMAIN + "/user/like/%s"
export const USER_LIKE_MESSAGE = DOMAIN + "/user/like/%s/%s"
export const USER_HIDE = DOMAIN + "/user/hide/%s"
export const USER_BLOCK = DOMAIN + "/user/block/%s"
export const USER_UNBLOCK = DOMAIN + "/user/unblock/%s"
export const USER_REPORT = DOMAIN + "/user/report/%s"
export const USER_ADD_IMAGE = DOMAIN + "/user/image/add"
export const USER_IMPORT_SOCIAL_IMAGE = DOMAIN + "/user/image/import-social"
export const USER_DELETE_IMAGE = DOMAIN + "/user/image/delete/%s"
export const USER_SOCIAL_CONNECT_START = DOMAIN + "/api/v1/social-connect/start/%s"
export const USER_SOCIAL_CONNECT_STATUS = DOMAIN + "/api/v1/social-connect/session/%s"
export const USER_SOCIAL_CONNECT_ACCOUNTS = DOMAIN + "/api/v1/social-connect/accounts"
export const USER_SOCIAL_CONNECT_UNLINK = DOMAIN + "/api/v1/social-connect/accounts/%s"

export const USER_PROMPT_DELETE = DOMAIN + "/user/prompt/delete/%s"
export const USER_PROMPT_ADD = DOMAIN + "/user/prompt/add"
export const USER_PROMPT_UPDATE = DOMAIN + "/user/prompt/update"

export const USER_SETTING_EMAIL_LIKE = DOMAIN + "/user/settings/emailLike/update/%s"
export const USER_SETTING_EMAIL_CHAT = DOMAIN + "/user/settings/emailChat/update/%s"
export const USER_SETTING_GROWTH_PRIVACY = DOMAIN + "/user/settings/growth-privacy"
export const USER_SETTING_SHARE_GROWTH_PROFILE = DOMAIN + "/user/settings/shareGrowthProfile/update/%s"
export const USER_SETTING_ALLOW_BEHAVIOR_SIGNALS = DOMAIN + "/user/settings/allowBehaviorSignals/update/%s"
export const USER_SETTING_MONTHLY_GROWTH_CHECKINS = DOMAIN + "/user/settings/monthlyGrowthCheckins/update/%s"

export const USER_UPDATE_LOCATION = DOMAIN + "/user/update/location/%s/%s"

export const USER_UPDATE_VERIFICATION_PICTURE = DOMAIN + "/user/update/verification-picture"
export const USER_UPDATE_VERIFICATION_PICTURE_UPVOTE = DOMAIN + "/user/update/verification-picture/upvote/%s"
export const USER_UPDATE_VERIFICATION_PICTURE_DOWNVOTE = DOMAIN + "/user/update/verification-picture/downvote/%s"

export const MESSAGE_SEND = DOMAIN + "/message/send/%s";

// ============================================
// AURA Platform API Endpoints
// ============================================

// Assessment
export const API_ASSESSMENT_QUESTIONS = DOMAIN + "/api/v1/assessment/questions";
export const API_ASSESSMENT_QUESTIONS_CATEGORY = DOMAIN + "/api/v1/assessment/questions/%s";
export const API_ASSESSMENT_QUESTIONS_RANDOM = DOMAIN + "/api/v1/assessment/questions/random/%s";
export const API_ASSESSMENT_ANSWER = DOMAIN + "/api/v1/assessment/answer";
export const API_ASSESSMENT_ANSWER_BULK = DOMAIN + "/api/v1/assessment/answer/bulk";
export const API_ASSESSMENT_PROFILE = DOMAIN + "/api/v1/assessment/profile";
export const API_ASSESSMENT_GROWTH_CONTEXT = DOMAIN + "/api/v1/assessment/growth-context";
export const API_ASSESSMENT_PROGRESS = DOMAIN + "/api/v1/assessment/progress";
export const API_ASSESSMENT_CATEGORIES = DOMAIN + "/api/v1/assessment/categories";
export const API_ASSESSMENT_RECALCULATE = DOMAIN + "/api/v1/assessment/recalculate";

// Political Assessment
export const API_POLITICAL_QUESTIONS = DOMAIN + "/api/v1/political/questions";
export const API_POLITICAL_ANSWER = DOMAIN + "/api/v1/political/answer";
export const API_POLITICAL_PROFILE = DOMAIN + "/api/v1/political/profile";
export const API_POLITICAL_COMPASS = DOMAIN + "/api/v1/political/compass";

// Compatibility / Matching
export const API_MATCHING_MATCHES = DOMAIN + "/api/v1/matching/matches";
export const API_MATCHING_SCORE = DOMAIN + "/api/v1/matching/score/%s";
export const API_MATCHING_BREAKDOWN = DOMAIN + "/api/v1/matching/breakdown/%s";
export const API_MATCHING_DAILY_LIMIT = DOMAIN + "/api/v1/matching/daily-limit";
export const API_MATCHING_REFRESH = DOMAIN + "/api/v1/matching/refresh";
export const API_MATCHING_FILTER = DOMAIN + "/api/v1/matching/filter";
export const API_MATCHING_TOP = DOMAIN + "/api/v1/matching/top/%s";

// Video Verification
export const API_VIDEO_VERIFICATION_STATUS = DOMAIN + "/api/v1/video/verification/status";
export const API_VIDEO_VERIFICATION_START = DOMAIN + "/api/v1/video/verification/start";
export const API_VIDEO_VERIFICATION_UPLOAD = DOMAIN + "/api/v1/video/verification/upload";
export const API_VIDEO_VERIFICATION_CONFIRM = DOMAIN + "/api/v1/video/verification/confirm";
export const API_VIDEO_VERIFICATION_RETRY = DOMAIN + "/api/v1/video/verification/retry";

// Video Introduction
export const API_VIDEO_INTRO_STATUS = DOMAIN + "/api/v1/video/intro/status";
export const API_VIDEO_INTRO_START = DOMAIN + "/api/v1/video/intro/start";
export const API_VIDEO_INTRO_UPLOAD = DOMAIN + "/api/v1/video/intro/upload";
export const API_VIDEO_INTRO_CONFIRM = DOMAIN + "/api/v1/video/intro/confirm";
export const API_VIDEO_INTRO_DELETE = DOMAIN + "/api/v1/video/intro/delete";
export const API_VIDEO_INTRO_PLAYBACK = DOMAIN + "/api/v1/video/intro/playback/%s";
export const API_VIDEO_INTRO_ANALYSIS = DOMAIN + "/api/v1/video/intro/analysis";

// Video-First Display
export const API_VIDEO_FIRST_WATCH = DOMAIN + "/api/video-first/watch/%s";
export const API_VIDEO_FIRST_PROGRESS = DOMAIN + "/api/video-first/watch/%s/progress";
export const API_VIDEO_FIRST_STATUS = DOMAIN + "/api/video-first/status/%s";
export const API_VIDEO_FIRST_REQUIRE = DOMAIN + "/api/video-first/require-video-first";
export const API_VIDEO_FIRST_STATS = DOMAIN + "/api/video-first/stats";

// Profile Coach
export const API_PROFILE_COACH_MESSAGES = DOMAIN + "/api/profile-coach/messages";
export const API_PROFILE_COACH_DISMISS = DOMAIN + "/api/profile-coach/messages/%s/dismiss";
export const API_PROFILE_COACH_STATS = DOMAIN + "/api/profile-coach/stats";
export const API_PROFILE_COACH_TIP = DOMAIN + "/api/profile-coach/tip";

// Exit Velocity (Success Metrics)
export const API_EXIT_VELOCITY_RELATIONSHIP = DOMAIN + "/api/exit-velocity/relationship-formed";
export const API_EXIT_VELOCITY_SURVEY = DOMAIN + "/api/exit-velocity/exit-survey";
export const API_EXIT_VELOCITY_SUMMARY = DOMAIN + "/api/exit-velocity/summary";
export const API_EXIT_VELOCITY_HISTORY = DOMAIN + "/api/exit-velocity/metrics/history";
export const API_EXIT_VELOCITY_STATS = DOMAIN + "/api/exit-velocity/stats";

// Video Dating
export const API_VIDEO_DATE_LIST = DOMAIN + "/api/v1/video-date/list";
export const API_VIDEO_DATE_PROPOSE = DOMAIN + "/api/v1/video-date/propose/%s";
export const API_VIDEO_DATE_ACCEPT = DOMAIN + "/api/v1/video-date/accept/%s";
export const API_VIDEO_DATE_DECLINE = DOMAIN + "/api/v1/video-date/decline/%s";
export const API_VIDEO_DATE_SCHEDULE = DOMAIN + "/api/v1/video-date/schedule/%s";
export const API_VIDEO_DATE_JOIN = DOMAIN + "/api/v1/video-date/join/%s";
export const API_VIDEO_DATE_LEAVE = DOMAIN + "/api/v1/video-date/leave/%s";
export const API_VIDEO_DATE_FEEDBACK = DOMAIN + "/api/v1/video-date/feedback/%s";
export const API_VIDEO_DATE_CANCEL = DOMAIN + "/api/v1/video-date/cancel/%s";

// Match Windows / Calendar
export const API_MATCH_WINDOW_LIST = DOMAIN + "/api/v1/match-window/list";
export const API_MATCH_WINDOW_CURRENT = DOMAIN + "/api/v1/match-window/current";
export const API_MATCH_WINDOW_RESPOND = DOMAIN + "/api/v1/match-window/respond/%s";
export const API_MATCH_WINDOW_SKIP = DOMAIN + "/api/v1/match-window/skip/%s";
export const API_MATCH_WINDOW_INTRO_MESSAGE = DOMAIN + "/api/v1/match-window/%s/intro-message";
export const API_CALENDAR_AVAILABILITY = DOMAIN + "/api/v1/calendar/availability";
export const API_CALENDAR_UPDATE = DOMAIN + "/api/v1/calendar/update";
export const API_CALENDAR_SLOTS = DOMAIN + "/api/v1/calendar/slots/%s";

// Bridge To Real World
export const API_BRIDGE_JOURNEY = DOMAIN + "/api/bridge/journey";
export const API_BRIDGE_MILESTONES = DOMAIN + "/api/bridge/milestones/%s";
export const API_BRIDGE_MILESTONE_RESPOND = DOMAIN + "/api/bridge/milestones/%s/respond";
export const API_BRIDGE_SUGGESTIONS = DOMAIN + "/api/bridge/suggestions/%s";
export const API_BRIDGE_SUGGESTIONS_GENERATE = DOMAIN + "/api/bridge/suggestions/%s/generate";
export const API_BRIDGE_SUGGESTION_ACCEPT = DOMAIN + "/api/bridge/suggestions/%s/accept";
export const API_BRIDGE_SUGGESTION_DISMISS = DOMAIN + "/api/bridge/suggestions/%s/dismiss";
export const API_BRIDGE_SUCCESS = DOMAIN + "/api/bridge/success/%s";

// Profile Scaffolding
export const API_SCAFFOLDING_PROFILE = DOMAIN + "/api/v1/intake/scaffolded-profile";
export const API_SCAFFOLDING_ADJUST = DOMAIN + "/api/v1/intake/scaffolded-profile/adjust";
export const API_SCAFFOLDING_CONFIRM = DOMAIN + "/api/v1/intake/scaffolded-profile/confirm";
export const API_SCAFFOLDING_CLARIFY = DOMAIN + "/api/v1/intake/scaffolded-profile/clarify";
export const API_SCAFFOLDING_RERECORD = DOMAIN + "/api/v1/intake/scaffolded-profile/re-record";

// Intake Flow
export const API_INTAKE_PROGRESS = DOMAIN + "/api/v1/intake/progress";
export const API_INTAKE_STEP = DOMAIN + "/api/v1/intake/step/%s";
export const API_INTAKE_COMPLETE_STEP = DOMAIN + "/api/v1/intake/complete/%s";
export const API_INTAKE_ENCOURAGEMENT = DOMAIN + "/api/v1/intake/encouragement";

// Accountability / Reporting
export const API_REPORT_CREATE = DOMAIN + "/api/v1/accountability/report";
export const API_REPORT_EVIDENCE = DOMAIN + "/api/v1/accountability/report/%s/evidence";
export const API_REPORT_LIST = DOMAIN + "/api/v1/accountability/reports";
export const API_REPORT_STATUS = DOMAIN + "/api/v1/accountability/report/%s/status";
export const API_REPORT_PUBLIC = DOMAIN + "/api/v1/accountability/public/%s";

// Reputation
export const API_REPUTATION_SCORE = DOMAIN + "/api/v1/reputation/score";
export const API_REPUTATION_HISTORY = DOMAIN + "/api/v1/reputation/history";
export const API_REPUTATION_BADGES = DOMAIN + "/api/v1/reputation/badges";
export const API_REPUTATION_LEADERBOARD = DOMAIN + "/api/v1/reputation/leaderboard";
export const API_REPUTATION_USER = DOMAIN + "/api/v1/reputation/user/%s";

// Profile Visitors
export const API_VISITORS_LIST = DOMAIN + "/api/v1/visitors/list";
export const API_VISITORS_COUNT = DOMAIN + "/api/v1/visitors/count";

// Essays (Extended Prompts)
export const API_ESSAYS_LIST = DOMAIN + "/api/v1/essays/list";
export const API_ESSAYS_PROMPTS = DOMAIN + "/api/v1/essays/prompts";
export const API_ESSAYS_ADD = DOMAIN + "/api/v1/essays/add";
export const API_ESSAYS_UPDATE = DOMAIN + "/api/v1/essays/update/%s";
export const API_ESSAYS_DELETE = DOMAIN + "/api/v1/essays/delete/%s";

// Waitlist
export const API_WAITLIST_JOIN = DOMAIN + "/api/v1/waitlist/join";
export const API_WAITLIST_STATUS = DOMAIN + "/api/v1/waitlist/status";
export const API_WAITLIST_POSITION = DOMAIN + "/api/v1/waitlist/position";

// Stripe / Subscription
export const API_SUBSCRIPTION_STATUS = DOMAIN + "/api/v1/subscription/status";
export const API_SUBSCRIPTION_PLANS = DOMAIN + "/api/v1/subscription/plans";
export const API_SUBSCRIPTION_CREATE = DOMAIN + "/api/v1/subscription/create";
export const API_SUBSCRIPTION_CANCEL = DOMAIN + "/api/v1/subscription/cancel";
export const API_SUBSCRIPTION_PORTAL = DOMAIN + "/api/v1/subscription/portal";

// Privacy-Safe Location
export const API_LOCATION_CENTROID = DOMAIN + "/api/v1/location/centroid";
export const API_LOCATION_TRAVEL_TIME = DOMAIN + "/api/v1/location/travel-time/%s";
export const API_LOCATION_UPDATE_SAFE = DOMAIN + "/api/v1/location/update-safe";

// Capture (Audio/Video Recording)
export const API_CAPTURE_SESSION = DOMAIN + "/api/v1/capture/sessions";
export const API_CAPTURE_CONFIRM = DOMAIN + "/api/v1/capture/sessions/%s/confirm";
export const API_CAPTURE_STATUS = DOMAIN + "/api/v1/capture/sessions/%s/status";
export const API_CAPTURE_PLAYBACK = DOMAIN + "/api/v1/capture/sessions/%s/playback";
export const API_CAPTURE_DELETE = DOMAIN + "/api/v1/capture/sessions/%s";
export const API_CAPTURE_REFRESH_URL = DOMAIN + "/api/v1/capture/sessions/%s/refresh-url";

// WebSocket
export const WS_CHAT = DOMAIN.replace("https", "wss").replace("http", "ws") + "/ws/chat";
export const WS_NOTIFICATIONS = DOMAIN.replace("https", "wss").replace("http", "ws") + "/ws/notifications";
