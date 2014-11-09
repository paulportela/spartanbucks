package spartaGoldPrototype;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.List;


import peerbase.*;






public class SpartaGoldNode extends Node //implements Serializable
{
	//Message Types
	public static final String INSERTPEER = "JOIN";
	public static final String LISTPEER = "LIST";
	public static final String PEERNAME = "NAME";
	public static final String PEERQUIT = "QUIT";
	public static final String FOUNDSOLUTION = "HSOL";
	public static final String TRANSACTION = "TRAN";
	public static final String FILEGET = "FGET";
	
	public static final String REPLY = "REPL";
	public static final String ERROR = "ERRO";
	
	
	private Hashtable<String, String> transactions;
	private Hashtable<String, String> ledger;
	private Hashtable<String,String> files;
	
	//Zeroes required for proof-work solution
	private static final int NUMOFZEROES = 4;
	
	public SpartaGoldNode(int maxPeers, PeerInfo myInfo)
	{
		super(maxPeers, myInfo);
		transactions = new Hashtable<String,String>();
		ledger = new Hashtable<String, String>();
		files = new Hashtable<String, String>();
		
		this.addRouter(new Router(this));
		
		//Handlers
		this.addHandler(INSERTPEER, new JoinHandler(this));
		this.addHandler(PEERNAME, new NameHandler(this));
		this.addHandler(PEERQUIT, new QuitHandler(this));
		this.addHandler(LISTPEER, new ListHandler(this));
		this.addHandler(FOUNDSOLUTION, new SolutionFoundHandler(this));
		this.addHandler(TRANSACTION, new TransactionHandler(this));
		
		this.addHandler(FILEGET, new FileGetHandler(this));
		
	}
	
	/**
	 * This connects with a peer and then it requests the list of known of that peer.
	 * Then it connects with those peers doing a depth first search in that list.
	 * @param host the IP address of the peer
	 * @param port the port the peer is listening on
	 * @param hops the hops this peer will travel to build its peer list
	 */
	public void buildPeers(String host, int port, int hops) 
	{
		LoggerUtil.getLogger().fine("build peers");
		
		if (this.maxPeersReached() || hops <= 0)
			return;
		PeerInfo pd = new PeerInfo(host, port);
		List<PeerMessage> resplist = this.connectAndSend(pd, PEERNAME, "", true);
		if (resplist == null || resplist.size() == 0)
			return;
		String peerid = resplist.get(0).getMsgData();
		LoggerUtil.getLogger().fine("contacted " + peerid);
		pd.setId(peerid);
		
		String resp = this.connectAndSend(pd, INSERTPEER,String.format("%s %s %d", this.getId(), this.getHost(), this.getPort()), true).get(0).getMsgType();
		if (!resp.equals(REPLY) || this.getPeerKeys().contains(peerid))
			return;
		
		this.addPeer(pd);
		
		// do recursive depth first search to add more peers
		resplist = this.connectAndSend(pd, LISTPEER, "", true);
		
		if (resplist.size() > 1) 
		{
			resplist.remove(0);
			for (PeerMessage pm : resplist) 
			{
				String[] data = pm.getMsgData().split("\\s");
				String nextpid = data[0];
				String nexthost = data[1];
				int nextport = Integer.parseInt(data[2]);
				if (!nextpid.equals(this.getId()))
					buildPeers(nexthost, nextport, hops - 1);
			}
		}
	}
	
	/**
	 * Broadcast Message
	 */
	public void broadcastMessage(String messageType, String messageData, boolean waitForReply)
	{
		if(this.getAllKnownPeers().isEmpty())
		{
			//No peers to send to
			return;
		}
		else
		{
			for(PeerInfo pd: this.getAllKnownPeers())
			{
				this.connectAndSend(pd, messageData, messageData, waitForReply);
			}
		}
	}
	
	
	private class JoinHandler implements HandlerInterface
	{
		private Node peer;
		
		public JoinHandler(Node peer) 
		{ 
			this.peer = peer; 
		}
				
		public void handleMessage(PeerConnection peerconn, PeerMessage msg)
		{
			if (peer.maxPeersReached()) 
			{
				LoggerUtil.getLogger().fine("maxpeers reached " + peer.getMaxPeers());
				peerconn.sendData(new PeerMessage(ERROR, "Join: " + "too many peers"));
				return;
			}
			
			String[] data = msg.getMsgData().split("\\s");
			
			// parse arguments into PeerInfo structure
			PeerInfo info = new PeerInfo(data[0], data[1],Integer.parseInt(data[2]));
			
			if (peer.getPeer(info.getId()) != null) 
				peerconn.sendData(new PeerMessage(ERROR, "Join: " + "peer already inserted"));
			else 
				if (info.getId().equals(peer.getId())) 
				peerconn.sendData(new PeerMessage(ERROR, "Join: " + "attempt to insert self"));
			else 
			{
				peer.addPeer(info);
				peerconn.sendData(new PeerMessage(REPLY, "Join: " + "peer added: " + info.getId()));
			}
		}
	}
	
	/**
	 * Sends this node's peer information to the requester.
	 */
	private class NameHandler implements HandlerInterface 
	{
		private Node peer;
		
		public NameHandler(Node peer) 
		{ 
			this.peer = peer;
		}
		
		public void handleMessage(PeerConnection peerconn, PeerMessage msg)
		{
			peerconn.sendData(new PeerMessage(REPLY, peer.getId()));
		}
	}
	
	/**
	 * Sends this peer's list of peers. 
	 */
	private class ListHandler implements HandlerInterface 
	{
		private Node peer;
		
		public ListHandler(Node peer) 
		{ 
			this.peer = peer;
		}
		
		public void handleMessage(PeerConnection peerconn, PeerMessage msg) 
		{
			peerconn.sendData(new PeerMessage(REPLY, String.format("%d", peer.getNumberOfPeers())));
			for (String pid : peer.getPeerKeys()) 
			{
				peerconn.sendData(new PeerMessage(REPLY, 
						String.format("%s %s %d", pid, peer.getPeer(pid).getHost(), peer.getPeer(pid).getPort())));
			}
		}
	}
	
	
	private class Router implements RouterInterface 
	{
		private Node peer;
		
		public Router(Node peer) 
		{
			this.peer = peer;
		}
		
		public PeerInfo route(String peerid) 
		{
			if (peer.getPeerKeys().contains(peerid)) 
				return peer.getPeer(peerid);
			else 
				return null;
		}
	}
	
	/**
	 * Removes any peer from its peers list that have log off.
	 */
	private class QuitHandler implements HandlerInterface 
	{
		private Node peer;
		
		public QuitHandler(Node peer) 
		{ 
			this.peer = peer;
		}
		
		public void handleMessage(PeerConnection peerconn, PeerMessage msg) 
		{
			String pid = msg.getMsgData().trim();
			if (peer.getPeer(pid) == null) 
			{
				//Doesn't do anything
				return;
			} 
			else 
			{
				peer.removePeer(pid);
			}
		}
	}
	
	public static StringBuilder hash(String data) throws NoSuchAlgorithmException
	{
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(data.getBytes());
		byte[] bytes = md.digest();
		StringBuilder binary = new StringBuilder();
		for (byte b : bytes)
		{
		   int val = b;
		   for (int i = 0; i < 8; i++)
		   {
		      binary.append((val & 128) == 0 ? 0 : 1);
		      val <<= 1;
		   }
		}
		return binary;
	}
	
	/**
	 * Handles a solution of the proof-of-work by verifying it.
	 */
	private class SolutionFoundHandler implements HandlerInterface 
	{
		private Node peer;
		
		public SolutionFoundHandler(Node peer) 
		{ 
			this.peer = peer;
		}
		
		public void handleMessage(PeerConnection peerconn, PeerMessage msg) 
		{
			boolean solution;
			try {
				solution = verifySolution(msg.getMsgData());
				if(!solution)
				{
					peerconn.sendData(new PeerMessage(ERROR, "Not a solution"));
				}
				else
				{
					
				}
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		/**
		 * Hashes the message info with the answer to verify the solution.
		 * @param answer the solution found
		 * @return the boolean value
		 * @throws UnsupportedEncodingException 
		 */
		public boolean verifySolution(String answer) throws UnsupportedEncodingException
		{
			//Hash verifier
			String zeroes = String.format(String.format("%%%ds", NUMOFZEROES), " ").replace(" ","0");
			return true;
		}
	}
	
	/**
	 * Handles a received transaction message by broadcasting it to its known peers.
	 */
	private class TransactionHandler implements HandlerInterface 
	{
		private Node peer;
		
		public TransactionHandler(Node peer) 
		{ 
			this.peer = peer;
		}
		
		public void handleMessage(PeerConnection peerconn, PeerMessage msg) 
		{
			transactions.put(peerconn.getPeerInfo().getId(), msg.getMsgData());
			
			//Sending a broadcast message to everybody in the list of peers.
			for (PeerInfo pd : peer.getAllKnownPeers()) 
			{
				peer.connectAndSend(pd, TRANSACTION, msg.getMsgData(), false);
			}
			
		}
	}
	
	/* msg syntax: FGET file-name */
	private class FileGetHandler implements HandlerInterface 
	{
		@SuppressWarnings("unused")
		private Node peer;
		
		public FileGetHandler(Node peer) 
		{ 
			this.peer = peer; 
		}
		
		public void handleMessage(PeerConnection peerconn, PeerMessage msg)
		{
			String filename = msg.getMsgData().trim();
			if (!files.containsKey(filename))
			{
				peerconn.sendData(new PeerMessage(ERROR, "Fget: " + "file not found " + filename));
				return;
			}
			
			byte[] filedata = null;
			try 
			{
				FileInputStream infile = new FileInputStream(filename);
				int len = infile.available();
				filedata = new byte[len];
				infile.read(filedata);
				infile.close();
			}
			catch (IOException e) 
			{
				LoggerUtil.getLogger().info("Fget: error reading file: " + e);
				peerconn.sendData(new PeerMessage(ERROR, "Fget: " + "error reading file " + filename));
				return;
			}
			
			peerconn.sendData(new PeerMessage(REPLY, filedata));
		}
	}
}
