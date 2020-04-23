package consensus.net;

import consensus.net.data.Message;
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
    private final BlockingQueue<Message> receiver = new LinkedBlockingQueue<>();
    private final BlockingQueue<Message> toSend = new LinkedBlockingQueue<>();

    final void receive(Message message) {
        onReceive(message);
        receiver.add(message);
    }

    final Message take() throws InterruptedException {
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
    protected void onReceive(Message message) {}

    /**
     * Sends a message to all other peers.
     */
    protected final void sendMessage(Message message) {
        toSend.add(message);
    }

    /**
     * Takes a message from the receiver, blocking until one is available.
     */
    protected final Message takeFromReceiver() throws InterruptedException {
        return receiver.take();
    }

    /**
     * Takes a message from the receiver if one is available, otherwise returns Empty.
     */
    protected final Optional<Message> pollReceiver() {
        return Optional.ofNullable(receiver.poll());
    }

    /**
     * Waits up to timeoutMs milliseconds for a message to be available from the receiver.
     * Returns Empty if no message arrives in that time.
     */
    protected final Optional<Message> pollTimeout(int timeoutMs) throws InterruptedException {
        return Optional.ofNullable(receiver.poll(timeoutMs, TimeUnit.MILLISECONDS));
    }

    /**
     * Returns a message from the receiver if one is available, but leaves it in the receiver.
     */
    protected final Optional<Message> peekReceiver() {
        return Optional.ofNullable(receiver.peek());
    }
}
