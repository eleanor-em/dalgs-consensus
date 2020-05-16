package blockchain.p2p;

import blockchain.block.BlockChain;
import blockchain.model.BlockchainMessage;
import blockchain.model.MessageType;
import blockchain.transaction.TransactionPool;
import consensus.crypto.StringUtils;
import consensus.net.Actor;
import consensus.net.data.IncomingMessage;

public class BlockchainActor extends Actor {
    private final int id;
    private final BlockchainClient blockchainClient;

    private BlockChain blockChain;
    private TransactionPool transactionPool;

    public BlockchainActor(int id, BlockchainClient blockchainClient) {
        this.id = id;
        this.blockchainClient = blockchainClient;
    }

    @Override
    protected void task() throws InterruptedException {
        while (!Thread.interrupted()) {
            var message = blockchainClient.getBroadcastQueue().take();
            blockchainClient.receiveEntry(new IncomingMessage(message, id));
            BlockchainMessage blockchainMessage = StringUtils.fromJson(message.data, BlockchainMessage.class);
            switch (blockchainMessage.getMessageType()) {
                case REPLICATE_CHAIN:
                case ADD_TRANSACTION:
                case CLEAR_TRANSACTIONS:
                    System.out.println(message);
            }
            // this.sendMessageToAll(message);
        }
    }
}
