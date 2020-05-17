package consensus.raft.rpc;

import consensus.net.data.Message;
import consensus.util.StringUtils;

import java.util.Optional;

public class RpcMessage {
    public final RpcMessageKind kind;
    public final String payload;

    public RpcMessage(AppendEntriesArgs args) {
        this.kind = RpcMessageKind.APPEND_ENTRIES;
        this.payload = StringUtils.toJson(args);
    }

    public RpcMessage(RequestVoteArgs args) {
        this.kind = RpcMessageKind.REQUEST_VOTE;
        this.payload = StringUtils.toJson(args);
    }

    public RpcMessage(RpcResult result) {
        this.kind = RpcMessageKind.RESULT;
        this.payload = StringUtils.toJson(result);
    }

    public Message encoded() {
        return new Message(StringUtils.toJson(this));
    }

    public Optional<AppendEntriesArgs> decodeAppendEntries() {
        if (kind == RpcMessageKind.APPEND_ENTRIES) {
            return Optional.of(StringUtils.fromJson(payload, AppendEntriesArgs.class));
        } else {
            return Optional.empty();
        }
    }

    public Optional<RequestVoteArgs> decodeRequestVote() {
        if (kind == RpcMessageKind.REQUEST_VOTE) {
            return Optional.of(StringUtils.fromJson(payload, RequestVoteArgs.class));
        } else {
            return Optional.empty();
        }
    }

    public Optional<RpcResult> decodeResult() {
        if (kind == RpcMessageKind.RESULT) {
            return Optional.of(StringUtils.fromJson(payload, RpcResult.class));
        } else {
            return Optional.empty();
        }
    }
}
