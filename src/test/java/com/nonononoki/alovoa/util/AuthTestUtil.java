package com.nonononoki.alovoa.util;

import java.util.Collection;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.service.AuthService;

public class AuthTestUtil {

	/**
	 * See {@link #setAuthTo(User, String, Collection)}
	 *
	 * @see #setAuthTo(User, String, Collection)
	 */
	public static void setAuthTo(User user) {
		setAuthTo(user, user.getPassword(), user.getAuthorities());
	}

	/**
	 * Sets the currently logged in user to the provided user.
	 * The user doesn't necessarily need to exist but calls to
	 * {@link AuthService#getCurrentUser()} will return null then.
	 *
	 * @param user User to set as current User
	 * @param password Password user by the User
	 * @param authorities Roles granted to the User
	 */
	public static void setAuthTo(User user, String password, Collection<? extends GrantedAuthority> authorities) {
		Authentication auth = new UsernamePasswordAuthenticationToken(user, password,
				authorities);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}
}
