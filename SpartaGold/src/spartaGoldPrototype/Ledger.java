package spartaGoldPrototype;

import java.io.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;

import javax.xml.bind.DatatypeConverter;

public class Ledger {

	private HashMap<String, Double> userMap;
	private File ledger; // not sure if this is needed yet
	private String currentBlock;
	private File tempPubKey;
	private String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
	private boolean chainFoundTrigger = false;
	private String sender;
	private String receiver;
	private String verifier;
	private double amount;
	private int chainLength;
	
	public Ledger(String sender, double amount, String receiver, String verifier) {
		//TODO: initialize sender, receiver, verifier, amount with actual values from parameters (meaning update parameters too)
		//TODO: change "ledger.txt" to "ledger.dat" near end of production
		//this.ledger = ledger;
		this.sender = sender;
		this.amount = amount;
		this.receiver = receiver;
		this.verifier = verifier;
		chainLength = 0;
		userMap = new HashMap<String, Double>();
		loadUserMap();
	}
	
	public void verifyBlock(String currentBlockString) throws Exception {
		currentBlock = currentBlockString;
		//tempPubKey = publicKeyFile;
		
		
		//TEMP ZONE
		
		userMap.put(sender, 1000.00);
		System.out.println("sender " + sender + " added to userMap with amount " + userMap.get(sender));
		userMap.put(receiver, 1000.00);
		System.out.println("receiver " + receiver + " added to userMap with amount " + userMap.get(receiver));
		userMap.put(verifier, 1000.00);
		System.out.println("verifier " + verifier + " added to userMap with amount " + userMap.get(verifier));
		
		
		//END TEMP ZONE
		
		
		/**
		//Verify that currentBlockString is signed by sender
		FileInputStream keyfis = new FileInputStream(tempPubKey);
        byte[] encKey = new byte[keyfis.available()];  
        keyfis.read(encKey);

        keyfis.close();

        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(encKey);

        KeyFactory keyFactory = KeyFactory.getInstance("DSA", "SUN");
        PublicKey pubKey = keyFactory.generatePublic(pubKeySpec);
        
        //... TODO: finish verification code
         * 
         */
        
        //if statements to check for existence of users
        if (sender != "" && amount != 0 && receiver != "") {
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
            		String randString = buffer.toString() + currentBlock + readLastBlock();
            		System.out.println(randString);
            		
            		//SHA256 hash on randString
            		MessageDigest md = MessageDigest.getInstance("SHA-256");
            		md.update(randString.getBytes("UTF-8"));
            		byte[] digest = md.digest();
            		System.out.println("SHA-256 hash complete.");
            		
            		//convert randString to readable hashedString, truncated to first three positions
            		String hashedString = DatatypeConverter.printHexBinary(digest);
            		String trunc = hashedString.substring(0, 1);
            		System.out.println("hashed string: " + hashedString);
            		System.out.println("truncation: " + trunc);
            		
            		//check to see if first three positions are zeroes
            		//writes hashedString to bottom of ledger, flips trigger
            		if (trunc.toLowerCase().contains("0")) {
            			BufferedWriter out = new BufferedWriter(new FileWriter("ledger.txt", true));
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
		      FileOutputStream saveFile = new FileOutputStream("userMap.dat");
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
	         FileInputStream fileIn = new FileInputStream("userMap.dat");
	         ObjectInputStream in = new ObjectInputStream(fileIn);
	         userMap = (HashMap<String, Double>) in.readObject();
	         in.close();
	         fileIn.close();
	         System.out.println("userMap.dat loaded.");
	      } catch (IOException i) {
	         i.printStackTrace();
	      } catch (ClassNotFoundException c) {
	         System.out.println("HashMap class not found");
	         c.printStackTrace();
	      };
	}
	
	public String readLastBlock() throws Exception {
		String sCurrentLine;
		String lastLine = "";
	    BufferedReader br = new BufferedReader(new FileReader("ledger.txt"));
	    while ((sCurrentLine = br.readLine()) != null) {
	        lastLine = sCurrentLine;
	    }
	    br.close();
	    System.out.println("last block: " + lastLine);
		return lastLine;
	}
	
	public int getChainLength() throws Exception {
		InputStream is = new BufferedInputStream(new FileInputStream("ledger.txt"));
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
	
	public double getBalance(String user) {
		return userMap.get(user);
	}
}
