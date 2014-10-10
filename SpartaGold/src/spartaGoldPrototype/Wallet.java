package spartaGoldPrototype;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Font;

import net.tomp2p.connection.Bindings;
import net.tomp2p.futures.FutureDHT;
import net.tomp2p.futures.FutureBootstrap; 
import net.tomp2p.futures.FutureDiscover;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerMaker;
import net.tomp2p.peers.Number160;
import net.tomp2p.storage.Data;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetAddress;

public class Wallet {
    
	private JFrame frmSpartagoldWallet;
	private JTextField tfAmount;
	private JTextField tfAddress;
	
	final private Peer peer1;
	private Number160 hashedID;
	
	private String myAddress;
	private String verifier;
	
	/**
	 * Launch the application.
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					//check system for usersht.dat
					//ask for student id in new jframe
					//if student id not found in usersht.dat, add  to usersht.dat with 0 balance
					//add user and ip to tracker.dat
					//broadcast new usersht.dat, tracker.dat, close jframe
					Wallet window = new Wallet();
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
	public Wallet() throws Exception {
		//input way to rotate ip to listen to
		myAddress = "0x7e3f0498c6033fe420d11a07b452e38f0d30f419";
		verifier = "3b55b765725f874ac5421250a71175623ee325f9";
		Bindings b1 = new Bindings(Bindings.Protocol.IPv4, InetAddress.getByName("24.130.213.13 "), 4005, 4005);
		System.out.println("Binding 1 created.");
		//fetch user's id from first jframe
		hashedID = Number160.createHash(6646679);
		System.out.println("hashedID: " + hashedID);
		peer1 = new PeerMaker(hashedID).setPorts(4005).setBindings(b1).makeAndListen();
		System.out.println("Peer created, listening.");
		peer1.getConfiguration().setBehindFirewall(true);
		
		InetAddress address = Inet4Address.getByName("192.168.1.20");
		FutureDiscover futureDiscover = peer1.discover().setInetAddress( address ).setPorts( 4000 ).start();
		futureDiscover.awaitUninterruptibly();
        FutureBootstrap futureBootstrap = peer1.bootstrap().setInetAddress( address ).setPorts( 4000 ).start();
        futureBootstrap.awaitUninterruptibly();
        if (futureBootstrap.getBootstrapTo() != null) {
            peer1.discover().setPeerAddress(futureBootstrap.getBootstrapTo().iterator().next()).start().awaitUninterruptibly();
        }
        
		
		frmSpartagoldWallet = new JFrame();
		frmSpartagoldWallet.setTitle("SpartaGold Wallet");
		frmSpartagoldWallet.setResizable(false);
		frmSpartagoldWallet.setBounds(100, 100, 613, 265);
		frmSpartagoldWallet.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frmSpartagoldWallet.getContentPane().add(tabbedPane, BorderLayout.CENTER);
		
		JPanel Send = new JPanel();
		tabbedPane.addTab("Send", null, Send, null);
		Send.setLayout(null);
		
		JLabel lblAmount = new JLabel("Amount:");
		lblAmount.setBounds(10, 60, 58, 26);
		Send.add(lblAmount);
		
		tfAmount = new JTextField();
		tfAmount.setBounds(78, 63, 100, 20);
		Send.add(tfAmount);
		tfAmount.setColumns(10);
		
		JLabel lblSG1 = new JLabel("SG");
		lblSG1.setBounds(188, 60, 20, 26);
		Send.add(lblSG1);
		
		JLabel lblAddress = new JLabel("Address:");
		lblAddress.setBounds(10, 23, 43, 26);
		Send.add(lblAddress);
		
		tfAddress = new JTextField();
		tfAddress.setBounds(78, 26, 320, 20);
		Send.add(tfAddress);
		tfAddress.setColumns(10);
		
		JLabel lblWalletAmount = new JLabel("0.0000");
		lblWalletAmount.setFont(new Font("Tahoma", Font.PLAIN, 27));
		lblWalletAmount.setBounds(464, 41, 92, 39);
		Send.add(lblWalletAmount);
		
		JLabel lblSG2 = new JLabel("SG");
		lblSG2.setFont(new Font("Tahoma", Font.PLAIN, 27));
		lblSG2.setBounds(558, 41, 44, 39);
		Send.add(lblSG2);
		
		JLabel lblBalance = new JLabel("Balance:");
		lblBalance.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblBalance.setBounds(495, 11, 65, 26);
		Send.add(lblBalance);
		
		JPanel Transactions = new JPanel();
		tabbedPane.addTab("Transactions", null, Transactions, null);
		
		JMenuBar menuBar = new JMenuBar();
		frmSpartagoldWallet.setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmSignMessage = new JMenuItem("Sign message");
		mnFile.add(mntmSignMessage);
		
		JMenuItem mntmVerifyMessage = new JMenuItem("Verify Message");
		mnFile.add(mntmVerifyMessage);
		
		JButton btnSend = new JButton("Send");
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (tfAmount.getText() != "" && tfAddress.getText()!= ""){
					
					//broadcast myAddress, tfAmount, tfAddress, verifierAddress, public key
					//TEMP ZONE
					double d = Double.parseDouble(tfAmount.getText());
					Ledger l = new Ledger(myAddress, d, tfAddress.getText(), verifier);
					try {
						l.verifyBlock(myAddress + ":" + tfAmount.getText() + ":" + tfAddress.getText());
						lblWalletAmount.setText("" + l.getBalance(myAddress));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
					//END TEMP ZONE
				}
				tfAmount.setText("");
				tfAddress.setText("");
			}
		});
		btnSend.setBounds(298, 60, 100, 26);
		Send.add(btnSend);
		
		
		
	}
	
	private String get(String peerID) throws ClassNotFoundException, IOException {
        FutureDHT futureDHT = peer1.get(Number160.createHash(peerID)).start();
        futureDHT.awaitUninterruptibly();
        if (futureDHT.isSuccess()) {
            return futureDHT.getData().getObject().toString();
        }
        return "not found";
    }

    private void store(String name, String ip) throws IOException {
        peer1.put(Number160.createHash(name)).setData(new Data(ip)).start().awaitUninterruptibly();
    }
}
