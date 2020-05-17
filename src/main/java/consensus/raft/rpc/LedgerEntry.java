package consensus.raft.rpc;

import consensus.net.data.IncomingMessage;

public class LedgerEntry {
    public final int index;
    public final int term;
    public final IncomingMessage message;

    LedgerEntry(int index, int term, IncomingMessage message) {
        this.index = index;
        this.term = term;
        this.message = message;
    }
}