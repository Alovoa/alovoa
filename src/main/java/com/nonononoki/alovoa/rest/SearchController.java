package com.nonononoki.alovoa.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.service.SearchService;

@Controller
@RequestMapping("/search")
public class SearchController {
	
	@Autowired
	private SearchService searchService;	

	@GetMapping("/users/{latitude}/{longitude}/{distance}/{search}")
    public String getUsers(Model model, @PathVariable String latitude, @PathVariable String longitude, @PathVariable int distance, @PathVariable int search) throws Exception{
		List<UserDto> users = searchService.search(latitude, longitude, distance, search);
		model.addAttribute("users", users);
		return "fragments :: search-users";
    }
}
