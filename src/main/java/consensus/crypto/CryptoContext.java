package consensus.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;

public class CryptoContext {
    private final SecureRandom rng = new SecureRandom();
    public final GroupElement g;
    final BigInteger p;
    final BigInteger q;

    public CryptoContext(BigInteger p) {
        this.g = new GroupElement(p, 4);
        this.p = p;
        this.q = p.subtract(BigInteger.ONE).divide(BigInteger.TWO);
    }

    public GroupElement id() {
        return new GroupElement(p, BigInteger.ONE);
    }

    public BigInteger nextPower() {
        byte[] bytes = new byte[q.bitCount()];
        rng.nextBytes(bytes);
        return new BigInteger(bytes).mod(q);
    }
}
