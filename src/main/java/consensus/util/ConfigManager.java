package consensus.util;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * A class to wrap Apache's Commons Config in a safer manner (to avoid NullPointerExceptions).
 */
public class ConfigManager {
    private static final Logger log = LogManager.getLogger(ConfigManager.class);
    private static final OnceAssignable<PropertiesConfiguration> config = new OnceAssignable<>();

    private static final String CONFIG_FILENAME = "consensus.properties";

    /**
     * Load the config properties.
     * Will do nothing if the properties were already loaded.
     */
    public static void loadProperties() {
        FileBasedConfigurationBuilder<PropertiesConfiguration> builder =
                new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
                        .configure(new Parameters()
                                .properties()
                                .setFileName(CONFIG_FILENAME));
        try {
            config.setIfEmpty(builder.getConfiguration());
        } catch (ConfigurationException e) {
            log.error("Failed loading configuration file \"" + CONFIG_FILENAME + "\": " + e.getMessage());
            System.exit(-1);
        }
    }

    public static boolean isDebug() {
        ConfigManager.loadProperties();
        var mode = ConfigManager.getString("mode").orElse("");
        return mode.equalsIgnoreCase("debug");
    }

    /**
     * Looks up the key from the properties, and returns an Optional which is empty if there is no such key,
     * or contains the value if there is such a key.
     *
     * In this case we assume the value is a String.
     */
    public static Optional<String> getString(String key) {
        return Optional.ofNullable(config.get().getString(key));
    }

    /**
     * Looks up the key from the properties, and returns an Optional which is empty if there is no such key,
     * or contains the value if there is such a key.
     *
     * In this case we assume the value is an integer.
     */
    public static Optional<Integer> getInt(String key) {
        return getString(key)
                .flatMap(Validation::tryParseUInt);
    }
}
