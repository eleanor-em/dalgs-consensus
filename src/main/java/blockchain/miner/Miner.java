package blockchain.miner;

import blockchain.block.Block;
import blockchain.block.Blockchain;
import blockchain.transaction.Transaction;
import blockchain.transaction.TransactionPool;

import java.util.List;

public class Miner {
    private final Blockchain blockchain;
    private final TransactionPool transactionPool;

    public Miner(Blockchain blockchain, TransactionPool transactionPool) {
        this.blockchain = blockchain;
        this.transactionPool = transactionPool;
    }

    public Block mine() {
        List<Transaction> validTransactions = transactionPool.filterValidTransactions();
        if (validTransactions.size() > 0) {
            return blockchain.addTransactions(validTransactions);
        } else {
            return null;
        }
    }
}
