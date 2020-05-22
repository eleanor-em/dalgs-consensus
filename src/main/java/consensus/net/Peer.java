package consensus.net;

import consensus.net.data.IncomingMessage;
import consensus.net.data.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.Future;

public class Peer {
    private static final Logger log = LogManager.getLogger(Peer.class);
    public final int id;
    private final IoSocket socket;
    private final PeerListener parent;
    private final Future<?> monitor;

    public Peer(PeerListener parent, int id, IoSocket socket) {
        this.parent = parent;
        this.id = id;
        this.socket = socket;

        // Start read thread
        monitor = parent.exec.submit(this::monitorInput);
    }

    /**
     * Send the given message to this peer.
     */
    public void send(Message message) {
        try {
            socket.writeLine(new IncomingMessage(message, parent.id).encoded());
        } catch (IOException unused) {
            close();
        }
    }

    private void monitorInput() {
        while (!Thread.interrupted()) {
            socket.readLine()
                    .flatMap(IncomingMessage::tryFrom)
                    .ifPresentOrElse(parent::receive,
                                     this::close);
        }
    }

    private void close() {
        log.warn(id + ": peer closed");
        socket.close();
        parent.removePeer(this);
        monitor.cancel(true);
    }
}
