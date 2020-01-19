package com.nonononoki.alovoa.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nonononoki.alovoa.service.SearchService;

@RestController
@RequestMapping("/search")
public class SearchController {
	
	@Autowired
	private SearchService searchService;	

	@GetMapping("/users/{latitude}/{longitude}/{distance}/{search}")
    public void getUsers(@PathVariable String latitude, @PathVariable String longitude, @PathVariable int distance, @PathVariable int search) throws Exception{
		searchService.search(latitude, longitude, distance, search);
    }
}
