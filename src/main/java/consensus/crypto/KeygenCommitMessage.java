package consensus.crypto;

import java.util.Arrays;

public class KeygenCommitMessage extends CryptoMessage {
    public static final CryptoMessageKind KIND = CryptoMessageKind.KEYGEN_COMMIT;
    public final byte[] commitment;
    public final GroupElement g;

    public KeygenCommitMessage(String sessionId, LocalKeygenShare share) {
        super(KIND, sessionId);
        this.commitment = share.commitment;
        this.g = share.g;

        var encoded = CryptoUtils.b64Encode(this.commitment);
        this.append("commitment", encoded);
        this.append("g", CryptoUtils.b64Encode(g.asBytes()));;
    }

    protected KeygenCommitMessage(String sessionId, byte[] commitment, GroupElement g) {
        super(KIND, sessionId);
        this.commitment = commitment;
        this.g = g;
    }

    public boolean verify(GroupElement opening) {
        return Arrays.equals(CryptoUtils.hash(opening), commitment);
    }
}
