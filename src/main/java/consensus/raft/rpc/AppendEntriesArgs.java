package consensus.raft.rpc;

import java.util.List;

public class AppendEntriesArgs {
    public final int term;
    public final int leaderId;
    public final int prevLogIndex;
    public final int prevLogTerm;
    public final List<LedgerEntry> entries;
    public final int leaderCommit;

    public AppendEntriesArgs(int term, int leaderId, int prevLogIndex, int prevLogTerm,
                             List<LedgerEntry> entries, int leaderCommit) {
        this.term = term;
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = entries;
        this.leaderCommit = leaderCommit;
    }
}
