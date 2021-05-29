package com.nonononoki.alovoa.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.WebPushDto;
import com.nonononoki.alovoa.service.NotificationService;

@RestController
@RequestMapping("/notification")
public class NotificationController {

	@Autowired
	private NotificationService notificationService;

	@ResponseBody
	@PostMapping(value = "/subscribe")
	public void subscribe(@RequestBody WebPushDto webPush) throws AlovoaException {
		notificationService.subscribe(WebPushDto.toEntity(webPush));
	}
}
