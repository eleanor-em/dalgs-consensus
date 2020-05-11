package consensus;
import consensus.net.data.IncomingMessage;
import consensus.net.data.Message;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Represents a client for the consensus protocol.
 *
 * Messages received from the broadcast queue are to be appended to the distributed ledger.
 * When a new entry is received on the distributed ledger, it should be passed as an argument to `receiveEntry`.
 */
public interface IConsensusClient {
    LinkedBlockingQueue<Message> getBroadcastQueue();

    void receiveEntry(IncomingMessage message);
}
