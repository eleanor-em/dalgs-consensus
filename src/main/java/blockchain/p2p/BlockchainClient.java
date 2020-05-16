package blockchain.p2p;

import blockchain.block.BlockChain;
import blockchain.model.BlockchainMessage;
import blockchain.model.MessageType;
import blockchain.transaction.Transaction;
import blockchain.transaction.TransactionPool;
import consensus.crypto.StringUtils;
import consensus.ipc.IpcServer;
import consensus.net.data.Message;

public class BlockchainClient extends IpcServer {
    private final BlockChain blockChain;
    private final TransactionPool transactionPool;

    public BlockchainClient(int id, BlockChain blockChain, TransactionPool transactionPool) {
        super(id);
        this.blockChain = blockChain;
        this.transactionPool = transactionPool;
    }

    public BlockChain getBlockChain() {
        return blockChain;
    }

    public TransactionPool getTransactionPool() {
        return transactionPool;
    }

    public void replicateChain() {
        BlockchainMessage blockchainMessage = new BlockchainMessage(MessageType.REPLICATE_CHAIN, blockChain);
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
