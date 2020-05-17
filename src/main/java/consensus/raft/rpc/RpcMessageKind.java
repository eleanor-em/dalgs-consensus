package consensus.raft.rpc;

public enum RpcMessageKind {
    APPEND_ENTRIES,
    REQUEST_VOTE,
    RESULT
}
