package blockchain.transaction;

import consensus.crypto.EccException;
import consensus.crypto.EccSignature;
import consensus.util.StringUtils;

import java.security.PublicKey;

public class TransactionInput {
    private final long timestamp;
    private final String address;
    private final float amount;
    private final String signature;

    public TransactionInput(long timestamp, String address, float amount, String signature) {
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

    public PublicKey getPublicKey() throws EccException {
        return EccSignature.hexToPublicKey(address);
    }

    public float getAmount() {
        return amount;
    }

    public byte[] getSignature() {
        return StringUtils.hexToByteArray(signature);
    }
}
