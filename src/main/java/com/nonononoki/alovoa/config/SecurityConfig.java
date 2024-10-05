package com.nonononoki.alovoa.config;

import com.nonononoki.alovoa.component.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${app.text.key}")
    private String key;

    @Value("${app.login.remember.key}")
    private String rememberKey;

    @Value("${app.url.front-end}")
    private String urlFrontEnd;

    @Value("${app.domain}")
    private String domain;

    @Autowired
    private Environment env;

    @Autowired
    private AuthFailureHandler failureHandler;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    private final AuthenticationConfiguration configuration;

    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String COOKIE_SESSION = "JSESSIONID";
    public static final String COOKIE_REMEMBER = "remember-me";

    public static String getRoleUser() {
        return ROLE_USER;
    }

    public static String getRoleAdmin() {
        return ROLE_ADMIN;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        AuthenticationManagerBuilder authenticationManagerBuilder = http
                .getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.authenticationProvider(authProvider());

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin").hasAnyAuthority(ROLE_ADMIN)
                        .requestMatchers("/admin/**").hasAnyAuthority(ROLE_ADMIN)
                        .requestMatchers("/css/**").permitAll()
                        .requestMatchers("/js/**").permitAll()
                        .requestMatchers("/img/**").permitAll()
                        .requestMatchers("/font/**").permitAll()
                        .requestMatchers("/json/**").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/login/**").permitAll()
                        .requestMatchers("/terms-conditions").permitAll()
                        .requestMatchers("/imprint").permitAll()
                        .requestMatchers("/imprint/*").permitAll()
                        .requestMatchers("/privacy").permitAll()
                        .requestMatchers("/faq").permitAll()
                        .requestMatchers("/tos").permitAll()
                        .requestMatchers("/register").permitAll()
                        .requestMatchers("/register/**").permitAll()
                        .requestMatchers("/captcha/**").permitAll()
                        .requestMatchers("/donate-list").permitAll()
                        .requestMatchers("/donate/received/**").permitAll()
                        .requestMatchers("/password/**").permitAll()
                        .requestMatchers("/favicon.ico").permitAll()
                        .requestMatchers("/sw.js").permitAll()
                        .requestMatchers("/robots.txt").permitAll()
                        .requestMatchers("/.well-known/assetlinks.json").permitAll()
                        .requestMatchers("/text/*").permitAll()
                        .requestMatchers("/manifest/**").permitAll()
                        .requestMatchers("/fonts/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/info").permitAll()
                        .requestMatchers("/user/delete-account-confirm").permitAll()
                        .requestMatchers("/delete-account/*").permitAll()
                        .requestMatchers("/media/*").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(formLogin -> formLogin
                        .loginPage("/login")
                        .permitAll()
                ).logout(logout ->
                        logout.deleteCookies("remove")
                                .invalidateHttpSession(true)
                                .deleteCookies(COOKIE_SESSION, COOKIE_REMEMBER)
                                .logoutUrl("/logout")
                                .logoutSuccessUrl("/?logout")
                ).oauth2Login(login -> login.loginPage("/?auth-error").defaultSuccessUrl("/login/oauth2/success"))
                .addFilterBefore(authenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .rememberMe(remember -> remember.rememberMeServices(oAuthRememberMeServices()).key(rememberKey))
                .sessionManagement(session -> session.maximumSessions(10).expiredSessionStrategy(getSessionInformationExpiredStrategy())
                        .sessionRegistry(sessionRegistry()))
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration configuration = new CorsConfiguration();
                    configuration.setAllowCredentials(true);
                    configuration.setAllowedOrigins(List.of(domain, urlFrontEnd));
                    configuration.setAllowedMethods(List.of("*"));
                    configuration.setAllowedHeaders(List.of("*"));
                    return configuration;
                }))
                .securityContext((securityContext) -> securityContext.requireExplicitSave(false));

        if (env.acceptsProfiles(Profiles.of("prod"))) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }
        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager() throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    AuthSuccessHandler successHandler() {
        return new AuthSuccessHandler(this);
    }

    @Bean
    AuthFilter authenticationFilter() throws Exception {
        AuthFilter filter = new AuthFilter();
        filter.setAuthenticationManager(authenticationManager());
        filter.setAuthenticationSuccessHandler(successHandler());
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
        return new CompositeSessionAuthenticationStrategy(stratList);
    }

    public SessionInformationExpiredStrategy getSessionInformationExpiredStrategy() {
        return new SimpleRedirectSessionInformationExpiredStrategy("/logout");
    }

    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    TokenBasedRememberMeServices rememberMeServices() {
        return new TokenBasedRememberMeServices(rememberKey, customUserDetailsService);
    }

    @Bean
    TokenBasedRememberMeServices oAuthRememberMeServices() {
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

    public CustomTokenBasedRememberMeServices getOAuthRememberMeServices() {
        return (CustomTokenBasedRememberMeServices) oAuthRememberMeServices();
    }
}