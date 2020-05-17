package blockchain.p2p;

import blockchain.block.Blockchain;
import blockchain.miner.Miner;
import blockchain.model.BlockchainMessage;
import blockchain.model.MessageType;
import blockchain.transaction.Transaction;
import blockchain.transaction.TransactionPool;
import blockchain.wallet.Wallet;
import consensus.IConsensusClient;
import consensus.crypto.StringUtils;
import consensus.net.Actor;
import consensus.net.data.IncomingMessage;
import consensus.net.data.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class BlockchainActor extends Actor {
    private static final Logger log = LogManager.getLogger(BlockchainActor.class);
    private final int id;
    private final IConsensusClient client;
    private final LinkedBlockingQueue<IncomingMessage> messages = new LinkedBlockingQueue<>();

    private final Blockchain blockchain;
    private final TransactionPool transactionPool;
    private final Miner miner;
    private final Wallet wallet;

    public BlockchainActor(int id, IConsensusClient client, Blockchain blockchain, TransactionPool transactionPool) {
        this.id = id;
        this.client = client;

        this.blockchain = blockchain;
        this.transactionPool = transactionPool;
        this.miner = new Miner(blockchain, transactionPool);
        this.wallet = new Wallet(blockchain, transactionPool);

        new Thread(this::mineThread).start();
    }

    public void createTransaction(String address, float amount) {
        wallet.createTransaction(address, amount);
    }

    public String getAddress() {
        return wallet.getAddress();
    }

    private void mineThread() {
        while (!Thread.interrupted()) {
            try {
                TimeUnit.SECONDS.sleep(3);
                var maybeBlock = miner.mine();
                if (maybeBlock.isPresent()) {
                    // Send to client
                    maybeBlock.get().getTransactionList().forEach(tx -> this.sendToClient(tx, id));
                    requestAllToReplicateBlockchain();
                    requestAllToClearTransactionPool();
                }
            } catch (InterruptedException ignored) {}
        }
    }

    public String readChain() {
        return StringUtils.toJson(blockchain.getBlockList());
    }

    @Override
    protected void onReceive(IncomingMessage message) {
        messages.add(message);
    }

    @Override
    protected void task() {
        while (!Thread.interrupted()) {
            try {
                var message = messages.take();
                BlockchainMessage blockchainMessage = StringUtils.fromJson(message.msg.data, BlockchainMessage.class);

                switch (blockchainMessage.getMessageType()) {
                    case REPLICATE_BLOCKCHAIN:
                        int priorLength = blockchain.getLength();
                        Blockchain newBlockchain = StringUtils.fromJson(blockchainMessage.getJsonData(), Blockchain.class);
                        blockchain.replaceListOfBlocks(newBlockchain);
                        for (int i = priorLength; i < newBlockchain.getLength(); ++i) {
                            newBlockchain.getBlockList()
                                    .get(i)
                                    .getTransactionList()
                                    .forEach(tx -> this.sendToClient(tx, id));
                        }
                        break;
                    case ADD_TRANSACTION:
                        Transaction newTransaction = StringUtils.fromJson(blockchainMessage.getJsonData(), Transaction.class);
                        transactionPool.updateOrAddTransaction(newTransaction);
                        this.sendToClient(newTransaction, message.src);
                        break;
                    case CLEAR_TRANSACTION_POOL:
                        transactionPool.clear();
                        break;
                }
            } catch (InterruptedException ignored) {}
        }
    }

    private void sendToClient(Transaction tx, int src) {
        client.receiveEntry(new IncomingMessage(new Message(StringUtils.toJson(tx.getTransactionOutputs())), src));
    }

    private void requestAllToReplicateBlockchain() {
        BlockchainMessage blockchainMessage = new BlockchainMessage(MessageType.REPLICATE_BLOCKCHAIN, blockchain);
        broadcast(blockchainMessage);
    }

    public void publishTransaction(Transaction transaction) {
        BlockchainMessage blockchainMessage = new BlockchainMessage(MessageType.ADD_TRANSACTION, transaction);
        broadcast(blockchainMessage);
        this.sendToClient(transaction, id);
    }

    public void requestAllToClearTransactionPool() {
        transactionPool.clear();
        BlockchainMessage blockchainMessage = new BlockchainMessage(MessageType.CLEAR_TRANSACTION_POOL);
        broadcast(blockchainMessage);
    }

    private void broadcast(BlockchainMessage blockchainMessage) {
        this.sendMessageToAll(new Message(StringUtils.toJson(blockchainMessage)));
    }
}
