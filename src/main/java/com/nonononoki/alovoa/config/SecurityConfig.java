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
public class SecurityConfig extends WebSecurityConfigurerAdapter{
	
	@Value("${app.text.key}")
	private String key;
	
	@Value("${spring.profiles.active}")
	private String profile;
	
	@Autowired
    private AuthProvider authProvider;
	
	@Autowired
	private SuccessHandler successHandler;
	
	@Autowired
	private FailureHandler failureHandler;
	 
	@Override
	public void configure(HttpSecurity http) throws Exception {
	    http
        .authorizeRequests()
        
	        .antMatchers("/css/**").permitAll()
	        .antMatchers("/js/**").permitAll()
	        .antMatchers("/img/**").permitAll()
	        .antMatchers("/font/**").permitAll()
	        .antMatchers("/json/**").permitAll()
	        
	        .antMatchers("/").permitAll()
	        .antMatchers("/login").permitAll()
	        .antMatchers("/terms-conditions").permitAll()
	        .antMatchers("/impressum").permitAll()
	        .antMatchers("/privacy").permitAll()
	        .antMatchers("/faq").permitAll()
	        .antMatchers("/register").permitAll()
	        .antMatchers("/register/**").permitAll()
	        .antMatchers("/captcha/**").permitAll()
	        .antMatchers("/donate-list").permitAll()
	        .antMatchers("/password/**").permitAll()
	        
	        .antMatchers("/favicon.ico").permitAll()
	        .antMatchers("/sw.js").permitAll()
	        .antMatchers("/manifest.json").permitAll()
	        
	        .antMatchers("/text/*").permitAll()
        .anyRequest().authenticated()
        .and()
        .formLogin()
	        .loginPage("/login")
	        //.successHandler(successHandler)
	        //.failureHandler(failureHandler)
	        //.defaultSuccessUrl("/profile", true)
	        //.failureUrl("/login?error")
	        .and()
	        .logout().deleteCookies("JSESSIONID")
	        .logoutUrl("/logout")
	        .logoutSuccessUrl("/?logout")
        .and()
        .addFilterBefore(authenticationFilter(), UsernamePasswordAuthenticationFilter.class)
        .rememberMe().key(key);
        //.and().csrf().disable();
	    if (!profile.equals("dev")) http.requiresChannel().anyRequest().requiresSecure();
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