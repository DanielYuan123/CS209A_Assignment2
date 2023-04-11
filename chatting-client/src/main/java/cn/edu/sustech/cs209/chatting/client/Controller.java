package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.util.Duration;
import jdk.nashorn.internal.codegen.CompilerConstants;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;

public class Controller implements Initializable {
    Stage stage;
    @FXML
    Label Location;
    @FXML
    Label currentUsername;
    @FXML
    Label currentOnlineCnt;
    @FXML
    VBox root;
    @FXML
    ListView<Message> chatContentList;
    @FXML
    ListView<String> chatList;
    @FXML
    TextArea inputArea;
    @FXML
    TextArea Notification;

    HashMap<String, ArrayList<Message>> usersDialogSet = new HashMap<>();
    HashMap<String, ArrayList<String>> userGroupSet = new HashMap<>();

    String username;

    String currentChatMate = null;
    Socket client;
    Socket messageClient;
    Socket onlineCntClient;
    Scanner in;
    PrintWriter out;
    ObjectInputStream messageIn = null;
    ObjectOutputStream messageOut = null;

    Thread listenThread;

    Thread onlineCntThread;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initialChatList();
        Notification.setEditable(false);

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

            while(username.equals("OVER")){
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Invalid name");
                alert.setContentText("User name is not available. Change your name.");
                alert.showAndWait();
                input = dialog.showAndWait();
                username = input.get();
            }

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
            try {

                quitServer();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Platform.exit();
        }

        currentUsername.setText("Current User: "+username);

        registerMessageServer();

        try {
            messageIn = new ObjectInputStream(messageClient.getInputStream());
            messageOut = new ObjectOutputStream(messageClient.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        chatContentList.setCellFactory(new MessageCellFactory());
        chatContentList.setItems(FXCollections.observableArrayList());

        //


            listenThread = new Thread(new ReceiveListener());
            listenThread.start();
            onlineCntThread = new Thread(new OnlineCntListener());
            onlineCntThread.start();


        //Platform.runLater(new ReceiveListener());
    }

    public void initialChatList(){
        chatList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if(currentChatMate!=null) {
                        usersDialogSet.get(currentChatMate).clear();
                        usersDialogSet.get(currentChatMate).addAll(chatContentList.getItems());
                    }
                    chatContentList.getItems().clear();
                    chatContentList.getItems().setAll(usersDialogSet.get(newValue));
                    currentChatMate = newValue;
                    Location.setText(currentChatMate);
                }
        );
    }

    public void initialStage(Stage stage){
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent){
                if(windowEvent.getSource() == stage){
                    try {
                        listenThread.interrupt();
//                        onlineCntThread.interrupt();
                        quitServer();
                        System.exit(0);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
        });
    }


    public void registerSocket(){
        try {
            client = new Socket("localhost",130);
            messageClient = new Socket("localhost",130);
            in = new Scanner(client.getInputStream());
            out = new PrintWriter(client.getOutputStream());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void quitServer() throws IOException {
        out.println("QUIT");
        out.flush();
        out.close();
        in.close();
        messageOut.writeObject(new Message(System.currentTimeMillis(),"SYSTEM","SERVER","CLOSE"));
        messageOut.flush();
        onlineCntClient.close();
    }

    public void registerMessageServer(){
        try {

            PrintStream messageOut = new PrintStream(messageClient.getOutputStream());
            messageOut.println("MESSAGEREGISTER "+username);
            messageOut.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
            if(nextLine.equals("OVER")){
                break;
            }
            if(!nextLine.equals(username)){
                output.add(nextLine);
            }

        }
        return output;
    }
    @FXML
    public void createPrivateChat() {
        AtomicReference<String> user = new AtomicReference<>();

        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();
        userSel.setItems(FXCollections.observableArrayList());
        userSel.setCellFactory(new UserCellFactory());

        // FIXME: get the user list from server, the current user's name should be filtered out
        ArrayList<String>users = getServerUsers();
        userSel.getItems().setAll(users);

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
            Location.setText(currentChatMate);

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
        Stage stage = new Stage();

        ListView<CheckBox> userSel = new ListView<>();
        userSel.setItems(FXCollections.observableArrayList());
        ArrayList<CheckBox> onlineUsers = new ArrayList<>();
        ArrayList<String> onlineUsersString = getServerUsers();
        for (String s : onlineUsersString) {
            onlineUsers.add(new CheckBox(s));
        }
        userSel.getItems().addAll(onlineUsers);
        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            stage.close();
            ArrayList<CheckBox> userSet = new ArrayList<>();
            ArrayList<String> selected = new ArrayList<>();

            userSet.addAll(userSel.getItems());

            for (CheckBox checkBox : userSet) {
                if(checkBox.isSelected()){
                    selected.add(checkBox.getText());
                }
            }

            if(selected.size()==1){
                Alert alert = new Alert(AlertType.ERROR);
                alert.setContentText("Please choose at least two user to create a group chat.");
                alert.showAndWait();
                return;
            }
            selected.add(username);
            Collections.sort(selected);
            String groupChatMate = null;
            if(selected.size() > 3){
                groupChatMate = String.format("%s, %s, %s...(%d)",selected.get(0),selected.get(1),selected.get(2),selected.size());
            }else{
                groupChatMate = String.format("%s, %s, %s(3)",selected.get(0),selected.get(1),selected.get(2));
            }
            int duplicated = 1;
            String finalGroupChatMate = groupChatMate + "";
            while(userGroupSet.containsKey(groupChatMate)){
                finalGroupChatMate = String.format("%s[%d]",groupChatMate,duplicated);
                duplicated++;
            }

            userGroupSet.put(finalGroupChatMate,selected);

            if(usersDialogSet.size()!=0){
                usersDialogSet.get(currentChatMate).clear();
                usersDialogSet.get(currentChatMate).addAll(chatContentList.getItems());
                chatContentList.getItems().clear();
            }

            chatList.getItems().add(finalGroupChatMate);
            usersDialogSet.put(finalGroupChatMate,new ArrayList<>(5));
            chatContentList.getItems().setAll(usersDialogSet.get(finalGroupChatMate));

            currentChatMate = finalGroupChatMate;
            Location.setText(currentChatMate);

        });

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();


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
        if(currentChatMate==null){
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("No available chat mate");
            alert.setContentText("Select an available from the left pane to chat with. If there's no content, generate a new chat from menu bar.");
            alert.showAndWait();
            return;
        }

        if(userGroupSet.containsKey(currentChatMate)){
            ArrayList<String> usersSendTo = userGroupSet.get(currentChatMate);

            Message message = new Message(System.currentTimeMillis(),username,currentChatMate,text);
            message.setIsGroupMessage();
            message.setGroupMembers(usersSendTo);

            usersDialogSet.get(currentChatMate).add(message);
            chatContentList.getItems().add(message);
            inputArea.clear();

            try {
                messageOut.writeObject(message);
                messageOut.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        //send to
        Message message = new Message(System.currentTimeMillis(),username,currentChatMate,text);
        usersDialogSet.get(currentChatMate).add(message);
        chatContentList.getItems().add(message);
        inputArea.clear();

        if(!getServerUsers().contains(currentChatMate)){
            Notification.setText("The user " + currentChatMate + " you are chatting with is offline now.");
            Timeline autoClear = new Timeline(new KeyFrame(Duration.seconds(5),e -> Notification.clear()));
            autoClear.setCycleCount(1);
            autoClear.play();
        }

        try {
            messageOut.writeObject(message);
            messageOut.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    public class OnlineCntListener implements Runnable{
        @Override
        public void run() {
            try {
                Thread.sleep(1);
                onlineCntClient = new Socket("localhost",130);
                Scanner onlineCntSc = new Scanner(onlineCntClient.getInputStream());
                PrintStream onlineInital = new PrintStream(onlineCntClient.getOutputStream());
                onlineInital.println("GRASP");
                onlineInital.flush();
                while(onlineCntSc.hasNext()){
                    String s = onlineCntSc.next();
                    if(!s.equals("")){
                        Platform.runLater(()->currentOnlineCnt.setText("Online: "+s));
                    }
                }
                onlineCntClient.close();
                onlineInital.close();
                onlineCntSc.close();
                System.out.println("Closed");
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e){
                try {
                    System.out.println("close");
                    onlineCntClient.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }


        }
    }
    public class ReceiveListener implements Runnable{

        @Override
        public void run() throws IllegalStateException {
            try {
                while(!Thread.currentThread().isInterrupted()){
                    Thread.sleep(1);
                    Message receivedMessage = (Message) messageIn.readObject();
                    System.out.println("get message");
                    String from = receivedMessage.getSentBy();
                    String to = receivedMessage.getSendTo();
                    if(receivedMessage.isGroupMessage()){
                        Notification.setText("You receive a group information from "+to+".");
                    }else{
                        Notification.setText("You receive an information from "+from+".");
                    }



                    Timeline autoClear = new Timeline(new KeyFrame(Duration.seconds(5),e -> Notification.clear()));
                    autoClear.setCycleCount(1);
                    autoClear.play();

                    if(receivedMessage.isGroupMessage()){
                        if(!chatList.getItems().contains(to)){
                            Platform.runLater(() -> chatList.getItems().add(to));
                        }
                        if(!userGroupSet.containsKey(from)){
                            userGroupSet.put(to,receivedMessage.getGroupMembers());
                            usersDialogSet.put(to,new ArrayList<Message>());
                        }
                        if(currentChatMate!=null){
                            if(currentChatMate.equals(to)){
                                Platform.runLater(() -> chatContentList.getItems().add(receivedMessage));
                            }
                        }
                        usersDialogSet.get(to).add(receivedMessage);
                        continue;
                    }


                    if(!chatList.getItems().contains(from)){
                        Platform.runLater(() -> chatList.getItems().add(from));
                        usersDialogSet.put(from,new ArrayList<Message>());
                    }
                    if(currentChatMate!=null){
                        if(currentChatMate.equals(from)){
                            Platform.runLater(() -> chatContentList.getItems().add(receivedMessage));
                        }
                    }
                    usersDialogSet.get(from).add(receivedMessage);
                }

            } catch (EOFException e){

            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e){
                try {
                    messageIn.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }finally {
                try {
                    messageIn.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    private class UserCellFactory implements Callback<ListView<String>, ListCell<String>>{

        @Override
        public ListCell<String> call(ListView<String> param) {
            return new ListCell<String>() {
                @Override
                public void updateItem(String msg, boolean empty){
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }else{
                        setText(msg);
                    }
                }
            };
        }
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
                public void updateItem(Message msg, boolean empty) throws IllegalStateException{
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
