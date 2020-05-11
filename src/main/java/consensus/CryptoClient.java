package consensus;
import consensus.crypto.*;
import consensus.net.data.IncomingMessage;
import consensus.net.data.Message;
import consensus.util.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class CryptoClient implements IConsensusClient, Runnable {
    private static final Logger log = LogManager.getLogger(CryptoClient.class);

    private final LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<>();
    private final Map<Integer, KeygenCommitMessage> keygenCommits = new ConcurrentHashMap<>();
    private final Map<Integer, KeygenOpeningMessage> keygenOpenings = new ConcurrentHashMap<>();
    private final Map<Integer, DecryptShareMessage> decryptShares = new ConcurrentHashMap<>();
    private final CryptoContext ctx;

    private final int id;
    private final int peerCount;

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
                case DECRYPT_SHARE:
                    decryptShares.put(message.src, (DecryptShareMessage) msg);
                    break;
            }
        }
    }

    public CryptoClient(int id, int peerCount) {
        this.id = id;
        this.peerCount = peerCount;

        var p = ConfigManager.getString("p").orElseGet(() -> {
            log.warn("using small default prime!");
            return "23817474847197617423";
        });
        this.ctx = new CryptoContext(new BigInteger(p));
    }

    @Override
    public void run() {
        // Sleep to ensure network is live
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}
        log.info("client " + id + " online");

        // Generate key share
        var maybeKeyShare = generateKey(ctx);
        if (maybeKeyShare.isEmpty()) {
            log.warn("client " + id + " failed key generation");
            return;
        }
        var keyShare = maybeKeyShare.get();
        log.info("client " + id + ": " + keyShare);

        // Test: distributed decryption
        var m = BigInteger.valueOf(1337);
        var r = BigInteger.valueOf(62553055);
        var ct = keyShare.encrypt(m, r);
        decrypt(ct, keyShare).ifPresent(result -> log.info("client " + id + ": decryption: " + result));
    }

    private Optional<KeyShare> generateKey(CryptoContext ctx) {
        // Publish commitment and wait for others
        var share = new LocalKeygenShare(ctx);
        var commitMessage = new KeygenCommitMessage(share);
        keygenCommits.put(id, commitMessage);
        queue.offer(commitMessage.encode());

        // Wait for other commitments
        while (keygenCommits.size() < peerCount) {
            Thread.yield();
        }

        // Publish opening and wait for others
        var openingMessage = new KeygenOpeningMessage(share);
        keygenOpenings.put(id, openingMessage);
        queue.offer(openingMessage.encode());
        while (keygenOpenings.size() < peerCount) {
            Thread.yield();
        }

        // Check openings and proofs
        for (var i : keygenOpenings.keySet()) {
            var commit = keygenCommits.get(i);
            var opening = keygenOpenings.get(i);
            if (!opening.verify(commit)) {
                log.warn(String.format("client %d: failed to verify commitment from %d", id, i));
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

    private Optional<BigInteger> decrypt(Ciphertext ct, KeyShare keyShare) {
        // Publish share and wait for others
        var share = new LocalDecryptShare(ctx, keyShare, ct);
        var shareMessage = new DecryptShareMessage(share);
        decryptShares.put(id, shareMessage);
        queue.offer(shareMessage.encode());

        // Wait for other share
        while (decryptShares.size() < peerCount) {
            Thread.yield();
        }

        // Verify proofs; track index for error reporting
        for (var i : decryptShares.keySet()) {
            var shareMsg = decryptShares.get(i);
            if (!shareMsg.verify()) {
                log.warn(String.format("client %d: failed to verify decrypt proof from %d", id, i));
                return Optional.empty();
            }
        }

        // Calculate decryption factor
        var A = ctx.id();
        for (var shareMsg : decryptShares.values()) {
            A = A.mul(shareMsg.a_i);
        }

        var decrypted = ct.b.div(A);
        return Optional.of(decrypted.decoded());
    }
}
