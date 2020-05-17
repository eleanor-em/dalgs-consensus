package consensus.net;

import consensus.net.data.IncomingMessage;
import consensus.net.data.Message;
import consensus.net.data.OutgoingMessage;
import consensus.net.service.PeerConnectService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.concurrent.*;

/**
 * This class will act on the messages it receives, and send messages to other peers.
 * Abstracts away the details of peer-to-peer message passing.
 */
public abstract class Actor implements Runnable {
    private static final Logger log = LogManager.getLogger(PeerConnectService.class);
    private final BlockingQueue<OutgoingMessage> toSend = new LinkedBlockingQueue<>();

    final void receive(IncomingMessage message) {
        onReceive(message);
    }

    final OutgoingMessage take() throws InterruptedException {
        return toSend.take();
    }

    @Override
    public void run() {
        try {
            this.task();
        } catch (InterruptedException unused) {
            log.fatal("actor thread interrupted");
            System.exit(-1);
        }
    }

    /**
     * The task to be run by this Actor.
     */
    protected abstract void task() throws InterruptedException;

    /**
     * May be overridden to perform an action when a message is received by the actor.
     */
    protected void onReceive(IncomingMessage message) {}

    /**
     * Sends a message to a specific other peer.
     */
    public final void sendMessage(Message message, int dest) {
        toSend.add(new OutgoingMessage(message, dest));
    }

    /**
     * Sends a message to all other peers.
     */
    public final void sendMessageToAll(Message message) {
        toSend.add(new OutgoingMessage(message));
    }
}
