package com.reservationhub.core.tokenmanager;

import com.reservationhub.controllers.apicontroller.AuthApiController;
import common.core.api.Auth;
import common.core.utils.ConfigLoader;
import common.core.utils.YamlReader;

import java.util.Map;

/**
 * Supplies credentials for authenticated calls. The user is named by {@code authUser} in config/{env}.properties
 * and its details are read from {@code USERS.<authUser>} in yml/{env}.yml.
 * <p>
 * A token is cached for one test method only: {@link com.reservationhub.core.BaseTest} calls
 * {@link #invalidateToken()} after every test. The sandbox resets periodically, and a token cached across tests
 * could silently go stale mid-run.
 */
public final class BookingTokenManager {

    private static final ThreadLocal<String> AUTH_TOKEN = new ThreadLocal<>();

    private BookingTokenManager() {
    }

    /** Token for the configured user, fetched on first use within the current test. */
    public static String getToken() {
        if (AUTH_TOKEN.get() == null) {
            AUTH_TOKEN.set(new AuthApiController().generateToken(getAuthUserDetails()));
        }
        return AUTH_TOKEN.get();
    }

    public static Auth getTokenAuth() {
        return Auth.token(getToken());
    }

    public static Auth getBasicAuth() {
        Map<String, Object> user = getAuthUserDetails();
        return Auth.basic(user.get("username").toString(), user.get("password").toString());
    }

    public static Map<String, Object> getAuthUserDetails() {
        return YamlReader.getYamlValues("USERS." + ConfigLoader.getInstance().getAuthUser());
    }

    public static void invalidateToken() {
        AUTH_TOKEN.remove();
    }
}
