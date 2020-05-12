package consensus.crypto;

import java.util.EnumSet;
import java.util.Optional;

public enum CryptoMessageKind {
    KEYGEN_COMMIT,
    KEYGEN_OPENING,
    POST_VOTE,
    DECRYPT_SHARE;

    public static Optional<CryptoMessageKind> tryFrom(String name) {
        for (var kind : EnumSet.allOf(CryptoMessageKind.class)) {
            if (kind.name().equals(name)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }
}
