import java.util.HashMap;
import java.util.HashSet;

public class Test {

	public static void main(String[] args) {
		
		HashMap<Integer, HashSet<Integer>> h = new HashMap<Integer, HashSet<Integer>>();
		
		BlockChain.addToMultiMap(1, 2, h);
		BlockChain.addToMultiMap(1, 3, h);
		
		for (Integer i: h.get(1)) {
			System.out.println(i);
		}
		
		byte ar[] = new byte[32];
		PRGen pr = new PRGen(ar);
		RSAKeyPair rk = new RSAKeyPair(pr, 3);
		RSAKeyPair rk2 = new RSAKeyPair(pr,5);
		Block genBlock = new Block(null, rk.getPublicKey());
		BlockChain bc = new BlockChain(genBlock);
		
		Transaction tx = new Transaction();
		tx.addInput(genBlock.getHash(), 0);
		tx.addOutput(25, rk2.getPublicKey());
		
		Block block2 = new Block(genBlock.getHash(), rk.getPublicKey());
		block2.addTransaction(tx);
		bc.addBlock(block2);
		
		
		
	}
	
}
