package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Scanner;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

public class Controller implements Initializable {
    Stage stage;
    @FXML
    VBox root;
    @FXML
    ListView<Message> chatContentList;
    @FXML
    ListView<String> chatList;
    @FXML
    TextArea inputArea;

    HashMap<String, ArrayList<Message>> usersDialogSet = new HashMap<>();

    String username;

    String currentChatMate;
    Socket client;
    Scanner in;
    PrintWriter out;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        chatList.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if(newValue!=oldValue){
                    usersDialogSet.get(currentChatMate).clear();
                    usersDialogSet.get(currentChatMate).addAll(chatContentList.getItems());

                    chatContentList.getItems().clear();
                    chatContentList.getItems().setAll(usersDialogSet.get(newValue));
                    currentChatMate = newValue;
                }
            }
        );

        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText(null);
        dialog.setContentText("Username:");

        registerSocket();


        Optional<String> input = dialog.showAndWait();
        if (input.isPresent() && !input.get().isEmpty()) {

            /*
               TODO: Check if there is a user with the same name among the currently logged-in users,
                     if so, ask the user to change the username
             */
            username = input.get();

            while(!registerServer()){
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Duplicated name");
                alert.setContentText("User name is already exist. Change your name.");
                alert.showAndWait();
                input = dialog.showAndWait();
                username = input.get();
            }
        } else {
            System.out.println("Invalid username " + input + ", exiting");
            quitServer();
            Platform.exit();
        }

        chatContentList.setCellFactory(new MessageCellFactory());
        chatContentList.setItems(FXCollections.observableArrayList());

        stage = (Stage)root.getScene().getWindow();
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                if(windowEvent.getSource() == stage){
                    quitServer();
                }
            }
        });
    }


    public void registerSocket(){
        try {
            client = new Socket("localhost",135);
            in = new Scanner(client.getInputStream());
            out = new PrintWriter(client.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void quitServer(){
        out.println("QUIT");
        out.flush();
        out.close();
        in.close();
    }

    public boolean registerServer(){
        out.println("REGISTER  "+username);
        out.flush();
        String response = in.next();
        if(response.equals("OK")){
            return true;
        }else if(response.equals("DENY")){
            return false;
        }
        return true;
    }

    public ArrayList<String> getServerUsers(){
        out.println("GET");
        out.flush();

        ArrayList<String> output = new ArrayList<>(5);
        String nextLine = null;
        while((nextLine = in.next())!=null){
            if(nextLine != username){
                output.add(nextLine);
            }
            if(nextLine.equals(in.next())){
                break;
            }
        }
        return output;
    }



    @FXML
    public void createPrivateChat() {
        AtomicReference<String> user = new AtomicReference<>();

        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();

        // FIXME: get the user list from server, the current user's name should be filtered out
        ArrayList<String>users = getServerUsers();

        userSel.getItems().addAll(users);

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            user.set(userSel.getSelectionModel().getSelectedItem());
            stage.close();



            //Add new a user chatting item to the left pane
            if(usersDialogSet.size()!=0){
                usersDialogSet.get(currentChatMate).clear();
                usersDialogSet.get(currentChatMate).addAll(chatContentList.getItems());
                chatContentList.getItems().clear();
            }


            if(chatList.getItems().contains(user.get())){
                //open the chat with user selected
                chatContentList.getItems().setAll(usersDialogSet.get(user.get()));
            }else{
                chatList.getItems().add(user.get());
                usersDialogSet.put(user.get(),new ArrayList<>(5));
                chatContentList.getItems().setAll(usersDialogSet.get(user.get()));
            }
            currentChatMate = user.get();
        });

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();


        // TODO: if the current user already chatted with the selected user, just open the chat with that user
        // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
    }

    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
    @FXML
    public void createGroupChat() {
    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() {
        String text = inputArea.getText();
        //send to
        Message message = new Message(System.currentTimeMillis(),username,"Yuan",text);
        chatContentList.getItems().add(message);
        inputArea.clear();


        // TODO: Trans to server
    }

    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
     */
    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {

                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        setGraphic(null);
                        setText(null);

                        return;
                    }

                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentBy());
                    Label msgLabel = new Label(msg.getData());

                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

                    if (username.equals(msg.getSentBy())) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }
}
