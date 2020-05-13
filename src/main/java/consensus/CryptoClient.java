package consensus;
import consensus.crypto.*;
import consensus.ipc.IpcClient;
import consensus.net.data.HostPort;
import consensus.net.data.IncomingMessage;
import consensus.net.data.Message;
import consensus.util.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class CryptoClient implements IConsensusClient, Runnable {
    private static final Logger log = LogManager.getLogger(CryptoClient.class);

    private final LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<>();
    private final Map<Integer, KeygenCommitMessage> keygenCommits = new ConcurrentHashMap<>();
    private final Map<Integer, KeygenOpeningMessage> keygenOpenings = new ConcurrentHashMap<>();
    private final Map<Integer, PostVoteMessage> postVotes = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, DecryptShareMessage>> decryptShares = new ConcurrentHashMap<>();

    private final CryptoContext ctx;
    private final int peerCount;
    private final Random rng = new Random();

    @Override
    public LinkedBlockingQueue<Message> getBroadcastQueue() {
        return queue;
    }

    @Override
    public void receiveEntry(IncomingMessage message) {
        var decoded = CryptoMessage.tryFrom(this.ctx, message.msg.data);
        if (decoded.isPresent()) {
            var msg = decoded.get();
            switch (msg.kind) {
                case KEYGEN_COMMIT:
                    keygenCommits.put(message.src, (KeygenCommitMessage) msg);
                    break;
                case KEYGEN_OPENING:
                    keygenOpenings.put(message.src, (KeygenOpeningMessage) msg);
                    break;
                case POST_VOTE:
                    postVotes.put(message.src, (PostVoteMessage) msg);
                    break;
                case DECRYPT_SHARE:
                    putDecryptShare(message.src, (DecryptShareMessage) msg);
                    break;
            }
        }
    }

    private void putDecryptShare(int src, DecryptShareMessage msg) {
        if (!decryptShares.containsKey(msg.id)) {
            decryptShares.put(msg.id, new ConcurrentHashMap<>());
        }
        decryptShares.get(msg.id).put(src, msg);
    }

    public static void main(String[] args) {
        ConfigManager.loadProperties();
        var ipcServerString = ConfigManager.getString("ipcServer");
        if (ipcServerString.isEmpty()) {
            System.exit(2);
        }

        var ipcServer = HostPort.tryFrom(ipcServerString.get());
        if (ipcServer.isEmpty()) {
            log.fatal("could not interpret address: " + ipcServerString.get());
            System.exit(2);
        }

        var peerCount = ConfigManager.getString("hosts").orElse("").split(",").length;
        if (peerCount == 0) {
            log.fatal("Must have at least one peer. Check \"hosts\" in the configuration file.");
            System.exit(2);
        }
        var cryptoClient = new CryptoClient(peerCount);
        IpcClient.open(ipcServer.get(), cryptoClient);
        cryptoClient.run();
        System.exit(0);
    }

    public CryptoClient(int peerCount) {
        this.peerCount = peerCount;

        var p = ConfigManager.getString("p").orElseGet(() -> {
            log.warn("using small default prime!");
            return "23817474847197617423";
        });
        this.ctx = new CryptoContext(new BigInteger(p));
    }

    private void unsafeSleep() {
        try {
            Thread.sleep(rng.nextInt(10000) + 1000);
        } catch (InterruptedException ignored) {}
    }

    @Override
    public void run() {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ignored) {}
        this.unsafeSleep();
        log.info("online");

        // Generate key share
        var maybeKeyShare = generateKey(ctx);
        if (maybeKeyShare.isEmpty()) {
            return;
        }

        var keyShare = maybeKeyShare.get();
        log.info("" + keyShare);

        // Send vote
        int vote = (int) (System.currentTimeMillis() % 1000);
        if (!sendVote(keyShare, vote)) {
            return;
        }

        // Decrypt votes
        var maybeVotes = decryptVotes(keyShare);
        if (maybeVotes.isEmpty()) {
            return;
        }

        log.info("result: " + maybeVotes.get());
    }

    private boolean sendVote(KeyShare keyShare, int vote) {
        // Send vote
        var msg = new PostVoteMessage(ctx, keyShare, vote);
        queue.offer(msg.encode());
        log.info("vote posted");

        // Wait for other votes
        while (postVotes.size() < peerCount) {
            Thread.yield();
        }
        this.unsafeSleep();

        // Check proofs
        for (var postVote : postVotes.values()) {
            if (!postVote.verify(ctx)) {
                log.warn("failed to verify others' votes");
                return false;
            }
        }

        return true;
    }

    private Optional<KeyShare> generateKey(CryptoContext ctx) {
        // Publish commitment and wait for others
        var share = new LocalKeygenShare(ctx);
        var commitMessage = new KeygenCommitMessage(share);
        queue.offer(commitMessage.encode());
        log.info("key generation commit posted");

        // Wait for other commitments
        while (keygenCommits.size() < peerCount) {
            Thread.yield();
        }
        this.unsafeSleep();

        // Publish opening and wait for others
        var openingMessage = new KeygenOpeningMessage(share);
        queue.offer(openingMessage.encode());
        log.info("key generation opening posted");
        while (keygenOpenings.size() < peerCount) {
            Thread.yield();
        }
        this.unsafeSleep();

        // Check openings and proofs
        for (var i : keygenOpenings.keySet()) {
            var commit = keygenCommits.get(i);
            var opening = keygenOpenings.get(i);
            if (!opening.verify(commit)) {
                log.warn(String.format("failed to verify commitment from %d", i));
                return Optional.empty();
            }
        }

        // Construct public key
        var pk = ctx.id();
        for (var opening : keygenOpenings.values()) {
            pk = pk.mul(opening.y_i);
        }

        return Optional.of(new KeyShare(ctx, pk, share));
    }

    private Optional<List<BigInteger>> decryptVotes(KeyShare keyShare) {
        List<BigInteger> votes = new ArrayList<>();
        Map<String, Ciphertext> ciphertexts = new HashMap<>();

        // Publish shares for each vote
        for (var postVoteMsg : postVotes.values()) {
            var shareMessage = new DecryptShareMessage(ctx, keyShare, postVoteMsg.vote);
            // Index ciphertext by ID
            ciphertexts.put(shareMessage.id, postVoteMsg.vote);
            log.info("decrypt share for " + shareMessage.id + " posted");
            queue.offer(shareMessage.encode());
            this.unsafeSleep();
        }

        // Wait for all shares
        while (decryptShares.values().stream().anyMatch(shares -> shares.size() < peerCount)) {
            Thread.yield();
        }

        // Decrypt each complete set of shares
        for (var id : decryptShares.keySet()) {
            var decrypted = decrypt(decryptShares.get(id), ciphertexts.get(id));
            if (decrypted.isEmpty()) {
                return Optional.empty();
            } else {
                votes.add(decrypted.get());
            }
        }

        return Optional.of(votes);
    }

    private Optional<BigInteger> decrypt(Map<Integer, DecryptShareMessage> map, Ciphertext ct) {
        // Verify proofs
        for (var i : map.keySet()) {
            var shareMessage = map.get(i);
            if (!shareMessage.verify(keygenOpenings.get(i).y_i, ct)) {
                log.warn("failed to verify decrypt proof from " + i);
                return Optional.empty();
            }
        }

        // Calculate decryption factor
        var A = ctx.id();
        for (var shareMsg : map.values()) {
            A = A.mul(shareMsg.a_i);
        }

        var decrypted = ct.b.div(A);
        return Optional.of(decrypted.decoded());
    }
}
