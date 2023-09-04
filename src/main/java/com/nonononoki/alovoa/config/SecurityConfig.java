package com.nonononoki.alovoa.config;

// See also https://www.baeldung.com/spring-boot-keycloak

import com.nonononoki.alovoa.component.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
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

import java.util.*;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${app.login.remember.key}")
    private String rememberKey;

    @Value("${app.domain}")
    private String appDomain;

    @Autowired
    private Environment env;

    @Autowired
    private AuthFailureHandler failureHandler;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    private final AuthenticationConfiguration configuration;

    private static String oauthRoleAdmin;

    @Value("${app.oauth2.adminrolename:}")
    private void setOauthAdminRole(String s) { oauthRoleAdmin = s; }

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

    private OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler successHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);

        successHandler.setPostLogoutRedirectUri(appDomain);

        return successHandler;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = http
                .getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.authenticationProvider(authProvider());

        http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
                        .requestMatchers("/admin").hasAnyAuthority(ROLE_ADMIN)
                        .requestMatchers("/admin/**").hasAnyAuthority(ROLE_ADMIN)
						.requestMatchers("/static/element-web").permitAll()
                        .requestMatchers("/static/element-web/**").permitAll()
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
                        .anyRequest().authenticated())
                .formLogin(formLogin -> formLogin
                        .loginPage("/login")
                )
                .logout(logout -> logout
                        .deleteCookies(COOKIE_SESSION, COOKIE_REMEMBER)
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(oidcLogoutSuccessHandler())
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .logoutSuccessUrl("/?logout")
                )
                .oauth2Login(
                        oauth2Login -> oauth2Login
                                .userInfoEndpoint(userInfo -> userInfo
                                        .userAuthoritiesMapper(this.userAuthoritiesMapper()))
                                .loginPage("/login")
                                .defaultSuccessUrl("/login/oauth2/success")

                )
                .addFilterBefore(
                        authenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class
                )
                .rememberMe(rememberMe -> rememberMe
                        .rememberMeServices(oAuthRememberMeServices())
                        .key(rememberKey)
                );

        http.sessionManagement(sessionManagement -> sessionManagement
                .maximumSessions(10)
                .expiredSessionStrategy(getSessionInformationExpiredStrategy())
                .sessionRegistry(sessionRegistry())
        );

        http.csrf(AbstractHttpConfigurer::disable);

        if (env.acceptsProfiles(Profiles.of("prod"))) {
            http.requiresChannel(
                    requiresChannel -> requiresChannel
                            .anyRequest()
                            .requiresSecure()
            );
        }

        http.cors(Customizer.withDefaults());
        logger.debug("filterChain configured");
        return http.build();
    }



    private GrantedAuthoritiesMapper userAuthoritiesMapper() {
		return (authorities) -> {
			Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
            logger.info(String.format("userAuthoritiesMapper authorities: %s", authorities.getClass().getName()));
            authorities.forEach(authority -> {
				if (authority instanceof OidcUserAuthority oidcUserAuthority) {
                    logger.trace("userAuthoritiesMapper oidc path");
                    OidcIdToken idToken = oidcUserAuthority.getIdToken();
					OidcUserInfo userInfo = oidcUserAuthority.getUserInfo();
					// Map the claims found in idToken and/or userInfo
					// to one or more GrantedAuthority's and add it to mappedAuthorities
                    getGroupMembership(mappedAuthorities, oidcUserAuthority.getAttributes());
                } else if (authority instanceof OAuth2UserAuthority oauth2UserAuthority) {
                    logger.trace("userAuthoritiesMapper oauth2 path");
                    getGroupMembership(mappedAuthorities, oauth2UserAuthority.getAttributes());
                } else {
                    authorities.forEach((v) -> {
                        logger.trace(String.format("userAuthoritiesMapper other: %s", v));
                    });
                    logger.trace("userAuthoritiesMapper other path");
                }
			});

			return mappedAuthorities;
		};
	}

    private void getGroupMembership(Set<GrantedAuthority> mappedAuthorities, Map<String, Object> attributes) {
        ArrayList<String> roles = (ArrayList<String>) attributes.get("groups");
        if (roles!=null && roles.contains(oauthRoleAdmin)) {
            logger.info(String.format("User %s is Admin", attributes.get("email")));
            mappedAuthorities.add(new SimpleGrantedAuthority(ROLE_ADMIN));
        } else {
            logger.trace(String.format("User %s is not Admin", attributes.get("email")));
        }
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

    @Bean
    CorsFilter corsFilter() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        final CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("*"));
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    public CustomTokenBasedRememberMeServices getOAuthRememberMeServices() {
        return (CustomTokenBasedRememberMeServices) oAuthRememberMeServices();
    }

}
