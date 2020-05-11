package consensus.crypto;

import consensus.net.data.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.Optional;

public abstract class CryptoMessage {
    private static final Logger log = LogManager.getLogger(CryptoMessage.class);
    private final JSONObject doc;
    public final CryptoMessageKind kind;

    protected CryptoMessage(CryptoMessageKind kind) {
        doc = new JSONObject();
        doc.put("kind", kind.name());

        this.kind = kind;
    }

    protected void append(String key, String str) {
        doc.put(key, str);
    }

    protected void append(String key, JSONObject obj) {
        doc.put(key, obj);
    }

    public static Optional<CryptoMessage> tryFrom(CryptoContext ctx, String string) {
        try {
            var doc = new JSONObject(string);
            var maybeKind = CryptoMessageKind.tryFrom(doc.getString("kind"));
            if (maybeKind.isEmpty()) {
                return Optional.empty();
            }

            switch (maybeKind.get()) {
                case KEYGEN_COMMIT:
                    var commitment = doc.getString("commitment");
                    if (commitment != null) {
                        return Optional.of(new KeygenCommitMessage(commitment));
                    } else {
                        return Optional.empty();
                    }

                case KEYGEN_OPENING:
                    var y_i = doc.getString("y_i");
                    var proof = doc.getJSONObject("proof");
                    Optional<ProofKnowDlog> maybeProof = Optional.empty();
                    if (proof != null) {
                        maybeProof = ProofKnowDlog.tryFrom(ctx, proof);
                    }
                    if (y_i != null && maybeProof.isPresent()) {
                        var y_iActual = new GroupElement(ctx.p, new BigInteger(CryptoUtils.b64Decode(y_i)));
                        return Optional.of(new KeygenOpeningMessage(y_iActual, maybeProof.get()));
                    } else {
                        return Optional.empty();
                    }

                default:
                    log.warn("switch missing enum case");
                    return Optional.empty();
            }
        } catch (JSONException e) {
            log.warn("failed parsing JSON: " + e.getMessage());
            return Optional.empty();
        }
    }

    public Message encode() {
        return new Message(doc.toString());
    }
}
