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
import consensus.util.ConfigManager;
import consensus.util.StringUtils;
import consensus.net.Actor;
import consensus.net.data.IncomingMessage;
import consensus.net.data.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class BlockchainActor extends Actor {
    private static final Logger log = LogManager.getLogger(BlockchainActor.class);
    private final int id;
    private final IConsensusClient client;
    private final LinkedBlockingQueue<IncomingMessage> messages = new LinkedBlockingQueue<>();
    private static final int MINE_RATE = ConfigManager.getInt("mineRate").orElse(10000);

    private final Blockchain blockchain;
    private final TransactionPool transactionPool;
    private final Miner miner;
    private final Wallet wallet;

    private final Object lock = new Object();

    public BlockchainActor(int id, IConsensusClient client, Blockchain blockchain, TransactionPool transactionPool, Miner miner, Wallet wallet) {
        this.id = id;
        this.client = client;

        this.blockchain = blockchain;
        this.transactionPool = transactionPool;
        this.miner = miner;
        this.wallet = wallet;

        new Thread(this::mineThread).start();
        new Thread(this::clientMonThread).start();
    }

    public void createTransaction(String address, String message, float amount) {
        Optional<Transaction> transaction = wallet.createTransaction(address, message, amount);
        transaction.ifPresent(this::publishTransaction);
    }

    public String getAddress() {
        return wallet.getAddress();
    }

    private void mineThread() {
        while (!Thread.interrupted()) {
            try {
                TimeUnit.MILLISECONDS.sleep(MINE_RATE);
                synchronized (lock) {
                    var maybeBlock = miner.mine();
                    if (maybeBlock.isPresent()) {
                        requestAllToReplicateBlockchain();
                        requestAllToClearTransactionPool();
                    }
                }
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void clientMonThread() {
        var queue = client.getBroadcastQueue();
        while (!Thread.interrupted()) {
            try {
                var msg = queue.take();
                synchronized (lock) {
                    this.createTransaction(this.wallet.getAddress(), msg.data, 0f);
                }
            } catch (InterruptedException ignored) {
            }
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
                        synchronized (lock) {
                            Blockchain newBlockchain = StringUtils.fromJson(blockchainMessage.getJsonData(), Blockchain.class);
                            blockchain.replaceListOfBlocks(newBlockchain);
                        }
                        break;
                    case ADD_TRANSACTION:
                        synchronized (lock) {
                            Transaction newTransaction = StringUtils.fromJson(blockchainMessage.getJsonData(), Transaction.class);
                            transactionPool.updateOrAddTransaction(newTransaction);
                            this.sendToClient(newTransaction, message.src);
                        }
                        break;
                    case CLEAR_TRANSACTION_POOL:
                        synchronized (lock) {
                            transactionPool.clear();
                        }
                        break;
                }
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void sendToClient(Transaction tx, int src) {
        List<TransactionOutput> list;
        list = List.copyOf(tx.getTransactionOutputs());
        list.stream()
            .map(output -> output.message)
            .forEach(msg -> client.receiveEntry(new IncomingMessage(new Message(msg), src)));
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

    private void requestAllToClearTransactionPool() {
        transactionPool.clear();
        BlockchainMessage blockchainMessage = new BlockchainMessage(MessageType.CLEAR_TRANSACTION_POOL);
        broadcast(blockchainMessage);
    }

    private void broadcast(BlockchainMessage blockchainMessage) {
        this.sendMessageToAll(new Message(StringUtils.toJson(blockchainMessage)));
    }
}
