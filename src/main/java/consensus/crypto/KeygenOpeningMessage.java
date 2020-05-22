package consensus.crypto;

public class KeygenOpeningMessage extends CryptoMessage {
    public static final CryptoMessageKind KIND = CryptoMessageKind.KEYGEN_OPENING;
    public final GroupElement y_i;
    public final ProofKnowDlog proof;

    public KeygenOpeningMessage(String sessionId, LocalKeygenShare share) {
        super(KIND, sessionId);
        this.y_i = share.y_i;
        this.proof = share.proof;
        var y_iEncoded = CryptoUtils.b64Encode(share.y_i.asBytes());
        var proofEncoded = share.proof.asJson();
        this.append("y_i", y_iEncoded);
        this.append("proof", proofEncoded);
    }

    protected KeygenOpeningMessage(String sessionId, GroupElement y_i, ProofKnowDlog proof) {
        super(KIND, sessionId);
        this.y_i = y_i;
        this.proof = proof;
    }

    public boolean verify(KeygenCommitMessage commit) {
        return commit.verify(y_i) && proof.verify()
            && proof.y.equals(y_i) && proof.g.equals(commit.g);
    }
}
