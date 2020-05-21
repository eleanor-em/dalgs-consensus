package consensus.raft.rpc;

public class RpcResult {
    public final String uuid;
    public final int id;
    public final int currentTerm;
    public final boolean success;

    private RpcResult(String uuid, int id, int currentTerm, boolean success) {
        this.uuid = uuid;
        this.id = id;
        this.currentTerm = currentTerm;
        this.success = success;
    }

    public static RpcResult failure(String uuid, int id, int currentTerm) {
        return new RpcResult(uuid, id, currentTerm, false);
    }

    public static RpcResult success(String uuid, int id, int currentTerm) {
        return new RpcResult(uuid, id, currentTerm, true);
    }
}
