package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.repo.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for Stripe payment integration.
 *
 * Handles:
 * - Creating checkout sessions for donations
 * - Processing webhook events
 * - Recording successful donations
 */
@Service
public class StripeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StripeService.class);

    @Value("${stripe.api-key:}")
    private String stripeApiKey;

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${stripe.public-key:}")
    private String publicKey;

    @Value("${app.domain:https://dateaura.com}")
    private String appDomain;

    @Autowired
    private DonationService donationService;

    @Autowired
    private UserRepository userRepo;

    @PostConstruct
    public void init() {
        if (stripeApiKey != null && !stripeApiKey.isEmpty() && !stripeApiKey.startsWith("sk_test_...")) {
            Stripe.apiKey = stripeApiKey;
            LOGGER.info("Stripe API initialized");
        } else {
            LOGGER.warn("Stripe API key not configured - payments disabled");
        }
    }

    /**
     * Check if Stripe is configured and enabled.
     */
    public boolean isEnabled() {
        return stripeApiKey != null && !stripeApiKey.isEmpty() && !stripeApiKey.startsWith("sk_test_...");
    }

    /**
     * Get the public key for frontend.
     */
    public String getPublicKey() {
        return publicKey;
    }

    // ============================================
    // Checkout Session Creation
    // ============================================

    /**
     * Create a checkout session for a donation.
     */
    public Map<String, String> createDonationSession(Long userId, long amountCents, Long promptId) throws Exception {
        if (!isEnabled()) {
            throw new IllegalStateException("Stripe is not configured");
        }

        // Build success/cancel URLs
        String successUrl = appDomain + "/donation/success?session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = appDomain + "/donation/cancel";

        // Create checkout session
        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount(amountCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("AURA Donation")
                                                                .setDescription("Thank you for supporting AURA!")
                                                                .build()
                                                )
                                                .build()
                                )
                                .setQuantity(1L)
                                .build()
                )
                .putMetadata("user_id", String.valueOf(userId))
                .putMetadata("type", "donation");

        if (promptId != null) {
            paramsBuilder.putMetadata("prompt_id", String.valueOf(promptId));
        }

        Session session = Session.create(paramsBuilder.build());

        Map<String, String> result = new HashMap<>();
        result.put("sessionId", session.getId());
        result.put("url", session.getUrl());
        return result;
    }

    /**
     * Create a quick payment link for a specific amount.
     */
    public String createQuickDonationUrl(long amountCents) throws Exception {
        Map<String, String> session = createDonationSession(null, amountCents, null);
        return session.get("url");
    }

    // ============================================
    // Webhook Processing
    // ============================================

    /**
     * Process a Stripe webhook event.
     */
    @Transactional
    public void processWebhook(String payload, String sigHeader) throws Exception {
        Event event;

        // Verify signature
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            LOGGER.error("Webhook signature verification failed: {}", e.getMessage());
            throw new SecurityException("Invalid webhook signature");
        }

        LOGGER.info("Processing Stripe event: {} ({})", event.getType(), event.getId());

        // Handle the event
        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            case "payment_intent.succeeded" -> handlePaymentSucceeded(event);
            case "charge.succeeded" -> handleChargeSucceeded(event);
            case "charge.refunded" -> handleChargeRefunded(event);
            default -> LOGGER.debug("Unhandled event type: {}", event.getType());
        }
    }

    /**
     * Handle checkout.session.completed event.
     * This is the main event we care about for donations.
     */
    private void handleCheckoutCompleted(Event event) {
        StripeObject stripeObject = event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("Failed to deserialize checkout session"));

        Session session = (Session) stripeObject;

        // Get metadata
        String userIdStr = session.getMetadata().get("user_id");
        String promptIdStr = session.getMetadata().get("prompt_id");
        String type = session.getMetadata().get("type");

        if (!"donation".equals(type)) {
            LOGGER.debug("Ignoring non-donation checkout: {}", session.getId());
            return;
        }

        // Get amount
        Long amountTotal = session.getAmountTotal();
        if (amountTotal == null || amountTotal <= 0) {
            LOGGER.warn("Invalid amount in checkout session: {}", session.getId());
            return;
        }

        BigDecimal amount = BigDecimal.valueOf(amountTotal).divide(BigDecimal.valueOf(100));

        // Find user
        User user = null;
        if (userIdStr != null && !userIdStr.equals("null")) {
            try {
                Long userId = Long.parseLong(userIdStr);
                user = userRepo.findById(userId).orElse(null);
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid user ID in metadata: {}", userIdStr);
            }
        }

        // Try to find user by email if not in metadata
        if (user == null && session.getCustomerEmail() != null) {
            user = userRepo.findByEmail(session.getCustomerEmail().trim().toLowerCase());
            if (user != null) {
                LOGGER.info("Matched Stripe checkout {} to user {} by email", session.getId(), user.getId());
            } else {
                LOGGER.info("Anonymous donation received: ${}", amount);
            }
        }

        // Parse prompt ID
        Long promptId = null;
        if (promptIdStr != null && !promptIdStr.equals("null")) {
            try {
                promptId = Long.parseLong(promptIdStr);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        // Record the donation
        if (user != null) {
            donationService.recordDonation(user, amount, promptId);
            LOGGER.info("Donation recorded: ${} from user {}", amount, user.getId());
        } else {
            // Anonymous donation - just log it
            LOGGER.info("Anonymous donation received: ${} (session: {})", amount, session.getId());
        }
    }

    /**
     * Handle payment_intent.succeeded event.
     */
    private void handlePaymentSucceeded(Event event) {
        StripeObject stripeObject = event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("Failed to deserialize payment intent"));

        PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
        LOGGER.info("Payment succeeded: {} for ${}", paymentIntent.getId(),
                paymentIntent.getAmount() / 100.0);
    }

    /**
     * Handle charge.succeeded event.
     */
    private void handleChargeSucceeded(Event event) {
        StripeObject stripeObject = event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("Failed to deserialize charge"));

        Charge charge = (Charge) stripeObject;
        LOGGER.info("Charge succeeded: {} for ${}", charge.getId(),
                charge.getAmount() / 100.0);
    }

    /**
     * Handle charge.refunded event.
     */
    private void handleChargeRefunded(Event event) {
        StripeObject stripeObject = event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("Failed to deserialize charge"));

        Charge charge = (Charge) stripeObject;
        LOGGER.warn("Charge refunded: {} for ${}", charge.getId(),
                charge.getAmountRefunded() / 100.0);
        applyRefundToUserIfPossible(charge);
    }

    // ============================================
    // Session Retrieval
    // ============================================

    /**
     * Get checkout session details (for success page).
     */
    public Map<String, Object> getSessionDetails(String sessionId) throws Exception {
        if (!isEnabled()) {
            return Map.of("error", "Stripe not configured");
        }

        Session session = Session.retrieve(sessionId);

        Map<String, Object> result = new HashMap<>();
        result.put("id", session.getId());
        result.put("amount", session.getAmountTotal() / 100.0);
        result.put("currency", session.getCurrency().toUpperCase());
        result.put("status", session.getPaymentStatus());
        result.put("email", session.getCustomerEmail());

        return result;
    }

    private void applyRefundToUserIfPossible(Charge charge) {
        try {
            Long refundedCents = charge.getAmountRefunded();
            if (refundedCents == null || refundedCents <= 0) {
                return;
            }

            String userIdMetadata = charge.getMetadata() != null ? charge.getMetadata().get("user_id") : null;
            if ((userIdMetadata == null || userIdMetadata.isBlank()) && charge.getPaymentIntent() != null) {
                try {
                    PaymentIntent paymentIntent = PaymentIntent.retrieve(charge.getPaymentIntent());
                    if (paymentIntent != null && paymentIntent.getMetadata() != null) {
                        userIdMetadata = paymentIntent.getMetadata().get("user_id");
                    }
                } catch (Exception e) {
                    LOGGER.debug("Could not fetch payment intent metadata for refund {}", charge.getId(), e);
                }
            }

            if (userIdMetadata == null || userIdMetadata.isBlank()) {
                LOGGER.info("Refund {} has no user metadata; skipping donation adjustment", charge.getId());
                return;
            }

            Long userId = Long.parseLong(userIdMetadata);
            User user = userRepo.findById(userId).orElse(null);
            if (user == null) {
                LOGGER.warn("Refund {} references missing user {}", charge.getId(), userId);
                return;
            }

            BigDecimal refundAmount = BigDecimal.valueOf(refundedCents).divide(BigDecimal.valueOf(100));
            donationService.applyRefund(user, refundAmount);
        } catch (Exception e) {
            LOGGER.error("Failed applying refund adjustment: {}", e.getMessage());
        }
    }
}
