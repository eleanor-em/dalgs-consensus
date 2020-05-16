package blockchain.p2p;

import blockchain.block.BlockChain;
import blockchain.transaction.TransactionPool;
import consensus.ipc.IpcServer;

public class BlockchainClient extends IpcServer {
    private BlockChain blockChain;
    private TransactionPool transactionPool;

    public BlockchainClient(int id) {
        super(id);
    }
}
