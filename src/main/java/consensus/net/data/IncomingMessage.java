package consensus.net.data;

import consensus.util.Validation;

import java.util.Arrays;
import java.util.Optional;

public class IncomingMessage {
    public final Message msg;
    public final int src;
    private static final String DELIM = ":";

    public IncomingMessage(Message msg, int src) {
        this.msg = msg;
        this.src = src;
    }

    public static Optional<IncomingMessage> tryFrom(String str) {
        var split = str.split(DELIM);
        if (split.length < 2) {
            return Optional.empty();
        }

        var maybeId = Validation.tryParseUInt(split[0]);
        return maybeId.flatMap(id -> {
            var data = String.join(DELIM, Arrays.copyOfRange(split, 1, split.length));
            var message = Message.tryFrom(data);
            return message.map(m -> new IncomingMessage(m, id));
        });
    }

    public String encoded() {
        return String.format("%d%s%s", src, DELIM, msg.encoded());
    }
}
