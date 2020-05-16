package blockchain;

import blockchain.block.BlockChain;
import blockchain.miner.Miner;
import blockchain.transaction.TransactionPool;
import blockchain.wallet.Wallet;
import consensus.crypto.StringUtils;

import java.security.PublicKey;

public class Test {
    private static Wallet wallet;
    private static BlockChain blockChain;
    private static TransactionPool transactionPool;
    private static Miner miner;

    public static void main(String[] args) {
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
        miner.mine();
        System.out.println(StringUtils.toJson(blockChain));
        System.out.println(blockChain.isValidChain());
    }

    private static void transact() {
        Wallet anotherWallet = new Wallet();
        PublicKey recipient = wallet.getAddress();
        anotherWallet.createTransaction(recipient, 1, blockChain, transactionPool);
        anotherWallet.createTransaction(recipient, 1, blockChain, transactionPool);
    }

    private static void mineTransactions() {
        miner.mine();
    }
}
