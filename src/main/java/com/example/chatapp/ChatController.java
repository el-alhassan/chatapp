package com.example.chatapp;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;

public class ChatController {
    @FXML
    public AnchorPane chatPane;
    @FXML
    private TextField messageField;
    @FXML
    private Button sendButton;
    @FXML
    private ListView<String> messageListView;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    @FXML
    public void addMessage() throws IOException {
        // Get the message from the text field and trim any leading/trailing whitespace
        String message = messageField.getText().trim();

        if (!message.isEmpty()) {
            // Add the message to the messageListView
            messageListView.getItems().add("You: " + message);

            // Clear the message field
            messageField.clear();

            // Scroll to the bottom of the messageListView
            messageListView.scrollTo(messageListView.getItems().size() - 1);

            // Check if the user input is "exit"
            if (message.equalsIgnoreCase("exit")) {
                // Disable the text field and button
                messageField.setDisable(true);
                sendButton.setDisable(true);

                // Send the "exit" message to the server
                out.println(message);

                // Close the socket and associated resources
                try {
                    out.close();
                    in.close();
                    socket.close();
                } catch (ConnectException e) {
                    // Handle the case when the server is offline
                    addMessageServer("Server is offline. Please try again later.");
                    messageField.setDisable(true);
                    sendButton.setDisable(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // Send the message to the server
                out.println(message);
            }
        }
    }

    @FXML
    public void addMessageServer(String message) {
        if (!message.isEmpty()) {
            // Run this code on the JavaFX application thread to update the UI
            Platform.runLater(() -> {
                // Add the message to the messageListView
                messageListView.getItems().add(message);

                // Clear the message field
                messageField.clear();

                // Scroll to the bottom of the messageListView
                messageListView.scrollTo(messageListView.getItems().size() - 1);
            });
        }
    }

    @FXML
    public void initialize() throws IOException {
        // Customize the appearance of the list cells in the messageListView
        messageListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    // Clear the cell if it's null or empty
                    setText(null);
                    setBackground(null);
                } else {
                    // Set the text of the cell to the message and customize its background and text color
                    setText(item);
                    setBackground(new Background(new BackgroundFill(Color.web("#2c2f33"), new CornerRadii(5), null)));
                    setTextFill(Color.WHITE);
                }
            }
        });
    }

    public void runClient() {
        try {
            // Connect to the server using a socket
            socket = new Socket("localhost", 1234);

            // Create input and output streams for communication with the server
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Send the username to the server
            out.println(username);

            // Start a separate thread for listening to server messages
            Thread listenThread = new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        // Add the server message to the messageListView
                        addMessageServer(message);
                    }
                } catch (IOException e) {
                    // Handle the case when the socket is closed
                    if (!(e instanceof SocketException && e.getMessage().equals("Socket closed"))) {
                        e.printStackTrace();
                    }
                }
            });
            listenThread.start();

            // Close the resources when the controller is destroyed
            Platform.runLater(() -> {
                sendButton.getScene().getWindow().setOnCloseRequest(event -> {
                    try {
                        // Send the "exit" message to the server
                        out.println("exit");
                        out.close();
                        in.close();
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            });
        } catch (ConnectException e) {
            // Handle the case when the server is offline
            addMessageServer("Server is offline. Please try again later.");
            sendButton.setDisable(true);
            messageField.setDisable(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
