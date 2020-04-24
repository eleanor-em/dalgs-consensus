package consensus;

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
public class Program {
    private static final Logger log = LogManager.getLogger(Program.class);
    private static final String USAGE = "usage: java -jar consensus.jar <id>";

    public static void main(String[] args) {
        runDebug();
    }

    private static List<HostPort> loadHosts() {
        ConfigManager.loadProperties("consensus.properties");

        // Load the targets to connect to and validate them
        var hostList = ConfigManager.getString("hosts").orElse("").split(",");
        var peerHostPorts = Arrays.stream(hostList)
                .map(String::trim)
                .map(HostPort::tryFrom)
                .collect(Collectors.toList());
        if (peerHostPorts.stream().anyMatch(Optional::isEmpty)) {
            log.fatal("Failed parsing hosts");
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
            new Thread(() -> new PeerListener(id, thisPeerHosts, new RaftActor(id))).start();
        }
    }

    private static void runActual(String[] args) {
        // Check command line arguments: the first argument is this peer's ID
        if (args.length < 1) {
            System.out.println(USAGE);
            // Return code 2 indicates the program was called incorrectly
            System.exit(2);
        }

        // Check that the argument is a valid integer
        var maybeId = Validation.tryParseUInt(args[0]);
        if (maybeId.isEmpty()) {
            System.out.println(USAGE);
            System.out.println("note: <id> must be an integer");
            System.exit(2);
        }
        var id = maybeId.get();

        new PeerListener(id, loadHosts(), new RaftActor(id));
    }
}
