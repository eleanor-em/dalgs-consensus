package blockchain.transaction;

import java.security.PublicKey;

public class TransactionOutput {
    private float amount;
    private PublicKey address;

    public TransactionOutput(float amount, PublicKey address) {
        this.amount = amount;
        this.address = address;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public PublicKey getAddress() {
        return address;
    }
}
