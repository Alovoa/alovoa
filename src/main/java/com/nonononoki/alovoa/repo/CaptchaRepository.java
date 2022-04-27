package com.nonononoki.alovoa.repo;

import java.util.Date;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.Captcha;

public interface CaptchaRepository extends JpaRepository<Captcha, Long> {

	void deleteByDateBefore(Date date);

	Captcha findByHashCode(String hashCode);
}
