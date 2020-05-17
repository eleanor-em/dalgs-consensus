package blockchain.transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TransactionPool {
    private final List<Transaction> transactionList;

    public TransactionPool() {
        transactionList = new ArrayList<>();
    }

    public void updateOrAddTransaction(Transaction newTransaction) {
        var existing = this.transactionList.stream()
                .filter(t -> t.getId().equals(newTransaction.getId()))
                .findAny();

        existing.ifPresentOrElse(
                trans -> transactionList.set(this.transactionList.indexOf(trans), newTransaction),
                () -> transactionList.add(newTransaction)
        );
    }

    public Optional<Transaction> existingTransaction(String address) {
        return transactionList.stream()
                .filter(transaction -> transaction.getTransactionInput()
                        .map(TransactionInput::getAddress)
                        .orElse("")
                        .equals(address))
                .findAny();
    }

    public List<Transaction> filterValidTransactions() {
        return transactionList.stream().filter(transaction -> {
            float outputTotal = transaction.getTransactionOutputs().stream()
                    .map(TransactionOutput::getAmount)
                    .reduce(0.0f, Float::sum);


            if (transaction.getTransactionInput().isEmpty() ||
                    transaction.getTransactionInput().get().getAmount() != outputTotal) {
                return false;
            }

            return Transaction.verifyTransaction(transaction);
        }).collect(Collectors.toList());
    }

    public void clear() {
        this.transactionList.clear();
    }
}
