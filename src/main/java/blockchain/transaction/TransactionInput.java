package blockchain.transaction;

import java.security.PublicKey;

public class TransactionInput {
    private final long timestamp;
    private final PublicKey address;
    private final float amount;
    private final byte[] signature;

    public TransactionInput(long timestamp, PublicKey address, float amount, byte[] signature) {
        this.timestamp = timestamp;
        this.address = address;
        this.amount = amount;
        this.signature = signature;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public PublicKey getAddress() {
        return address;
    }

    public float getAmount() {
        return amount;
    }

    public byte[] getSignature() {
        return signature;
    }
}
