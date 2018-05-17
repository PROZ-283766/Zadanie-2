package application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

public class WebSocketChatStageControler {
	@FXML
	TextField userTextField;
	@FXML
	TextArea chatTextArea;
	@FXML
	TextField messageTextField;
	@FXML
	Button btnSet;
	@FXML
	Button btnSend;
	@FXML
	Button btnAttach;
	@FXML
	Button btnSendAttachment;
	@FXML
	Button btnSDD;
	@FXML
	ListView<File> attach_ListView;
	private String user;
	private WebSocketClient webSocketClient;
	File selectedDirectory;

	@FXML
	private void initialize() {
		webSocketClient = new WebSocketClient();
		user = userTextField.getText();
		btnSend.setDisable(true);
		btnAttach.setDisable(true);
		btnSendAttachment.setDisable(true);
	}

	@FXML
	private void btnSet_Click() {
		user = userTextField.getText();
		if (userTextField.getText().trim().isEmpty() || user.trim().isEmpty()) {
			return;
		}
		btnSet.setDisable(true);
		btnSend.setDisable(false);
		btnAttach.setDisable(false);
		userTextField.setDisable(true);
	}

	@FXML
	private void btnSend_Click() {
		sendMessage();
	}

	@FXML
	private void keyEntered(KeyEvent event) {
		if (event.getCode() == KeyCode.ENTER) {
			sendMessage();
		}
	}

	private void sendMessage() {
		if (messageTextField.getText().trim().isEmpty())
			return;
		webSocketClient.sendMessage(messageTextField.getText());
		messageTextField.clear();
	}

	@FXML
	private void btnAttach_Click() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Resource File");
		File selectedFile = fileChooser.showOpenDialog(null);
		if (selectedFile != null) {
			System.out.println(selectedFile);
			attach_ListView.getItems().add(selectedFile);
			System.out.println("Sth was attached: " + selectedFile.getPath());
		}
		if (btnSendAttachment.isDisable() && !attach_ListView.getItems().isEmpty()) {
			btnSendAttachment.setDisable(false);
			//btnAttach.setDisable(true);
		}
	}

	@FXML
	private void btnSendAttachment_Click() {

		webSocketClient.sendFiles();

		attach_ListView.getItems().clear();
		btnSendAttachment.setDisable(true);
		//btnAttach.setDisable(false);
	}

	@FXML
	private void btnSDD_Click() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		selectedDirectory = directoryChooser.showDialog(null);
		System.out.println(" :) " + selectedDirectory);
	}

	public void closeSession(CloseReason closeReason) {
		try {
			webSocketClient.session.close(closeReason);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@ClientEndpoint
	public class WebSocketClient {
		private Session session;

		public WebSocketClient() {
			connectToWebSocket();
		}

		@OnOpen
		public void onOpen(Session session) {
			System.out.println("Connection is opened.");
			this.session = session;
		}

		@OnClose
		public void onClose(CloseReason closeReason) {
			System.out.println("Connection is closed: " + closeReason.getReasonPhrase());
		}

		@OnError
		public void onError(Throwable throwable) {
			System.out.println("Error occured");
			throwable.printStackTrace();
		}

		@OnMessage
		public void onMessage(String message, Session session) {
			System.out.println("Message was received");
			chatTextArea.setText(chatTextArea.getText() + message + "\n");
		}

		@OnMessage
		public void onMessage(ByteBuffer message, Session session) {
			if(selectedDirectory==null)
				return;
			int length=message.getInt();
			String name="";
			for(int i=0;i!=length;++i) {
				name+=(char)message.get();
			}
			int lengthOfFile=message.getInt();
			System.out.println("File was received "+length + " " + name+ " " + lengthOfFile);
			System.out.println(selectedDirectory.toString() + "\\" + name);
			File file = new File(selectedDirectory.toString() + "\\" + name);
			try {
				FileOutputStream ostream = new FileOutputStream(file, false);
				byte[] hej=new byte[lengthOfFile];
				message.get(hej, 0, lengthOfFile);
				ostream.write(hej, 0, lengthOfFile);
				ostream.flush();
				ostream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void connectToWebSocket() {
			WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
			try {
				URI uri = URI.create("ws://localhost:9090/WebServer/websocketendpoint");
				webSocketContainer.connectToServer(this, uri);
			} catch (DeploymentException | IOException e) {
				e.printStackTrace();
			}
		}

		public void sendMessage(String message) {
			try {
				System.out.println("Message was sent: " + message);
				session.getBasicRemote().sendText(user + ": " + message);

			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		public void sendFiles() {
			try {
				// attach_ListView.getItems().size();
				for (int i = 0; i != attach_ListView.getItems().size(); ++i) {
					System.out.println(user + " send: " + attach_ListView.getItems().get(i).toString());
					session.getBasicRemote().sendText(user + ": " + attach_ListView.getItems().get(i).getName());
					InputStream is = new FileInputStream(attach_ListView.getItems().get(i));

					int length = attach_ListView.getItems().get(i).getName().length();
					ByteBuffer bufor = ByteBuffer.allocateDirect((int) attach_ListView.getItems().get(i).length()+8+length);//+4 for each int
					bufor.putInt(length);
					String fileName=attach_ListView.getItems().get(i).getName();
					for(int j=0;j!=length;++j) {
						bufor.put((byte)fileName.charAt(j));
					}
					long lengthOfFile=attach_ListView.getItems().get(i).length();
					int sizeOfInputStream=is.available();
					bufor.putInt(sizeOfInputStream);
					System.out.println(length+ "        "+lengthOfFile+ "        "+sizeOfInputStream);
					byte[] byteArray=new byte[sizeOfInputStream];
					is.read(byteArray, 0, sizeOfInputStream);
					bufor.put(byteArray);
					is.close();

					bufor.flip();
					session.getBasicRemote().sendBinary(bufor);
				}

			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

	} // public class WebSocketClient
} // public class WebSocketChatStageControler
