package blockchain.block;

import blockchain.transaction.Transaction;
import consensus.crypto.CryptoUtils;
import consensus.crypto.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Block {
    private static final int DIFFICULTY = 0;
    private static final int MINE_RATE = 500000;
    private final long timestamp;
    private final String lastHash;
    private final String hashValue;
    private final List<Transaction> transactionList;

    public Block(long timestamp, String lastHash, String hashValue, List<Transaction> data) {
        this.timestamp = timestamp;
        this.lastHash = lastHash;
        this.hashValue = hashValue;
        this.transactionList = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getLastHash() {
        return lastHash;
    }

    public String getHashValue() {
        return hashValue;
    }

    public List<Transaction> getTransactionList() {
        return transactionList;
    }

    public static Block mineBlock(Block lastBlock, List<Transaction> transactions) {
        String hashValue;
        long timestamp = (new Date()).getTime();
        String lastHash = lastBlock.getHashValue();
        int difficulty = 1; // adjust this
        int nonce = 0;
        do {
            nonce++;
            difficulty = Block.adjustDifficulty(lastBlock, timestamp); // #TODO: update this
            String sum = timestamp + lastHash; // #TODO: add nonce to make POW
            hashValue = CryptoUtils.hash(sum);
        } while (!hashValue.substring(0, DIFFICULTY).equals("0".repeat(DIFFICULTY)));

        return new Block(timestamp, lastHash, hashValue, transactions);
    }

    public static int adjustDifficulty(Block block, double currentTime) {
        int difficulty = 0;
        difficulty = block.getTimestamp() + MINE_RATE > currentTime ? difficulty + 1 : difficulty - 1;
        return difficulty;
    }

    public static String blockHash(Block block) {
        String blockContent = block.getTimestamp()  + block.getLastHash();
        return CryptoUtils.hash(blockContent);
    }

    public static Block genesis() {
        return new Block(0, "---", "genesis-hash", new ArrayList<>());
    }
}
