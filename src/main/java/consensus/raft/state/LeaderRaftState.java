package consensus.raft.state;

import consensus.IConsensusClient;
import consensus.net.Actor;
import consensus.raft.rpc.AppendEntriesArgs;
import consensus.raft.rpc.RequestVoteArgs;
import consensus.raft.rpc.RpcMessage;
import consensus.raft.rpc.RpcResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class LeaderRaftState extends AbstractRaftState {
    private static final Logger log = LogManager.getLogger(LeaderRaftState.class);
    // Volatile leader state
    private final List<Integer> nextIndex = new ArrayList<>();
    private final List<Integer> matchIndex = new ArrayList<>();

    private boolean shouldBecomeFollower = false;

    LeaderRaftState(int id, int serverCount, Actor actor, IConsensusClient client) {
        super(id, serverCount, actor, client);
        leaderId = id;

        for (int i = 0; i < serverCount; ++i) {
            nextIndex.add(lastLogIndex + 1);
            matchIndex.add(0);
        }
    }

    @Override
    protected AbstractRaftState onTick() {
        if (shouldBecomeFollower) {
            return this.asFollower();
        } else {
            var args = new AppendEntriesArgs(currentTerm, id, -1, 0, new ArrayList<>(), commitIndex);
            this.sendMessageToAll(new RpcMessage(args).encoded());
            try {
                Thread.sleep((int) (Math.random() * 2000));
            } catch (InterruptedException ignored) {
            }

            return this;
        }
    }

    @Override
    protected void onAppendEntries(AppendEntriesArgs args) {
        if (args.term > currentTerm) {
            log.debug(id + ": append term higher: " + args.term  + " vs " + currentTerm);
            shouldBecomeFollower = true;
        }
    }

    @Override
    protected void onRequestVote(RequestVoteArgs args) {
        if (args.term > currentTerm) {
            log.debug(id + ": request term higher: " + args.term  + " vs " + currentTerm);
            shouldBecomeFollower = true;
        }
    }

    @Override
    protected void onReceiveResult(RpcResult result) {
        if (result.currentTerm > currentTerm) {
            log.debug(id + ": result term higher: " + result.currentTerm  + " vs " + currentTerm);
            shouldBecomeFollower = true;
        }
    }
}
