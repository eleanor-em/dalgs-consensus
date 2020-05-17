package consensus.raft.state;

import consensus.IConsensusClient;
import consensus.net.Actor;
import consensus.raft.rpc.AppendEntriesArgs;
import consensus.raft.rpc.RequestVoteArgs;
import consensus.raft.rpc.RpcMessage;
import consensus.raft.rpc.RpcResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

public class CandidateRaftState extends AbstractRaftState {
    private static final Logger log = LogManager.getLogger(CandidateRaftState.class);
    private final Set<Integer> votesReceived = new HashSet<>();
    private final Timer timer = new Timer();

    private boolean shouldBecomeFollower = false;

    CandidateRaftState(int id, int serverCount, Actor actor, IConsensusClient client) {
        super(id, serverCount, actor, client);

        // Grant vote to self
        votesReceived.add(id);
    }

    void startElection() {
        timer.reset();
        votesReceived.clear();
        ++currentTerm;

        // Request votes from others
        var args = new RequestVoteArgs(this.currentTerm, this.id, this.lastLogIndex, this.lastLogTerm);
        this.sendMessageToAll(new RpcMessage(args).encoded());
    }

    @Override
    protected AbstractRaftState onTick() {
        if (shouldBecomeFollower) {
            return this.asFollower();
        } else if (votesReceived.size() >= (serverCount + 1) / 2) {
            // add 1 to ensure strict majority
            return this.asLeader();
        } else {
            if (timer.expired()) {
                startElection();
            }
            return this;
        }
    }

    @Override
    protected void onAppendEntries(AppendEntriesArgs args) {
        if (args.term > currentTerm) {
            shouldBecomeFollower = true;
        }
    }

    @Override
    protected void onRequestVote(RequestVoteArgs args) {
        if (args.term > currentTerm) {
            shouldBecomeFollower = true;
        }
    }

    @Override
    protected void onReceiveResult(RpcResult result) {
        if (result.currentTerm > currentTerm) {
            shouldBecomeFollower = true;
        } else if (result.success) {
            votesReceived.add(result.id);
        }
    }
}
