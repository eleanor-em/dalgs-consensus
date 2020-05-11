package consensus.crypto;

public class LocalDecryptShare {
    public final GroupElement a_i;
    public final ProofEqDlogs proof;
    public final GroupElement g;

    public LocalDecryptShare(CryptoContext ctx, KeyShare keyShare, Ciphertext ct) {
        this.a_i = ct.a.pow(keyShare.x_i);
        this.proof = new ProofEqDlogs(ctx, ctx.g, ct.a, keyShare.y_i, a_i, keyShare.x_i);
        this.g = ctx.g;
    }
}