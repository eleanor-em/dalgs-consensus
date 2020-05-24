package blockchain.p2p;

import blockchain.block.Blockchain;
import blockchain.miner.Miner;
import blockchain.model.BlockchainMessage;
import blockchain.transaction.Transaction;
import blockchain.transaction.TransactionPool;
import blockchain.wallet.Wallet;
import consensus.IConsensusClient;
import consensus.net.data.IncomingMessage;
import consensus.raft.RaftActor;
import consensus.raft.rpc.RpcMessage;
import consensus.raft.state.AbstractRaftState;
import consensus.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class BlockchainRaftActor extends BlockchainActor {
    private static final Logger log = LogManager.getLogger(RaftActor.class);
    private AbstractRaftState state;

    public BlockchainRaftActor(int id, int serverCount, IConsensusClient client, Blockchain blockchain,
                               TransactionPool transactionPool, Miner miner, Wallet wallet) {
        super(id, client, blockchain, transactionPool, miner, wallet);
        this.state = AbstractRaftState.create(id, serverCount, this, client);
        new Thread(() -> monitorBroadcastQueue(client)).start();
    }

    private void monitorBroadcastQueue(IConsensusClient client) {
        var queue = client.getBroadcastQueue();
        while (!Thread.interrupted()) {
            try {
                var message = queue.take();
                var wrapped = new IncomingMessage(message, id);
                state.rpcReceiveEntry(wrapped.encoded());
            } catch (InterruptedException ignored) {
            }
        }
    }

    protected void task() {
        while (!Thread.interrupted()) {
            state = state.tick();
            Thread.yield();
        }
    }

    @Override
    protected void onReceive(IncomingMessage message) {
        var decoded = (RpcMessage) StringUtils.fromJson(message.msg.data, RpcMessage.class);
        switch (decoded.kind) {
            case APPEND_ENTRIES:
                // #TODO: replicate the blockchain here
                this.requestAllToReplicateBlockchain();
                decoded.decodeAppendEntries().ifPresent(args -> state.rpcAppendEntries(decoded.uuid, args));
                break;
            case REQUEST_VOTE:
                decoded.decodeRequestVote().ifPresent(args -> state.rpcRequestVote(decoded.uuid, args));
                break;
            case RESULT:
                decoded.decodeResult().ifPresent(state::rpcReceiveResult);
                break;
            case NEW_ENTRY:
                // #TODO: publish a transaction here
                this.publishTransaction(null);;
                state.rpcReceiveEntry(decoded.payload);
                break;
        }
    }

    protected void sendToClient(Transaction tx, int src) {
    }

    protected void requestAllToReplicateBlockchain() {
    }

    protected void publishTransaction(Transaction transaction) {
    }

    @Override
    protected void requestAllToClearTransactionPool() {
    }

    protected void broadcast(BlockchainMessage blockchainMessage) {
    }
}
