package blockchain.block;

import blockchain.transaction.Transaction;
import consensus.crypto.CryptoUtils;
import consensus.util.ConfigManager;
import consensus.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Block {
    private static final Logger log = LogManager.getLogger(Block.class);
    private static final int INITIAL_POW_DIFFICULTY = ConfigManager.getInt("initialPowDifficulty").orElse(0);
    private static final int MINE_RATE = ConfigManager.getInt("mineRate").orElse(10000);
    private final long timestamp;
    private final String lastHash;
    private final String blockHash;
    private final List<Transaction> transactionList;

    public Block(long timestamp, String lastHash, String blockHash, List<Transaction> data) {
        this.timestamp = timestamp;
        this.lastHash = lastHash;
        this.blockHash = blockHash;
        this.transactionList = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getLastHash() {
        return lastHash;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public List<Transaction> getTransactionList() {
        return transactionList;
    }

    public static Block mineBlock(Block lastBlock, List<Transaction> transactions) {
        String hashValue;
        long timestamp = (new Date()).getTime();
        String lastHash = lastBlock.getBlockHash();
        int difficulty;
        int nonce = 0;
        do {
            nonce++;
            difficulty = Block.adjustDifficulty(lastBlock, timestamp);
            String sum = timestamp + lastHash + nonce;
            hashValue = CryptoUtils.hash(sum);
        } while (!hashValue.substring(0, difficulty).equals("0".repeat(difficulty)));
        log.debug(String.format("Solved the POW problem (difficulty=%d) with %d attempt(s)", difficulty, nonce));

        String blockHash = CryptoUtils.hash(timestamp + lastHash + StringUtils.toJson(transactions));

        return new Block(timestamp, lastHash, blockHash, transactions);
    }

    public static int adjustDifficulty(Block block, long currentTime) {
        int difficulty = INITIAL_POW_DIFFICULTY;
        difficulty = (block.getTimestamp() + MINE_RATE) > currentTime ? difficulty + 1 : difficulty - 1;
        return difficulty;
    }

    public static String blockHash(Block block) {
        String blockContent = block.getTimestamp() + block.getLastHash() + StringUtils.toJson(block.getTransactionList());
        return CryptoUtils.hash(blockContent);
    }

    public static Block genesis() {
        return new Block(0, "---", "genesis-hash", new ArrayList<>());
    }
}
