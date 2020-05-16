package blockchain;

import blockchain.block.BlockChain;
import blockchain.p2p.BlockchainActor;
import blockchain.p2p.BlockchainClient;
import blockchain.transaction.Transaction;
import blockchain.transaction.TransactionPool;
import consensus.ConsensusPeer;
import consensus.net.PeerListener;
import consensus.net.data.HostPort;
import consensus.net.data.Message;
import consensus.util.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Main {
    private static final Logger log = LogManager.getLogger(ConsensusPeer.class);
    private static final String USAGE = "usage: java -cp consensus.jar consensus.ConsensusPeer <id>";

    public static void main(String[] args) {
        ConfigManager.loadProperties();
        run();
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

    private static void run() {
        var hosts = loadHosts();

        List<BlockchainClient> blockchainClients = new ArrayList<>();
        // Start a peer for each id
        for (int i = 0; i < hosts.size(); ++i) {
            final int id = i;
            var thisPeerHosts = new ArrayList<>(hosts);
            var blockChain = new BlockChain();
            var transactionPool = new TransactionPool();
            var p2pClient = new BlockchainClient(i, blockChain, transactionPool);
            var blockchainActor = new BlockchainActor(id, p2pClient);
            new Thread(() -> {
                new PeerListener(id, thisPeerHosts, blockchainActor);
            }).start();
            blockchainClients.add(p2pClient);
        }

        for (var blockchainClient : blockchainClients) {
            blockchainClient.sendTransaction(new Transaction());
        }

        for (var blockchainClient : blockchainClients) {
            blockchainClient.broadcastClearTransactions();
        }
    }
}
