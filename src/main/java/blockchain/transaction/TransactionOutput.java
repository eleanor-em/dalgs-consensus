package blockchain.transaction;

public class TransactionOutput {
    private float amount;
    public final String message;
    private final String address;

    public TransactionOutput(float amount, String message, String address) {
        this.amount = amount;
        this.message = message;
        this.address = address;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public String getAddress() {
        return address;
    }
}
