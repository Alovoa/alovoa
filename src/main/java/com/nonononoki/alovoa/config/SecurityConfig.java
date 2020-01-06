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

import com.nonononoki.alovoa.component.AuthProvider;

@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter{
	
	@Value("${app.text.key}")
	private String key;
	
	@Autowired
    private AuthProvider authProvider;
	 
	@Override
	public void configure(HttpSecurity http) throws Exception {
	    http
	    .authenticationProvider(authProvider)
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
	        .antMatchers("/register").permitAll()
	        .antMatchers("/register/**").permitAll()
	        .antMatchers("/captcha/**").permitAll()
        .anyRequest().authenticated()
        .and()
        .formLogin()
	        .loginPage("/login")
	        .defaultSuccessUrl("/profile", true)
	        .failureUrl("/login?error")
	        .and()
	        .logout().deleteCookies("JSESSIONID")
	        .logoutUrl("/logout")
	        .logoutSuccessUrl("/?logout")
        .and()
        .rememberMe().key(key);
        //.and().csrf().disable();
	}
	
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(authProvider);
    }
	
	@Bean
	public PasswordEncoder passwordEncoder() {
	    return new BCryptPasswordEncoder();
	}
}