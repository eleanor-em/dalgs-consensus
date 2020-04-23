package consensus.util;

import java.util.Optional;

/**
 * Static class that contains utility methods to assist with data validation.
 */
public class Validation {
    private Validation() {}

    /**
     * Attempts to parse the string as an integer, returning empty if it fails.
     */
    public static Optional<Integer> tryParseInt(String string) {
        if (string.matches("\\d+")) {
            return Optional.of(Integer.parseInt(string));
        } else {
            return Optional.empty();
        }
    }
}
