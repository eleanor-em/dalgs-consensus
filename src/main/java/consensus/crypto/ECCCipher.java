package consensus.crypto;

import java.security.*;
import java.security.spec.ECGenParameterSpec;

public class ECCCipher {
    private ECCCipher() {
    }

    private static final KeyPairGenerator keyGen;

    static {
        try {
            keyGen = KeyPairGenerator.getInstance("EC");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Missing EC support");
        }
    }

    public static KeyPair generateKeyPair() {
        try {
            keyGen.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
            return keyGen.generateKeyPair();
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException("Failed to generate a pair of keys");
        }
    }

    public static byte[] sign(PrivateKey privateKey, String message) throws Exception {
        Signature signature = Signature.getInstance("SHA1withECDSA");
        signature.initSign(privateKey);
        signature.update(message.getBytes());
        return signature.sign();
    }

    public static boolean verifySignature(PublicKey publicKey, byte[] signature, String message) throws Exception {
        Signature signatureVerifier = Signature.getInstance("SHA1withECDSA");
        signatureVerifier.initVerify(publicKey);
        signatureVerifier.update(message.getBytes());
        return signatureVerifier.verify(signature);
    }
}
