package blockchain.p2p;

import consensus.net.Actor;
import consensus.net.data.IncomingMessage;

public class BlockchainActor extends Actor {
    private final int id;
    private final BlockchainClient blockchainClient;

    public BlockchainActor(int id, BlockchainClient blockchainClient) {
        this.id = id;
        this.blockchainClient = blockchainClient;
    }

    @Override
    protected void task() throws InterruptedException {
        var message = blockchainClient.getBroadcastQueue().take();
        blockchainClient.receiveEntry(new IncomingMessage(message, id));
        System.out.println(message.data + id);
        this.sendMessageToAll(message);
    }

    protected void onReceive(IncomingMessage message) {
        blockchainClient.receiveEntry(message);
    }
}
