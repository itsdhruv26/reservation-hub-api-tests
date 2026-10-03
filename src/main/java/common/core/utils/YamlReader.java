package common.core.utils;

import common.exception.FrameworkException;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads environment-specific test data from {@code yml/{env}.yml} on the classpath, addressed with dot
 * notation, e.g. {@code getYamlValues("USERS.admin")} or {@code getYamlValue("USERS.admin.username")}.
 * The file is parsed once; every call returns a copy, so a test may change what it gets back.
 */
@SuppressWarnings("unchecked")
public final class YamlReader {

    private static volatile Map<String, Object> data;

    private YamlReader() {
    }

    public static Map<String, Object> getYamlValues(String token) {
        Object node = lookup(token);
        if (!(node instanceof Map)) {
            throw new FrameworkException("[YamlReader] '" + token + "' in " + file() + " is not a section");
        }
        return new LinkedHashMap<>((Map<String, Object>) node);
    }

    public static String getYamlValue(String token) {
        Object node = lookup(token);
        if (node instanceof Map) {
            throw new FrameworkException("[YamlReader] '" + token + "' in " + file() + " is a section, not a value");
        }
        return String.valueOf(node);
    }

    private static Object lookup(String token) {
        Object node = data();
        for (String key : token.split("\\.")) {
            if (!(node instanceof Map) || !((Map<String, Object>) node).containsKey(key)) {
                throw new FrameworkException("[YamlReader] Key '" + key + "' of '" + token + "' not found in " + file());
            }
            node = ((Map<String, Object>) node).get(key);
        }
        return node;
    }

    private static Map<String, Object> data() {
        if (data == null) {
            synchronized (YamlReader.class) {
                if (data == null) {
                    data = load();
                }
            }
        }
        return data;
    }

    private static Map<String, Object> load() {
        try (InputStream in = YamlReader.class.getClassLoader().getResourceAsStream(file())) {
            if (in == null) {
                throw new FrameworkException("[YamlReader] " + file() + " not found on the classpath");
            }
            return new Yaml().load(in);
        } catch (IOException e) {
            throw new FrameworkException("[YamlReader] Failed to read " + file(), e);
        }
    }

    private static String file() {
        return "yml/" + ConfigLoader.getInstance().getEnv() + ".yml";
    }
}
