package com.nonononoki.alovoa.repo;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserVerificationPicture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserVerificationPictureRepository extends JpaRepository<UserVerificationPicture, Long> {
    List<UserVerificationPicture> findTop20ByVerifiedByAdminFalseOrderByDateAsc();

    List<UserVerificationPicture> findByUserNo(User user);

    List<UserVerificationPicture> findByUserYes(User user);
}

