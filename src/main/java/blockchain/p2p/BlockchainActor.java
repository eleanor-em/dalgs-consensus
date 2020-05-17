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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class BlockchainActor extends Actor {
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

    public Wallet getWallet() {
        return wallet;
    }

    private void mineThread() {
        while (!Thread.interrupted()) {
            try {
                TimeUnit.SECONDS.sleep(3);
                var maybeBlock = miner.mine();
                if (maybeBlock.isPresent()) {
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
                System.out.println("=======");
                System.out.format("Node %d - %s\n", id, blockchainMessage.getMessageType());
                if (blockchainMessage.getJsonData() != null) {
                    System.out.println(blockchainMessage.getJsonData());
                }
                System.out.println("=======");
                switch (blockchainMessage.getMessageType()) {
                    case REPLICATE_BLOCKCHAIN:
                        Blockchain newBlockchain = StringUtils.fromJson(blockchainMessage.getJsonData(), Blockchain.class);
                        blockchain.replaceListOfBlocks(newBlockchain);
                        break;
                    case ADD_TRANSACTION:
                        Transaction newTransaction = StringUtils.fromJson(blockchainMessage.getJsonData(), Transaction.class);
                        transactionPool.updateOrAddTransaction(newTransaction);
                        client.receiveEntry(new IncomingMessage(new Message(StringUtils.toJson(newTransaction.getTransactionOutputs())), message.src));
                        break;
                    case CLEAR_TRANSACTION_POOL:
                        transactionPool.clear();
                        break;
                }
            } catch (InterruptedException ignored) {}
        }
    }


    private void requestAllToReplicateBlockchain() {
        BlockchainMessage blockchainMessage = new BlockchainMessage(MessageType.REPLICATE_BLOCKCHAIN, blockchain);
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

    private void broadcast(BlockchainMessage blockchainMessage) {
        this.sendMessageToAll(new Message(StringUtils.toJson(blockchainMessage)));
    }
}
