package blockchain;

import blockchain.block.Blockchain;
import blockchain.miner.Miner;
import blockchain.transaction.TransactionPool;
import blockchain.wallet.Wallet;
import consensus.util.StringUtils;
import consensus.util.ConfigManager;

public class Test {
    private static Wallet wallet;
    private static Blockchain blockChain;
    private static TransactionPool transactionPool;
    private static Miner miner;

    public static void main(String[] args) throws Exception {
        ConfigManager.loadProperties();
        run();
    }

    public static void run() throws InterruptedException {
        blockChain = new Blockchain();
        transactionPool = new TransactionPool();
        wallet = new Wallet(blockChain, transactionPool);
        miner = new Miner(blockChain, transactionPool);
        transact();
        System.out.println(StringUtils.toJson(transactionPool));
        System.out.println(StringUtils.toJson(blockChain));
        miner.mine();
        System.out.println(StringUtils.toJson(blockChain));
        transact();
        transact();
        miner.mine();
        System.out.println(StringUtils.toJson(blockChain));
        System.out.println(blockChain.isValidChain());
    }

    private static void transact() {
        Wallet anotherWallet = new Wallet(blockChain, transactionPool);
        String recipient = wallet.getAddress();
        anotherWallet.createTransaction(recipient, 1);
        anotherWallet.createTransaction(recipient, 1);
    }
}
