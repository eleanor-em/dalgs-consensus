package consensus.crypto;

import java.math.BigInteger;

public class Ciphertext {
    private final GroupElement a;
    private final GroupElement b;

    Ciphertext(GroupElement a, GroupElement b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public int hashCode() {
        return (a.toString() + b.toString()).hashCode();
    }

    @Override
    public boolean equals(Object rhs) {
        if (rhs instanceof Ciphertext) {
            Ciphertext o = (Ciphertext) rhs;
            return a.equals(o.a) && b.equals(o.b);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", a.value.toString(36), b.value.toString(36));
    }
}
