package blockchain.model;

import consensus.util.StringUtils;

public class BlockchainMessage {
    private final MessageType messageType;
    private final String jsonData;

    public BlockchainMessage(MessageType messageType, Object jsonData) {
        this.messageType = messageType;
        this.jsonData = StringUtils.toJson(jsonData);
    }

    public BlockchainMessage(MessageType messageType) {
        this.messageType = messageType;
        this.jsonData = "{}";
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getJsonData() {
        return jsonData;
    }
}
