package consensus.crypto;

import java.math.BigInteger;
import java.util.Optional;


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

    public Ciphertext encrypt(BigInteger value) {
        var r = ctx.nextPower();
        return encrypt(value, r);
    }

    @Override
    public String toString() {
        return String.format("KeyShare: pk = %s, secret share = %s", publicKey.toString(), x_i);
    }
}
