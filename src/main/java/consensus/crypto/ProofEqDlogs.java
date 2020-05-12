package consensus.crypto;

import org.json.JSONObject;

import java.math.BigInteger;
import java.util.Optional;

class ProofEqDlogs {
    public final GroupElement a;
    public final GroupElement b;
    public final GroupElement d;
    public final GroupElement e;
    public final GroupElement g;
    public final GroupElement h;
    public final BigInteger r;

    public ProofEqDlogs(CryptoContext ctx, GroupElement a, GroupElement b, GroupElement d, GroupElement e, BigInteger x) {
        var z = ctx.nextPower();
        this.a = a;
        this.b = b;
        this.d = d;
        this.e = e;
        this.g = a.pow(z);
        this.h = b.pow(z);

        var c = getChallenge();
        this.r = z.add(c.multiply(x)).mod(ctx.q);
    }

    private ProofEqDlogs(GroupElement a, GroupElement b, GroupElement d, GroupElement e, GroupElement g, GroupElement h,
                         BigInteger r) {
        this.a = a;
        this.b = b;
        this.d = d;
        this.e = e;
        this.g = g;
        this.h = h;
        this.r = r;
    }

    public boolean verify() {
        var c = getChallenge();
        return a.pow(r).equals(g.mul(d.pow(c))) && b.pow(r).equals(h.mul(e.pow(c)));
    }

    public BigInteger getChallenge() {
        var hasher = CryptoUtils.getHasher();
        hasher.update(a.asBytes());
        hasher.update(b.asBytes());
        hasher.update(d.asBytes());
        hasher.update(e.asBytes());
        hasher.update(g.asBytes());
        hasher.update(h.asBytes());
        return new BigInteger(hasher.digest());
    }

    public JSONObject asJson() {
        return new JSONObject()
                .put("a", CryptoUtils.b64Encode(a.asBytes()))
                .put("b", CryptoUtils.b64Encode(b.asBytes()))
                .put("d", CryptoUtils.b64Encode(d.asBytes()))
                .put("e", CryptoUtils.b64Encode(e.asBytes()))
                .put("g", CryptoUtils.b64Encode(g.asBytes()))
                .put("h", CryptoUtils.b64Encode(h.asBytes()))
                .put("r", CryptoUtils.b64Encode(r.toByteArray()));
    }

    public static Optional<ProofEqDlogs> tryFrom(CryptoContext ctx, JSONObject obj) {
        if (obj == null) {
            return Optional.empty();
        }

        var aStr = obj.getString("a");
        var bStr = obj.getString("b");
        var dStr = obj.getString("d");
        var eStr = obj.getString("e");
        var gStr = obj.getString("g");
        var hStr = obj.getString("h");
        var rStr = obj.getString("r");

        if (aStr != null && bStr != null && dStr != null && eStr != null && gStr != null && hStr != null) {
            var a = new GroupElement(ctx.p, CryptoUtils.b64toBigInt(aStr));
            var b = new GroupElement(ctx.p, CryptoUtils.b64toBigInt(bStr));
            var d = new GroupElement(ctx.p, CryptoUtils.b64toBigInt(dStr));
            var e = new GroupElement(ctx.p, CryptoUtils.b64toBigInt(eStr));
            var g = new GroupElement(ctx.p, CryptoUtils.b64toBigInt(gStr));
            var h = new GroupElement(ctx.p, CryptoUtils.b64toBigInt(hStr));
            var r = CryptoUtils.b64toBigInt(rStr);
            return Optional.of(new ProofEqDlogs(a, b, d, e, g, h, r));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return this.asJson().toString(4);
    }
}
