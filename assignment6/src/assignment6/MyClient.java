package assignment6;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import javafx.scene.paint.Color;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class MyClient extends Application{
	//variables declared up here are modified in some way by the dataInStream messsages by the server
	DataInputStream dataInStream;
	DataOutputStream dataOutStream;
	VBox sidebar;
	Text errorNotification;
	String myUsername;
	HashMap<Integer,Chatroom> chats;
	HashMap<String,Boolean> allUsersOnlineStatus;
	HashMap<String,Integer> availableChatsToJoin;
	public Stage initialStage;
	String serverIPAddress;

	public static void main(String[] args) {
		launch(args);
	}
	@Override
	public void start(Stage initialStage) throws Exception {
		this.initialStage = initialStage;
		chats = new HashMap<Integer,Chatroom>();
		allUsersOnlineStatus = new HashMap<String,Boolean>();
		availableChatsToJoin = new HashMap<String ,Integer>();
		getIpAddress(initialStage);
		initialStage.show();
		//chatroomInit(initialStage);
		//homeScreenInit(initialStage);
	}

	private void getIpAddress(Stage initialStage) {
		TextField ipAdd = new TextField();
		ipAdd.setPromptText("Enter the IP address of the server");
		Button go = new Button("Go!");
		Button useLocal = new Button("Use local host");
		HBox h = new HBox();
		h.getChildren().addAll(ipAdd,go);
		VBox v = new VBox();
		v.getChildren().addAll(h,useLocal);
		initialStage.setScene(new Scene(v));
		go.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				serverIPAddress = ipAdd.getText();
				try {
					networkInit();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		useLocal.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				serverIPAddress = "Local Host";
				try {
					networkInit();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}

	public void homeScreenInit(Stage initialStage) {
		Text signIn = new Text("Enter your information");
		TextField username = new TextField();
		username.setPromptText("Enter your username");
		PasswordField password = new PasswordField();
		password.setPromptText("Enter your password");
		Button login = new Button("Log in");
		Text orSignUp = new Text("Or sign up for an account!");
		TextField newUsername = new TextField();
		newUsername.setPromptText("Pick a username");
		PasswordField newPassword = new PasswordField();
		PasswordField pwConfirmation = new PasswordField();
		newPassword.setPromptText("Enter password");
		pwConfirmation.setPromptText("Re enter password");
		Button signUp = new Button("Sign up");
		VBox left = new VBox();
		left.getChildren().addAll(signIn,username,password,login);
		VBox right = new VBox();
		right.getChildren().addAll(orSignUp,newUsername,newPassword,pwConfirmation,signUp);
		Text empty = new Text("       ");
		HBox homePage = new HBox();
		homePage.getChildren().addAll(left,empty,right);
		errorNotification = new Text("");
		errorNotification.setFill(javafx.scene.paint.Color.RED);
		VBox layout = new VBox();
		layout.getChildren().addAll(homePage,errorNotification);
		initialStage.setTitle("Welcome to the Chatroom, please log in!");
		initialStage.setScene(new Scene(layout));

		signUp.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if (newUsername.getLength() != 0 && newPassword.getLength() != 0) {
					try {
						dataOutStream.writeByte(0);			//case 0 signifies to server that client wants to sign up a username and 3 strings are following
						dataOutStream.writeUTF(newUsername.getText());
						dataOutStream.writeUTF(newPassword.getText());
						dataOutStream.writeUTF(pwConfirmation.getText());
						dataOutStream.flush();
						
					}
					catch(Exception e) {
						System.out.println("error");
					}
				}
				else {
					errorNotification.setText("Username and password cannot be null");
				}
			}
		});
		login.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {
					dataOutStream.writeByte(1); //case 1 signifies to server that client wants to log in with an existing username and 2 strings will be following
					dataOutStream.writeUTF(username.getText());
					dataOutStream.writeUTF(password.getText());
					dataOutStream.flush();
					myUsername = username.getText();
				}
				catch(Exception e) {
					System.out.println("error");
				}
			}
		});
	}

	public class Chatroom  {
		VBox dataInStreamChatLog;
		VBox dataOutStreamChatLog;
		Button sendBtn;
		Stage chatStage;
		int chatroomID;
		String name;
		Chatroom(Stage chatStage,int unique,String name) {
			this.name = name;
			chatroomID = unique;
			this.chatStage = chatStage;
			//where messages from others will appear
			dataInStreamChatLog = new VBox();
			Text welcomeMessage = new Text("Welcome to the chatroom: "+name);
			welcomeMessage.setFill(javafx.scene.paint.Color.GREEN);
			dataInStreamChatLog.getChildren().add(welcomeMessage);
			dataInStreamChatLog.setPrefHeight(700);

			//where my sent messages will appear
			dataOutStreamChatLog = new VBox();
			dataOutStreamChatLog.setPrefHeight(700);

			//putting side bar, dataInStream and dataOutStream chat logs side by side
			HBox chatLogs = new HBox();
			chatLogs.setPrefWidth(350);
			chatLogs.getChildren().add(dataInStreamChatLog);
			VBox spaceBetweenLogs = new VBox();
			spaceBetweenLogs.setPrefHeight(350);
			spaceBetweenLogs.setPrefWidth(50);
			chatLogs.getChildren().add(spaceBetweenLogs);
			chatLogs.getChildren().add(dataOutStreamChatLog);
			ScrollPane scrollChat = new ScrollPane(chatLogs); //adding scroll bar

			//adding a place where the user can type messages to send
			TextField myMessage = new TextField();
			myMessage.setPrefWidth(250);
			myMessage.setPrefHeight(50);

			//creating send button
			Button sendBtn = new Button("SEND");
			sendBtn.setPrefHeight(50);
			sendBtn.setPrefWidth(100);

			//creating chat history button
			sendBtn.setPrefHeight(50);
			sendBtn.setPrefWidth(100);

			// putting them together: creating bottom type and send bar
			HBox typeNSend = new HBox();
			typeNSend.getChildren().add(myMessage);
			typeNSend.getChildren().add(sendBtn);

			//putting it all together
			VBox layout = new VBox();
			layout.getChildren().add(scrollChat);
			layout.getChildren().add(typeNSend);
			Scene chatroom = new Scene(layout,350,400);
			chatStage.setTitle("User: "+ myUsername+ "    Chatroom: " +name);
			chatStage.setScene(chatroom);
			chatStage.setHeight(400);

			sendBtn.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					try {
						dataOutStream.writeByte(3); //client is sending a new message to a chatroom : expect unique chat ID next, followed by message to send
						dataOutStream.writeByte(chatroomID);
						dataOutStream.writeUTF(myUsername + ": "+ myMessage.getText());
						dataOutStream.flush();

						myMessage.setText(""); 		//clear user's typing area
					}
					catch(Exception e) {
						System.out.println("error");
					}
				}
			});
			chatStage.setOnCloseRequest(new EventHandler<WindowEvent>() {

				@Override
				public void handle(WindowEvent event) {
					try {
						chats.remove(chatroomID);

						dataOutStream.writeByte(6); //signifies logout from chatroom 
						dataOutStream.writeByte(chatroomID);
						dataOutStream.writeUTF(myUsername);
						dataOutStream.flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
				}


			});
		}
	}

	public void chatControlPanelInit(Stage initialStage) {
		errorNotification.setText("");
		ArrayList<CheckBox> boxes = new ArrayList<CheckBox>();
		VBox userOptions = new VBox();

		for(String str:allUsersOnlineStatus.keySet()){
			if (!str.equals(myUsername)) {
				HBox status = new HBox();
				CheckBox cb = new CheckBox(str);
				cb.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						errorNotification.setText("");
					}
				});
				Text l = new Text(" Online");
				l.setFont(new Font(11));
				status.getChildren().addAll(cb,l);
				if (allUsersOnlineStatus.get(str)) {
					l.setText(" ONLINE");
					l.setFill(javafx.scene.paint.Color.GREEN);
				}
				else {
					l.setText(" OFFLINE");
					l.setFill(javafx.scene.paint.Color.RED);
				}
				boxes.add(cb);
				userOptions.getChildren().add(status);
			}
		}
		Label l = new Label("Users to add to chatroom:        ");
		Button createChat = new Button("Create Chatroom!");
		Button logout= new Button("Log out");
		VBox chatCreatorBar = new VBox();
		TextField roomName = new TextField();
		roomName.setPromptText("Chatroom name");
		if (boxes.isEmpty()) {
			l.setText("No users are currently online    ");
			createChat.setVisible(false);
			roomName.setVisible(false);
		}
		chatCreatorBar.getChildren().addAll(l,userOptions,roomName,createChat,errorNotification);

		//Join Chats Section
		VBox joinChats = new VBox();
		Label lab = new Label("Chats to Join");
		if (availableChatsToJoin.keySet().size() == 0) {
			lab.setText("No chats to join");
		}
		joinChats.getChildren().add(lab);
		for(String chatName: availableChatsToJoin.keySet()){
			Button b = new Button(chatName);
			b.setMinWidth(80);
			joinChats.getChildren().add(b);

			b.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					//Create Chatroom here
					//Check if chatroom already created
					int chatNum = availableChatsToJoin.get(chatName);
					if(chats.keySet().contains(chatNum)){
						//MyClient already has chatroom open
						errorNotification.setText("You have already joined "+chatName);
					}else{
						Stage st = new Stage();
						Chatroom c = new Chatroom(st,availableChatsToJoin.get(chatName),chatName);
						st.show();
						chats.put(availableChatsToJoin.get(chatName),c); //adds and displays a new chatroom that can be referenced by it's key #
						try {
							dataOutStream.writeByte(7); //add me to the chatroom current list 
							dataOutStream.writeByte(availableChatsToJoin.get(chatName));
							dataOutStream.writeUTF(myUsername);
							dataOutStream.flush();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			});
		}


		//Combine ChatCreator Side (left) and JoinChatsSection (right)
		HBox totalBox = new HBox();
		totalBox.getChildren().addAll(chatCreatorBar,joinChats);
		BorderPane border = new BorderPane();
		border.setCenter(totalBox);
		border.setBottom(logout);
		initialStage.setTitle(myUsername+"'s chat control panel");
		initialStage.setScene(new Scene(border,300,500));
		createChat.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if (!roomName.getText().isEmpty() ) {
					int numChecked = 0; 
					for (CheckBox cb: boxes) {
						if (cb.isSelected()) {
							numChecked++;
						}
					}
					if(numChecked!=0){
						try {
							dataOutStream.writeByte(2); //case 2 signifies client wants to create a new Chat with a single user whose username is being sent
							ArrayList<String> usersToAdd = new ArrayList<String>();
							for (CheckBox cb: boxes) {
								if (cb.isSelected()) {
									usersToAdd.add(cb.getText());
								}
							}
							dataOutStream.writeUTF(myUsername);
							dataOutStream.writeByte(usersToAdd.size()); //indicates how many other usernames are about to come
							for (String u : usersToAdd) {
								dataOutStream.writeUTF(u);
							}
							dataOutStream.writeUTF(roomName.getText()); //send chatroom name 
							dataOutStream.flush();
							System.out.println("Sent new chat request to server");
						}
						catch(Exception e) {
							System.out.println("error");
						}
					} else {
						//NUMCHECKED == 0
						errorNotification.setText("Select someone to chat with");

					}
				} else {
					errorNotification.setText("Enter a chatroom name");
				}
			}
		});
		logout.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {

				for( Chatroom c : chats.values()) {
					c.chatStage.close();
					try {
						chats.remove(c.chatroomID);

						dataOutStream.writeByte(6); //signifies logout from chatroom 
						dataOutStream.writeByte(c.chatroomID);
						dataOutStream.writeUTF(myUsername);
						dataOutStream.flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 	
				}
				try {
					dataOutStream.writeByte(4); //signifies logout from program
					dataOutStream.writeUTF(myUsername);
					dataOutStream.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			
				homeScreenInit(initialStage);
				initialStage.show();
				chats.clear();
			}
		});
		initialStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent event) {
				for( Chatroom c : chats.values()) {
					c.chatStage.close();
					try {
						chats.remove(c.chatroomID);

						dataOutStream.writeByte(6); //signifies logout from chatroom 
						dataOutStream.writeByte(c.chatroomID);
						dataOutStream.writeUTF(myUsername);
						dataOutStream.flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
					
					
					
				}
				try {
					dataOutStream.writeByte(4); //signifies logout from program
					dataOutStream.writeUTF(myUsername);
					dataOutStream.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
		});
	}

	@SuppressWarnings("synthetic-access")
	void networkInit() throws UnknownHostException, IOException {
		Socket sock;
		if (serverIPAddress.equals("Local Host")) {
			sock = new Socket(InetAddress.getLocalHost(),4242);	 //Request connection from server
		} 
		else {
			sock = new Socket(serverIPAddress,4242);	 //Request connection from server
		}
		dataInStream = new DataInputStream(sock.getInputStream());		//used to read various message types from server
		dataOutStream = new DataOutputStream(sock.getOutputStream());	//used to write various messages to server
		System.out.println("You are now connected");
		homeScreenInit(initialStage);
		Thread t = new Thread(new GetMessages());						//run a new thread to monitor dataInStream messages from server
		t.start();
	}

	private class GetMessages implements Runnable {
		boolean done;
		@Override
		public void run() {
			while(true) {
				//done = false;
				//while(!done) {
				try {
					byte messageType = dataInStream.readByte();
					switch(messageType) {
					case 0: //successful sign up or sign in -- go to users control panel 
						myUsername = dataInStream.readUTF();
						System.out.println("got the ok to sign up");
						int numUsers = dataInStream.readByte(); //it's about to send this many usernames 
						allUsersOnlineStatus.clear();
						for (int i = 0; i < numUsers; i++) {
							allUsersOnlineStatus.put(dataInStream.readUTF(),dataInStream.readBoolean());		//these are all usernames of users who have logged in 
						}
						Platform.runLater(new Runnable() {
							@Override 
							public void run() {
								chatControlPanelInit(initialStage);
								initialStage.show();
							}
						});

						break;
					case 1: //some sign up error has been thrown, print it to errorNotification
						errorNotification.setText(dataInStream.readUTF());
						break;
					case 2: //creating a new chatroom server sent unique chatroom identifier integer
						System.out.println("recieved okay to create new chatroom");
						int unique = dataInStream.readByte();
						String chatroomName = dataInStream.readUTF();
						Platform.runLater(new Runnable() {
							@Override 
							public void run() {
								Stage s = new Stage();
								Chatroom c = new Chatroom(s,unique,chatroomName);
								s.show();
								chats.put(unique,c); //adds and displays a new chatroom that can be referenced by it's key #
								System.out.println("added to list");
							}
						});
						break;
					case 3: //dataInStream message to display
						unique = dataInStream.readByte();
						String mes = dataInStream.readUTF();
						Platform.runLater(new Runnable() {
							@Override
							public void run() {	
								if (mes.contains("has left the chatroom")) {
									Text colored = new Text(mes);
									colored.setFill(javafx.scene.paint.Color.BLUE);
									chats.get(unique).dataInStreamChatLog.getChildren().add(colored);
								}
								else if (mes.contains("has joined the chatroom")) {
									Text colored = new Text(mes);
									colored.setFill(javafx.scene.paint.Color.GREEN);
									chats.get(unique).dataInStreamChatLog.getChildren().add(colored);
								}
								else if (mes.startsWith(myUsername)) {
									chats.get(unique).dataInStreamChatLog.getChildren().add(new Text("\t\t\t\t\t\t"+mes));
								}
								else {
									chats.get(unique).dataInStreamChatLog.getChildren().add(new Text(mes));
								}
							}
						});
						break;
					case 4: //refreshing list of online users 
						System.out.println("refresh:called" + myUsername);
						numUsers = dataInStream.readByte(); //it's about to send this many usernames 
						allUsersOnlineStatus.clear();
						for (int i = 0; i < numUsers; i++) {
							allUsersOnlineStatus.put(dataInStream.readUTF(),dataInStream.readBoolean());		//these are all usernames of users who have logged in 
						}
						int numChatRooms = dataInStream.readByte();
						availableChatsToJoin.clear();
						for (int i = 0; i < numChatRooms; i++) {
							availableChatsToJoin.put(dataInStream.readUTF(),(int)dataInStream.readByte());
						}
						Platform.runLater(new Runnable() {
							@Override 
							public void run() {
								chatControlPanelInit(initialStage);
								//initialStage.show();
							}
						});
						System.out.println("refresh done");
						break;
					case 5: //recieving chat history 
						int chatID = dataInStream.readByte();
						int bound = dataInStream.readByte();
						ArrayList<String> tempHistory = new ArrayList<String>();
						for (int i = 0; i < bound; i++) {
							tempHistory.add(dataInStream.readUTF());	 //read in all dataInStream strings
						}
						Platform.runLater(new Runnable() {
							@Override 
							public void run() {

								for(String s: tempHistory) {
									if (s.contains("has left the chatroom")) {
										Text colored = new Text(s);
										colored.setFill(javafx.scene.paint.Color.BLUE);
										chats.get(chatID).dataInStreamChatLog.getChildren().add(colored);
									}
									else if (s.contains("has joined the chatroom")) {
										Text colored = new Text(s);
										colored.setFill(javafx.scene.paint.Color.GREEN);
										chats.get(chatID).dataInStreamChatLog.getChildren().add(colored);
									}
									else if (s.startsWith(myUsername)) {
										chats.get(chatID).dataInStreamChatLog.getChildren().add(new Text("\t\t\t\t\t\t"+s));
									}
									else {
										chats.get(chatID).dataInStreamChatLog.getChildren().add(new Text(s)); //display list onto javafx interface
									}
								}

							}
						});
						break;
					}
				} catch (IOException e) {
					System.out.println("Error");
					e.printStackTrace();
				}
			}
		}
	}
}