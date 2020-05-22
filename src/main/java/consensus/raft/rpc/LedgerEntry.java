package consensus.raft.rpc;

import consensus.net.data.IncomingMessage;

public class LedgerEntry {
    public final int index;
    public final int term;
    public final IncomingMessage message;

    public LedgerEntry(int index, int term, IncomingMessage message) {
        this.index = index;
        this.term = term;
        this.message = message;
    }

    public String toString() {
        return String.format("(%d, %d): from %d: %s", index, term, message.src, message.msg.data);
    }
}