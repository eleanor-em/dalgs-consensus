package blockchain.miner;

import blockchain.block.Block;
import blockchain.block.BlockChain;
import blockchain.transaction.Transaction;
import blockchain.transaction.TransactionPool;

import java.util.List;

public class Miner {
    private final BlockChain blockChain;
    private final TransactionPool transactionPool;

    public Miner(BlockChain blockChain, TransactionPool transactionPool) {
        this.blockChain = blockChain;
        this.transactionPool = transactionPool;
    }

    public Block mine() {
        List<Transaction> validTransactions = transactionPool.filterValidTransactions();
        // TODO: implement p2p here:
        return blockChain.addTransactions(validTransactions);
    }
}
