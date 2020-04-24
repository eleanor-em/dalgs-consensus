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
    private final BlockingQueue<IncomingMessage> receiver = new LinkedBlockingQueue<>();
    private final BlockingQueue<OutgoingMessage> toSend = new LinkedBlockingQueue<>();

    final void receive(IncomingMessage message) {
        onReceive(message);
        receiver.add(message);
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
     * Sends a message to all other peers.
     */
    protected final void sendMessage(Message message, int dest) {
        toSend.add(new OutgoingMessage(message, dest));
    }

    protected final void sendMessageToAll(Message message) {
        toSend.add(new OutgoingMessage(message));
    }

    /**
     * Takes a message from the receiver, blocking until one is available.
     */
    protected final IncomingMessage takeFromReceiver() throws InterruptedException {
        return receiver.take();
    }

    /**
     * Takes a message from the receiver if one is available, otherwise returns Empty.
     */
    protected final Optional<IncomingMessage> pollReceiver() {
        return Optional.ofNullable(receiver.poll());
    }

    /**
     * Waits up to timeoutMs milliseconds for a message to be available from the receiver.
     * Returns Empty if no message arrives in that time.
     */
    protected final Optional<IncomingMessage> pollTimeout(int timeoutMs) throws InterruptedException {
        return Optional.ofNullable(receiver.poll(timeoutMs, TimeUnit.MILLISECONDS));
    }

    /**
     * Returns a message from the receiver if one is available, but leaves it in the receiver.
     */
    protected final Optional<IncomingMessage> peekReceiver() {
        return Optional.ofNullable(receiver.peek());
    }
}
