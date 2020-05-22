package consensus.net;

import consensus.net.data.HostPort;
import consensus.net.data.IncomingMessage;
import consensus.net.service.ClientConfirmService;
import consensus.net.service.PeerConnectService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * A class that contains all of the management logic for the peer.
 */
public class PeerListener {
    private static final Logger log = LogManager.getLogger(PeerListener.class);

    public final ExecutorService exec = Executors.newCachedThreadPool();
    public final int id;

    private final Map<Integer, Peer> activePeers = new ConcurrentHashMap<>();
    private final Actor actor;
    private final PeerConnectService connectService;

    public PeerListener(int id, List<HostPort> allPeers, Actor actor) {
        this.id = id;
        this.actor = actor;

        // Look up this peer's port, and pass the rest to the connect list
        var hostPort = allPeers.get(id);

        // Start worker threads
        connectService = new PeerConnectService(this, allPeers);
        exec.execute(() -> listen(hostPort));
        exec.execute(this::sendMonitor);
        actor.run();
    }

    /**
     * Attempt to add the given peer to the collection of peers.
     * Returns true if the peer was not already in the collection.
     */
    public boolean addPeer(Peer peer) {
        synchronized (activePeers) {
            if (isMissingPeer(peer.id)) {
                log.info(id + ": connected to peer " + peer.id);
                activePeers.put(peer.id, peer);
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Returns true if the peer is not currently in the collection of peers.
     * Note that this may change before you call {@link PeerListener::addPeer}.
     */
    public boolean isMissingPeer(int id) {
        return !activePeers.containsKey(id);
    }

    /**
     * Attempt to remove the peer from the collection. If the collection contains a different peer with the same id,
     * do nothing. If the collection contains this peer, add it to the retry list.
     *
     * Returns true if the peer was successfully removed.
     */
    public boolean removePeer(Peer peer) {
        synchronized (activePeers) {
            // Check if we're being asked to close the right peer;
            // there may have been multiple attempted peers with the same id
            if (activePeers.containsValue(peer)) {
                activePeers.remove(peer.id);
                connectService.retry(peer.id);
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Handle receiving the given message.
     */
    public void receive(IncomingMessage message) {
        actor.receive(message);
    }

    private void listen(HostPort hostPort) {
        try {
            var serverSocket = new ServerSocket(hostPort.port);
            log.info(id + ": listening on port " + hostPort.port + "...");
            exec.execute(connectService);
            while (!serverSocket.isClosed()) {
                var clientSocket = new IoSocket(serverSocket.accept());
                exec.execute(new ClientConfirmService(this, clientSocket));
            }
        } catch (IOException e) {
            log.fatal(id + ": server socket closed");
            System.exit(-1);
        }
    }

    private void sendMonitor() {
        try {
            while (!Thread.interrupted()) {
                var message = actor.take();
                if (message.dest.isPresent()) {
                    var dest = message.dest.get();
                    Optional.ofNullable(activePeers.get(dest))
                            .ifPresent(peer -> peer.send(message.msg));
                } else {
                    activePeers.values().forEach(peer -> peer.send(message.msg));
                }
            }
        } catch (InterruptedException e) {
            log.fatal(id + ": message sender interrupted.");
            System.exit(-1);
        }
    }
}
