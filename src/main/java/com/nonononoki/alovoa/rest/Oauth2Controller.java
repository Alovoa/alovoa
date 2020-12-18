package com.nonononoki.alovoa.rest;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.html.ProfileResource;
import com.nonononoki.alovoa.html.RegisterResource;
import com.nonononoki.alovoa.repo.UserRepository;

@RestController
@RequestMapping("/")
public class Oauth2Controller {

	//@Autowired
	//private HttpServletRequest request;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private OAuth2AuthorizedClientService clientService;
	
	@Autowired
	private RegisterResource registerResource;
	
	@Autowired
	private ProfileResource profileResource;

	@SuppressWarnings("rawtypes")
	@GetMapping("/login/oauth2/success")
	public ModelAndView oauth2Success() throws Exception {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

		String clientRegistrationId = oauthToken.getAuthorizedClientRegistrationId();

		OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(clientRegistrationId,
				oauthToken.getName());
		String endpoint = client.getClientRegistration().getProviderDetails().getUserInfoEndpoint()
				.getUri();
		
		if (!endpoint.isEmpty()) {
			RestTemplate template = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + client.getAccessToken().getTokenValue());
			HttpEntity<String> entity = new HttpEntity<String>("", headers);

			// get user data via URL from the oauth2 provider
			ResponseEntity<Map> response = template.exchange(endpoint, HttpMethod.GET, entity,
					Map.class);
			Map attributes = response.getBody();
			String email = (String) attributes.get("email");
			email = email.toLowerCase();

			User user = userRepo.findByEmail(email);
			if (user == null) {
				user = new User();
				user.setEmail(email);
			}
			
			//administrator cannot use oauth for security reason e.g. password breach on oath provider
			if (user.isAdmin()) {
				throw new Exception("");
			} 

			if (!user.isConfirmed()) {
				return registerResource.registerOauth(user);				
			} else {
				return profileResource.profile();	
			}
		}

		throw new Exception("");
	}
}
