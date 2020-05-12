package consensus.crypto;

public class PostVoteMessage extends CryptoMessage {
    public static final CryptoMessageKind KIND = CryptoMessageKind.POST_VOTE;
    public final Ciphertext vote;
    public final ProofKnowDlog proofPlaintextKnow;

    public PostVoteMessage(CryptoContext ctx, KeyShare keyShare, int candidate) {
        super(KIND);

        var r = ctx.nextPower();
        this.vote = keyShare.encrypt(candidate, r);
        this.proofPlaintextKnow = new ProofKnowDlog(ctx, ctx.g, r, vote.a);

        this.append("vote", vote.asJson());
        this.append("proof", proofPlaintextKnow.asJson());
    }

    protected PostVoteMessage(Ciphertext vote, ProofKnowDlog proofPlaintextKnow) {
        super(KIND);
        this.vote = vote;
        this.proofPlaintextKnow = proofPlaintextKnow;
    }

    public boolean verify(CryptoContext ctx) {
        return proofPlaintextKnow.verify() && proofPlaintextKnow.g.equals(ctx.g)
                && vote.a.p.equals(ctx.p) && vote.b.p.equals(ctx.p)
                && proofPlaintextKnow.y.equals(vote.a);
    }
}
