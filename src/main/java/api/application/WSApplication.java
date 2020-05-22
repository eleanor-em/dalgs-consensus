package api.application;

import api.resource.BlockchainResource;
import blockchain.block.Blockchain;
import blockchain.miner.Miner;
import blockchain.transaction.TransactionPool;
import blockchain.wallet.Wallet;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;

public class WSApplication extends Application<Configuration> {
    private final Blockchain blockchain;
    private final TransactionPool transactionPool;
    private final Miner miner;
    private final Wallet wallet;

    public WSApplication(Blockchain blockchain, TransactionPool transactionPool, Miner miner, Wallet wallet) {
        this.blockchain = blockchain;
        this.transactionPool = transactionPool;
        this.miner = miner;
        this.wallet = wallet;
    }

    @Override
    public void run(Configuration configuration, Environment environment) {
        final BlockchainResource blockchainResource = new BlockchainResource(blockchain, transactionPool, miner, wallet);
        environment.jersey().register(blockchainResource);
    }
}
