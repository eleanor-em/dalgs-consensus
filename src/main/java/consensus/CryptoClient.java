package consensus;
import consensus.crypto.*;
import consensus.net.data.IncomingMessage;
import consensus.net.data.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class CryptoClient implements IConsensusClient, Runnable {
    private static final Logger log = LogManager.getLogger(CryptoClient.class);

    // 64-bit: 23817474847197617423
    // 512-bit: 17576155655779058271814901328806178171487950168424777102087080655104162734827238302572043932624849068810300718730750252373895635484455492913940374855814283
    // 3072-bit: 3201854198135043281751355951827302660717511577891039451100174903440311942574941872652748640372349512474471423953808906137608197612556723743299109196846441296496088482068282777108403527044577914570548914539399761423404843302059458784166935553050250420159684903245869881204419231081072120037774160529439943413158579371036269117493761439144021603528995268238221293920629088577318006153707974989347456966453436389819000544893522248739862085759243075712049060578785622437016843286797368864289250348491723387972989428297309759691415454847960021754463768773770208123987139826657993458486866489786078782212118563122872570425025853708528722340323683612559816749109375758702868941992463509690971106804076715429672139592578626217662466329615330744227347983762186481264480941285296858085874235763952664402560076176529492177264268491764539440999807667849978260275623298009971415037610988759111052545874375581802776689278470202020490499443
    private static final BigInteger p = new BigInteger("23817474847197617423");

    private final LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<>();
    private final Map<Integer, KeygenCommitMessage> keygenCommits = new ConcurrentHashMap<>();
    private final Map<Integer, KeygenOpeningMessage> keygenOpenings = new ConcurrentHashMap<>();
    private final Map<Integer, DecryptShareMessage> decryptShares = new ConcurrentHashMap<>();
    private final CryptoContext ctx = new CryptoContext(p);

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
    }
    private void unsafeSleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }

    @Override
    public void run() {
        // Sleep to ensure network is live
        unsafeSleep(1000);
        log.info("client " + id + " online");

        // Generate parameters
        var ctx = new CryptoContext(p);

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
