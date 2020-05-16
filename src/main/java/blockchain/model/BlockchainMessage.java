package blockchain.model;

public class BlockchainMessage {
    private MessageType messageType;
    private Object data;

    public BlockchainMessage(MessageType messageType, Object data) {
        this.messageType = messageType;
        this.data = data;
    }

    public BlockchainMessage(MessageType messageType) {
        this.messageType = messageType;
        this.data = null;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public Object getData() {
        return data;
    }
}
