package com.reservationhub.utilities;

/**
 * Gives a data-provider value a readable name. TestNG and Allure print parameters with toString(), which for
 * a lambda is something like {@code AuthTest$$Lambda@41862a32}; wrapping it prints the name instead.
 */
public record Named<T>(String name, T value) {

    public static <T> Named<T> named(String name, T value) {
        return new Named<>(name, value);
    }

    @Override
    public String toString() {
        return name;
    }
}
