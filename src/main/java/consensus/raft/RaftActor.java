package consensus.raft;

import consensus.IConsensusClient;
import consensus.net.Actor;
import consensus.net.data.IncomingMessage;
import consensus.net.data.Message;

/**
 * Stub class
 */
public class RaftActor extends Actor {
    private final int id;
    private final IConsensusClient client;

    public RaftActor(int id, IConsensusClient client) {
        this.id = id;
        this.client = client;
    }

    protected void task() throws InterruptedException {
        while (!Thread.interrupted()) {
            // For now, simply broadcast what the client has.
            var message = client.getBroadcastQueue().take();
            this.sendMessageToAll(message);
        }
    }

    @Override
    protected void onReceive(IncomingMessage message) {
        client.receiveEntry(message);
    }
}
