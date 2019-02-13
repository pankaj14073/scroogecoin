public class BlockHandler {
   private BlockChain blockChain;

   // assume blockChain has the genesis block
   public BlockHandler(BlockChain bChain) {
      blockChain = bChain;
   }

   // add the block to the blockchain if it is valid and return true
   // else return false
   public boolean processBlock(Block block) {
      if (block == null)
         return false;
      return blockChain.addBlock(block);
   }

   // create a new block over the max height block
   public Block createBlock(RSAKey myAddress) {
      Block parent = blockChain.getMaxHeightBlock();
      byte[] parentHash = parent.getHash();
      Block current = new Block(parentHash, myAddress);
      UTXOPool uPool = blockChain.getMaxHeightUTXOPool();
      TransactionPool txPool = blockChain.getTransactionPool();
      TxHandler handler = new TxHandler(uPool);
      Transaction[] txs = txPool.getTransactions().toArray(new Transaction[0]);
      Transaction[] rTxs = handler.handleTxs(txs);
      for (int i = 0; i < rTxs.length; i++)
         current.addTransaction(rTxs[i]);
      current.finalize();
      if (blockChain.addBlock(current))
         return current;
      else
         return null;
   }

   // process a transaction
   public void processTx(Transaction tx) {
      blockChain.addTransaction(tx);
   }
   
   public static void main(String[] args) {
	   // Build Genesis Block
	   byte zeros[] = new byte[32];
	   
	   RSAKeyPair genKey = new RSAKeyPair(new PRGen(zeros), 2048);
	   Block genesisBlock = new Block(zeros, genKey.getPublicKey());
	   genesisBlock.finalize();
	   BlockChain chain = new BlockChain(genesisBlock);
	   BlockHandler bHandler = new BlockHandler(chain);
	   
	   Transaction tx = new Transaction();
	   tx.addInput(genesisBlock.getCoinbase().getHash(), 0);
	   tx.addOutput(5, genKey.getPublicKey());
	   tx.addSignature(genKey.getPrivateKey().sign(tx.getRawDataToSign(0)), 0);
	   
	   Block validBlock = new Block(genesisBlock.getHash(), genKey.getPublicKey());
	   validBlock.addTransaction(tx);
	   
	   validBlock.finalize();
	   
	   if(bHandler.processBlock(validBlock)) System.out.println("Yay!");
	   else System.out.println("Boo!");
   }
}
