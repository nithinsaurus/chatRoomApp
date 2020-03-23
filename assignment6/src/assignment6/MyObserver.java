package assignment6;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

public class MyObserver implements Observer{
	String username;
	DataOutputStream toClientStream;
	public MyObserver(Socket client) {
		try {
			toClientStream = new DataOutputStream(client.getOutputStream()); //this printer will write to the client's chatroom
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void setUsername(String s) {
		username = s;
	}
	@SuppressWarnings("unchecked")
	@Override
	public void update(Observable o, Object list) { //list will be a list in this format : {list of relevantObservers,protocol #,list of relevant parameters,unique chatroom ID,chatroom name}
		System.out.println("in update method");
		ArrayList message = (ArrayList) list;
		Byte protocolNum  = (Byte) message.get(1);
		if (thisMessageAppliesToMe(message)) {
			try {
				switch(protocolNum) {
				case 2: //create a new chat
					System.out.println("in the observer about to notify client");
					toClientStream.writeByte(2); //2 signifies to the client to create a new chatroom uniquely identified with the following number
					System.out.println("about to send chatNum");
					int chatNum = (Integer) message.get(2); //get unique chatroom ID
					toClientStream.writeByte(chatNum);
					toClientStream.writeUTF((String) message.get(3)); //sending the chatroom name to be displayed on title
					toClientStream.flush();
					System.out.println("just sent chatNum");
					chatNum = chatNum+1;
					break;
				case 3: //send chat out to all members of a chatroom
					toClientStream.writeByte(3);
					toClientStream.writeByte((Integer)message.get(2)); //send chatroom ID
					toClientStream.writeUTF((String)message.get(3)); // send message
					toClientStream.flush();
					break;
				case 4: //refresh
					toClientStream.writeByte(4);
					sendAllUsers((ArrayList<String>)message.get(3),(ArrayList<String>)message.get(2));
					ArrayList<Integer> chatIDs = new ArrayList<Integer>();
					HashMap<Integer,String> chatroomNames = (HashMap<Integer, String>) message.get(5);
					HashMap<Integer,ArrayList<String>> usersInRoom = (HashMap<Integer, ArrayList<String>>) message.get(4);
					for (Integer ID : usersInRoom.keySet()) {
						if (usersInRoom.get(ID).contains(username)) {
							chatIDs.add(ID);
						}
					}
					toClientStream.writeByte(chatIDs.size()); // so they know how many id's are coming
					for (Integer ID: chatIDs) {
						toClientStream.writeUTF(chatroomNames.get(ID));
						toClientStream.writeByte(ID);
					}
					toClientStream.flush();
					break;
				case 5: 
					sendAllUsers((ArrayList<String>)message.get(0),(ArrayList<String>)message.get(2));
					toClientStream.flush();
					
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}
	private boolean thisMessageAppliesToMe(ArrayList list) {

		ArrayList<String> relevantObservers = (ArrayList<String>) list.get(0);
		for(String user : relevantObservers) {
			if (user.equals(this.username)) {
				//the message applies to me
				return true;
			}
		}
		return false;
	}
	private void sendAllUsers(ArrayList<String> users,ArrayList<String> online) {
		try {
			toClientStream.writeByte(users.size()); //how many usernames we're about to send
			System.out.println(users.size());
			for(String u : users) {	
				toClientStream.writeUTF(u);				//sending every existing username
				if (online.contains(u)) {
					toClientStream.writeBoolean(true); //online
				}
				else {
					toClientStream.writeBoolean(false); //not online
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}