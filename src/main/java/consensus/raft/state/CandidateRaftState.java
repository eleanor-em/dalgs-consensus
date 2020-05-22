package consensus.raft.state;

import consensus.IConsensusClient;
import consensus.net.Actor;
import consensus.raft.rpc.RequestVoteArgs;
import consensus.raft.rpc.RpcMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class CandidateRaftState extends AbstractRaftState {
    private static final Logger log = LogManager.getLogger(CandidateRaftState.class);
    private final Set<Integer> votesReceived = new HashSet<>();
    private final Timer timer = new Timer();

    CandidateRaftState(int id, int serverCount, Actor actor, IConsensusClient client) {
        super(id, serverCount, actor, client);
    }

    void startElection() {
        synchronized (lock) {
            timer.reset();
            votesReceived.clear();

            // Grant vote to self
            ++currentTerm;
            votedFor = Optional.of(id);
            votesReceived.add(id);

            log.debug(id + ": running election for term " + currentTerm);
        }

        // Request votes from others
        var args = new RequestVoteArgs(this.currentTerm, this.id, this.lastLogIndex, this.lastLogTerm);
        this.sendMessageToAll(new RpcMessage(args), result -> {
            if (result.success) {
                votesReceived.add(result.id);
            }
        });
    }

    @Override
    protected AbstractRaftState onTick() {
        // Check if we should yield or be promoted
        if (shouldBecomeFollower) {
            log.debug(id + ": yielding candidacy");
            return this.asFollower();
        } else if (votesReceived.size() > serverCount / 2) {
            return this.asLeader();
        } else {
            // Check if we need to restart our election
            if (timer.expired()) {
                startElection();
            }
            return this;
        }
    }
}
