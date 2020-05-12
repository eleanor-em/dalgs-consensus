package consensus.crypto;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

class CryptoUtils {
    private CryptoUtils() {}

    public static MessageDigest getHasher() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Missing SHA-256 support");
        }
    }

    public static byte[] hash(BigInteger input) {
        var hasher = getHasher();
        hasher.update(input.toByteArray());
        return hasher.digest();
    }

    public static byte[] hash(Ciphertext input) {
        var hasher = getHasher();
        hasher.update(input.a.asBytes());
        hasher.update(input.b.asBytes());
        return hasher.digest();
    }

    public static byte[] hash(GroupElement input) {
        var hasher = getHasher();
        hasher.update(input.asBytes());
        return hasher.digest();
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
