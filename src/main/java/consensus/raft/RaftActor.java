package consensus.raft;

import consensus.IConsensusClient;
import consensus.net.Actor;
import consensus.net.data.IncomingMessage;
import consensus.raft.rpc.RpcMessage;
import consensus.raft.state.AbstractRaftState;
import consensus.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Stub class
 */
public class RaftActor extends Actor {
    private static final Logger log = LogManager.getLogger(RaftActor.class);
    private final int id;
    private final IConsensusClient client;
    private AbstractRaftState state;

    public RaftActor(int id, int serverCount, IConsensusClient client) {
        this.id = id;
        this.client = client;
        state = AbstractRaftState.create(id, serverCount, this, client);
    }

    protected void task() {
        while (!Thread.interrupted()) {
            state = state.tick();
            Thread.yield();
        }
    }

    @Override
    protected void onReceive(IncomingMessage message) {
        var decoded = (RpcMessage) StringUtils.fromJson(message.msg.data, RpcMessage.class);
        switch (decoded.kind) {
            case APPEND_ENTRIES:
                decoded.decodeAppendEntries().ifPresent(state::rpcAppendEntries);
                break;
            case REQUEST_VOTE:
                decoded.decodeRequestVote().ifPresent(state::rpcRequestVote);
                break;
            case RESULT:
                decoded.decodeResult().ifPresent(state::rpcReceiveResult);
                break;
        }
    }
}
