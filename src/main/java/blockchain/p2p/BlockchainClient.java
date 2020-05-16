package blockchain.p2p;

import blockchain.model.BlockchainMessage;
import blockchain.model.MessageType;
import blockchain.transaction.Transaction;
import consensus.crypto.StringUtils;
import consensus.ipc.IpcServer;
import consensus.net.data.Message;

public class BlockchainClient extends IpcServer {
    public BlockchainClient(int id) {
        super(id);
    }

    public void replicateChain() {
        BlockchainMessage blockchainMessage = new BlockchainMessage(MessageType.REPLICATE_CHAIN);
        broadcast(blockchainMessage);
    }

    public void sendTransaction(Transaction transaction) {
        BlockchainMessage blockchainMessage = new BlockchainMessage(MessageType.ADD_TRANSACTION, transaction);
        broadcast(blockchainMessage);
    }

    public void broadcastClearTransactions() {
        BlockchainMessage blockchainMessage = new BlockchainMessage(MessageType.CLEAR_TRANSACTIONS);
        broadcast(blockchainMessage);
    }

    protected void broadcast(BlockchainMessage blockchainMessage) {
        this.broadcast(new Message(StringUtils.toJson(blockchainMessage)));
    }
}
