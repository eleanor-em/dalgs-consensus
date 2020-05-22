package consensus.raft.state;

import consensus.IConsensusClient;
import consensus.net.Actor;

class FollowerRaftState extends AbstractRaftState {
    private final Timer timer = new Timer();

    FollowerRaftState(int id, int serverCount, Actor actor, IConsensusClient client) {
        super(id, serverCount, actor, client);
    }

    @Override
    protected void onRpc() {
        timer.reset();
    }

    @Override
    protected AbstractRaftState onTick() {
        synchronized (lock) {
            // Check if we should start an election
            if (timer.expired()) {
                return this.asCandidate();
            } else {
                return this;
            }
        }
    }
}
