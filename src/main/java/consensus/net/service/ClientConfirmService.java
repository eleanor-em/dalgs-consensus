package consensus.net.service;

import consensus.net.Peer;
import consensus.net.IoSocket;
import consensus.net.PeerListener;
import consensus.util.Validation;

public class ClientConfirmService implements Runnable {
    private final PeerListener parent;
    private final IoSocket socket;

    public ClientConfirmService(PeerListener parent, IoSocket socket) {
        this.parent = parent;
        this.socket = socket;
    }

    @Override
    public void run() {
        socket.readLine()
                .flatMap(Validation::tryParseUInt)
                .ifPresent(id -> {
                    // If we already have a connection to this peer, close the socket
                    if (!parent.addPeer(new Peer(parent, id, socket))) {
                        socket.close();
                    }
                });
    }
}
