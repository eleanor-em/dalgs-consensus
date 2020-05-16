package blockchain.block;

import blockchain.transaction.Transaction;
import consensus.crypto.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class BlockChain {
    private List<Block> blockList = new ArrayList<>();

    public BlockChain() {
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
            if (!block.getLastHash().equals(lastBlock.getHashValue())) {
                return false;
            }

            if (!block.getHashValue().equals(Block.blockHash(block))) {
                return false;
            }
        }

        return true;
    }

    public void replaceChain(BlockChain newChain) {
        if (this.getLength() <= newChain.getLength()) {
            return;
        }

        if (newChain.isValidChain()) {
            return;
        }

        this.blockList = newChain.getBlockList();
    }
}