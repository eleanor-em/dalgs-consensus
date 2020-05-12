package consensus.crypto;

import java.math.BigInteger;

public class KeyShare {
    private final CryptoContext ctx;
    public final BigInteger x_i;
    public final GroupElement y_i;
    public final GroupElement publicKey;

    public KeyShare(CryptoContext ctx, GroupElement publicKey, LocalKeygenShare share) {
        this.ctx = ctx;
        this.x_i = share.x_i;
        this.y_i = share.y_i;
        this.publicKey = publicKey;
    }
    public Ciphertext encrypt(int value, BigInteger r) {
        return encrypt(BigInteger.valueOf(value), r);
    }

    public Ciphertext encrypt(BigInteger value, BigInteger r) {
        if (value.equals(BigInteger.ZERO) || value.compareTo(ctx.q) >= 0) {
            throw new IllegalArgumentException("Value to encrypt must be > 0 and < q");
        }

        // Encode into subgroup
        var encoded = value.modPow(BigInteger.TWO, ctx.p);

        var a = ctx.g.pow(r);
        var b = publicKey.pow(r).mul(encoded);
        return new Ciphertext(a, b);
    }

    @Override
    public String toString() {
        // Display only a signature of the key, for ease of use.
        // Remove the padding bytes since they don't add anything.
        var pkHash = CryptoUtils.b64Encode(CryptoUtils.hash(publicKey)).replace("=", "");
        var skHash = CryptoUtils.b64Encode(CryptoUtils.hash(x_i)).replace("=", "");

        return String.format("(pk: %s, sk_i: %s)", pkHash, skHash);
    }
}
