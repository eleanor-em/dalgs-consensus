package blockchain.transaction;

import blockchain.wallet.Wallet;
import consensus.crypto.CryptoUtils;
import consensus.crypto.ECCCipher;
import consensus.crypto.StringUtils;

import java.security.PublicKey;
import java.util.*;

public class Transaction {
    private final String id;
    private TransactionInput transactionInput;
    private List<TransactionOutput> transactionOutputs;

    public Transaction() {
        this.id = UUID.randomUUID().toString();
        this.transactionInput = null;
        this.transactionOutputs = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public TransactionInput getTransactionInput() {
        return transactionInput;
    }

    public void setTransactionInput(TransactionInput transactionInput) {
        this.transactionInput = transactionInput;
    }

    public List<TransactionOutput> getTransactionOutputs() {
        return transactionOutputs;
    }

    public void setTransactionOutputs(List<TransactionOutput> transactionOutputs) {
        this.transactionOutputs = transactionOutputs;
    }

    public void update(Wallet wallet, PublicKey recipient, float amount) {
        TransactionOutput senderOutput = this.transactionOutputs.stream()
                .filter(t -> t.getAddress().equals(wallet.getAddress()))
                .findAny()
                .orElse(null);

        if (senderOutput == null) {
            return;
        }

        if (amount > wallet.getAmount()) {
            System.out.println("Not enough money!!!");
            return;
        }

        senderOutput.setAmount(senderOutput.getAmount() - amount);
        this.transactionOutputs.add(new TransactionOutput(amount, recipient));
        wallet.signTransaction(this);
    }

    public static boolean verifyTransaction(Transaction transaction) {
        try {
            PublicKey address = transaction.getTransactionInput().getAddress();
            byte[] signature = transaction.getTransactionInput().getSignature();
            List<TransactionOutput> transactionOutputs = transaction.getTransactionOutputs();
            String dataHash = CryptoUtils.hash(StringUtils.toJson(transactionOutputs));
            return ECCCipher.verifySignature(address, signature, dataHash);
        } catch (Exception e) {
            return false;
        }
    }

    public static Transaction newTransaction(Wallet wallet, PublicKey recipient, float amount) {
        if (amount > wallet.getAmount()) {
            return null;
        }

        List<TransactionOutput> transactionOutputs = new ArrayList<>();
        transactionOutputs.add(new TransactionOutput(wallet.getAmount() - amount, wallet.getAddress()));
        transactionOutputs.add(new TransactionOutput(amount, recipient));
        return Transaction.transactionWithOutputs(wallet, transactionOutputs);
    }

    public static Transaction transactionWithOutputs(Wallet wallet, List<TransactionOutput> transactionOutputs) {
        Transaction transaction = new Transaction();
        transaction.setTransactionOutputs(transactionOutputs);
        if (wallet.signTransaction(transaction)) {
            return transaction;
        } else {
            return null;
        }
    }
}