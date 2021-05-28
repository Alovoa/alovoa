package com.nonononoki.alovoa.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.nonononoki.alovoa.component.AuthFilter;
import com.nonononoki.alovoa.component.AuthProvider;
import com.nonononoki.alovoa.component.FailureHandler;
import com.nonononoki.alovoa.component.SuccessHandler;

@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Value("${app.text.key}")
	private String key;

	@Autowired
	private AuthProvider authProvider;

	@Autowired
	private SuccessHandler successHandler;

	@Autowired
	private FailureHandler failureHandler;

	private static final String ROLE_USER = "ROLE_USER";
	private static final String ROLE_ADMIN = "ROLE_ADMIN";
	
	public static String getRoleUser() {
		return ROLE_USER;
	}
	
	public static String getRoleAdmin() {
		return ROLE_ADMIN;
	}

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests().antMatchers("/admin/**").hasAnyAuthority(ROLE_ADMIN)

				.antMatchers("/css/**").permitAll().antMatchers("/js/**").permitAll().antMatchers("/img/**").permitAll()
				.antMatchers("/font/**").permitAll().antMatchers("/json/**").permitAll().antMatchers("/oauth2/**")
				.permitAll().antMatchers("/").permitAll().antMatchers("/login/**").permitAll()
				.antMatchers("/terms-conditions").permitAll().antMatchers("/imprint").permitAll()
				.antMatchers("/imprint/*").permitAll().antMatchers("/privacy").permitAll().antMatchers("/faq")
				.permitAll().antMatchers("/tos").permitAll().antMatchers("/register").permitAll()
				.antMatchers("/register/**").permitAll().antMatchers("/captcha/**").permitAll()
				.antMatchers("/donate-list").permitAll().antMatchers("/donate/received/**").permitAll()
				.antMatchers("/password/**").permitAll().antMatchers("/favicon.ico").permitAll().antMatchers("/sw.js")
				.permitAll().antMatchers("/text/*").permitAll().antMatchers("/manifest/**").permitAll()
				.antMatchers("/fonts/**").permitAll()

				.anyRequest().authenticated().and().formLogin().loginPage("/login").and().logout()
				.deleteCookies("JSESSIONID").logoutUrl("/logout").logoutSuccessUrl("/?logout").and().oauth2Login()
				.loginPage("/login").defaultSuccessUrl("/login/oauth2/success").and()
				.addFilterBefore(authenticationFilter(), UsernamePasswordAuthenticationFilter.class).rememberMe()
				.key(key);

		http.csrf().ignoringAntMatchers("/donate/received/**");
		http.requiresChannel().anyRequest().requiresSecure();
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.authenticationProvider(authProvider);
	}

	@Bean
	public AuthFilter authenticationFilter() throws Exception {
		AuthFilter filter = new AuthFilter();
		filter.setAuthenticationManager(authenticationManager());
		filter.setAuthenticationSuccessHandler(successHandler);
		filter.setAuthenticationFailureHandler(failureHandler);
		return filter;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}