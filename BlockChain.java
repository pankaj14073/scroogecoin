import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/* Block Chain should maintain only limited block nodes to satisfy the functions
   You should not have the all the blocks added to the block chain in memory 
   as it would overflow memory
 */

public class BlockChain {

	public static final int CUT_OFF_AGE = 10;

	private HashMap<Integer, HashSet<BlockNode>> nodesAtHeight;
	private HashMap<byte[], BlockNode> allRecentNodes;
	private BlockNode highestNode;
	private int maxHeight;

	private TransactionPool tPool;

	public static <A,B> void addToMultiMap(A x, B y, HashMap<A, HashSet<B>> h) {
		HashSet<B> s = h.get(x);
		if (s== null) {
			s = new HashSet<B>();
		}
		s.add(y);
		h.put(x,s);
		//does this mutate h?
	}

	// all information required in handling a block in block chain
	private class BlockNode {
		public Block b;
		public int parentHash;
		public ArrayList<BlockNode> children;
		public int height;
		// utxo pool for making a new block on top of this block
		private UTXOPool uPool;

		public BlockNode(Block b, BlockNode parent, UTXOPool uPool) {
			this.b = b;
			children = new ArrayList<BlockNode>();
			this.uPool = uPool;
			if (parent != null) {
				this.parentHash = parent.hashCode();
				height = parent.height + 1;
				parent.children.add(this);
			} else {
				this.parentHash = 0;
				height = 1;
			}
		}

		public UTXOPool getUTXOPoolCopy() {
			return new UTXOPool(uPool);
		}
	}

	/* create an empty block chain with just a genesis block.
	 * Assume genesis block is a valid block
	 */
	public BlockChain(Block genesisBlock) {
		ArrayList<Transaction> txs = genesisBlock.getTransactions();
		UTXOPool uPool = new UTXOPool();

		txs.add(genesisBlock.getCoinbase());

		int index = 0;

		for(Transaction tx : txs) {
			index = 0;
			for(Transaction.Output output : tx.getOutputs()) {
				uPool.addUTXO(new UTXO(tx.getHash(), index), output);
				index++;
			}
		}

		BlockNode genBlockNode = new BlockNode(genesisBlock, null, uPool);

		this.nodesAtHeight = new HashMap<Integer, HashSet<BlockNode>>();
		addToMultiMap(1,genBlockNode,nodesAtHeight);
		this.maxHeight = 1;
		this.allRecentNodes = new HashMap<byte[], BlockNode>();
		this.allRecentNodes.put(genesisBlock.getHash(), genBlockNode);

		this.highestNode = genBlockNode;
		this.tPool = new TransactionPool();
		//      
		//      this.nodesAtHeight = new HashMap<Integer, HashSet<BlockNode>>();
		//      this.nodesAtHeight.put(0, new HashSet<BlockNode>());
		//      this.nodesAtHeight.get(0).add(genBlockNode);
		//      this.maxHeight = 0;
		//      this.allRecentNodes = new HashMap<byte[], BlockNode>();
		//      this.allRecentNodes.put(genesisBlock.getHash(), genBlockNode);
		//      this.highestNode = genBlockNode;
		//      this.tPool = new TransactionPool();
	}

	/* Get the maximum height block
	 */
	public Block getMaxHeightBlock() {
		//      for(BlockNode bn : this.nodesAtHeight.get(this.maxHeight)) {
		//    	  return bn.b;
		//      }
		//      return null;
		return highestNode.b;
	}

	/* Get the UTXOPool for mining a new block on top of 
	 * max height block
	 */
	public UTXOPool getMaxHeightUTXOPool() {
		//	   for(BlockNode bn : this.nodesAtHeight.get(this.maxHeight)) {
		//	    	  return bn.getUTXOPoolCopy();
		//	   }
		//	   return null;
		return highestNode.uPool;
	}

	/* Get the transaction pool to mine a new block
	 */
	public TransactionPool getTransactionPool() {
		return tPool;
	}

	private void removeAtHeight(int h) {
		if (h<=0) {
			return;
		}
		HashSet<BlockNode> bns = nodesAtHeight.get(h);
		if (bns != null) {
			for (BlockNode bn : bns) {
				allRecentNodes.remove(bn.b.getHash());
			}
		}
		nodesAtHeight.remove(h);
	}

	/* Add a block to block chain if it is valid.
	 * For validity, all transactions should be valid
	 * and block should be at height > (maxHeight - CUT_OFF_AGE).
	 * For example, you can try creating a new block over genesis block 
	 * (block height 2) if blockChain height is <= CUT_OFF_AGE + 1. 
	 * As soon as height > CUT_OFF_AGE + 1, you cannot create a new block at height 2.
	 * Return true of block is successfully added
	 */
	public boolean addBlock(Block b) {
		byte[] parentHash = b.getPrevBlockHash();
		if (parentHash == null) {
			return false;
		}
		BlockNode parentNode = allRecentNodes.get(parentHash);
		if (parentNode == null) {
			return false;
		}
		TxHandler txh = new TxHandler(parentNode.getUTXOPoolCopy());
		int myHeight = parentNode.height + 1;
		if (myHeight <= maxHeight - CUT_OFF_AGE) {
			return false;
		}
		//	   for (Transaction tx : b.getTransactions()) {
		//		   if (!txh.isValidTx(tx)) {
		//			   return false;
		//		   }		   
		//	   }

		ArrayList<Transaction> btxArray = b.getTransactions();
		Transaction[] btxs = new Transaction[btxArray.size()];
		btxs = btxArray.toArray(btxs);
		
		for (int i=0;i<btxArray.size();i++) {
			btxs[i].finalize(); //This is needed!
			//System.out.println(btxs[i].toString());
		}
		
		Transaction[] validBtxs = txh.handleTxs(btxs);
		
		UTXOPool up = txh.getUTXOPool();
		if (validBtxs.length < btxs.length) {
			return false;
		}
		up.addUTXO(new UTXO(b.getCoinbase().getHash(), 0), b.getCoinbase().getOutput(0));
		BlockNode bn = new BlockNode(b, parentNode, up);
		addToMultiMap(myHeight, bn, nodesAtHeight);
		allRecentNodes.put(b.getHash(), bn);
		if (myHeight > maxHeight) {
			maxHeight++;
			removeAtHeight(maxHeight - CUT_OFF_AGE - 1);//off by 1?
			highestNode = bn;
		} 
		return true;
	}

	/* Add a transaction in transaction pool
	 */
	public void addTransaction(Transaction tx) {
		tPool.addTransaction(tx);
	}

	public HashMap<Integer, HashSet<BlockNode>> getNodesAtHeight() {
		return nodesAtHeight;
	}

	public HashMap<byte[], BlockNode> getAllRecentNodes() {
		return allRecentNodes;
	}

	public BlockNode getHighestNode() {
		return highestNode;
	}

	public int getMaxHeight() {
		return maxHeight;
	}
}
