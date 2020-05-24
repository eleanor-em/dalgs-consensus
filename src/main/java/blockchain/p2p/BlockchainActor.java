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

    protected abstract void sendToClient(Transaction tx, int src);

    protected abstract void requestAllToReplicateBlockchain();

    protected abstract void publishTransaction(Transaction transaction);

    protected abstract void requestAllToClearTransactionPool();

    protected abstract void broadcast(BlockchainMessage blockchainMessage);
}
