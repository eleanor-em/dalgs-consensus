package consensus.crypto;

import java.math.BigInteger;

public class GroupElement implements IByteSerialisable {
    private final BigInteger p;
    private final BigInteger q;
    final BigInteger value;

    GroupElement(CryptoContext ctx, BigInteger value) {
        this(ctx.p, value);
    }
    GroupElement(BigInteger p, long value) {
        this(p, BigInteger.valueOf(value));
    }
    GroupElement(BigInteger p, BigInteger value) {
        this.p = p;
        this.q = p.subtract(BigInteger.ONE).divide(BigInteger.TWO);
        this.value = value;
    }

    public GroupElement mul(BigInteger rhs) {
        return mul(new GroupElement(p, rhs));
    }

    public GroupElement mul(GroupElement rhs) {
        return new GroupElement(p, value.multiply(rhs.value).mod(p));
    }

    public GroupElement pow(BigInteger n) {
        return new GroupElement(p, value.modPow(n, p));
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object rhs) {
        if (rhs instanceof GroupElement) {
            GroupElement o = (GroupElement) rhs;
            return value.equals(o.value);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return value.toString(36);
    }

    @Override
    public byte[] asBytes() {
        return value.toByteArray();
    }
}
