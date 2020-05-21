package consensus.crypto;

import org.json.JSONObject;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;

class ProofKnowDlog {
    public final GroupElement g;
    public final GroupElement y;
    public final GroupElement a;
    public final BigInteger r;

    public ProofKnowDlog(CryptoContext ctx, GroupElement g, BigInteger x, GroupElement y) {
        this.g = g;
        this.y = y;

        var z = ctx.nextPower();
        this.a = g.pow(z);

        var c = getChallenge();
        this.r = z.add(c.multiply(x)).mod(ctx.q);
    }

    private ProofKnowDlog(GroupElement g, GroupElement y, GroupElement a, BigInteger r) {
        this.g = g;
        this.y = y;
        this.a = a;
        this.r = r;
    }

    public boolean verify() {
        var c = getChallenge();
        return g.pow(r).equals(a.mul(y.pow(c)));
    }

    public BigInteger getChallenge() {
        return new BigInteger(CryptoUtils.hash(g, y, a));
    }

    public JSONObject asJson() {
        return new JSONObject()
                .put("g", CryptoUtils.b64Encode(g.asBytes()))
                .put("y", CryptoUtils.b64Encode(y.asBytes()))
                .put("a", CryptoUtils.b64Encode(a.asBytes()))
                .put("r", CryptoUtils.b64Encode(r.toByteArray()));
    }

    public static Optional<ProofKnowDlog> tryFrom(CryptoContext ctx, JSONObject obj) {
        if (obj == null) {
            return Optional.empty();
        }

        var gStr = obj.optString("g");
        var yStr = obj.optString("y");
        var aStr = obj.optString("a");
        var rStr = obj.optString("r");

        if (gStr.length() > 0 && yStr.length() > 0 && aStr.length() > 0 && rStr.length() > 0) {
            var g = new GroupElement(ctx.p, CryptoUtils.b64toBigInt(gStr));
            var y = new GroupElement(ctx.p, CryptoUtils.b64toBigInt(yStr));
            var a = new GroupElement(ctx.p, CryptoUtils.b64toBigInt(aStr));
            var r = CryptoUtils.b64toBigInt(rStr);
            return Optional.of(new ProofKnowDlog(g, y, a, r));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return this.asJson().toString(4);
    }
}
