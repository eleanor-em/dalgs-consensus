package consensus.raft.rpc;

public class RpcResult {
    public final String uuid;
    public final int id;
    public final int currentTerm;
    public final boolean success;
    public final int lastLogIndex;

    private RpcResult(String uuid, int id, int currentTerm, boolean success, int lastLogIndex) {
        this.uuid = uuid;
        this.id = id;
        this.currentTerm = currentTerm;
        this.success = success;
        this.lastLogIndex = lastLogIndex;
    }

    public static RpcResult failure(String uuid, int id, int currentTerm, int lastLogIndex) {
        return new RpcResult(uuid, id, currentTerm, false, lastLogIndex);
    }

    public static RpcResult success(String uuid, int id, int currentTerm, int lastLogIndex) {
        return new RpcResult(uuid, id, currentTerm, true, lastLogIndex);
    }
}
