package consensus.raft.state;

import consensus.IConsensusClient;
import consensus.net.Actor;
import consensus.net.data.Message;
import consensus.raft.rpc.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractRaftState {
    private static final Logger log = LogManager.getLogger(AbstractRaftState.class);
    private final Actor actor;
    private final IConsensusClient client;

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

    AbstractRaftState(int id, int serverCount, Actor actor, IConsensusClient client) {
        this.id = id;
        this.serverCount = serverCount;
        this.actor = actor;
        this.client = client;
    }

    private boolean ledgerExistsNeq(int index, int targetTerm) {
        if (index >= 0 && ledger.containsKey(index)) {
            return ledger.get(index).term != targetTerm;
        } else {
            return false;
        }
    }

    public synchronized void rpcAppendEntries(AppendEntriesArgs args) {
        onAppendEntries(args);

        RpcResult result = RpcResult.success(id, currentTerm);

        if (args.term < currentTerm) {
            result = RpcResult.failure(id, currentTerm);
        } else {
            if (args.term > currentTerm) {
                currentTerm = args.term;
            }
            this.leaderId = args.leaderId;

            if (!args.entries.isEmpty()) {
                // Check consistency of logs
                if (!this.ledgerExistsNeq(args.prevLogIndex, args.prevLogTerm)) {
                    result = RpcResult.failure(id, currentTerm);
                } else {
                    //
                    for (var newEntry : args.entries) {
                        if (this.ledgerExistsNeq(newEntry.index, newEntry.term)) {
                            // Truncate the log
                            for (var key : ledger.keySet()) {
                                if (key >= newEntry.index) {
                                    ledger.remove(key);
                                }
                            }
                        }
                    }

                    updateLog(args);
                }
            }
        }

        sendMessage(new RpcMessage(result).encoded(), args.leaderId);
    }

    private void updateLog(AppendEntriesArgs args) {
        int lastNewIndex = -1;
        int lastNewTerm = -1;
        for (var newEntry : args.entries) {
            if (newEntry.index > lastNewIndex) {
                lastNewIndex = newEntry.index;
            }
            if (newEntry.term > lastNewTerm) {
                lastNewTerm = newEntry.term;
            }
            ledger.put(newEntry.index, newEntry);
        }

        if (lastNewIndex > this.lastLogIndex) {
            this.lastLogIndex = lastNewIndex;
        }
        if (lastNewTerm > this.lastLogTerm) {
            this.lastLogTerm = lastNewTerm;
        }

        if (args.leaderCommit > commitIndex) {
            commitIndex = Math.min(args.leaderCommit, lastNewIndex);
        }
    }

    protected abstract void onAppendEntries(AppendEntriesArgs args);

    public synchronized void rpcRequestVote(RequestVoteArgs args) {
        onRequestVote(args);
        RpcResult result;


        if (args.term < currentTerm) {
            result = RpcResult.failure(id, currentTerm);
        } else {
            if (args.term > currentTerm) {
                currentTerm = args.term;
                votedFor = Optional.empty();
            }

            if ((votedFor.isEmpty() || votedFor.get() == args.candidateId)
                    && args.lastLogIndex >= this.lastLogIndex) {
                log.debug(id + ": voted for " + args.candidateId + " in term " + currentTerm);
                votedFor = Optional.of(args.candidateId);
                result = RpcResult.success(id, currentTerm);
            } else {
                result = RpcResult.failure(id, currentTerm);
            }
        }

        sendMessage(new RpcMessage(result).encoded(), args.candidateId);
    }

    protected abstract void onRequestVote(RequestVoteArgs args);

    public AbstractRaftState tick() {
        while (commitIndex > lastApplied) {
            client.receiveEntry(ledger.get(lastApplied++).message);
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
    }

    protected AbstractRaftState asFollower() {
        log.debug(id + ": convert to follower");
        var state = new FollowerRaftState(id, serverCount, actor, client);
        copyState(state);
        return state;
    }

    protected AbstractRaftState asCandidate() {
        log.debug(id + ": convert to candidate");
        var state = new CandidateRaftState(id, serverCount, actor, client);
        copyState(state);
        state.startElection();
        return state;
    }

    protected AbstractRaftState asLeader() {
        log.debug(id + ": convert to leader");
        var state = new LeaderRaftState(id, serverCount, actor, client);
        copyState(state);
        return state;
    }

    protected void sendMessage(Message message, int dest) {
        actor.sendMessage(message, dest);
    }

    protected void sendMessageToAll(Message message) {
        actor.sendMessageToAll(message);
    }

    public synchronized void rpcReceiveResult(RpcResult result) {
        onReceiveResult(result);

        if (result.currentTerm > currentTerm) {
            log.debug(id + ": result had higher term: " + result.currentTerm + " vs " + currentTerm);
            currentTerm = result.currentTerm;
        }
    }

    protected abstract void onReceiveResult(RpcResult result);
}
