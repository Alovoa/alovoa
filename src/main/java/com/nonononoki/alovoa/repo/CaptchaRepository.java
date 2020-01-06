package com.nonononoki.alovoa.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.Captcha;

public interface CaptchaRepository extends JpaRepository<Captcha, Long> {

}

