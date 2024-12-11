package com.nonononoki.alovoa.rest;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.SearchService;

@Controller
@RequestMapping("/search")
public class SearchController {

	@Autowired
	private SearchService searchService;

	@Autowired
	private AuthService authService;

	/**
	 * Only used in admin search. For regular search, see {@link ApiController}
	 * @param model
	 * @return The search fragment
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidAlgorithmParameterException
	 * @throws UnsupportedEncodingException
	 * @throws AlovoaException
	 */
	@GetMapping("/users/default")
	public String getUsersDefault(Model model)
			throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
		getUsersDefaultModel(model);
		return "fragments :: search-users";
	}

	public Model getUsersDefaultModel(Model model)
			throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
		if(model == null) {
			model = new ConcurrentModel();
		}
		model.addAttribute("dto", searchService.searchComplete(SearchService.SearchParams.builder().build()));
		model.addAttribute("currUser", authService.getCurrentUser(true));
		return model;
	}

	@Deprecated
	@GetMapping("/users/{latitude}/{longitude}/{distance}/{search}")
	public String getUsers(Model model, @PathVariable Double latitude, @PathVariable Double longitude,
			@PathVariable int distance, @PathVariable int search)
			throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
		model = getUsersModel(model, latitude, longitude, distance, search);
		return "fragments :: search-users";
	}

	@Deprecated
	public Model getUsersModel(Model model, Double latitude, Double longitude, int distance, int sortId)
			throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
		if(model == null) {
			model = new ConcurrentModel();
		}
		model.addAttribute("dto", searchService.searchComplete(SearchService.SearchParams.builder()
				.latitude(latitude).longitude(longitude)
				.distance(distance).sort(sortId).build()));
		model.addAttribute("currUser", authService.getCurrentUser(true));
		return model;
	}
}
