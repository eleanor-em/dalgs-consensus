package consensus.raft.rpc;

public class RpcResult {
    public final int id;
    public final int currentTerm;
    public final boolean success;

    private RpcResult(int id, int currentTerm, boolean success) {
        this.id = id;
        this.currentTerm = currentTerm;
        this.success = success;
    }

    public static RpcResult failure(int id, int currentTerm) {
        return new RpcResult(id, currentTerm, false);
    }

    public static RpcResult success(int id, int currentTerm) {
        return new RpcResult(id, currentTerm, true);
    }
}
