package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.entity.WaitlistEntry;
import com.nonononoki.alovoa.entity.WaitlistEntry.*;
import com.nonononoki.alovoa.repo.WaitlistEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for managing the pre-launch waitlist.
 *
 * Features:
 * - Sign up with email, gender, seeking, location
 * - Referral system (each user gets 3 invite codes)
 * - Priority queue (women first, then referrals, then sign-up order)
 * - UTM tracking for marketing
 */
@Service
public class WaitlistService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitlistService.class);

    // Market thresholds for opening (gender-balanced)
    private static final int MIN_WOMEN_PER_MARKET = 200;
    private static final int MIN_MEN_PER_MARKET = 200;
    private static final int MIN_TOTAL_PER_MARKET = 500;

    // Batch sizes for invites
    private static final int WOMEN_FIRST_BATCH_SIZE = 100;
    private static final int REGULAR_BATCH_SIZE = 50;

    @Value("${app.waitlist.referral-bonus:10}")
    private int referralBonus;

    @Autowired
    private WaitlistEntryRepository waitlistRepo;

    @Autowired(required = false)
    private MailService mailService;

    // ============================================
    // Signup
    // ============================================

    /**
     * Add a new signup to the waitlist.
     */
    @Transactional
    public WaitlistEntry signup(String email, Gender gender, Seeking seeking, Location location,
                                 String referralCode, String source, String utmSource,
                                 String utmMedium, String utmCampaign) {

        // Normalize email
        email = email.toLowerCase().trim();

        // Check if already exists
        if (waitlistRepo.existsByEmail(email)) {
            LOGGER.info("Duplicate waitlist signup attempt: {}", email);
            return waitlistRepo.findByEmail(email).orElse(null);
        }

        // Create entry
        WaitlistEntry entry = new WaitlistEntry();
        entry.setEmail(email);
        entry.setGender(gender);
        entry.setSeeking(seeking);
        entry.setLocation(location);
        entry.setSource(source);
        entry.setUtmSource(utmSource);
        entry.setUtmMedium(utmMedium);
        entry.setUtmCampaign(utmCampaign);

        // Handle referral
        if (referralCode != null && !referralCode.isBlank()) {
            Optional<WaitlistEntry> referrer = waitlistRepo.findByInviteCode(referralCode.toUpperCase());
            if (referrer.isPresent()) {
                entry.setReferredBy(referralCode.toUpperCase());
                // Boost referrer's priority
                WaitlistEntry ref = referrer.get();
                ref.setPriorityScore(ref.getPriorityScore() + referralBonus);
                waitlistRepo.save(ref);
                LOGGER.info("Referral credited to {}", ref.getEmail());
            }
        }

        // Generate their invite code
        entry.generateInviteCode();

        // Save
        waitlistRepo.save(entry);

        LOGGER.info("New waitlist signup: {} from {} (priority: {})",
                email, location, entry.getPriorityScore());

        // Send confirmation email
        sendConfirmationEmail(entry);

        return entry;
    }

    /**
     * Get entry by email (for status check).
     */
    public Optional<WaitlistEntry> getByEmail(String email) {
        return waitlistRepo.findByEmail(email.toLowerCase().trim());
    }

    /**
     * Get entry by invite code.
     */
    public Optional<WaitlistEntry> getByInviteCode(String code) {
        return waitlistRepo.findByInviteCode(code.toUpperCase());
    }

    // ============================================
    // Invites
    // ============================================

    /**
     * Send invites to next batch of users.
     */
    @Transactional
    public List<WaitlistEntry> sendInviteBatch(int batchSize) {
        Page<WaitlistEntry> pending = waitlistRepo.findPendingByPriority(PageRequest.of(0, batchSize));
        List<WaitlistEntry> invited = new ArrayList<>();

        for (WaitlistEntry entry : pending.getContent()) {
            try {
                sendInviteEmail(entry);
                entry.setStatus(Status.INVITED);
                entry.setInvitedAt(new Date());
                waitlistRepo.save(entry);
                invited.add(entry);
                LOGGER.info("Sent invite to {}", entry.getEmail());
            } catch (Exception e) {
                LOGGER.error("Failed to send invite to {}: {}", entry.getEmail(), e.getMessage());
            }
        }

        return invited;
    }

    /**
     * Mark user as registered (called after successful signup).
     */
    @Transactional
    public void markRegistered(String email) {
        Optional<WaitlistEntry> entry = waitlistRepo.findByEmail(email.toLowerCase().trim());
        if (entry.isPresent()) {
            WaitlistEntry e = entry.get();
            e.setStatus(Status.REGISTERED);
            e.setRegisteredAt(new Date());
            waitlistRepo.save(e);
            LOGGER.info("Waitlist user {} completed registration", email);
        }
    }

    // ============================================
    // Stats
    // ============================================

    /**
     * Get waitlist statistics.
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("total", waitlistRepo.count());
        stats.put("pending", waitlistRepo.countByStatus(Status.PENDING));
        stats.put("invited", waitlistRepo.countByStatus(Status.INVITED));
        stats.put("registered", waitlistRepo.countByStatus(Status.REGISTERED));

        // Location breakdown
        Map<String, Long> byLocation = new HashMap<>();
        for (Object[] row : waitlistRepo.countByLocationGrouped()) {
            byLocation.put(row[0].toString(), (Long) row[1]);
        }
        stats.put("byLocation", byLocation);

        // Gender breakdown
        Map<String, Long> byGender = new HashMap<>();
        for (Object[] row : waitlistRepo.countByGenderGrouped()) {
            byGender.put(row[0].toString(), (Long) row[1]);
        }
        stats.put("byGender", byGender);

        // Last 7 days
        Calendar weekAgo = Calendar.getInstance();
        weekAgo.add(Calendar.DAY_OF_MONTH, -7);
        stats.put("last7Days", waitlistRepo.countSignupsSince(weekAgo.getTime()));

        return stats;
    }

    /**
     * Get position in line for a user.
     */
    public long getPositionInLine(WaitlistEntry entry) {
        if (entry.getStatus() != Status.PENDING) {
            return 0; // Not in line
        }
        return waitlistRepo.getPositionInLine(entry.getPriorityScore(), entry.getSignedUpAt()) + 1;
    }

    /**
     * Get referral count for a user.
     */
    public long getReferralCount(WaitlistEntry entry) {
        if (entry.getInviteCode() == null) {
            return 0;
        }
        return waitlistRepo.countByReferredBy(entry.getInviteCode());
    }

    // ============================================
    // Emails
    // ============================================

    private void sendConfirmationEmail(WaitlistEntry entry) {
        if (mailService == null) {
            LOGGER.warn("MailService not available, skipping confirmation email");
            return;
        }

        try {
            String subject = "You're on the AURA waitlist (yes, it's really free)";
            String body = buildConfirmationEmailBody(entry);
            mailService.sendAdminMail(entry.getEmail(), subject, body);
            LOGGER.info("Sent waitlist confirmation email to {}", entry.getEmail());
        } catch (Exception e) {
            LOGGER.error("Failed to send confirmation email: {}", e.getMessage());
        }
    }

    private void sendInviteEmail(WaitlistEntry entry) {
        if (mailService == null) {
            LOGGER.warn("MailService not available, skipping invite email");
            return;
        }

        try {
            String subject = "Your AURA invite is ready!";
            String body = buildInviteEmailBody(entry);
            mailService.sendAdminMail(entry.getEmail(), subject, body);
            LOGGER.info("Sent waitlist invite email to {}", entry.getEmail());
        } catch (Exception e) {
            LOGGER.error("Failed to send invite email: {}", e.getMessage());
        }
    }

    private String buildConfirmationEmailBody(WaitlistEntry entry) {
        return String.format("""
            Hey!

            You're in. Welcome to the AURA waitlist.

            You might be wondering: "Is it really free?" Yes.

            No subscription. No premium tier. No ads. No selling your data.

            We're funded by donations, like Wikipedia. If AURA helps you find someone,
            you'll probably delete the app — that's the goal. Before you go, we'll ask
            if you'd like to donate so others can have the same experience. No pressure.

            **What happens next:**
            1. We're launching in waves (women get priority access)
            2. You'll get an invite email when it's your turn
            3. You have 3 invite codes to share

            **Your invite code: %s**
            Share it with friends to move up the waitlist!

            Questions? Just reply to this email.

            – The AURA Team
            """, entry.getInviteCode());
    }

    private String buildInviteEmailBody(WaitlistEntry entry) {
        return String.format("""
            Hey!

            Your AURA invite is ready!

            Click here to create your profile: https://dateaura.com/register?code=%s

            Remember:
            - You'll need to complete video verification (it only takes 30 seconds)
            - Answer the values questions honestly
            - You have 3 invite codes to share with friends

            We're excited to have you. Let's make dating better together.

            – The AURA Team
            """, entry.getInviteCode());
    }

    // ============================================
    // Market Thresholds
    // ============================================

    /**
     * Get status for a specific market (location).
     */
    public MarketStatus getMarketStatus(Location location) {
        long women = waitlistRepo.countByLocationAndGender(location, Gender.WOMAN);
        long men = waitlistRepo.countByLocationAndGender(location, Gender.MAN);
        long total = waitlistRepo.countByLocation(location);

        MarketStatus status = new MarketStatus();
        status.location = location;
        status.womenCount = women;
        status.menCount = men;
        status.totalCount = total;
        status.womenNeeded = Math.max(0, MIN_WOMEN_PER_MARKET - women);
        status.menNeeded = Math.max(0, MIN_MEN_PER_MARKET - men);
        status.totalNeeded = Math.max(0, MIN_TOTAL_PER_MARKET - total);
        status.readyToOpen = women >= MIN_WOMEN_PER_MARKET &&
                             men >= MIN_MEN_PER_MARKET &&
                             total >= MIN_TOTAL_PER_MARKET;
        status.percentReady = calculatePercentReady(women, men, total);

        return status;
    }

    /**
     * Get stats for all markets.
     */
    public List<MarketStatus> getAllMarketStats() {
        List<MarketStatus> markets = new ArrayList<>();
        for (Location loc : Location.values()) {
            markets.add(getMarketStatus(loc));
        }
        // Sort by percent ready (highest first)
        markets.sort((a, b) -> Double.compare(b.percentReady, a.percentReady));
        return markets;
    }

    /**
     * Get summary stats for display.
     */
    public Map<String, Object> getMarketStats() {
        Map<String, Object> stats = new HashMap<>();
        List<MarketStatus> markets = getAllMarketStats();

        // Find best market
        MarketStatus best = markets.isEmpty() ? null : markets.get(0);
        if (best != null) {
            stats.put("leadingMarket", best.location.name());
            stats.put("leadingPercent", best.percentReady);
            stats.put("leadingReady", best.readyToOpen);
        }

        // Count ready markets
        long readyCount = markets.stream().filter(m -> m.readyToOpen).count();
        stats.put("readyMarkets", readyCount);
        stats.put("totalMarkets", markets.size());

        return stats;
    }

    private double calculatePercentReady(long women, long men, long total) {
        double womenPercent = Math.min(100.0, (women * 100.0) / MIN_WOMEN_PER_MARKET);
        double menPercent = Math.min(100.0, (men * 100.0) / MIN_MEN_PER_MARKET);
        double totalPercent = Math.min(100.0, (total * 100.0) / MIN_TOTAL_PER_MARKET);
        // Average of all three requirements
        return (womenPercent + menPercent + totalPercent) / 3.0;
    }

    // ============================================
    // Batch Invites by Market
    // ============================================

    /**
     * Send invites for a specific market when it opens.
     * Sends to women first, then by priority score.
     */
    @Transactional
    public List<WaitlistEntry> sendMarketOpeningInvites(Location location) {
        MarketStatus status = getMarketStatus(location);
        if (!status.readyToOpen) {
            LOGGER.warn("Market {} is not ready to open yet", location);
            return Collections.emptyList();
        }

        List<WaitlistEntry> invited = new ArrayList<>();

        // Phase 1: Invite women first
        Page<WaitlistEntry> womenBatch = waitlistRepo.findPendingByLocationAndGender(
                location, Gender.WOMAN, PageRequest.of(0, WOMEN_FIRST_BATCH_SIZE));
        for (WaitlistEntry entry : womenBatch.getContent()) {
            if (sendInvite(entry)) {
                invited.add(entry);
            }
        }

        LOGGER.info("Sent {} invites to women in {}", invited.size(), location);

        // Phase 2: Invite others by priority
        Page<WaitlistEntry> othersBatch = waitlistRepo.findPendingByLocationOrderByPriority(
                location, PageRequest.of(0, REGULAR_BATCH_SIZE));
        for (WaitlistEntry entry : othersBatch.getContent()) {
            if (sendInvite(entry)) {
                invited.add(entry);
            }
        }

        LOGGER.info("Total {} invites sent for {} market opening", invited.size(), location);
        return invited;
    }

    /**
     * Send a single invite.
     */
    private boolean sendInvite(WaitlistEntry entry) {
        try {
            sendInviteEmail(entry);
            entry.setStatus(Status.INVITED);
            entry.setInvitedAt(new Date());
            waitlistRepo.save(entry);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to send invite to {}: {}", entry.getEmail(), e.getMessage());
            return false;
        }
    }

    // ============================================
    // Market Status DTO
    // ============================================

    public static class MarketStatus {
        public Location location;
        public long womenCount;
        public long menCount;
        public long totalCount;
        public long womenNeeded;
        public long menNeeded;
        public long totalNeeded;
        public boolean readyToOpen;
        public double percentReady;

        public String getLocationDisplayName() {
            return switch (location) {
                case DC -> "Washington, DC";
                case ARLINGTON -> "Arlington, VA";
                case ALEXANDRIA -> "Alexandria, VA";
                case NOVA_OTHER -> "Northern Virginia";
                case MARYLAND -> "Maryland Suburbs";
                case OTHER -> "Other Areas";
            };
        }
    }
}
