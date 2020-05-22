package api.resource;

import api.pojo.TransactionPOJO;
import blockchain.block.Blockchain;
import blockchain.miner.Miner;
import blockchain.transaction.TransactionPool;
import blockchain.wallet.Wallet;
import com.codahale.metrics.annotation.Timed;
import consensus.util.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("")
public class BlockchainResource {
    private final Blockchain blockchain;
    private final TransactionPool transactionPool;
    private final Miner miner;
    private final Wallet wallet;

    public BlockchainResource(Blockchain blockchain, TransactionPool transactionPool, Miner miner, Wallet wallet) {
        this.blockchain = blockchain;
        this.transactionPool = transactionPool;
        this.miner = miner;
        this.wallet = wallet;
    }

    @GET
    @Path("/get-public-key")
    @Timed
    public String getPublicKey() {
        return wallet.getAddress();
    }

    @GET
    @Path("/get-block-list")
    @Timed
    public Response getBlockList() {
        try {
            return Response.ok(StringUtils.toJson(blockchain.getBlockList()), MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @GET
    @Timed
    @Path("/get-transaction-list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTransactionList() {
        try {
            return Response.ok(StringUtils.toJson(transactionPool.getTransactionList()), MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @POST
    @Path("/post-transaction")
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postTransaction(TransactionPOJO transactionPOJO) {
        try {
            wallet.createTransaction(transactionPOJO.getRecipient(), transactionPOJO.getAmount());
            return Response.ok(transactionPool.getTransactionList(), MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @GET
    @Path("/mine-transactions")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response mineTransactions() {
        try {
            miner.mine();
            return Response.ok(StringUtils.toJson(blockchain.getBlockList()), MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }
}
