package blockchain.block;

import blockchain.transaction.Transaction;
import consensus.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class Blockchain {
    private List<Block> blockList = new ArrayList<>();

    public Blockchain() {
        this.blockList.add(Block.genesis());
    }

    public int getLength() {
        return blockList.size();
    }

    public List<Block> getBlockList() {
        return blockList;
    }

    public Block addTransactions(List<Transaction> transactions) {
        Block lastBlock = this.blockList.get(this.blockList.size() - 1);
        Block newBlock = Block.mineBlock(lastBlock, transactions);
        this.blockList.add(newBlock);
        return newBlock;
    }

    public boolean isValidChain() {
        Block firstBlock = blockList.get(0);
        if (!StringUtils.toJson(firstBlock).equals(StringUtils.toJson(Block.genesis()))) {
            return false;
        }

        for (int i = 1; i < this.blockList.size(); i++) {
            Block block = this.blockList.get(i);
            Block lastBlock = this.blockList.get(i - 1);
            if (!block.getLastHash().equals(lastBlock.getBlockHash())) {
                return false;
            }

            if (!block.getBlockHash().equals(Block.blockHash(block))) {
                return false;
            }
        }

        return true;
    }

    public void replaceListOfBlocks(Blockchain newChain) {
        // Eleanor: I'm pretty sure this was the wrong way around. It used to be "if not this.len <= new.len", which
        // doesn't make sense.
        if (this.getLength() < newChain.getLength() && newChain.isValidChain()) {
            this.blockList = newChain.getBlockList();
        }
    }
}
