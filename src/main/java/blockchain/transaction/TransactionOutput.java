package blockchain.transaction;

public class TransactionOutput {
    private float amount;
    private final String address;

    public TransactionOutput(float amount, String address) {
        this.amount = amount;
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
