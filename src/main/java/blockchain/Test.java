package blockchain;

import blockchain.block.BlockChain;
import blockchain.miner.Miner;
import blockchain.transaction.TransactionPool;
import blockchain.wallet.Wallet;
import consensus.crypto.ECCCipher;
import consensus.crypto.StringUtils;
import consensus.util.ConfigManager;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.concurrent.TimeUnit;

public class Test {
    private static Wallet wallet;
    private static BlockChain blockChain;
    private static TransactionPool transactionPool;
    private static Miner miner;

    public static void main(String[] args) throws Exception {
        ConfigManager.loadProperties();
        run();
    }

    public static void run() throws InterruptedException {
        wallet = new Wallet();
        blockChain = new BlockChain();
        transactionPool = new TransactionPool();
        miner = new Miner(blockChain, transactionPool);
        transact();
        System.out.println(StringUtils.toJson(transactionPool));
        System.out.println(StringUtils.toJson(blockChain));
        miner.mine();
        System.out.println(StringUtils.toJson(blockChain));
        transact();
        transact();
        TimeUnit.SECONDS.sleep(9);
        miner.mine();
        System.out.println(StringUtils.toJson(blockChain));
        System.out.println(blockChain.isValidChain());
    }

    private static void transact() {
        Wallet anotherWallet = new Wallet();
        String recipient = wallet.getAddress();
        anotherWallet.createTransaction(recipient, 1, blockChain, transactionPool);
        anotherWallet.createTransaction(recipient, 1, blockChain, transactionPool);
    }
}
