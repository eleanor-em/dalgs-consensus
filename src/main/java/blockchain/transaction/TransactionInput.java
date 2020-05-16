package blockchain.transaction;

import consensus.crypto.ECCCipher;

import java.security.PublicKey;

public class TransactionInput {
    private final long timestamp;
    private final String address;
    private final float amount;
    private final byte[] signature;

    public TransactionInput(long timestamp, String address, float amount, byte[] signature) {
        this.timestamp = timestamp;
        this.address = address;
        this.amount = amount;
        this.signature = signature;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getAddress() {
        return address;
    }

    public PublicKey getPublicKey() throws Exception {
        return ECCCipher.hexToPublicKey(address);
    }

    public float getAmount() {
        return amount;
    }

    public byte[] getSignature() {
        return signature;
    }
}
