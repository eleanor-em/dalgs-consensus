package consensus.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Optional;


public class KeyShare {
    private final CryptoContext ctx;
    public final BigInteger x_i;
    public final GroupElement y_i;
    public final GroupElement publicKey;

    public KeyShare(CryptoContext ctx, GroupElement publicKey, LocalShare share) {
        this.ctx = ctx;
        this.x_i = share.x_i;
        this.y_i = share.y_i;
        this.publicKey = publicKey;
    }

    public Optional<Ciphertext> encrypt(BigInteger value) {
        if (value.equals(BigInteger.ZERO) || value.compareTo(ctx.q) >= 0) {
            return Optional.empty();
        }

        // Choose randomness
        var r = ctx.nextPower();
        // Encode into subgroup
        var encoded = value.modPow(BigInteger.TWO, ctx.p);

        var a = ctx.g.pow(r);
        var b = publicKey.pow(r).mul(encoded);
        return Optional.of(new Ciphertext(a, b));
    }

    @Override
    public String toString() {
        return String.format("KeyShare: pk = %s, secret share = %s", publicKey.toString(), x_i);
    }
}
