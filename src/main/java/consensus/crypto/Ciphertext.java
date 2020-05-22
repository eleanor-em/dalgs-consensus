package consensus.crypto;

import org.json.JSONObject;

import java.util.Optional;

public class Ciphertext {
    public final GroupElement a;
    public final GroupElement b;

    Ciphertext(GroupElement a, GroupElement b) {
        this.a = a;
        this.b = b;
    }

    public JSONObject asJson() {
        var doc = new JSONObject();
        return doc.put("a", CryptoUtils.b64Encode(a.asBytes()))
                  .put("b", CryptoUtils.b64Encode(b.asBytes()));
    }

    public static Optional<Ciphertext> tryFrom(CryptoContext ctx, JSONObject doc) {
        if (doc == null) {
            return Optional.empty();
        }

        var aStr = doc.optString("a");
        var bStr = doc.optString("b");

        if (aStr.length() > 0 && bStr.length() > 0) {
            var a = CryptoUtils.b64toBigInt(aStr);
            var b = CryptoUtils.b64toBigInt(bStr);
            return Optional.of(new Ciphertext(new GroupElement(ctx.p, a),
                                              new GroupElement(ctx.p, b)));
        } else {
            return Optional.empty();
        }
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
        return String.format("(%s, %s)", a, b);
    }
}
