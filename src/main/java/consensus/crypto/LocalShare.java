package consensus.crypto;
import java.math.BigInteger;

public class LocalShare {
    public final byte[] commitment;
    public BigInteger x_i;
    public GroupElement y_i;
    public ProofKnowDlog proof;

    public LocalShare(CryptoContext ctx) {
        this.x_i = ctx.nextPower();
        this.y_i = ctx.g.pow(x_i);
        this.proof = new ProofKnowDlog(ctx, ctx.g, x_i, y_i);
        this.commitment = CryptoUtils.hash(y_i);
    }
}