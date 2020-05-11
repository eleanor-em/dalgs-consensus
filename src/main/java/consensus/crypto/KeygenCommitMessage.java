package consensus.crypto;

public class KeygenCommitMessage extends CryptoMessage {
    public static final CryptoMessageKind KIND = CryptoMessageKind.KEYGEN_COMMIT;
    public final byte[] commitment;

    public KeygenCommitMessage(LocalShare share) {
        super(KIND);
        this.commitment = share.commitment;
        var encoded = CryptoUtils.b64Encode(share.commitment);
        this.append("commitment", encoded);
    }

    protected KeygenCommitMessage(String commitment) {
        super(KIND);
        this.commitment = CryptoUtils.b64Decode(commitment);
    }
}
