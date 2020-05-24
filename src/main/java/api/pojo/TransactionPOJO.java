package api.pojo;

public class TransactionPOJO {
    private String recipient;
    private String message;
    private float amount;

    public String getMessage() { return message; }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }
}
