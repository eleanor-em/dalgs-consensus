package consensus.net.data;

import java.util.Optional;

public class OutgoingMessage {
    public final Message msg;
    public final Optional<Integer> dest;

    public OutgoingMessage(Message msg) {
        this.msg = msg;
        this.dest = Optional.empty();
    }

    public OutgoingMessage(Message msg, int dest) {
        this.msg = msg;
        this.dest = Optional.of(dest);
    }
}
