package consensus.crypto;

import com.google.gson.Gson;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class CryptoUtils {
    private CryptoUtils() {
    }

    private static final MessageDigest hasher;

    static {
        try {
            hasher = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Missing SHA-256 support");
        }
    }

    public static byte[] hash(GroupElement... input) {
        synchronized (hasher) {
            for (var elem : input) {
                hasher.update(elem.asBytes());
            }
            return hasher.digest();
        }
    }

    public static byte[] hash(BigInteger input) {
        synchronized (hasher) {
            hasher.update(input.toByteArray());
            return hasher.digest();
        }
    }

    public static byte[] hash(Ciphertext input) {
        synchronized (hasher) {
            hasher.update(input.a.asBytes());
            hasher.update(input.b.asBytes());
            return hasher.digest();
        }
    }

    public static byte[] hash(GroupElement input) {
        synchronized (hasher) {
            hasher.update(input.asBytes());
            return hasher.digest();
        }
    }

    public static String hash(String input) {
        synchronized (hasher) {
            try {
                byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
                byte[] hash = hasher.digest(inputBytes);
                return StringUtils.toHex(hash);
            } catch (Exception exception) {
                return "";
            }
        }
    }

    public static String b64Encode(BigInteger x) {
        return b64Encode(x.toByteArray());
    }

    public static String b64Encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] b64Decode(String b64) {
        return Base64.getDecoder().decode(b64);
    }

    public static BigInteger b64toBigInt(String b64) {
        return new BigInteger(b64Decode(b64));
    }
}
