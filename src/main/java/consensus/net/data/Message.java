package consensus.net.data;

import java.util.Optional;

public class Message {
    public final String data;

    public Message(String data) {
        this.data = data;
    }

    public static Optional<Message> tryFrom(String message) {
        return Optional.of(new Message(message));
    }

    public String encoded() {
        return data;
    }

    @Override
    public String toString() {
        return data;
    }
}
