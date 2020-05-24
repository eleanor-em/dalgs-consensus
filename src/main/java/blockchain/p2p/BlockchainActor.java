package blockchain.p2p;

import blockchain.block.Blockchain;
import blockchain.miner.Miner;
import blockchain.model.BlockchainMessage;
import blockchain.model.MessageType;
import blockchain.transaction.Transaction;
import blockchain.transaction.TransactionOutput;
import blockchain.transaction.TransactionPool;
import blockchain.wallet.Wallet;
import consensus.IConsensusClient;
import consensus.net.Actor;
import consensus.net.data.IncomingMessage;
import consensus.net.data.Message;
import consensus.util.StringUtils;

import java.util.List;

public abstract class BlockchainActor extends Actor {
    protected int id;
    protected IConsensusClient client;
    protected Blockchain blockchain;
    protected TransactionPool transactionPool;
    protected Miner miner;
    protected Wallet wallet;

    public BlockchainActor(int id, IConsensusClient client, Blockchain blockchain, TransactionPool transactionPool, Miner miner, Wallet wallet) {
        this.id = id;
        this.client = client;
        this.blockchain = blockchain;
        this.transactionPool = transactionPool;
        this.miner = miner;
        this.wallet = wallet;
    }

    protected void sendToClient(Transaction tx, int src) {
        List<TransactionOutput> list;
        list = List.copyOf(tx.getTransactionOutputs());
        list.stream()
                .map(output -> output.message)
                .forEach(msg -> client.receiveEntry(new IncomingMessage(new Message(msg), src)));
    }

    protected void requestAllToReplicateBlockchain() {
        BlockchainMessage blockchainMessage = new BlockchainMessage(MessageType.REPLICATE_BLOCKCHAIN, blockchain);
        broadcast(blockchainMessage);
    }

    protected void publishTransaction(Transaction transaction) {
        BlockchainMessage blockchainMessage = new BlockchainMessage(MessageType.ADD_TRANSACTION, transaction);
        broadcast(blockchainMessage);
        this.sendToClient(transaction, id);
    }

    protected void requestAllToClearTransactionPool() {
        transactionPool.clear();
        BlockchainMessage blockchainMessage = new BlockchainMessage(MessageType.CLEAR_TRANSACTION_POOL);
        broadcast(blockchainMessage);
    }

    protected void broadcast(BlockchainMessage blockchainMessage) {
        this.sendMessageToAll(new Message(StringUtils.toJson(blockchainMessage)));
    }
}
