package consensus;

import consensus.ipc.IpcServer;
import consensus.net.data.HostPort;
import consensus.net.PeerListener;
import consensus.raft.RaftActor;
import consensus.util.ConfigManager;
import consensus.util.Validation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Entry point for the program.
 */
public class ConsensusPeer {
    private static final Logger log = LogManager.getLogger(ConsensusPeer.class);
    private static final String USAGE = "usage: java -cp consensus.jar consensus.ConsensusPeer <id>";

    public static void main(String[] args) {
        ConfigManager.loadProperties();
        if (ConfigManager.isDebug()) {
            runDebug();
        } else {
            runRelease();
        }
    }

    private static List<HostPort> loadHosts() {
        // Load the targets to connect to and validate them
        var hostList = ConfigManager.getString("hosts").orElse("").split(",");
        var peerHostPorts = Arrays.stream(hostList)
                .map(String::trim)
                .map(HostPort::tryFrom)
                .collect(Collectors.toList());
        if (peerHostPorts.stream().anyMatch(Optional::isEmpty)) {
            log.fatal("failed parsing hosts");
            System.exit(-1);
        }

        // Validation done
        return peerHostPorts.stream()
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private static void runDebug() {
        var hosts = loadHosts();

        // Start a peer for each id
        for (int i = 0; i < hosts.size(); ++i) {
            final int id = i;
            var thisPeerHosts = new ArrayList<>(hosts);
            var client = new IpcServer(id);
            new Thread(() -> new PeerListener(id, thisPeerHosts, new RaftActor(id, client))).start();
        }
    }

    private static void runRelease() {
        var maybeId = ConfigManager.getInt("id");
        if (maybeId.isEmpty()) {
            log.fatal("id must be an integer (check configuration file)");
            System.exit(2);
        }
        int id = maybeId.get();

        var hosts = loadHosts();
        var client = new IpcServer(id);
        new Thread(() -> new PeerListener(id, hosts, new RaftActor(id, client))).start();
    }
}
