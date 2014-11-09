package spartaGoldPrototype;

import java.io.*;
import java.security.*;
import java.util.HashMap;

import javax.xml.bind.DatatypeConverter;

public class ProofOfWork {

	private HashMap<String, Double> userMap;
	private File currentBlock;
	private File pubKey;
	private File sig;
	private String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
	private boolean chainFoundTrigger = false;
	private String sender;
	private String receiver;
	private String verifier;
	private double amount;
	private int chainLength;
	private boolean verified = false;
	
	public ProofOfWork(String sender, double amount, String receiver) {
		//TODO: change "ledger.txt" to "ledger.dat" near end of production
		this.sender = sender;
		this.amount = amount;
		this.receiver = receiver;
		//this.verifier = verifier;
		chainLength = 0;
		userMap = new HashMap<String, Double>();
		loadUserMap();
	}
	
	public void verifyBlock(File pubKeyFile, File sigFile, File currentBlockFile) throws Exception {
		pubKey = pubKeyFile;
		sig = sigFile;
		currentBlock = currentBlockFile;
		
        
		VerSig verifySig = new VerSig(pubKey, sig, currentBlock);
		if (verifySig.isVerified()) verified = true;
		
		
        //if statements to check for existence of users
        if (verified && sender != "" && amount != 0 && receiver != "") {
        	if (!userMap.containsKey(sender)) {
        		userMap.put(sender, 0.00);
        		System.out.println("sender " + sender + "added.\n");
        	}
        	if (!userMap.containsKey(receiver)) {
        		userMap.put(receiver, 0.00);
        		System.out.println("receiver " + receiver + "added.\n");
        	}
        	if (!userMap.containsKey(verifier)) {
        		userMap.put(verifier, 0.00);
        		System.out.println("verifier " + verifier + "added.\n");
        	}
        	if (userMap.get(sender) >= amount) {
        		while (!chainFoundTrigger) {
        			
        			//generate random string from random length between 1 and 10
        			int randomNum = 1 + (int)(Math.random()*10); 
            		StringBuffer buffer = new StringBuffer();
            		int charactersLength = characters.length();
            		for (int i = 0; i < randomNum; i++) {
            			double index = Math.random() * charactersLength;
            			buffer.append(characters.charAt((int) index));
            		}
            		
            		//concatenate random string with sender, amount, receiver, then last block on ledger
            		String randString = buffer.toString() + this.sender + this.amount + this.receiver + readLastBlock();
            		System.out.println(randString);
            		
            		//SHA256 hash on randString
            		MessageDigest md = MessageDigest.getInstance("SHA-256");
            		md.update(randString.getBytes("UTF-8"));
            		byte[] digest = md.digest();
            		System.out.println("SHA-256 hash complete.");
            		
            		//convert randString to readable hashedString, truncated to first three positions
            		String hashedString = DatatypeConverter.printHexBinary(digest);
            		String trunc = hashedString.substring(0, 3);
            		System.out.println("hashed string: " + hashedString);
            		System.out.println("truncation: " + trunc);
            		
            		//check to see if first three positions are zeroes
            		//writes hashedString to bottom of ledger, flips trigger
            		if (trunc.toLowerCase().contains("000")) {
            			BufferedWriter out = new BufferedWriter(new FileWriter("ledger", true));
    		            out.append(hashedString + "\r\n");
    		            out.close();
    		            chainFoundTrigger = true;
    		            System.out.println("chain found, ledger updated. Check ledger.txt for confirmation.");
            		}
        		}
        	}
        	
        	//true if three zeroes are found
        	if (chainFoundTrigger) {
        		//TODO: verifier broadcasts new ledger to everyone
        		//TODO: listen to port if winner or loser
        		updateBalances();
        		
        		System.out.println("chain length: " + getChainLength());
        	}
        	
        	saveUserMap(userMap);
        }
		
	}
	
	public void updateBalances() {
		double newReceiverTotal = userMap.get(receiver) + amount;
		userMap.put(receiver, newReceiverTotal);
		System.out.println("receiver: " + receiver + ", amount: " + userMap.get(receiver));
		double newSenderTotal = userMap.get(sender) - amount;
		userMap.put(sender, newSenderTotal);
		System.out.println("sender: " + sender + ", amount: " + userMap.get(sender));
		double verifierRewardTotal = userMap.get(verifier) + 1;
		userMap.put(verifier, verifierRewardTotal);
		System.out.println("verifier: " + verifier + ", amount: " + userMap.get(verifier));
	}
	
	public void saveUserMap(Serializable object) {
		//saves userMap locally
		try {
		      FileOutputStream saveFile = new FileOutputStream("userMap");
		      ObjectOutputStream out = new ObjectOutputStream(saveFile);
		      out.writeObject(object);
		      out.close();
		      saveFile.close();
		    } catch (IOException e) {
		      e.printStackTrace();
		    }
	}
	
	@SuppressWarnings("unchecked")
	public void loadUserMap() {
		//loads userMap
		try {
	         FileInputStream fileIn = new FileInputStream("userMap");
	         ObjectInputStream in = new ObjectInputStream(fileIn);
	         userMap = (HashMap<String, Double>) in.readObject();
	         in.close();
	         fileIn.close();
	         System.out.println("userMap loaded.");
	      } catch (IOException i) {
	         i.printStackTrace();
	      } catch (ClassNotFoundException c) {
	         System.out.println("HashMap class not found");
	         c.printStackTrace();
	      };
	}
	
	public double getBalance(String user) {
		return userMap.get(user);
	}
	
	public String readLastBlock() throws Exception {
		String sCurrentLine;
		String lastLine = "";
	    BufferedReader br = new BufferedReader(new FileReader("ledger"));
	    while ((sCurrentLine = br.readLine()) != null) {
	        lastLine = sCurrentLine;
	    }
	    br.close();
	    System.out.println("last block: " + lastLine);
		return lastLine;
	}
	
	public int getChainLength() throws Exception {
		InputStream is = new BufferedInputStream(new FileInputStream("ledger"));
	    try {
	        byte[] c = new byte[1024];
	        int readChars = 0;
	        boolean empty = true;
	        while ((readChars = is.read(c)) != -1) {
	            empty = false;
	            for (int i = 0; i < readChars; ++i) {
	                if (c[i] == '\n') {
	                    ++chainLength;
	                }
	            }
	        }
	        return (chainLength == 0 && !empty) ? 1 : chainLength;
	    } finally {
	        is.close();
	    }
	}

}
