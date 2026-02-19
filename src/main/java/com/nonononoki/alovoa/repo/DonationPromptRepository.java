package com.nonononoki.alovoa.repo;

import com.nonononoki.alovoa.entity.DonationPrompt;
import com.nonononoki.alovoa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.math.BigDecimal;

public interface DonationPromptRepository extends JpaRepository<DonationPrompt, Long> {

    List<DonationPrompt> findByUser(User user);

    List<DonationPrompt> findByUserOrderByShownAtDesc(User user);

    /**
     * Count prompts shown to user in time range.
     * Used to avoid over-prompting.
     */
    @Query("SELECT COUNT(dp) FROM DonationPrompt dp WHERE dp.user = :user AND dp.shownAt >= :since")
    long countPromptsShownSince(@Param("user") User user, @Param("since") Date since);

    /**
     * Count specific prompt type shown to user.
     */
    @Query("SELECT COUNT(dp) FROM DonationPrompt dp WHERE dp.user = :user AND dp.promptType = :type AND dp.shownAt >= :since")
    long countPromptTypeSince(@Param("user") User user, @Param("type") DonationPrompt.PromptType type, @Param("since") Date since);

    /**
     * Find if user donated after a specific prompt.
     */
    @Query("SELECT dp FROM DonationPrompt dp WHERE dp.user = :user AND dp.donated = true ORDER BY dp.shownAt DESC")
    List<DonationPrompt> findDonationsBy(@Param("user") User user);

    /**
     * Count total donations made by user.
     */
    @Query("SELECT COUNT(dp) FROM DonationPrompt dp WHERE dp.user = :user AND dp.donated = true")
    long countDonationsBy(@Param("user") User user);

    /**
     * Count total dismissed prompts (for analytics).
     */
    @Query("SELECT COUNT(dp) FROM DonationPrompt dp WHERE dp.user = :user AND dp.dismissed = true")
    long countDismissedBy(@Param("user") User user);

    /**
     * Get last prompt shown to user.
     */
    @Query("SELECT dp FROM DonationPrompt dp WHERE dp.user = :user ORDER BY dp.shownAt DESC LIMIT 1")
    DonationPrompt findLastPromptFor(@Param("user") User user);

    /**
     * Check if monthly prompt was already shown this month.
     */
    @Query("SELECT COUNT(dp) > 0 FROM DonationPrompt dp WHERE dp.user = :user AND dp.promptType = 'MONTHLY' AND dp.shownAt >= :monthStart")
    boolean hasMonthlyPromptThisMonth(@Param("user") User user, @Param("monthStart") Date monthStart);

    @Query("SELECT COUNT(dp) FROM DonationPrompt dp WHERE dp.donated = true")
    long countDonatedPrompts();

    @Query("SELECT COUNT(dp) FROM DonationPrompt dp WHERE dp.donated = true AND dp.user IS NOT NULL")
    long countDonationEvents();

    @Query("SELECT COUNT(DISTINCT dp.user.id) FROM DonationPrompt dp WHERE dp.donated = true")
    long countDistinctDonors();

    @Query("SELECT COALESCE(SUM(dp.donationAmount), 0) FROM DonationPrompt dp WHERE dp.donated = true AND dp.donationAmount IS NOT NULL")
    BigDecimal sumDonations();

    @Query("SELECT COALESCE(AVG(dp.donationAmount), 0) FROM DonationPrompt dp WHERE dp.donated = true AND dp.donationAmount IS NOT NULL")
    Double averageDonationAmount();

    @Query("SELECT COUNT(dp) FROM DonationPrompt dp WHERE dp.promptType = :type")
    long countByPromptType(@Param("type") DonationPrompt.PromptType type);
}
