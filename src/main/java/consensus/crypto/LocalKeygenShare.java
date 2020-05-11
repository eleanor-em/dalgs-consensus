package consensus.crypto;
import java.math.BigInteger;

public class LocalKeygenShare {
    public final byte[] commitment;
    public final BigInteger x_i;
    public final GroupElement y_i;
    public final ProofKnowDlog proof;
    public final GroupElement g;

    public LocalKeygenShare(CryptoContext ctx) {
        this.x_i = ctx.nextPower();
        this.y_i = ctx.g.pow(x_i);
        this.proof = new ProofKnowDlog(ctx, ctx.g, x_i, y_i);
        this.commitment = CryptoUtils.hash(y_i);
        this.g = ctx.g;
    }
}