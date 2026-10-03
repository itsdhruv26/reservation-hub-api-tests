package common.core.utils;

import common.exception.FrameworkException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralized configuration loader. The environment is chosen with {@code -Denv=qa} (or the TEST_ENV environment
 * variable) and selects {@code config/{env}.properties} from the classpath.
 * <p>
 * Resolution order per key:
 * <ol>
 *   <li>System property ({@code -DbaseUrl=...})</li>
 *   <li>Environment variable: the key in upper snake case ({@code baseUrl} → {@code BASE_URL})</li>
 *   <li>Properties file ({@code config/{env}.properties})</li>
 *   <li>FrameworkException with a clear message</li>
 * </ol>
 * Thread-safe singleton.
 */
public final class ConfigLoader {

    private static final String DEFAULT_ENV = "qa";
    private static volatile ConfigLoader configLoader;

    private final String env;
    private final Properties properties;

    private ConfigLoader() {
        env = resolveEnv();
        properties = loadProperties(env);
    }

    public static ConfigLoader getInstance() {
        if (configLoader == null) {
            synchronized (ConfigLoader.class) {
                if (configLoader == null) {
                    configLoader = new ConfigLoader();
                }
            }
        }
        return configLoader;
    }

    public String getEnv()                  { return env; }
    public String getBaseUrl()              { return getPropertyValue("baseUrl"); }
    /** Key of the user under USERS in yml/{env}.yml whose credentials are used for authenticated calls. */
    public String getAuthUser()             { return getPropertyValue("authUser"); }
    public int getConnectTimeoutMs()        { return getInt("connectTimeoutMs"); }
    public int getReadTimeoutMs()           { return getInt("readTimeoutMs"); }
    public int getWarmupTimeoutSeconds()    { return getInt("warmupTimeoutSeconds"); }
    public int getRetryMaxAttempts()        { return getInt("retryMaxAttempts"); }
    public long getRetryBackoffMs()         { return getInt("retryBackoffMs"); }
    public int getBrokenTestRetries()       { return getInt("brokenTestRetries"); }
    public int getPerfConcurrentCreates()   { return getInt("perfConcurrentCreates"); }
    public long getPerfP95ThresholdMs()     { return getInt("perfP95ThresholdMs"); }
    /** Print every reported request and response to the console. */
    public boolean isVerboseConsole()       { return Boolean.parseBoolean(getPropertyValueOrDefault("verbose.console", "false")); }
    /** Add request and response bodies to the Extent report (the method, URL and status are always logged). */
    public boolean isVerboseReport()        { return Boolean.parseBoolean(getPropertyValueOrDefault("verbose.report", "false")); }

    public String getPropertyValue(String key) {
        String value = resolve(key);
        if (value == null) {
            throw new FrameworkException("[ConfigLoader] Required property '" + key + "' not found for env='" + env
                    + "'. Set -D" + key + "=..., the " + toEnvVarName(key) + " environment variable, or add it to config/"
                    + env + ".properties.");
        }
        return value;
    }

    public String getPropertyValueOrDefault(String key, String defaultValue) {
        String value = resolve(key);
        return value != null ? value : defaultValue;
    }

    private int getInt(String key) {
        return Integer.parseInt(getPropertyValue(key));
    }

    private String resolve(String key) {
        String value = System.getProperty(key);
        if (isSet(value)) return value.trim();
        value = System.getenv(toEnvVarName(key));
        if (isSet(value)) return value.trim();
        value = properties.getProperty(key);
        if (isSet(value)) return value.trim();
        return null;
    }

    private static boolean isSet(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /** baseUrl → BASE_URL, verbose.console → VERBOSE_CONSOLE */
    static String toEnvVarName(String key) {
        return key.replaceAll("([a-z0-9])([A-Z])", "$1_$2").replace('.', '_').toUpperCase();
    }

    private static String resolveEnv() {
        String value = System.getProperty("env");
        if (isSet(value)) return value.trim();
        value = System.getenv("TEST_ENV");
        if (isSet(value)) return value.trim();
        System.out.println("[ConfigLoader] -Denv not set, defaulting to '" + DEFAULT_ENV + "'");
        return DEFAULT_ENV;
    }

    private static Properties loadProperties(String env) {
        String file = "config/" + env + ".properties";
        try (InputStream in = ConfigLoader.class.getClassLoader().getResourceAsStream(file)) {
            if (in == null) {
                throw new FrameworkException("[ConfigLoader] " + file + " not found on the classpath. Set -Denv to an "
                        + "environment that has a file under src/main/resources/config/.");
            }
            Properties props = new Properties();
            props.load(in);
            return props;
        } catch (IOException e) {
            throw new FrameworkException("[ConfigLoader] Failed to load " + file, e);
        }
    }
}
