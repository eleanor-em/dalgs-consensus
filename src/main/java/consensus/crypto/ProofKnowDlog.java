package consensus.crypto;

import org.json.JSONObject;

import java.math.BigInteger;
import java.util.Optional;

public class ProofKnowDlog {
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
        var hasher = CryptoUtils.getHasher();
        hasher.update(g.asBytes());
        hasher.update(y.asBytes());
        hasher.update(a.asBytes());
        return new BigInteger(hasher.digest());
    }

    public JSONObject asJson() {
        return new JSONObject()
                .put("g", CryptoUtils.b64Encode(g.asBytes()))
                .put("y", CryptoUtils.b64Encode(y.asBytes()))
                .put("a", CryptoUtils.b64Encode(a.asBytes()))
                .put("r", CryptoUtils.b64Encode(r.toByteArray()));
    }

    public static Optional<ProofKnowDlog> tryFrom(CryptoContext ctx, JSONObject obj) {
        var gStr = obj.getString("g");
        var yStr = obj.getString("y");
        var aStr = obj.getString("a");
        var rStr = obj.getString("r");

        if (gStr != null && yStr != null && aStr != null && rStr != null) {
            var g = new GroupElement(ctx.p, new BigInteger(CryptoUtils.b64Decode(gStr)));
            var y = new GroupElement(ctx.p, new BigInteger(CryptoUtils.b64Decode(yStr)));
            var a = new GroupElement(ctx.p, new BigInteger(CryptoUtils.b64Decode(aStr)));
            var r = new BigInteger(CryptoUtils.b64Decode(rStr));
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
