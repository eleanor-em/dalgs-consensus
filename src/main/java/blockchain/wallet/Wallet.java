package blockchain.wallet;

import blockchain.block.Block;
import blockchain.block.Blockchain;
import blockchain.transaction.Transaction;
import blockchain.transaction.TransactionInput;
import blockchain.transaction.TransactionOutput;
import blockchain.transaction.TransactionPool;
import consensus.crypto.CryptoUtils;
import consensus.crypto.EccSignature;
import consensus.crypto.StringUtils;
import consensus.util.ConfigManager;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

public class Wallet {
    private static final float INITIAL_BALANCE = ConfigManager.getInt("initialBalance").orElse(0);
    private float balance;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;
    private final Blockchain blockchain;
    private final TransactionPool transactionPool;

    public Wallet(Blockchain blockchain, TransactionPool transactionPool) {
        balance = INITIAL_BALANCE;
        KeyPair keyPair = EccSignature.generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
        this.blockchain = blockchain;
        this.transactionPool = transactionPool;
    }

    public float getBalance() {
        return balance;
    }

    public String getAddress() {
        return EccSignature.publicKeyToHex(publicKey);
    }

    public Optional<Transaction> createTransaction(String recipient, float amount) {
        balance = calculateBalance();
        if (amount > balance) {
            System.out.println("Your wallet does not have enough money!");
            return Optional.empty();
        }

        var maybeTrans = transactionPool.existingTransaction(getAddress());
        maybeTrans.ifPresentOrElse(
                trans -> trans.update(this, recipient, amount),
                () -> Transaction.newTransaction(this, recipient, amount)
                        .ifPresent(transactionPool::updateOrAddTransaction)
        );

        return maybeTrans;
    }

    private byte[] sign(String data) throws Exception {
        return EccSignature.sign(privateKey, data);
    }

    public boolean signTransaction(Transaction transaction) {
        try {
            long timestamp = (new Date()).getTime();
            List<TransactionOutput> transactionOutputs = transaction.getTransactionOutputs();
            String data = CryptoUtils.hash(StringUtils.toJson(transactionOutputs));
            byte[] signature = sign(data);
            TransactionInput transactionInput = new TransactionInput(timestamp, getAddress(), balance, signature);
            transaction.setTransactionInput(transactionInput);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public float calculateBalance() {
        float balance = this.balance;

        List<Transaction> allTransactions = new ArrayList<>();
        List<Block> blockList = blockchain.getBlockList();
        for (Block block : blockList) {
            List<Transaction> transactionList = block.getTransactionList();
            allTransactions.addAll(transactionList);
        }

        allTransactions = allTransactions.stream()
                .filter(transaction -> transaction.getTransactionInput()
                        .map(TransactionInput::getAddress)
                        .orElse("")
                        .equals(getAddress()))
                .collect(Collectors.toList());

        double startTime = 0.0f;
        if (allTransactions.size() > 0) {
            Transaction recentTx = allTransactions.stream()
                    .max(Comparator.comparingLong(tx -> tx.getTransactionInput()
                            .map(TransactionInput::getTimestamp)
                            .orElse(0L)))
                    .get();
            var txOutput = recentTx.getTransactionOutputs().stream()
                    .filter(output -> output.getAddress().equals(getAddress()))
                    .findAny();

            if (txOutput.isPresent()) {
                balance = txOutput.get().getAmount();
                startTime = recentTx.getTransactionInput()
                        .map(TransactionInput::getTimestamp)
                        .orElse(0L);
            }
        }

        for (Transaction transaction : allTransactions) {
            double finalStartTime = startTime;
            if (transaction.getTransactionInput()
                    .map(in -> in.getTimestamp() > finalStartTime)
                    .orElse(false)) {
                List<TransactionOutput> transactionOutputs = transaction.getTransactionOutputs();
                for (TransactionOutput transactionOutput : transactionOutputs) {
                    if (transactionOutput.getAddress().equals(getAddress())) {
                        balance += transactionOutput.getAmount();
                    }
                }
            }
        }

        return balance;
    }
}
