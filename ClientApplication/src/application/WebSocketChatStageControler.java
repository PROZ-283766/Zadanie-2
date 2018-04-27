package application;

import java.io.File;
import java.io.IOException;
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
	ListView<String> attach_ListView;
	private String user;
	private WebSocketClient webSocketClient;

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
			webSocketClient.attach(selectedFile.getPath());
		}
		if (btnSendAttachment.isDisable() && !attach_ListView.getItems().isEmpty()) {
			btnSendAttachment.setDisable(false);
		}
	}

	@FXML
	private void btnSendAttachment_Click() {

		webSocketClient.sendFiles();

		attach_ListView.getItems().clear();
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

		private void connectToWebSocket() {
			WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
			try {
				URI uri = URI.create("ws://localhost:8080/WebServer/websocketendpoint");
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

		public void attach(String message) {
			System.out.println("Sth was attached: " + message);
			attach_ListView.getItems().add(message);
		}

		public void sendFiles() {
			try {
				attach_ListView.getItems().size();
				for (int i = 0; i != attach_ListView.getItems().size(); ++i) {
					System.out.println(user + " send: " + attach_ListView.getItems().get(i).toString());
					session.getBasicRemote().sendText(user + ": " + attach_ListView.getItems().get(i));
				}

			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

	} // public class WebSocketClient
} // public class WebSocketChatStageControler
