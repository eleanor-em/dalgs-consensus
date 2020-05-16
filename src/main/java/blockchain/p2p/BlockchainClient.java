package blockchain.p2p;

import blockchain.block.Block;
import blockchain.block.Blockchain;
import blockchain.miner.Miner;
import blockchain.model.BlockchainMessage;
import blockchain.model.MessageType;
import blockchain.transaction.Transaction;
import blockchain.transaction.TransactionPool;
import consensus.crypto.StringUtils;
import consensus.ipc.IpcServer;
import consensus.net.data.Message;

public class BlockchainClient extends IpcServer {
    private final Blockchain blockChain;
    private final TransactionPool transactionPool;
    private final Miner miner;

    public BlockchainClient(int id, Blockchain blockChain, TransactionPool transactionPool) {
        super(id);
        this.blockChain = blockChain;
        this.transactionPool = transactionPool;
        this.miner = new Miner(blockChain, transactionPool);
    }

    public Blockchain getBlockChain() {
        return blockChain;
    }

    public TransactionPool getTransactionPool() {
        return transactionPool;
    }

    public Block mine() {
        Block block = miner.mine();
        if (block != null) {
            requestAlllToReplicateBlockchain();
            requestAllToClearTransactionPool();
        }
        return block;
    }

    private void requestAlllToReplicateBlockchain() {
        BlockchainMessage blockchainMessage = new BlockchainMessage(MessageType.REPLICATE_BLOCKCHAIN, blockChain);
        broadcast(blockchainMessage);
    }

    public void publishTransaction(Transaction transaction) {
        BlockchainMessage blockchainMessage = new BlockchainMessage(MessageType.ADD_TRANSACTION, transaction);
        broadcast(blockchainMessage);
    }

    public void requestAllToClearTransactionPool() {
        transactionPool.clear();
        BlockchainMessage blockchainMessage = new BlockchainMessage(MessageType.CLEAR_TRANSACTION_POOL);
        broadcast(blockchainMessage);
    }

    protected void broadcast(BlockchainMessage blockchainMessage) {
        this.broadcast(new Message(StringUtils.toJson(blockchainMessage)));
    }
}
