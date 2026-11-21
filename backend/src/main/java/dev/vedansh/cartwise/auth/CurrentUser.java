package dev.vedansh.cartwise.auth;

import org.springframework.security.oauth2.jwt.Jwt;

/** Reads the user id that AuthService puts into every token. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {
        Object uid = jwt.getClaim("uid");
        return uid instanceof Number n ? n.longValue() : Long.valueOf(String.valueOf(uid));
    }
}
