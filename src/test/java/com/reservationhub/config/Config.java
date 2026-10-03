package com.reservationhub.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Single source of environment configuration.
 * Lookup order per key: JVM system property, then environment variable, then config.properties.
 */
public final class Config {

    private static final Properties DEFAULTS = loadDefaults();

    private Config() {
    }

    public static String baseUrl() {
        return get("base.url");
    }

    public static String username() {
        return get("auth.username");
    }

    public static String password() {
        return get("auth.password");
    }

    public static int connectTimeoutMs() {
        return getInt("http.connect.timeout.ms");
    }

    public static int readTimeoutMs() {
        return getInt("http.read.timeout.ms");
    }

    public static int warmupTimeoutSeconds() {
        return getInt("warmup.timeout.seconds");
    }

    public static int retryMaxAttempts() {
        return getInt("retry.max.attempts");
    }

    public static long retryBackoffMs() {
        return getInt("retry.backoff.ms");
    }

    public static int perfConcurrentCreates() {
        return getInt("perf.concurrent.creates");
    }

    public static long perfP95ThresholdMs() {
        return getInt("perf.p95.threshold.ms");
    }

    private static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    private static String get(String key) {
        String fromSystem = System.getProperty(key);
        if (fromSystem != null) {
            return fromSystem;
        }
        String fromEnv = System.getenv(key.toUpperCase().replace('.', '_'));
        if (fromEnv != null) {
            return fromEnv;
        }
        String fromFile = DEFAULTS.getProperty(key);
        if (fromFile == null) {
            throw new IllegalStateException("Missing configuration key: " + key);
        }
        return fromFile;
    }

    private static Properties loadDefaults() {
        Properties props = new Properties();
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new IllegalStateException("config.properties not found on the test classpath");
            }
            props.load(in);
            return props;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
