package assignment6;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

public class MyServer extends Observable{
	HashMap<String,Integer> users;
	HashMap<Integer,ArrayList<String>> uniqueRoomClientList;
	HashMap<Integer,ArrayList<String>> roomHistory;
	HashMap<Integer,String> roomNames;
	HashMap<Integer,ArrayList<String>> usersCurrentlyInChat;
	ArrayList<String> onlineUsers;
	ServerSocket sock;
	int port = 4242; //THIS IS THE PORT TO USE
	static int chatRoomID;
	public MyServer() {
		users = new HashMap<String,Integer>(); //create empty map of lists of users and their corresponding sockets
		uniqueRoomClientList = new HashMap<Integer,ArrayList<String>>();
		roomHistory = new HashMap<Integer,ArrayList<String>>();
		usersCurrentlyInChat = new HashMap<Integer,ArrayList<String>>();
		onlineUsers = new ArrayList<String>();
		roomNames = new HashMap<Integer,String>();
		try {
			sock = new ServerSocket(port); //creates a socket to monitor connection requests
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void main(String[] args ){
		chatRoomID = 0;
		MyServer s = new MyServer();		
		s.runServer();
	}

	public void runServer() {
		while(true) {
			try {
				Socket clientConnect = sock.accept();				//continuously waits for new clients to request service
				MyObserver c = new MyObserver(clientConnect); //observes the server and writes updates to their client when neccessary
				this.addObserver(c);
				Thread t = new Thread(new ClientHandler(clientConnect,c));	//this thread handles various client requests such as setting up a username & pw, logging in, creating new chats and sending messages to existing chats
				t.start();
				System.out.println("connection");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	private class ClientHandler implements Runnable{
		private DataInputStream dataIn;
		private DataOutputStream dataOut;
		Socket client;
		MyObserver observer;
		public ClientHandler(Socket client,MyObserver observer) {
			try {
				dataIn = new DataInputStream(client.getInputStream());
				dataOut = new DataOutputStream(client.getOutputStream());
				this.client = client;
				this.observer = observer;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		@SuppressWarnings("synthetic-access")
		@Override
		public void run() {
			while(true) {
				try {
					byte messageType = dataIn.readByte();
					ArrayList observerProtocolList = new ArrayList();
					ArrayList<String> relevantObservers;
					ArrayList relevantParameters;
					switch(messageType) {
					case 0: //Client is requesting to sign up a new username and password
						String user = dataIn.readUTF();
						String pw1 = dataIn.readUTF();
						String pw2 = dataIn.readUTF();
						System.out.println("new sign up request: "+user+pw1+pw2);
						if (!pw1.equals(pw2)) { //UNSUCCESSFUL SIGN UP
							dataOut.writeByte(1); //signifies incoming error message
							dataOut.writeUTF("Passwords don't match! Check your typing!");
							dataOut.flush();
						} 
						else if (usernameTaken(user)) { //UNSUCCESSFUL SIGN UP
							dataOut.writeByte(1); //signifies incoming error message		
							dataOut.writeUTF("Username is already taken! Try another!");
							dataOut.flush();
						}
						else { //SUCCESSFUL SIGN UP : create new username and password to identify this client socket 
							users.put(user,pw1.hashCode()); //create a new entry in the mappings 
							onlineUsers.add(user);
							observer.setUsername(user);		//now the observer can distinguish between messages intended for it and messages it can skip
							dataOut.writeByte(0); //success message to client
							dataOut.writeUTF(user); //write back user's username
							sendAllUsers();
							dataOut.flush();
							relevantObservers = new ArrayList<String>();
							ArrayList<String> all = new ArrayList<String>();
							if (!users.keySet().isEmpty()) {
								Byte protocolNum = 4;
								observerProtocolList.clear();
								for(String s: users.keySet()) {
									all.add(s);
									if (!s.equals(user)) {
									relevantObservers.add(s);
									}
								}
								observerProtocolList.add(relevantObservers);		//these are the observers that need to care about this message (all users)
								observerProtocolList.add(protocolNum);					//we are creating a new chat 
								observerProtocolList.add(onlineUsers);
								observerProtocolList.add(all);
								observerProtocolList.add(uniqueRoomClientList);
								observerProtocolList.add(roomNames);

								setChanged();
								notifyObservers(observerProtocolList);
							}
						}
						break;
					case 1: //Client is requesting to log in with an existing username and password
						String username = dataIn.readUTF();
						String password = dataIn.readUTF();
						//TODO: If already logged in prevent another log in
						System.out.println("New sign in request: "+username+password);

						if (passwordMatchChecker(username,password) && !onlineUsers.contains(username)) { //SUCCESSFUL SIGN IN
							onlineUsers.add(username);
							observer.setUsername(username); //now the observer knows which messages are intended for it
							dataOut.writeByte(0); //success message to client
							dataOut.writeUTF(username); //write back user's username
							sendAllUsers();
							dataOut.flush();
							relevantObservers = new ArrayList<String>();
							ArrayList<String> all = new ArrayList<String>();
							if (!users.keySet().isEmpty()) {
								Byte protocolNum = 4;
								observerProtocolList.clear();
								for(String s: users.keySet()) {
									all.add(s);
										relevantObservers.add(s);
								}
								observerProtocolList.add(relevantObservers);		//these are the observers that need to care about this message (all users)
								observerProtocolList.add(protocolNum);					//we are creating a new chat 
								observerProtocolList.add(onlineUsers); 
								observerProtocolList.add(all); 
								observerProtocolList.add(uniqueRoomClientList);
								observerProtocolList.add(roomNames);
								setChanged();
								notifyObservers(observerProtocolList);
							}
						}
						else {     //UNSUCCESSFUL SIGN IN
							dataOut.writeByte(1); //signifies incoming error message 
							if (onlineUsers.contains(username)) {
								dataOut.writeUTF("This user is already logged in.");
							}
							else {
								dataOut.writeUTF("Username and password combination doesn't exist! Try again!");
							}
							dataOut.flush();
						}
						break;
					case 2: //New chatroom to be created between users! 
						//next notify observers with a list of objects including create keyword, and 2 usernames so they know which observers the message is for
						String initiator = dataIn.readUTF();
						relevantObservers = new ArrayList<String>(); //will hold list of observers who need to respond, protocol #, and list of parameters if neccessary
						relevantObservers.add(initiator);	
						int numOtherUsers = dataIn.readByte(); // about to come in
						for (int i = 0; i < numOtherUsers; i++) {
							relevantObservers.add(dataIn.readUTF());
						}
						roomNames.put(chatRoomID, dataIn.readUTF()); //The name of this chatroom 
						System.out.println("Recieved chat request from "+initiator);
						dataOut.writeByte(2); //opening chatroom for initiator
						dataOut.writeByte(chatRoomID);
						dataOut.writeUTF(roomNames.get(chatRoomID)); //sending the chatroom name to be displayed on title
						dataOut.flush();
						uniqueRoomClientList.put(chatRoomID, relevantObservers); //
						roomHistory.put(chatRoomID, new ArrayList<String>());
						ArrayList<String> chatters = new ArrayList<String>();
						chatters.add(initiator);
						usersCurrentlyInChat.put(chatRoomID,chatters);
						System.out.println("Created chat ID : " + chatRoomID);
						chatRoomID = chatRoomID+1;
						relevantObservers = new ArrayList<String>();
						ArrayList<String> all = new ArrayList<String>();
						if (!users.keySet().isEmpty()) {
							Byte protocolNum = 4;
							observerProtocolList.clear();
							for(String s: users.keySet()) {
								all.add(s);
								relevantObservers.add(s);
							}
						
							observerProtocolList.add(relevantObservers);		//these are the observers that need to care about this message (all users)
							observerProtocolList.add(protocolNum);					//we are creating a new chat 
							observerProtocolList.add(onlineUsers); 
							observerProtocolList.add(all); 
							observerProtocolList.add(uniqueRoomClientList);
							observerProtocolList.add(roomNames);
							setChanged();
							notifyObservers(observerProtocolList);
						}
						break;
					case 3: //CLIENT SENDING A MESSAGE IN THE CHATROOM
						int chatID = dataIn.readByte();
						System.out.print(chatID);
						String message = dataIn.readUTF();
						roomHistory.get(chatID).add(message); //add to chatlog history
						observerProtocolList.clear();
						relevantObservers = usersCurrentlyInChat.get(chatID);
						Byte protocolNumber = 3;			//we are sending a message in an existing chat
						observerProtocolList.add(relevantObservers);		//these are the observers that need to care about this message
						observerProtocolList.add(protocolNumber);					
						observerProtocolList.add(chatID);
						observerProtocolList.add(message);					//message to send to relevant clients
						setChanged();
						notifyObservers(observerProtocolList);
						break;
					case 4: //logout from control panel (complete logout)
						String userLoggingOut = dataIn.readUTF();
						onlineUsers.remove(userLoggingOut);
						relevantObservers = new ArrayList<String>();
						all = new ArrayList<String>();
						if (!users.keySet().isEmpty()) {
							Byte protocolNum = 4;
							observerProtocolList.clear();
							for(String s: users.keySet()) {
								all.add(s);
								if (!s.equals(userLoggingOut)) {
									relevantObservers.add(s);
									}
							}
							observerProtocolList.add(relevantObservers);		//these are the observers that need to care about this message (all users)
							observerProtocolList.add(protocolNum);					//we are creating a new chat 
							observerProtocolList.add(onlineUsers); 
							observerProtocolList.add(all); 
							observerProtocolList.add(uniqueRoomClientList);
							observerProtocolList.add(roomNames);
							
							
							setChanged();
							notifyObservers(observerProtocolList);
						}
						break;
					case 5: //user wants chatlog history
						chatID = dataIn.readByte();
						dataOut.writeByte(5); //sending history back will send number of strings followed by strings
						dataOut.writeByte(chatID);
						dataOut.writeByte(roomHistory.get(chatID).size()); 
						for (String s : roomHistory.get(chatID)) {
							dataOut.writeUTF(s);
						}
						dataOut.flush();
						break;
					case 6: //user wants to exit a chatroom 
						chatID = dataIn.readByte(); 
						String userLeaving = new String(dataIn.readUTF());
						usersCurrentlyInChat.get(chatID).remove(userLeaving);
						message = new String(userLeaving + " has left the chatroom");
						
						roomHistory.get(chatID).add(message); //add to chatlog history
						observerProtocolList.clear();
						relevantObservers = usersCurrentlyInChat.get(chatID);
						protocolNumber = 3;			//we are sending a message in an existing chat
						observerProtocolList.add(relevantObservers);		//these are the observers that need to care about this message
						observerProtocolList.add(protocolNumber);					
						observerProtocolList.add(chatID);
						observerProtocolList.add(message);					//message to send to relevant clients
						setChanged();
						notifyObservers(observerProtocolList);
						break;
					case 7: //adding user to chat room
						chatID = dataIn.readByte();
						System.out.println("Adding user to : " +chatID);
						user = dataIn.readUTF();
						usersCurrentlyInChat.get(chatID).add(user);
						dataOut.writeByte(5); //sending history back will send number of strings followed by strings
						dataOut.writeByte(chatID);
						dataOut.writeByte(roomHistory.get(chatID).size()); 
						for (String s : roomHistory.get(chatID)) {
							dataOut.writeUTF(s);
						}
						dataOut.flush();
						
						//Let everyone know user joined chatroom
						message = new String (user+" has joined the chatroom");
						roomHistory.get(chatID).add(message); //add to chatlog history
						observerProtocolList.clear();
						relevantObservers = usersCurrentlyInChat.get(chatID);
						protocolNumber = 3;			//we are sending a message in an existing chat
						observerProtocolList.add(relevantObservers);		//these are the observers that need to care about this message
						observerProtocolList.add(protocolNumber);					
						observerProtocolList.add(chatID);
						observerProtocolList.add(message);					//message to send to relevant clients
						setChanged();
						notifyObservers(observerProtocolList);
					}

				}	
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		private boolean passwordMatchChecker(String username, String password) {
			for (Map.Entry<String,Integer> entry : users.entrySet()) {
				if (entry.getKey().equals(username)) { //found the user name
					if (entry.getValue().equals(password.hashCode())) {
						return true;		//password matches
					}
					else return false;		//if they don't match return false
				}
			}
			return false;					//if username isn't in database return false;
		}

		private boolean usernameTaken(String user) {
			for (Map.Entry<String,Integer> entry : users.entrySet()) {
				if (entry.getKey().equals(user)) { //the user already exists
					return true;
				}
			}
			return false;
		}
		private void sendAllUsers() {
			try {
				dataOut.writeByte(users.size()); //how many usernames we're about to send
				for(String u : users.keySet()) {	
					dataOut.writeUTF(u);	//sending every existing username
					if (onlineUsers.contains(u)) {
						dataOut.writeBoolean(true); //online
					}
					else {
						dataOut.writeBoolean(false); //not online
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}