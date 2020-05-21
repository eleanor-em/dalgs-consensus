package consensus.raft.state;

import consensus.IConsensusClient;
import consensus.net.Actor;
import consensus.net.data.IncomingMessage;
import consensus.raft.rpc.*;
import consensus.util.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class LeaderRaftState extends AbstractRaftState {
    private static final Logger log = LogManager.getLogger(LeaderRaftState.class);
    // Volatile leader state
    private final List<Integer> nextIndex = new ArrayList<>();
    private final List<Integer> matchIndex = new ArrayList<>();

    private final int sleepTime = ConfigManager.getInt("leaderLag").orElse(165);

    LeaderRaftState(int id, int serverCount, Actor actor, IConsensusClient client) {
        super(id, serverCount, actor, client);
        this.leaderId = id;

        for (int i = 0; i < serverCount; ++i) {
            nextIndex.add(lastLogIndex + 1);
            matchIndex.add(0);
        }
    }

    protected void heartbeat() {
        // Send heartbeat to all followers
        var args = new AppendEntriesArgs(currentTerm, id, 0, 0, new ArrayList<>(), commitIndex);
        this.sendMessageToAll(new RpcMessage(args));
    }

    private void updateFollowers() {
        for (int followerId = 0; followerId < serverCount; ++followerId) {
            if (followerId == id) {
                continue;
            }

            // Check if follower needs an update
            int followerNextIndex = this.nextIndex.get(followerId);
            if (lastLogIndex >= followerNextIndex) {
                // Send update to follower
                var prevLogIndex = followerNextIndex - 1;
                var prevLogTerm = 0;
                if (prevLogIndex > 0) {
                    prevLogTerm = this.ledger.get(prevLogIndex).term;
                }

                var dest = followerId;
                var newEntries = new ArrayList<LedgerEntry>();
                for (int i = prevLogIndex + 1; i <= lastLogIndex; ++i) {
                    log.debug(id + ": sending #" + i + " to " + followerId);
                    newEntries.add(ledger.get(i));
                }

                var args = new AppendEntriesArgs(this.currentTerm, this.id, prevLogIndex, prevLogTerm,
                        newEntries, this.commitIndex);
                this.sendMessage(new RpcMessage(args), dest, result -> {
                    if (result.success) {
                        log.debug(id + ": AppendEntries succeeded for " + dest);
                        matchIndex.set(dest, lastLogIndex);
                    } else {
                        // decrement index and try again
                        log.debug(id + ": AppendEntries failed for " + dest);
                        nextIndex.set(dest, prevLogIndex);
                    }
                });

                nextIndex.set(dest, lastLogIndex + 1);
            } else {
                // No new entries; send heartbeat to this follower
                var args = new AppendEntriesArgs(currentTerm, id, 0, 0, new ArrayList<>(), commitIndex);
                this.sendMessage(new RpcMessage(args), followerId);
            }
        }
    }

    private void updateCommit() {
        for (int n = this.commitIndex + 1; n <= this.lastLogIndex; ++n) {
            int finalN = n;
            int numMatched = (int) this.matchIndex.stream()
                    .filter(i -> i >= finalN)
                    .count();
            if (numMatched > serverCount / 2 && this.ledger.get(n).term == this.currentTerm) {
                log.debug(id + ": leader committed to index " + n);
                this.commitIndex = n;
            }
        }
    }

    @Override
    protected AbstractRaftState onTick() {
        synchronized (lock) {
            updateFollowers();
            updateCommit();
        }

        try {
            // sleep random amount of time to simulate unreliable connection
            Thread.sleep((int) (Math.random() * sleepTime * Timer.TIME_SCALE));
        } catch (InterruptedException ignored) {}

        synchronized (lock) {
            if (shouldBecomeFollower) {
                log.debug(id + ": yielding leadership");
                return this.asFollower();
            } else {
                return this;
            }
        }
    }

    @Override
    public void rpcReceiveEntry(String entry) {
        var maybeMessage = IncomingMessage.tryFrom(entry);
        if (maybeMessage.isPresent()) {
            synchronized (lock) {
                // Add the message to our ledger
                var message = maybeMessage.get();
                ++this.lastLogIndex;
                log.debug(id + ": leader received entry #" + this.lastLogIndex
                        + " from " + message.src + ": " + message.msg.data);

                this.nextIndex.set(id, lastLogIndex + 1);
                this.matchIndex.set(id, lastLogIndex);
                this.ledger.put(this.lastLogIndex, new LedgerEntry(this.lastLogIndex, this.currentTerm, maybeMessage.get()));
            }
        } else {
            log.warn(id + ": leader received malformed entry: " + entry);
        }
    }
}
