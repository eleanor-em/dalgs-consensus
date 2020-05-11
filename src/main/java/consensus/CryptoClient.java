package consensus;
import consensus.crypto.*;
import consensus.net.data.IncomingMessage;
import consensus.net.data.Message;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class CryptoClient implements IConsensusClient, Runnable {
    private static final BigInteger p = new BigInteger("23817474847197617423");

    private final LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<>();
    private final Map<Integer, KeygenCommitMessage> keygenCommitments = new ConcurrentHashMap<>();
    private final Map<Integer, KeygenOpeningMessage> keygenOpenings = new ConcurrentHashMap<>();
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
                    keygenCommitments.put(message.src, (KeygenCommitMessage) msg);
                    break;
                case KEYGEN_OPENING:
                    keygenOpenings.put(message.src, (KeygenOpeningMessage) msg);
                    break;
            }
        }
    }

    public CryptoClient(int id, int peerCount) {
        this.id = id;
        this.peerCount = peerCount;
    }

    @Override
    public void run() {
        // Sleep to ensure network is live
        try {
            Thread.sleep(2000);
        } catch (InterruptedException unused) {}
        System.out.println("client " + id + " online");

        // Generate parameters, and provide commitment
        var ctx = new CryptoContext(p);
        var keyShare = generateKey(ctx);
        System.out.format("Client %d: %s\n", id, keyShare);
    }

    private KeyShare generateKey(CryptoContext ctx) {
        var share = new LocalShare(ctx);
        var commitMessage = new KeygenCommitMessage(share);
        keygenCommitments.put(id, commitMessage);
        queue.offer(commitMessage.encode());

        // Wait for other commitments
        while (keygenCommitments.size() < peerCount) {
            Thread.yield();
        }

        // Publish & wait for openings
        var openingMessage = new KeygenOpeningMessage(share);
        keygenOpenings.put(id, openingMessage);
        queue.offer(openingMessage.encode());
        while (keygenOpenings.size() < peerCount) {
            Thread.yield();
        }

        // Check openings and proofs
        for (var i : keygenOpenings.keySet()) {
            var commit = keygenCommitments.get(i);
            var opening = keygenOpenings.get(i);
            if (!Arrays.equals(CryptoUtils.hash(opening.y_i), commit.commitment)) {
                System.out.println("client " + id + ": failed commitment " + i);
            }
            if (!opening.proof.verify() || !opening.proof.y.equals(opening.y_i) || !opening.proof.g.equals(ctx.g)) {
                System.out.println("client " + id + ": failed ProofKnowDlog " + i);
            }
        }

        // Construct public key
        var pk = ctx.id();
        for (var opening : keygenOpenings.values()) {
            pk = pk.mul(opening.y_i);
        }

        return new KeyShare(ctx, pk, share);
    }
}
