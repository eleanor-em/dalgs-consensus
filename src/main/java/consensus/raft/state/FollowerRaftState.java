package consensus.raft.state;

import consensus.IConsensusClient;
import consensus.net.Actor;
import consensus.raft.rpc.AppendEntriesArgs;
import consensus.raft.rpc.RequestVoteArgs;
import consensus.raft.rpc.RpcResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class FollowerRaftState extends AbstractRaftState {
    private static final Logger log = LogManager.getLogger(FollowerRaftState.class);
    private final Timer timer = new Timer();

    FollowerRaftState(int id, int serverCount, Actor actor, IConsensusClient client) {
        super(id, serverCount, actor, client);
    }

    @Override
    protected void onAppendEntries(AppendEntriesArgs args) {
        timer.reset();
    }

    @Override
    protected void onRequestVote(RequestVoteArgs args) {
        timer.reset();
    }

    @Override
    protected AbstractRaftState onTick() {
        if (timer.expired()) {
            return this.asCandidate();
        } else {
            return this;
        }
    }
}
