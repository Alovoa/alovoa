package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.entity.DonationPrompt;
import com.nonononoki.alovoa.entity.DonationPrompt.PromptType;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.entity.User.DonationTier;
import com.nonononoki.alovoa.repo.DonationPromptRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service for the donation-only monetization model.
 *
 * Philosophy:
 * - ALL features work without donating
 * - Prompts are shown at positive moments (after matches, dates)
 * - Never aggressive or guilt-tripping
 * - Focus on gratitude and keeping the service running
 *
 * Prompt Timing:
 * - AFTER_MATCH: After a mutual match is confirmed
 * - AFTER_DATE: After completing a video date
 * - MONTHLY: Once per month for active users
 * - FIRST_LIKE: First time someone likes them (encouragement)
 * - MILESTONE: When hitting milestones (10 matches, etc.)
 *
 * Tier Recognition:
 * - SUPPORTER: Any donation (gets a small badge)
 * - CONTRIBUTOR: $10+ total (slightly larger badge)
 * - PATRON: $50+ total (badge + featured in supporters page)
 * - CHAMPION: $100+ total (all above + special thank you)
 */
@Service
public class DonationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DonationService.class);

    // Donation tier thresholds (aligned with marketing)
    // Supporter: $5-20 → Thank you email
    // Believer: $21-50 → Name in supporters page
    // Builder: $51-100 → Early access, founder badge
    // Founding Member: $100+ → All above + quarterly updates
    private static final BigDecimal BELIEVER_THRESHOLD = new BigDecimal("21.00");
    private static final BigDecimal BUILDER_THRESHOLD = new BigDecimal("51.00");
    private static final BigDecimal FOUNDING_MEMBER_THRESHOLD = new BigDecimal("100.00");

    // Prompt rate limiting (max prompts per week)
    private static final int MAX_PROMPTS_PER_WEEK = 2;

    // Minimum hours between prompts
    private static final int MIN_HOURS_BETWEEN_PROMPTS = 48;

    // Amount validation constraints
    public static final int MIN_DONATION_AMOUNT = 5;
    public static final int MAX_DONATION_AMOUNT = 500;

    @Value("${app.donation.enabled:true}")
    private boolean donationEnabled;

    @Value("${app.donation.payment-url:https://donate.stripe.com/aura}")
    private String donationPaymentUrl;

    @Value("${stripe.api-key:}")
    private String stripeApiKey;

    @Value("${app.domain:https://dateaura.com}")
    private String appDomain;

    @Autowired
    private DonationPromptRepository promptRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private AuthService authService;

    // ============================================
    // Prompt Creation
    // ============================================

    /**
     * Record that a donation prompt should be shown to user.
     * Returns true if prompt can be shown (not rate-limited).
     */
    @Transactional
    public boolean showPrompt(User user, PromptType type) {
        if (!donationEnabled) {
            return false;
        }

        // Check rate limiting
        if (isRateLimited(user)) {
            LOGGER.debug("Donation prompt rate-limited for user {}", user.getId());
            return false;
        }

        // Don't prompt users who donated recently
        if (isDonorCooldown(user)) {
            LOGGER.debug("User {} is in donor cooldown period", user.getId());
            return false;
        }

        // Create prompt record
        DonationPrompt prompt = new DonationPrompt();
        prompt.setUser(user);
        prompt.setPromptType(type);
        prompt.setShownAt(new Date());
        promptRepo.save(prompt);

        LOGGER.info("Showing {} donation prompt to user {}", type, user.getId());
        return true;
    }

    /**
     * Show prompt after a match is confirmed.
     */
    @Transactional
    public boolean showAfterMatchPrompt(User user) {
        return showPrompt(user, PromptType.AFTER_MATCH);
    }

    /**
     * Show prompt after a video date completes.
     */
    @Transactional
    public boolean showAfterDatePrompt(User user) {
        return showPrompt(user, PromptType.AFTER_DATE);
    }

    /**
     * Show prompt when user receives their first like.
     */
    @Transactional
    public boolean showFirstLikePrompt(User user) {
        // Only show once ever
        if (promptRepo.countPromptTypeSince(user, PromptType.FIRST_LIKE, new Date(0)) > 0) {
            return false;
        }
        return showPrompt(user, PromptType.FIRST_LIKE);
    }

    /**
     * Show milestone prompt (e.g., 10 matches).
     */
    @Transactional
    public boolean showMilestonePrompt(User user, String milestone) {
        return showPrompt(user, PromptType.MILESTONE);
    }

    /**
     * Show prompt when user marks relationship status and is about to leave.
     * This is the highest-value moment - they got what they wanted!
     *
     * Message: "Leaving because you found someone? That's the goal.
     * Consider donating so others can too."
     */
    @Transactional
    public boolean showRelationshipExitPrompt(User user) {
        // Only show once ever - this is a special moment
        if (promptRepo.countPromptTypeSince(user, PromptType.RELATIONSHIP_EXIT, new Date(0)) > 0) {
            return false;
        }
        // Skip rate limiting for this - it's the most important moment
        DonationPrompt prompt = new DonationPrompt();
        prompt.setUser(user);
        prompt.setPromptType(PromptType.RELATIONSHIP_EXIT);
        prompt.setShownAt(new Date());
        promptRepo.save(prompt);

        LOGGER.info("Showing relationship exit prompt to user {} - they found someone!", user.getId());
        return true;
    }

    /**
     * Get the relationship exit message (for the email/UI).
     */
    public String getRelationshipExitMessage() {
        return "Looks like AURA worked! You just marked your relationship status as 'in a relationship.'\n\n" +
                "That means we did our job. If you're about to delete the app (totally fine — that's literally the goal), " +
                "we have one ask:\n\n" +
                "Would you consider donating so someone else can have the same experience?\n\n" +
                "AURA is funded entirely by donations from people like you — people who found someone and want to pay it forward. " +
                "No ads. No investors. Just people helping people date better.";
    }

    // ============================================
    // Prompt Responses
    // ============================================

    /**
     * User dismissed the prompt without donating.
     */
    @Transactional
    public void dismissPrompt(Long promptId) {
        DonationPrompt prompt = promptRepo.findById(promptId).orElse(null);
        if (prompt != null) {
            prompt.setDismissed(true);
            promptRepo.save(prompt);
            LOGGER.debug("Prompt {} dismissed by user", promptId);
        }
    }

    /**
     * Record a donation from user.
     */
    @Transactional
    public void recordDonation(User user, BigDecimal amount, Long promptId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            LOGGER.warn("Ignoring invalid donation amount {}", amount);
            return;
        }
        LOGGER.info("Recording donation of ${} from user {}", amount, user.getId());

        // Update prompt if provided
        if (promptId != null) {
            DonationPrompt prompt = promptRepo.findById(promptId).orElse(null);
            if (prompt != null) {
                prompt.setDonated(true);
                prompt.setDonationAmount(amount);
                promptRepo.save(prompt);
            }
        }

        // Update user's total donations
        double newTotal = user.getTotalDonations() + amount.doubleValue();
        user.setTotalDonations(newTotal);
        user.setLastDonationDate(new Date());

        // Update streak
        updateDonationStreak(user);

        // Update tier
        updateDonationTier(user);

        userRepo.save(user);
        LOGGER.info("User {} now at tier {} with ${} total", user.getId(), user.getDonationTier(), newTotal);
    }

    /**
     * Apply a refund to a user's donation total and update tier.
     */
    @Transactional
    public void applyRefund(User user, BigDecimal refundAmount) {
        if (user == null || refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        double updatedTotal = Math.max(0.0, user.getTotalDonations() - refundAmount.doubleValue());
        user.setTotalDonations(updatedTotal);
        if (updatedTotal == 0.0) {
            user.setDonationStreakMonths(0);
            user.setLastDonationDate(null);
        }
        updateDonationTier(user);
        userRepo.save(user);

        LOGGER.info("Applied refund ${} to user {}. New total ${} and tier {}",
                refundAmount, user.getId(), updatedTotal, user.getDonationTier());
    }

    /**
     * Record a donation from current user.
     */
    @Transactional
    public void recordCurrentUserDonation(BigDecimal amount, Long promptId) throws AlovoaException {
        User user = authService.getCurrentUser(true);
        recordDonation(user, amount, promptId);
    }

    // ============================================
    // Tier Management
    // ============================================

    /**
     * Update user's donation tier based on total donations.
     * Tiers:
     * - Supporter: $5-20 → Thank you email
     * - Believer: $21-50 → Name in supporters page
     * - Builder: $51-100 → Early access, founder badge
     * - Founding Member: $100+ → All above + quarterly updates, feature input
     */
    private void updateDonationTier(User user) {
        BigDecimal total = BigDecimal.valueOf(user.getTotalDonations());

        DonationTier newTier;
        if (total.compareTo(FOUNDING_MEMBER_THRESHOLD) >= 0) {
            newTier = DonationTier.FOUNDING_MEMBER;
        } else if (total.compareTo(BUILDER_THRESHOLD) >= 0) {
            newTier = DonationTier.BUILDER;
        } else if (total.compareTo(BELIEVER_THRESHOLD) >= 0) {
            newTier = DonationTier.BELIEVER;
        } else if (total.compareTo(BigDecimal.ZERO) > 0) {
            newTier = DonationTier.SUPPORTER;
        } else {
            newTier = DonationTier.NONE;
        }

        if (user.getDonationTier() != newTier) {
            LOGGER.info("User {} upgraded from {} to {}", user.getId(), user.getDonationTier(), newTier);
            user.setDonationTier(newTier);
        }
    }

    /**
     * Update user's donation streak (consecutive months with donations).
     */
    private void updateDonationStreak(User user) {
        Date lastDonation = user.getLastDonationDate();
        Date now = new Date();

        if (lastDonation == null) {
            user.setDonationStreakMonths(1);
            return;
        }

        // Check if last donation was within the last 35 days (allows for monthly variance)
        long daysSinceLast = (now.getTime() - lastDonation.getTime()) / (1000 * 60 * 60 * 24);

        if (daysSinceLast <= 35) {
            // Continuing streak
            user.setDonationStreakMonths(user.getDonationStreakMonths() + 1);
        } else if (daysSinceLast <= 65) {
            // Missed one month but resuming
            user.setDonationStreakMonths(1);
        } else {
            // Streak broken
            user.setDonationStreakMonths(1);
        }
    }

    // ============================================
    // Rate Limiting
    // ============================================

    /**
     * Check if user is rate-limited from receiving prompts.
     */
    private boolean isRateLimited(User user) {
        // Check weekly limit
        Calendar weekAgo = Calendar.getInstance();
        weekAgo.add(Calendar.DAY_OF_MONTH, -7);

        long promptsThisWeek = promptRepo.countPromptsShownSince(user, weekAgo.getTime());
        if (promptsThisWeek >= MAX_PROMPTS_PER_WEEK) {
            return true;
        }

        // Check minimum hours between prompts
        DonationPrompt lastPrompt = promptRepo.findLastPromptFor(user);
        if (lastPrompt != null) {
            long hoursSinceLast = (new Date().getTime() - lastPrompt.getShownAt().getTime()) / (1000 * 60 * 60);
            if (hoursSinceLast < MIN_HOURS_BETWEEN_PROMPTS) {
                return true;
            }
        }

        return false;
    }

    /**
     * Recent donors get a cooldown period without prompts.
     * Founding Members get 6 months, Builders 3 months, others 1 month.
     */
    private boolean isDonorCooldown(User user) {
        Date lastDonation = user.getLastDonationDate();
        if (lastDonation == null) {
            return false;
        }

        int cooldownMonths = switch (user.getDonationTier()) {
            case FOUNDING_MEMBER -> 6;
            case BUILDER -> 3;
            case BELIEVER -> 2;
            case SUPPORTER -> 1;
            case NONE -> 0;
        };

        Calendar cooldownEnd = Calendar.getInstance();
        cooldownEnd.setTime(lastDonation);
        cooldownEnd.add(Calendar.MONTH, cooldownMonths);

        return new Date().before(cooldownEnd.getTime());
    }

    // ============================================
    // Monthly Prompt Scheduler
    // ============================================

    /**
     * Check monthly active users and show prompts.
     * Runs daily at 10 AM.
     */
    @Scheduled(cron = "0 0 10 * * *")
    @Transactional
    public void sendMonthlyPrompts() {
        if (!donationEnabled) {
            return;
        }

        // Get first day of current month
        Calendar monthStart = Calendar.getInstance();
        monthStart.set(Calendar.DAY_OF_MONTH, 1);
        monthStart.set(Calendar.HOUR_OF_DAY, 0);
        monthStart.set(Calendar.MINUTE, 0);
        monthStart.set(Calendar.SECOND, 0);

        // Only process on the 1st of the month
        Calendar today = Calendar.getInstance();
        if (today.get(Calendar.DAY_OF_MONTH) != 1) {
            return;
        }

        LOGGER.info("Processing monthly donation prompts...");

        Calendar activeSince = Calendar.getInstance();
        activeSince.add(Calendar.DAY_OF_MONTH, -30);

        // Get recently active users and prompt at most once per month.
        List<User> activeUsers = userRepo.findActiveUsersSince(activeSince.getTime());
        int prompted = 0;

        for (User user : activeUsers) {
            if (promptRepo.hasMonthlyPromptThisMonth(user, monthStart.getTime())) {
                continue;
            }
            if (showPrompt(user, PromptType.MONTHLY)) {
                prompted++;
            }
        }

        LOGGER.info("Sent {} monthly donation prompts", prompted);
    }

    // ============================================
    // Analytics & Reporting
    // ============================================

    /**
     * Get donation statistics for admin dashboard.
     */
    public Map<String, Object> getDonationStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalPrompts = promptRepo.count();
        long donatedPrompts = promptRepo.countDonatedPrompts();
        long totalDonors = promptRepo.countDistinctDonors();
        BigDecimal totalAmount = Optional.ofNullable(promptRepo.sumDonations()).orElse(BigDecimal.ZERO);
        double avgDonation = Optional.ofNullable(promptRepo.averageDonationAmount()).orElse(0.0);

        double conversionRate = totalPrompts > 0
                ? (donatedPrompts * 100.0 / totalPrompts)
                : 0.0;
        long monthlyRecurring = userRepo
                .countByDonationStreakMonthsGreaterThanEqualAndTotalDonationsGreaterThanEqual(3, 0.01);

        stats.put("totalPrompts", totalPrompts);
        stats.put("donatedPrompts", donatedPrompts);
        stats.put("totalDonors", totalDonors);
        stats.put("totalAmount", totalAmount.doubleValue());
        stats.put("avgDonation", avgDonation);
        stats.put("conversionRate", conversionRate);
        stats.put("monthlyRecurring", monthlyRecurring);
        stats.put("byPromptType", Map.of(
                "AFTER_MATCH", promptRepo.countByPromptType(PromptType.AFTER_MATCH),
                "AFTER_DATE", promptRepo.countByPromptType(PromptType.AFTER_DATE),
                "MONTHLY", promptRepo.countByPromptType(PromptType.MONTHLY),
                "FIRST_LIKE", promptRepo.countByPromptType(PromptType.FIRST_LIKE),
                "MILESTONE", promptRepo.countByPromptType(PromptType.MILESTONE),
                "RELATIONSHIP_EXIT", promptRepo.countByPromptType(PromptType.RELATIONSHIP_EXIT)
        ));

        return stats;
    }

    /**
     * Get current user's donation history.
     */
    public Map<String, Object> getCurrentUserDonationInfo() throws AlovoaException {
        User user = authService.getCurrentUser(true);

        Map<String, Object> info = new HashMap<>();
        info.put("tier", user.getDonationTier());
        info.put("totalDonations", user.getTotalDonations());
        info.put("streakMonths", user.getDonationStreakMonths());
        info.put("lastDonation", user.getLastDonationDate());
        info.put("paymentUrl", donationPaymentUrl);

        // Check if a prompt should be shown
        boolean shouldShow = !isRateLimited(user) && !isDonorCooldown(user);
        info.put("showPrompt", shouldShow);

        // If showing, include prompt details
        if (shouldShow) {
            DonationPrompt pendingPrompt = promptRepo.findLastPromptFor(user);
            if (pendingPrompt != null && !pendingPrompt.isDismissed() && !pendingPrompt.isDonated()) {
                info.put("promptType", pendingPrompt.getPromptType().name());
                info.put("promptId", pendingPrompt.getId());
            } else {
                // Default to MONTHLY if no pending prompt
                info.put("promptType", "MONTHLY");
                info.put("promptId", null);
            }
            // Send cooldown info for client-side secondary guard coordination
            info.put("cooldownHours", MIN_HOURS_BETWEEN_PROMPTS);
        }

        return info;
    }

    /**
     * Validate donation amount is within allowed bounds.
     * @param amount Amount in dollars
     * @return true if valid
     */
    public boolean isValidAmount(int amount) {
        return amount >= MIN_DONATION_AMOUNT && amount <= MAX_DONATION_AMOUNT;
    }

    /**
     * Create a Stripe Checkout session for a donation.
     */
    public Map<String, Object> createCheckoutSession(int amount, Long promptId, String promptType) throws AlovoaException {
        // Validate amount
        if (!isValidAmount(amount)) {
            throw new AlovoaException("invalid_amount");
        }

        User user = authService.getCurrentUser(true);
        LOGGER.info("Creating checkout session for user {} amount ${} prompt {}", user.getId(), amount, promptId);

        String normalizedPromptType = promptType != null ? promptType : "DEFAULT";
        long amountCents = amount * 100L;

        if (isStripeCheckoutEnabled()) {
            try {
                Stripe.apiKey = stripeApiKey;

                SessionCreateParams params = SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(appDomain + "/donation/success?session_id={CHECKOUT_SESSION_ID}")
                        .setCancelUrl(appDomain + "/donation/cancel")
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency("usd")
                                                        .setUnitAmount(amountCents)
                                                        .setProductData(
                                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                        .setName("AURA Donation")
                                                                        .setDescription("Thank you for supporting AURA.")
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .setQuantity(1L)
                                        .build()
                        )
                        .putMetadata("prompt_id", promptId != null ? String.valueOf(promptId) : "")
                        .putMetadata("prompt_type", normalizedPromptType)
                        .putMetadata("user_id", String.valueOf(user.getId()))
                        .putMetadata("type", "donation")
                        .build();

                Session session = Session.create(params);
                Map<String, Object> result = new HashMap<>();
                result.put("checkoutUrl", session.getUrl());
                result.put("sessionId", session.getId());
                result.put("provider", "stripe");
                return result;
            } catch (Exception e) {
                LOGGER.error("Stripe checkout session creation failed, falling back to configured payment URL", e);
            }
        }

        String checkoutUrl = String.format("%s?amount=%d&promptId=%s&promptType=%s",
                donationPaymentUrl, amountCents, promptId != null ? promptId : "", normalizedPromptType);
        Map<String, Object> result = new HashMap<>();
        result.put("checkoutUrl", checkoutUrl);
        result.put("sessionId", UUID.randomUUID().toString());
        result.put("provider", "fallback");
        return result;
    }

    /**
     * Get thank you message based on donation tier.
     * Aligned with marketing messaging.
     */
    public String getThankYouMessage(DonationTier tier) {
        return switch (tier) {
            case FOUNDING_MEMBER -> "You're a Founding Member! Your incredible generosity helps us " +
                    "prove that dating can be a public good. Expect quarterly updates and a seat at the table " +
                    "when we plan new features.";
            case BUILDER -> "Thank you, Builder! Your support earns you early access to new cities " +
                    "and a founder badge on your profile. You're literally building this with us.";
            case BELIEVER -> "Thank you for believing in us! Your name will appear on our supporters page " +
                    "(if you opt in). Every donation keeps AURA ad-free and paywall-free.";
            case SUPPORTER -> "Thank you for your support! You're helping prove that a donation-funded " +
                    "dating app can work. Every bit helps us stay free for everyone.";
            case NONE -> "AURA is free — like Wikipedia. If you'd like to donate so others can find " +
                    "someone too, any amount helps cover server costs.";
        };
    }

    private boolean isStripeCheckoutEnabled() {
        return stripeApiKey != null
                && !stripeApiKey.isBlank()
                && !stripeApiKey.startsWith("sk_test_...");
    }
}
