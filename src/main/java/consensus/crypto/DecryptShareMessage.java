package consensus.crypto;

public class DecryptShareMessage extends CryptoMessage {
    public static final CryptoMessageKind KIND = CryptoMessageKind.DECRYPT_SHARE;
    public final GroupElement a_i;
    public final ProofEqDlogs proof;
    public final GroupElement g;

    public DecryptShareMessage(LocalDecryptShare share) {
        super(KIND);
        this.a_i = share.a_i;
        this.proof = share.proof;
        this.g = share.g;

        var a_iEncoded = CryptoUtils.b64Encode(this.a_i.asBytes());
        var proofEncoded = proof.asJson();
        this.append("a_i", a_iEncoded);
        this.append("proof", proofEncoded);
        this.append("g", g);
    }

    protected DecryptShareMessage(GroupElement a_i, ProofEqDlogs proof, GroupElement g) {
        super(KIND);
        this.a_i = a_i;
        this.proof = proof;
        this.g = g;
    }

    public boolean verify() {
        return proof.verify();
    }
}
