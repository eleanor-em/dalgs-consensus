package consensus.raft;

import consensus.IConsensusClient;
import consensus.net.Actor;
import consensus.net.data.IncomingMessage;
import consensus.raft.rpc.RpcMessage;
import consensus.raft.state.AbstractRaftState;
import consensus.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RaftActor extends Actor {
    private static final Logger log = LogManager.getLogger(RaftActor.class);
    private final int id;
    private AbstractRaftState state;

    public RaftActor(int id, int serverCount, IConsensusClient client) {
        this.id = id;
        state = AbstractRaftState.create(id, serverCount, this, client);
        new Thread(() -> monitorBroadcastQueue(client)).start();
    }

    private void monitorBroadcastQueue(IConsensusClient client) {
        var queue = client.getBroadcastQueue();
        while (!Thread.interrupted()) {
            try {
                var message = queue.take();
                log.debug(id + ": received entry: " + message);
                var wrapped = new IncomingMessage(message, id);
                state.rpcReceiveEntry(wrapped.encoded());
            } catch (InterruptedException ignored) {}
        }
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
                decoded.decodeAppendEntries().ifPresent(args -> state.rpcAppendEntries(decoded.uuid, args));
                break;
            case REQUEST_VOTE:
                decoded.decodeRequestVote().ifPresent(args -> state.rpcRequestVote(decoded.uuid, args));
                break;
            case RESULT:
                decoded.decodeResult().ifPresent(state::rpcReceiveResult);
                break;
            case NEW_ENTRY:
                state.rpcReceiveEntry(decoded.payload);
                break;
        }
    }
}
