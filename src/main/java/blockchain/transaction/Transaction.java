package blockchain.transaction;

import blockchain.wallet.Wallet;
import consensus.crypto.CryptoUtils;
import consensus.crypto.EccException;
import consensus.crypto.EccSignature;
import consensus.crypto.StringUtils;

import java.util.*;

public class Transaction {
    private final String id;
    private Optional<TransactionInput> transactionInput;
    private List<TransactionOutput> transactionOutputs;

    public Transaction() {
        this.id = UUID.randomUUID().toString();
        this.transactionInput = Optional.empty();
        this.transactionOutputs = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public Optional<TransactionInput> getTransactionInput() {
        return transactionInput;
    }

    public void setTransactionInput(TransactionInput transactionInput) {
        this.transactionInput = Optional.of(transactionInput);
    }

    public List<TransactionOutput> getTransactionOutputs() {
        return transactionOutputs;
    }

    public void setTransactionOutputs(List<TransactionOutput> transactionOutputs) {
        this.transactionOutputs = transactionOutputs;
    }

    public void update(Wallet wallet, String recipient, float amount) {
        TransactionOutput senderOutput = this.transactionOutputs.stream()
                .filter(t -> t.getAddress().equals(wallet.getAddress()))
                .findAny()
                .orElse(null);

        if (senderOutput == null) {
            return;
        }

        if (amount > wallet.getBalance()) {
            System.out.println("Not enough money!!! Cannot create a transaction!!!");
            return;
        }

        senderOutput.setAmount(senderOutput.getAmount() - amount);
        this.transactionOutputs.add(new TransactionOutput(amount, recipient));
        wallet.signTransaction(this);
    }

    public static boolean verifyTransaction(Transaction transaction) {
        var maybeTxInput = transaction.getTransactionInput();
        if (maybeTxInput.isEmpty()) {
            return false;
        }

        try {
            var pubkey = maybeTxInput.get().getPublicKey();
            var signature = maybeTxInput.get().getSignature();
            List<TransactionOutput> transactionOutputs = transaction.getTransactionOutputs();
            String dataHash = CryptoUtils.hash(StringUtils.toJson(transactionOutputs));
            return EccSignature.verifySignature(pubkey, signature, dataHash);
        } catch (EccException e) {
            return false;
        }
    }

    public static Optional<Transaction> newTransaction(Wallet wallet, String recipient, float amount) {
        if (amount > wallet.getBalance()) {
            return Optional.empty();
        }

        List<TransactionOutput> transactionOutputs = new ArrayList<>();
        transactionOutputs.add(new TransactionOutput(wallet.getBalance() - amount, wallet.getAddress()));
        transactionOutputs.add(new TransactionOutput(amount, recipient));
        return Transaction.transactionWithOutputs(wallet, transactionOutputs);
    }

    public static Optional<Transaction> transactionWithOutputs(Wallet wallet, List<TransactionOutput> transactionOutputs) {
        Transaction transaction = new Transaction();
        transaction.setTransactionOutputs(transactionOutputs);
        if (wallet.signTransaction(transaction)) {
            return Optional.of(transaction);
        } else {
            return Optional.empty();
        }
    }
}
