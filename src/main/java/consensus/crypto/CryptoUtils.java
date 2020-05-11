package consensus.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class CryptoUtils {

    private CryptoUtils() {}

    public static MessageDigest getHasher() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Missing SHA-256 support");
        }
    }

    public static byte[] hash(IByteSerialisable input) {
        var hasher = getHasher();
        hasher.update(input.asBytes());
        return hasher.digest();
    }

    public static String b64Encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] b64Decode(String b64) {
        return Base64.getDecoder().decode(b64);
    }
}
