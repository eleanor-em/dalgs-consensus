package consensus.net.service;

import consensus.net.IoSocket;
import consensus.net.Peer;
import consensus.net.PeerListener;
import consensus.net.data.HostPort;
import consensus.util.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class PeerConnectService implements Runnable {
    private static final Logger log = LogManager.getLogger(PeerConnectService.class);

    private final BlockingQueue<Integer> toConnect = new LinkedBlockingQueue<>();
    private final BlockingQueue<Integer> toReconnect = new LinkedBlockingQueue<>();
    private final PeerListener parent;
    private final Map<Integer, HostPort> hosts = new ConcurrentHashMap<>();

    private final int retryTime;

    public PeerConnectService(PeerListener parent, List<HostPort> peers) {
        this.parent = parent;

        // Load the host information
        for (int i = 0; i < peers.size(); ++i) {
            if (i != parent.id) {
                hosts.put(i, peers.get(i));
            }
        }

        // Initially, only attempt to connect to those peers whose id is lower than ours
        for (int i = 0 ; i < parent.id; ++i) {
            toConnect.add(i);
        }

        retryTime = ConfigManager.getInt("retryTime").orElse(1000);
        parent.exec.execute(this::handleReconnect);
    }

    public void retry(int id) {
        toReconnect.add(id);
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                var id = toConnect.take();
                if (parent.isMissingPeer(id)) {
                    var dest = hosts.get(id);
                    try {
                        var socket = dest.connect();
                        socket.writeLine(parent.id);
                        parent.addPeer(new Peer(parent, id, socket));
                    } catch (IOException e) {
                        log.warn(parent.id + ": failed to connect to " + dest);
                        toReconnect.add(id);
                    }
                }
            }
        } catch (InterruptedException e) {
            log.fatal(parent.id + ": thread interrupted");
            System.exit(-1);
        }
    }

    private void handleReconnect() {
        try {
            while (!Thread.interrupted()) {
                var retryDest = toReconnect.take();
                Thread.sleep(retryTime);
                toConnect.add(retryDest);
            }
        } catch (InterruptedException unused) {
            log.fatal(parent.id + ": thread interrupted");
            System.exit(-1);
        }
    }
}
