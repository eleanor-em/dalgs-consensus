package blockchain.transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionPool {
    private final List<Transaction> transactionList;

    public TransactionPool() {
        transactionList = new ArrayList<>();
    }

    public void updateOrAddTransaction(Transaction newTransaction) {
        Transaction existingTransaction = this.transactionList.stream()
                .filter(t -> t.getId().equals(newTransaction.getId()))
                .findAny()
                .orElse(null);
        if (existingTransaction != null) {
            transactionList.set(this.transactionList.indexOf(existingTransaction), newTransaction);
        } else {
            transactionList.add(newTransaction);
        }
    }

    public Transaction existingTransaction(String address) {
        return transactionList.stream()
                .filter(transaction -> transaction.getTransactionInput().getAddress().equals(address))
                .findAny()
                .orElse(null);
    }

    public List<Transaction> filterValidTransactions() {
        return transactionList.stream().filter(transaction -> {
            float outputTotal = transaction.getTransactionOutputs().stream()
                    .map(TransactionOutput::getAmount)
                    .reduce(0.0f, Float::sum);


            if (transaction.getTransactionInput().getAmount() != outputTotal) {
                return false;
            }

            return Transaction.verifyTransaction(transaction);
        }).collect(Collectors.toList());
    }

    public void clear() {
        this.transactionList.clear();
    }
}
