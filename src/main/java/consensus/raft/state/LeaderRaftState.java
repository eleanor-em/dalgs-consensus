package consensus.raft.state;

import consensus.IConsensusClient;
import consensus.net.Actor;
import consensus.net.data.IncomingMessage;
import consensus.raft.rpc.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class LeaderRaftState extends AbstractRaftState {
    private static final Logger log = LogManager.getLogger(LeaderRaftState.class);
    // Volatile leader state
    private final List<Integer> nextIndex = new ArrayList<>();
    private final List<Integer> matchIndex = new ArrayList<>();

    private boolean shouldBecomeFollower = false;

    LeaderRaftState(int id, int serverCount, Actor actor, IConsensusClient client) {
        super(id, serverCount, actor, client);
        leaderId = id;

        for (int i = 0; i < serverCount; ++i) {
            nextIndex.add(lastLogIndex + 1);
            matchIndex.add(0);
        }
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
                    log.debug(id + ": sending entry " + i);
                    newEntries.add(ledger.get(i));
                }

                var args = new AppendEntriesArgs(this.currentTerm, this.id, prevLogIndex, prevLogTerm,
                        newEntries, this.commitIndex);
                this.sendMessage(new RpcMessage(args), dest, result -> {
                    if (result.success) {
                        matchIndex.set(dest, lastLogIndex + 1);
                    } else {
                        // decrement index and try again
                        log.debug(id + ": AppendEntries failed for " + dest);
                        nextIndex.set(dest, followerNextIndex - 1);
                    }
                });

                nextIndex.set(dest, lastLogIndex + 1);
            } else {
                // Send heartbeat to this follower
                var args = new AppendEntriesArgs(currentTerm, id, 0, 0, new ArrayList<>(), commitIndex);
                this.sendMessage(new RpcMessage(args), followerId, this::onReceiveResult);
            }
        }
    }

    private void updateCommit() {
        for (int n = this.commitIndex + 1; n <= this.lastLogIndex; ++n) {
            int finalN = n;
            int numMatched = (int) this.matchIndex.stream()
                    .filter(i -> i > finalN)
                    .count();
            if (numMatched > serverCount / 2 && this.ledger.get(n).term == this.currentTerm) {
                log.debug(id + ": leader committed to index " + n);
                this.commitIndex = n;
            }
        }
    }

    @Override
    protected AbstractRaftState onTick() {
        if (shouldBecomeFollower) {
            return this.asFollower();
        } else {
            updateFollowers();
            updateCommit();

            try {
                // sleep random amount of time to simulate unreliable connection
                Thread.sleep((int) (Math.random() * 10 * Timer.TIME_SCALE));
            } catch (InterruptedException ignored) {}

            return this;
        }
    }

    @Override
    protected void onAppendEntries(AppendEntriesArgs args) {
        if (args.term > currentTerm) {
            shouldBecomeFollower = true;
        }
    }

    @Override
    protected void onRequestVote(RequestVoteArgs args) {
        if (args.term > currentTerm) {
            shouldBecomeFollower = true;
        }
    }

    private void onReceiveResult(RpcResult result) {
        if (result.currentTerm > currentTerm) {
            shouldBecomeFollower = true;
        }
    }

    @Override
    public synchronized void rpcReceiveEntry(String entry) {
        var maybeMessage = IncomingMessage.tryFrom(entry);
        if (maybeMessage.isPresent()) {
            var message = maybeMessage.get();
            log.debug(id + ": leader received entry from " + message.src + ": " + message.msg.data);
            ++this.lastLogIndex;
            this.ledger.put(this.lastLogIndex, new LedgerEntry(this.lastLogIndex, this.currentTerm, maybeMessage.get()));
        } else {
            log.warn(id + ": leader received malformed entry: " + entry);
        }
    }
}
