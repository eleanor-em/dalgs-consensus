package consensus.raft.state;

import consensus.IConsensusClient;
import consensus.net.Actor;
import consensus.raft.rpc.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

class CallbackWrapper {
    public final int timesToCall;
    private int timesCalled;
    public final Consumer<RpcResult> action;

    CallbackWrapper(int timesToCall, Consumer<RpcResult> action) {
        this.timesToCall = timesToCall;
        this.action = action;
    }

    boolean call(RpcResult result) {
        if (timesCalled++ < timesToCall) {
            action.accept(result);
            return true;
        } else {
            return false;
        }
    }
}

public abstract class AbstractRaftState {
    private static final Logger log = LogManager.getLogger(AbstractRaftState.class);
    private final Actor actor;
    private final IConsensusClient client;
    protected final Object lock = new Object();

    // Handle RPC returns
    private final Map<String, CallbackWrapper> onReturn = new HashMap<>();

    // Persistent state
    protected final int id;
    protected final int serverCount;
    protected int currentTerm = 0;
    protected Optional<Integer> votedFor = Optional.empty();
    protected Map<Integer, LedgerEntry> ledger = new ConcurrentHashMap<>();
    protected int lastLogIndex = 0;
    protected int lastLogTerm = 0;

    // Volatile state
    protected int commitIndex = 0;
    protected int lastApplied = 0;
    protected int leaderId = 0;
    protected boolean shouldBecomeFollower = false;

    AbstractRaftState(int id, int serverCount, Actor actor, IConsensusClient client) {
        this.id = id;
        this.serverCount = serverCount;
        this.actor = actor;
        this.client = client;
    }

    private void yield(int newTerm) {
        shouldBecomeFollower = true;
        currentTerm = newTerm;
        votedFor = Optional.empty();
    }

    protected void onRpc() {}

    public void rpcAppendEntries(String uuid, AppendEntriesArgs args) {
        synchronized (lock) {
            onRpc();
            RpcResult result = RpcResult.success(uuid, id, currentTerm);

            if (args.term < currentTerm) {
                log.warn(id + ": failed AppendEntries due to outdated term");
                result = RpcResult.failure(uuid, id, currentTerm);
            } else {
                // Check if we're behind
                if (args.term > currentTerm) {
                    this.yield(args.term);
                }

                // Check if there is a new leader
                if (this.leaderId != args.leaderId) {
                    log.debug(id + ": new leader: " + args.leaderId);
                    this.leaderId = args.leaderId;
                }

                if (!args.entries.isEmpty()) {
                    log.debug(id + ": received entries: " + args.entries);
                }

                // Check consistency of logs: first, check the leader has the right assumptions about us.
                if (args.prevLogIndex > 0 && args.prevLogTerm > 0
                    && (!ledger.containsKey(args.prevLogIndex) || ledger.get(args.prevLogIndex).term != args.prevLogTerm)) {
                    log.warn(id + ": failed AppendEntries due to inconsistent logs: " + args.prevLogIndex + ", " + args.prevLogTerm);
                    result = RpcResult.failure(uuid, id, currentTerm);
                } else {
                    // Next, check if our log needs to be truncated to come in line with the leader's.
                    for (var newEntry : args.entries) {
                        log.debug(id + ": checking entry #" + newEntry.index);
                        if (ledger.containsKey(newEntry.index) && ledger.get(newEntry.index).term != newEntry.term) {
                            log.warn(id + ": incorrect ledger: index " + newEntry.index + " has term "
                                    + newEntry.term + " != " + ledger.get(newEntry.index).term
                                    + ", truncating");
                            lastLogIndex = newEntry.index - 1;
                            if (lastLogIndex > 0) {
                                lastLogTerm = ledger.get(lastLogIndex).term;
                            } else {
                                lastLogTerm = 0;
                            }
                            // Truncate the log
                            for (var key : ledger.keySet()) {
                                if (key >= newEntry.index) {
                                    ledger.remove(key);
                                }
                            }
                        }
                    }
                    // Finally, update our log.
                    updateLog(args);
                }
            }

            sendMessage(new RpcMessage(result), args.leaderId);
        }
    }

    private void updateLog(AppendEntriesArgs args) {
        // Default values are such that no change is made when there are no new entries
        int lastNewIndex = lastLogIndex;
        int lastNewTerm = lastLogTerm;

        for (var newEntry : args.entries) {
            if (newEntry.index > lastNewIndex) {
                lastNewIndex = newEntry.index;
            }
            if (newEntry.term > lastNewTerm) {
                lastNewTerm = newEntry.term;
            }
            log.debug(id + ": appending entry #" + newEntry.index + ": " + newEntry.message.msg.data);
            ledger.put(newEntry.index, newEntry);
        }

        this.lastLogIndex = lastNewIndex;
        this.lastLogTerm = lastNewTerm;

        // Update the commit index; do nothing if there is no change.
        var newCommitIndex = Math.min(args.leaderCommit, lastNewIndex);
        if (args.leaderCommit > commitIndex && newCommitIndex > commitIndex) {
            commitIndex = newCommitIndex;
            log.debug(id + ": follower committed to index " + commitIndex);
        }
    }

    public void rpcRequestVote(String uuid, RequestVoteArgs args) {
        synchronized (lock) {
            onRpc();
            RpcResult result;

            if (args.term < currentTerm) {
                result = RpcResult.failure(uuid, id, currentTerm);
            } else {
                if (args.term > currentTerm) {
                    this.yield(args.term);
                }

                if (votedFor.map(candidateId -> candidateId == args.candidateId).orElse(true)
                        && args.lastLogIndex >= this.lastLogIndex) {
                    log.debug(id + ": voted for " + args.candidateId + " in term " + currentTerm);
                    votedFor = Optional.of(args.candidateId);
                    result = RpcResult.success(uuid, id, currentTerm);
                } else {
                    result = RpcResult.failure(uuid, id, currentTerm);
                }
            }

            sendMessage(new RpcMessage(result), args.candidateId);
        }
    }

    public AbstractRaftState tick() {
        synchronized (lock) {
            while (commitIndex > lastApplied) {
                var message = ledger.get(++lastApplied).message;
                log.debug(id + ": committed to message #" + lastApplied + " from " + message.src + ": " + message.msg.data);
                client.receiveEntry(message);
            }
        }

        return this.onTick();
    }

    protected abstract AbstractRaftState onTick();

    public static AbstractRaftState create(int id, int serverCount, Actor actor, IConsensusClient client) {
        return new FollowerRaftState(id, serverCount, actor, client);
    }

    private void copyState(AbstractRaftState state) {
        state.currentTerm = currentTerm;
        state.commitIndex = commitIndex;
        state.lastApplied = lastApplied;
        state.ledger = ledger;
        state.lastLogIndex = lastLogIndex;
        state.lastLogTerm = lastLogTerm;
        state.shouldBecomeFollower = false;
    }

    protected AbstractRaftState asFollower() {
        var state = new FollowerRaftState(id, serverCount, actor, client);
        copyState(state);
        return state;
    }

    protected AbstractRaftState asCandidate() {
        var state = new CandidateRaftState(id, serverCount, actor, client);
        copyState(state);
        state.startElection();
        return state;
    }

    protected AbstractRaftState asLeader() {
        log.debug(id + ": convert to leader");
        var state = new LeaderRaftState(id, serverCount, actor, client);
        copyState(state);
        state.heartbeat();
        return state;
    }

    protected void sendMessage(RpcMessage message, int dest) {
        actor.sendMessage(message.encoded(), dest);
    }

    protected void sendMessage(RpcMessage message, int dest, Consumer<RpcResult> callback) {
        this.onReturn.put(message.uuid, new CallbackWrapper(1, callback));
        actor.sendMessage(message.encoded(), dest);
    }

    protected void sendMessageToAll(RpcMessage message) {
        actor.sendMessageToAll(message.encoded());
    }

    protected void sendMessageToAll(RpcMessage message, Consumer<RpcResult> callback) {
        this.onReturn.put(message.uuid, new CallbackWrapper(serverCount - 1, callback));
        actor.sendMessageToAll(message.encoded());
    }

    public synchronized void rpcReceiveResult(RpcResult result) {
        // Call the callback
        if (onReturn.containsKey(result.uuid)) {
            if (!onReturn.get(result.uuid).call(result)) {
                onReturn.remove(result.uuid);
            }
        }

        if (result.currentTerm > currentTerm) {
            log.debug(id + ": result had higher term: " + result.currentTerm + " vs " + currentTerm);
            this.yield(result.currentTerm);
        }
    }

    public void rpcReceiveEntry(String entry) {
        log.debug(id + ": received entry: " + entry);
        if (id != leaderId) {
            this.sendMessage(new RpcMessage(entry), leaderId);
        }
    }
}
