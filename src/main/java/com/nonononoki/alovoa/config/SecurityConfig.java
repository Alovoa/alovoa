package com.nonononoki.alovoa.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;
import org.springframework.security.web.session.SimpleRedirectSessionInformationExpiredStrategy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.nonononoki.alovoa.component.AuthFilter;
import com.nonononoki.alovoa.component.AuthProvider;
import com.nonononoki.alovoa.component.CustomTokenBasedRememberMeServices;
import com.nonononoki.alovoa.component.CustomUserDetailsService;
import com.nonononoki.alovoa.component.FailureHandler;
import com.nonononoki.alovoa.component.SuccessHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	@Value("${app.text.key}")
	private String key;

	@Value("${app.login.remember.key}")
	private String rememberKey;

	@Autowired
	private SuccessHandler successHandler;

	@Autowired
	private FailureHandler failureHandler;

	@Autowired
	private CustomUserDetailsService customUserDetailsService;
	
    private final AuthenticationConfiguration configuration;

	private static final String ROLE_USER = "ROLE_USER";
	private static final String ROLE_ADMIN = "ROLE_ADMIN";
	private static final String COOKIE_SESSION = "JSESSIONID";
	private static final String COOKIE_REMEMBER = "remember-me";

	public static String getRoleUser() {
		return ROLE_USER;
	}

	public static String getRoleAdmin() {
		return ROLE_ADMIN;
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		
		AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
		authenticationManagerBuilder.authenticationProvider(authProvider());
		
		http.authorizeRequests().antMatchers("/admin").hasAnyAuthority(ROLE_ADMIN).antMatchers("/admin/**")
				.hasAnyAuthority(ROLE_ADMIN).antMatchers("/css/**").permitAll().antMatchers("/js/**").permitAll()
				.antMatchers("/img/**").permitAll().antMatchers("/font/**").permitAll().antMatchers("/json/**")
				.permitAll().antMatchers("/oauth2/**").permitAll().antMatchers("/").permitAll().antMatchers("/login/**")
				.permitAll().antMatchers("/terms-conditions").permitAll().antMatchers("/imprint").permitAll()
				.antMatchers("/imprint/*").permitAll().antMatchers("/privacy").permitAll().antMatchers("/faq")
				.permitAll().antMatchers("/tos").permitAll().antMatchers("/register").permitAll()
				.antMatchers("/register/**").permitAll().antMatchers("/captcha/**").permitAll()
				.antMatchers("/donate-list").permitAll().antMatchers("/donate/received/**").permitAll()
				.antMatchers("/password/**").permitAll().antMatchers("/favicon.ico").permitAll().antMatchers("/sw.js")
				.permitAll().antMatchers("/robots.txt").permitAll().antMatchers("/.well-known/assetlinks.json")
				.permitAll().antMatchers("/text/*").permitAll().antMatchers("/manifest/**").permitAll()
				.antMatchers("/fonts/**").permitAll().antMatchers("/error").permitAll()

				.anyRequest().authenticated().and().formLogin().loginPage("/login").and().logout()
				.deleteCookies(COOKIE_SESSION, COOKIE_REMEMBER).logoutUrl("/logout").logoutSuccessUrl("/?logout").and()
				.oauth2Login().loginPage("/login").defaultSuccessUrl("/login/oauth2/success").and()
				.addFilterBefore(authenticationFilter(), UsernamePasswordAuthenticationFilter.class).rememberMe()
				.rememberMeServices(oAuthRememberMeServices()).key(rememberKey);

		http.sessionManagement().maximumSessions(10).expiredSessionStrategy(getSessionInformationExpiredStrategy())
				.sessionRegistry(sessionRegistry());
		http.csrf().disable();
		http.requiresChannel().anyRequest().requiresSecure();
		http.cors();
		return http.build();
	}

    @Bean
    AuthenticationManager authenticationManager() throws Exception {
        return configuration.getAuthenticationManager();
    }
	
	@Bean
	AuthFilter authenticationFilter() throws Exception {
		AuthFilter filter = new AuthFilter();
		filter.setAuthenticationManager(authenticationManager());
		filter.setAuthenticationSuccessHandler(successHandler);
		filter.setAuthenticationFailureHandler(failureHandler);
		filter.setRememberMeServices(rememberMeServices());
		filter.setSessionAuthenticationStrategy(sessionAuthenticationStrategy());
		return filter;
	}

	// https://stackoverflow.com/questions/32463022/sessionregistry-is-empty-when-i-use-concurrentsessioncontrolauthenticationstrate
	public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
		List<SessionAuthenticationStrategy> stratList = new ArrayList<>();
		SessionFixationProtectionStrategy concStrat = new SessionFixationProtectionStrategy();
		stratList.add(concStrat);
		RegisterSessionAuthenticationStrategy regStrat = new RegisterSessionAuthenticationStrategy(sessionRegistry());
		stratList.add(regStrat);
		CompositeSessionAuthenticationStrategy compStrat = new CompositeSessionAuthenticationStrategy(stratList);
		return compStrat;
	}

	public SessionInformationExpiredStrategy getSessionInformationExpiredStrategy() {
		SessionInformationExpiredStrategy strat = new SimpleRedirectSessionInformationExpiredStrategy("/logout");
		return strat;
	}

	@Bean
	SessionRegistry sessionRegistry() {
		return new SessionRegistryImpl();
	}

	@Bean
	TokenBasedRememberMeServices rememberMeServices() throws Exception {
		return new TokenBasedRememberMeServices(rememberKey, customUserDetailsService);
	}

	@Bean
	TokenBasedRememberMeServices oAuthRememberMeServices() throws Exception {
		CustomTokenBasedRememberMeServices rememberMeService = new CustomTokenBasedRememberMeServices(rememberKey,
				customUserDetailsService);
		rememberMeService.setAlwaysRemember(true);
		return rememberMeService;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthProvider authProvider() {
		return new AuthProvider();
	}
	
	@Bean
	CorsFilter corsFilter() {
	    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	    final CorsConfiguration config = new CorsConfiguration();
	    config.setAllowCredentials(true);
	    config.setAllowedOrigins(Collections.singletonList("*"));
	    config.setAllowedHeaders(Arrays.asList("Origin", "Content-Type", "Accept"));
	    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "OPTIONS", "DELETE", "PATCH"));
	    source.registerCorsConfiguration("/**", config);
	    return new CorsFilter(source);
	}
}