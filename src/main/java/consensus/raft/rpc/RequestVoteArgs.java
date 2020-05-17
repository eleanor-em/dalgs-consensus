package consensus.raft.rpc;

public class RequestVoteArgs {
    public final int term;
    public final int candidateId;
    public final int lastLogIndex;
    public final int lastLogTerm;

    public RequestVoteArgs(int term, int candidateId, int lastLogIndex, int lastLogTerm) {
        this.term = term;
        this.candidateId = candidateId;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }
}
