package com.nonononoki.alovoa.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserRestController {
	
	@GetMapping("/delete/request")
    public void deleteRequest(long userId, String password){
        //TODO
		
    }
}
