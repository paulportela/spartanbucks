package spartaGoldPrototype;

import java.awt.*;

import javax.swing.*;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Scanner;
import java.util.List;

import peerbase.*;


public class WalletGUI {
    
	private JFrame frmSpartagoldWallet;
	private JTextField tfAmount;
	private JTextField tfAddress;
	
	private static String myAddress;
	private static String myIpAddress;
	private double myBalance;
	private JTable table;
	private Object[][] previousTransactions = {};
	private String[] transactionColumns = {"Date", "Address", "Amount"};
	private SpartaGoldNode peer;
	private static HashMap<String, Double> userMap;
	
	/**
	 * Launch the application.
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					myIpAddress = getIpAddress();
					myAddress = encryptIpAddress(myIpAddress);
					System.out.println(myIpAddress + ", " + myAddress);
					userMap = new HashMap<String, Double>();
					loadUserMap();
					System.out.println("User map loaded");
					WalletGUI window = new WalletGUI("localhost", 9001, 5, new PeerInfo(myIpAddress, 9000));
					window.frmSpartagoldWallet.setVisible(true);
			        
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 * @throws Exception 
	 */
	public WalletGUI(String initialhost, int initialport, int maxpeers, PeerInfo mypd) throws Exception {
		
		peer = new SpartaGoldNode(maxpeers, mypd);
		peer.buildPeers(initialhost, initialport, 2);
		(new Thread() { public void run() { peer.mainLoop(); }}).start();
		
		myBalance = getBalance(myAddress);
		System.out.println("Balance: " + myBalance);
		saveUserMap(userMap);
		
		List<PeerMessage> fileData = null;
		Scanner in = new Scanner(System.in);
		String s = in.next();
		if(s.equals("get"))
		{
			for (String pid : peer.getPeerKeys()) 
			{
				fileData = peer.sendToPeer(pid, SpartaGoldNode.FILEGET, "message.zip", true);
			}
			System.out.println(fileData);
		}
		in.close();
        
		frmSpartagoldWallet = new JFrame();
		frmSpartagoldWallet.setBackground(new Color(11, 46, 70));
		frmSpartagoldWallet.getContentPane().setBackground(new Color(11, 46, 70));
		frmSpartagoldWallet.setTitle("SpartaGold Wallet");
		frmSpartagoldWallet.setResizable(false);
		frmSpartagoldWallet.setBounds(100, 100, 625, 350);
		frmSpartagoldWallet.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//frmSpartagoldWallet.setUndecorated(true);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setForeground(new Color(11, 46, 70));
		tabbedPane.setBackground(new Color(11, 46, 70));
		frmSpartagoldWallet.getContentPane().add(tabbedPane, BorderLayout.CENTER);
		
		JPanel send = new JPanel();
		send.setBackground(new Color(11, 46, 70));
		tabbedPane.addTab("Send", null, send, null);
		send.setLayout(null);
		
		JLabel background = new JLabel(new ImageIcon("screen-6.jpg"));
		send.add(background);
		background.setLayout(new FlowLayout());
		
		JPanel transactions = new JPanel();
		transactions.setBackground(new Color(11, 46, 70));
		tabbedPane.addTab("Transactions", null, transactions, null);
		transactions.setLayout(null);
		
		table = new JTable(previousTransactions, transactionColumns);
		table.setShowHorizontalLines(false);
		
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setBounds(5, 5, 605, 250);
		table.setFillsViewportHeight(true);
		transactions.add(scrollPane);
		
		JPanel mine = new JPanel();
		mine.setBackground(new Color(11, 46, 70));
		tabbedPane.addTab("Mine", null, mine, null);
		mine.setLayout(null);
		
		JButton btnMine = new JButton("Mine for Gold");
		btnMine.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				//TODO:
				//listens for message.zip
				//ProofOfWork p = new ProofOfWork(get sender, amount, receiver from transaction file)
				//p.verifyBlock(get pubkey file, sig file, and transaction file)
			}
		});
		btnMine.setBounds(215, 162, 154, 44);
		mine.add(btnMine);
		
		JPanel panel = new JPanel();
		panel.setBackground(UIManager.getColor("activeCaption"));
		panel.setBounds(20, 23, 567, 136);
		send.add(panel);
		panel.setLayout(null);
		
		JLabel lblWalletAmount = new JLabel(Double.toString(myBalance));
		lblWalletAmount.setBounds(421, 66, 92, 39);
		panel.add(lblWalletAmount);
		lblWalletAmount.setForeground(new Color(234, 230, 118));
		lblWalletAmount.setFont(new Font("Segoe UI Light", Font.BOLD, 27));
		
		JLabel lblSG2 = new JLabel("SG");
		lblSG2.setBounds(523, 66, 44, 39);
		panel.add(lblSG2);
		lblSG2.setForeground(new Color(234, 230, 118));
		lblSG2.setFont(new Font("Segoe UI Semibold", Font.BOLD, 27));
		
		JLabel lblBalance = new JLabel("Balance:");
		lblBalance.setBounds(445, 29, 92, 26);
		panel.add(lblBalance);
		lblBalance.setForeground(Color.BLACK);
		lblBalance.setFont(new Font("Segoe UI Semibold", Font.BOLD, 20));
		
		JButton btnSend = new JButton("Send");
		btnSend.setBounds(292, 80, 100, 26);
		panel.add(btnSend);
		
		tfAmount = new JTextField();
		tfAmount.setBounds(78, 83, 100, 20);
		panel.add(tfAmount);
		tfAmount.setColumns(10);
		
		JLabel lblAmount = new JLabel("Amount:");
		lblAmount.setBounds(10, 80, 58, 26);
		panel.add(lblAmount);
		lblAmount.setForeground(Color.BLACK);
		
		JLabel lblAddress = new JLabel("Address:");
		lblAddress.setBounds(10, 27, 43, 26);
		panel.add(lblAddress);
		lblAddress.setForeground(Color.BLACK);
		
		JLabel lblSG1 = new JLabel("SG");
		lblSG1.setBounds(188, 80, 20, 26);
		panel.add(lblSG1);
		lblSG1.setForeground(Color.BLACK);
		
		tfAddress = new JTextField();
		tfAddress.setBounds(72, 30, 320, 20);
		panel.add(tfAddress);
		tfAddress.setColumns(10);
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (tfAmount.getText() != "" && tfAddress.getText()!= ""){
					
					//TEMP ZONE
					
					System.out.println("Sending " + tfAmount.getText() + " SG...");
					double d = Double.parseDouble(tfAmount.getText());
					String transactionString = myAddress + ":" + d + ":" + "3b55b765725f874ac5421250a71175623ee325f9";
					System.out.println(transactionString);
					saveTransaction(transactionString);
					new GenSig();
					System.out.println("Transaction file, signature file, and public key file created.");
					new ZipFiles();
					lblWalletAmount.setText(Double.toString(getBalance(myAddress)));
					//TODO: broadcast message.zip
					
					
					//END TEMP ZONE
					
					
				}
				tfAmount.setText("");
				tfAddress.setText("");
			}
		});
	}
	
	public static String getIpAddress() { 
        URL myIP;
        try {
            myIP = new URL("http://myip.dnsomatic.com/");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(myIP.openStream())
                    );
            return in.readLine();
        } catch (Exception e) 
        {
            try 
            {
                myIP = new URL("http://api.externalip.net/ip/");

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(myIP.openStream())
                        );
                return in.readLine();
            } catch (Exception e1) 
            {
                try {
                    myIP = new URL("http://icanhazip.com/");

                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(myIP.openStream())
                            );
                    myIpAddress = in.readLine();
                    return in.readLine();
                } catch (Exception e2) {
                    e2.printStackTrace(); 
                }
            }
        }

	    return null;
	}
	
	private static String encryptIpAddress(String ip){
		String sha1 = "";
	    try
	    {
	        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
	        crypt.reset();
	        crypt.update(ip.getBytes("UTF-8"));
	        sha1 = byteToHex(crypt.digest());
	    }
	    catch(NoSuchAlgorithmException e)
	    {
	        e.printStackTrace();
	    }
	    catch(UnsupportedEncodingException e)
	    {
	        e.printStackTrace();
	    }
	    return sha1;
	}

	private static String byteToHex(final byte[] hash){
	    Formatter formatter = new Formatter();
	    for (byte b : hash)
	    {
	        formatter.format("%02x", b);
	    }
	    String result = formatter.toString();
	    formatter.close();
	    return result;
	}
	
	public void saveTransaction(Serializable object) {
		//saves transaction locally
		try {
		      FileOutputStream saveFile = new FileOutputStream("transaction");
		      ObjectOutputStream out = new ObjectOutputStream(saveFile);
		      out.writeObject(object);
		      out.close();
		      saveFile.close();
		    } catch (IOException e) {
		      e.printStackTrace();
		    }
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
	public static void loadUserMap() {
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
}


