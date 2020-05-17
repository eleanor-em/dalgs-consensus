package blockchain.miner;

import blockchain.block.Block;
import blockchain.block.Blockchain;
import blockchain.transaction.Transaction;
import blockchain.transaction.TransactionPool;

import java.util.List;
import java.util.Optional;

public class Miner {
    private final Blockchain blockchain;
    private final TransactionPool transactionPool;

    public Miner(Blockchain blockchain, TransactionPool transactionPool) {
        this.blockchain = blockchain;
        this.transactionPool = transactionPool;
    }

    public Optional<Block> mine() {
        List<Transaction> validTransactions = transactionPool.filterValidTransactions();
        if (validTransactions.size() > 0) {
            return Optional.of(blockchain.addTransactions(validTransactions));
        } else {
            return Optional.empty();
        }
    }
}
