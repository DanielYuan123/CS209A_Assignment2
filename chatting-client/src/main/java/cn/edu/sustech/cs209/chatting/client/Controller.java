package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.*;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javafx.util.Duration;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

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
    ListView<String> groupUserList;
    @FXML
    ListView<String> onlineUserList;
    @FXML
    TextArea inputArea;
    @FXML
    TextArea Notification;


    ArrayList<String> userOnline = new ArrayList<>(5);

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

    registerTextFieldListener rd;

    Stage logInStage = null;

    Stage registerStage = null;

    Thread listenThread;

    Thread onlineCntThread;
    Thread onlineUserThread;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        registerSocket();

        showLogInWindow();

        initialChatList();

        Notification.setEditable(false);
//        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("login.fxml"));
//        try {
//            stage1.setScene(new Scene(fxmlLoader.load()));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        stage1.setTitle("Log in");
//        stage1.show();
//        logInController logInController = new logInController();



//        Dialog<String> dialog = new TextInputDialog();
//        dialog.setTitle("Login");
//        dialog.setHeaderText(null);
//        dialog.setContentText("Username:");
//
//        registerSocket();

//        Optional<String> input = dialog.showAndWait();

//        if (input.isPresent() && !input.get().isEmpty()) {
//            /*
//               TODO: Check if there is a user with the same name among the currently logged-in users,
//                     if so, ask the user to change the username
//             */
//            username = input.get();
//
//            while(username.equals("OVER")){
//                Alert alert = new Alert(AlertType.ERROR);
//                alert.setTitle("Invalid name");
//                alert.setContentText("User name is not available. Change your name.");
//                alert.showAndWait();
//                input = dialog.showAndWait();
//                username = input.get();
//            }
//
//            while(!registerServer()){
//                Alert alert = new Alert(AlertType.ERROR);
//                alert.setTitle("Duplicated name");
//                alert.setContentText("User name is already exist. Change your name.");
//                alert.showAndWait();
//                input = dialog.showAndWait();
//                username = input.get();
//            }
//        } else {
//            System.out.println("Invalid username " + input + ", exiting");
//            try {
//
//                quitServer();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//            Platform.exit();
//        }

        //successful log in
        currentUsername.setText("Current User: "+username);

        registerMessageServer();

        try {
            messageIn = new ObjectInputStream(messageClient.getInputStream());
            messageOut = new ObjectOutputStream(messageClient.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        onlineUserList.setCellFactory(new UserCellFactory());
        onlineUserList.setItems(FXCollections.observableArrayList());

        groupUserList.setCellFactory(new UserCellFactory());
        groupUserList.setItems(FXCollections.observableArrayList());


        chatContentList.setCellFactory(new MessageCellFactory());
        chatContentList.setItems(FXCollections.observableArrayList());

        listenThread = new Thread(new ReceiveListener());
        listenThread.start();
        onlineCntThread = new Thread(new OnlineCntListener());
        onlineCntThread.start();
        onlineUserThread = new Thread(new OnlineUserListener());
        onlineUserThread.start();

    }

    public void showLogInWindow(){
        logInStage = new Stage();
        boolean isRegistering = false;

        AnchorPane login = new AnchorPane();
        login.setPrefSize(600,400);
        Label userNameLabel = new Label("User name:");
        Label passwordLabel = new Label("Password:");
        userNameLabel.setLayoutX(71.0);
        userNameLabel.setLayoutY(76.0);
        userNameLabel.setPrefSize(114.0,33.0);
        userNameLabel.setFont(new Font(19.0));

        passwordLabel.setLayoutX(78.0);
        passwordLabel.setLayoutY(142.0);
        passwordLabel.setPrefSize(99.0,33.0);
        passwordLabel.setFont(new Font(19.0));

        TextField nameTextField = new TextField();
        TextField passwordTextField = new PasswordField();
        nameTextField.setLayoutX(196.0);
        nameTextField.setLayoutY(74.0);
        nameTextField.setPrefSize(275,38);
        passwordTextField.setLayoutX(196.0);
        passwordTextField.setLayoutY(140.0);
        passwordTextField.setPrefSize(275,38);

        Label tipsLabel = new Label("Don't have an account?");
        tipsLabel.setPrefSize(140.0,15.0);
        tipsLabel.setLayoutX(177.0);
        tipsLabel.setLayoutY(359.0);

        Label reigisterLabel = new Label("Register here.");
        reigisterLabel.setLayoutX(317.0);
        reigisterLabel.setLayoutY(359.0);
        reigisterLabel.setPrefSize(99.0,15.0);
        reigisterLabel.setTextFill(Color.BLUE);
        reigisterLabel.setUnderline(true);
        reigisterLabel.setCursor(Cursor.HAND);

        rd = new registerTextFieldListener(reigisterLabel);
        reigisterLabel.setOnMouseClicked(rd);

        Button logInBtn = new Button("Log in");
        logInBtn.setLayoutX(243.0);
        logInBtn.setLayoutY(250.0);
        logInBtn.setPrefSize(114.0,38.0);
        logInBtn.setStyle("-fx-background-color: skyblue;");
        logInBtn.setFont(new Font(19));

        logInBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
            String userName;
            String password;
            @Override
            public void handle(MouseEvent event) {
                //todo:Check whether the password is correct or whether there are user with same name online.
                userName = nameTextField.getText();
                password = passwordTextField.getText();

                if(userName == null || password == null){
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setContentText("Name or password shouldn't be null.");
                    alert.showAndWait();
                    return;
                }

                try {
                    Socket logInSocket = new Socket("localhost",134);
                    PrintWriter out  = new PrintWriter(logInSocket.getOutputStream());

                    out.println("LOGIN");
                    out.flush();
                    out.println(userName);
                    out.flush();
                    out.println(password);
                    out.flush();

                    ObjectInputStream inputStream = new ObjectInputStream(logInSocket.getInputStream());

                    String respond =(String)inputStream.readObject();

                    if(respond.equals("Yes")){
                        Integer responds = (Integer)inputStream.readObject();
                        switch(responds){
                            //none exist in db
                            case 1:
                                break;
                            //history exist in db
                            case 2:
                                usersDialogSet = (HashMap<String, ArrayList<Message>>) inputStream.readObject();
                                chatList.getItems().addAll(usersDialogSet.keySet());
                                break;
                            case 3:
                                userGroupSet = (HashMap<String, ArrayList<String>>) inputStream.readObject();
                                break;
                            case 4:
                                usersDialogSet = (HashMap<String, ArrayList<Message>>) inputStream.readObject();
                                userGroupSet = (HashMap<String, ArrayList<String>>) inputStream.readObject();
                                chatList.getItems().addAll(usersDialogSet.keySet());
                                break;
                        }
                        username = userName;
                        if(!registerServer()){
                            Alert alert = new Alert(AlertType.ERROR);
                            alert.setContentText("Duplicated user existing.");
                            alert.showAndWait();
                            return;
                        }
                        logInSocket.close();
                        logInStage.close();
                    }else{
                        Alert alert = new Alert(AlertType.INFORMATION);
                        alert.setContentText("No such user exists or password is wrong.");
                        alert.showAndWait();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        login.getChildren().addAll(logInBtn,nameTextField,passwordTextField,tipsLabel,userNameLabel,passwordLabel,reigisterLabel);
        Scene s = new Scene(login);
        logInStage.setResizable(false);
        logInStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                if(event.getSource()==logInStage){
                    try {
                        disconnectServer();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    System.exit(0);
                }
            }
        });
        logInStage.setScene(s);
        logInStage.showAndWait();
    }

    class registerTextFieldListener implements EventHandler<MouseEvent>{
        boolean isRegistering = false;
        Label reigisterLabel = null;
        @Override
        public void handle(MouseEvent event) {
            if(isRegistering==false){
                reigisterLabel.setTextFill(Color.GRAY);
                isRegistering = true;
                //new register window
                showRegisterWindow();
            }else{
                registerStage.toFront();
            }
        }
        public registerTextFieldListener(Label reigisterLabel){
            this.reigisterLabel = reigisterLabel;
        }
    }


    //enter the register window and wait
    private void showRegisterWindow(){
        registerStage = new Stage();
        AnchorPane registerPane = new AnchorPane();
        registerPane.setPrefSize(600.0,400.0);

        Label userNameLabel = new Label("User name :");
        userNameLabel.setPrefSize(117.0,29.0);
        userNameLabel.setLayoutX(114.0);
        userNameLabel.setLayoutY(71.0);
        userNameLabel.setFont(new Font(19.0));

        Label passwordLabel = new Label("Password :");
        passwordLabel.setPrefSize(102.0,29.0);
        passwordLabel.setLayoutX(159.0);
        passwordLabel.setLayoutY(131.0);
        passwordLabel.setFont(new Font(19.0));

        Label passwordCheckLabel = new Label("Password * :");
        passwordCheckLabel.setPrefSize(117.0,29.0);
        passwordCheckLabel.setLayoutX(144.0);
        passwordCheckLabel.setLayoutY(192.0);
        passwordCheckLabel.setFont(new Font(19.0));

        TextField userNameTextField = new TextField("Enter your user name.");
        TextField passwordTextField = new PasswordField();
        TextField passwordCheckTextField = new PasswordField();

        passwordTextField.setText("Enter your password.");
        passwordCheckTextField.setText("Enter your password again.");

        userNameTextField.setPrefSize(170.0,29.0);
        passwordTextField.setPrefSize(170.0,29.0);
        passwordCheckTextField.setPrefSize(170.0,29.0);

        userNameTextField.setLayoutX(285.0);
        userNameTextField.setLayoutY(71.0);

        passwordTextField.setLayoutX(285.0);
        passwordTextField.setLayoutY(131.0);

        passwordCheckTextField.setLayoutX(285.0);
        passwordCheckTextField.setLayoutY(192.0);

        Button registerBtn = new Button("Register");
        Button cancelBtn = new Button("Cancel");
        registerBtn.setFont(new Font(19.0));
        registerBtn.setCursor(Cursor.HAND);
        cancelBtn.setFont(new Font(19.0));
        cancelBtn.setCursor(Cursor.HAND);

        registerBtn.setLayoutX(159.0);
        registerBtn.setLayoutY(283.0);
        registerBtn.setPrefSize(102.0,29.0);
        registerBtn.setStyle("-fx-background-color: SKYBLUE");
        registerBtn.setFont(new Font(19.0));

        cancelBtn.setLayoutX(330.0);
        cancelBtn.setLayoutY(283.0);
        cancelBtn.setPrefSize(102.0,29.0);
        cancelBtn.setFont(new Font(19.0));

        cancelBtn.setOnMouseClicked(event -> {
            registerStage.close();
            rd.isRegistering = false;
        });

        Label duplicatedPWD = new Label("You enter two different passwords.");
        duplicatedPWD.setPrefSize(206.0,15.0);
        duplicatedPWD.setTextFill(Color.RED);
        duplicatedPWD.setLayoutX(144.0);
        duplicatedPWD.setLayoutY(232.0);
        duplicatedPWD.setVisible(false);

        Label duplicatedUser = new Label("The user name you enter has been taken.");
        duplicatedUser.setPrefSize(206.0,15.0);
        duplicatedUser.setTextFill(Color.RED);
        duplicatedUser.setLayoutX(144.0);
        duplicatedUser.setLayoutY(232.0);
        duplicatedUser.setVisible(false);

        registerBtn.setOnMouseClicked(event -> {
            String userName = userNameTextField.getText();
            String pwd = passwordTextField.getText();
            if(pwd.equals(passwordCheckLabel.getText())){
                duplicatedPWD.setVisible(true);
                return;
            }
            //todo:Examine the SQL in server and register a new user account.
            out.println("REGISTERDB");
            out.flush();

            out.println(userName);
            out.flush();

            out.println(pwd);
            out.flush();

            String respond = in.next();
            if(respond.equals("OK")){
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Register successfully!");
                alert.setContentText("You have successfully registered user "+userName);
                alert.showAndWait();

                rd.isRegistering = false;
                registerStage.close();
            }else{
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Register failed.");
                alert.setContentText("User exists.");
                alert.showAndWait();
            }
        });

        registerStage.setOnCloseRequest(event -> rd.isRegistering = false);

        Scene scene = new Scene(registerPane);
        registerPane.getChildren().addAll(userNameLabel,passwordLabel,passwordCheckLabel,userNameTextField,passwordTextField,passwordCheckTextField,registerBtn,cancelBtn,duplicatedUser,duplicatedPWD);
        registerStage.setScene(scene);
        registerStage.showAndWait();
    }


    //set the alter rule of chat list in main UI
    private void initialChatList(){
        chatList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if(currentChatMate!=null) {
                        usersDialogSet.get(currentChatMate).clear();
                        usersDialogSet.get(currentChatMate).addAll(chatContentList.getItems());
                    }
                    if(newValue != null){
                        if(userGroupSet.containsKey(newValue)){
                            groupUserList.getItems().clear();
                            groupUserList.getItems().setAll(userGroupSet.get(newValue));
                        }else{
                            groupUserList.getItems().clear();
                        }
                    }
                    chatContentList.getItems().clear();
                    chatContentList.getItems().setAll(usersDialogSet.get(newValue));
                    currentChatMate = newValue;
                    Location.setText(currentChatMate);
                }
        );
    }

    private void saveData(){

    }


    //set the onCloseQuest of the main stage
    void initialStage(Stage stage){
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent){
                //todo: savedata

                if(windowEvent.getSource() == stage){
                    try {
                        listenThread.interrupt();
                        quitServer();
                        System.exit(0);
                    } catch (IOException e) {
                    }
                }
            }
        });
    }


    //initialize the in and out port of local socket
    private void registerSocket(){
        try {
            client = new Socket("localhost",134);
            messageClient = new Socket("localhost",134);
            in = new Scanner(client.getInputStream());
            out = new PrintWriter(client.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void quitServer() throws IOException {
        out.println("QUIT");
        out.flush();
        out.close();
        in.close();
        messageOut.writeObject(new Message(System.currentTimeMillis(),"SYSTEM","SERVER","CLOSE"));
        messageOut.flush();
        onlineCntClient.close();
    }

    private void disconnectServer() throws IOException{
        out.println("DISCONNECT");
        out.flush();
        out.close();
        in.close();
        client.close();
    }

    private void registerMessageServer(){
        try {
            PrintStream messageOut = new PrintStream(messageClient.getOutputStream());
            messageOut.println("MESSAGEREGISTER "+username);
            messageOut.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private boolean registerServer(){
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
    private ArrayList<String> getServerUsers() throws SocketException{
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
    private void createPrivateChat() {
        AtomicReference<String> user = new AtomicReference<>();

        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();
        userSel.setItems(FXCollections.observableArrayList());
        userSel.setCellFactory(new UserCellFactory());

        // FIXME: get the user list from server, the current user's name should be filtered out
        try{
        ArrayList<String>users = getServerUsers();
        userSel.getItems().setAll(users);
        }catch (NoSuchElementException | SocketException e){
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("No available chat mate");
            alert.setContentText("Select an available from the left pane to chat with. If there's no content, generate a new chat from menu bar.");
            alert.showAndWait();
            return;
        }


        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            user.set(userSel.getSelectionModel().getSelectedItem());
            stage.close();

            //Add new a user chatting item to the left pane
            if(usersDialogSet.size()!=0){
                if(currentChatMate != null){
                    usersDialogSet.get(currentChatMate).clear();
                    usersDialogSet.get(currentChatMate).addAll(chatContentList.getItems());
                    chatContentList.getItems().clear();
                }
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
    private void createGroupChat() {
        try {
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
                    if (checkBox.isSelected()) {
                        selected.add(checkBox.getText());
                    }
                }

                if (selected.size() == 1) {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setContentText("Please choose at least two user to create a group chat.");
                    alert.showAndWait();
                    return;
                }
                selected.add(username);
                Collections.sort(selected);
                String groupChatMate = null;
                if (selected.size() > 3) {
                    groupChatMate = String.format("%s, %s, %s...(%d)", selected.get(0),
                        selected.get(1), selected.get(2), selected.size());
                } else {
                    groupChatMate = String.format("%s, %s, %s(3)", selected.get(0), selected.get(1),
                        selected.get(2));
                }
                int duplicated = 1;
                String finalGroupChatMate = groupChatMate + "";
                while (userGroupSet.containsKey(groupChatMate)) {
                    finalGroupChatMate = String.format("%s[%d]", groupChatMate, duplicated);
                    duplicated++;
                }

                userGroupSet.put(finalGroupChatMate, selected);

                if (usersDialogSet.size() != 0) {
                    usersDialogSet.get(currentChatMate).clear();
                    usersDialogSet.get(currentChatMate).addAll(chatContentList.getItems());
                    chatContentList.getItems().clear();
                }

                chatList.getItems().add(finalGroupChatMate);
                usersDialogSet.put(finalGroupChatMate, new ArrayList<>(5));
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
        }catch (SocketException | NoSuchElementException e){
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Server closed");
            alert.setContentText("You can't send message to closed server.");
            alert.showAndWait();
            return;
        }
    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    private void doSendMessage() {
        String text = inputArea.getText();

        if(text.equals("")){
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Empty chat detected");
            alert.setContentText("You can't enter an empty chat.");
            alert.showAndWait();
            return;
        }

        if(currentChatMate==null){
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("No available chat mate");
            alert.setContentText("Select an available from the left pane to chat with. If there's no content, generate a new chat from menu bar.");
            alert.showAndWait();
            return;
        }

        if(userGroupSet.containsKey(currentChatMate)){
            try {
            ArrayList<String> usersSendTo = userGroupSet.get(currentChatMate);

            Message message = new Message(System.currentTimeMillis(),username,currentChatMate,text);
            message.setIsGroupMessage();
            message.setGroupMembers(usersSendTo);

            usersDialogSet.get(currentChatMate).add(message);
            chatContentList.getItems().add(message);
            inputArea.clear();


            messageOut.writeObject(message);
            messageOut.flush();
            }  catch (SocketException e){
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Server closed");
                alert.setContentText("You can't send message to closed server.");
                alert.showAndWait();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        //send to
        try {
            Message message = new Message(System.currentTimeMillis(), username, currentChatMate,
                text);
            usersDialogSet.get(currentChatMate).add(message);
            chatContentList.getItems().add(message);
            inputArea.clear();

            if (!getServerUsers().contains(currentChatMate)) {
                Notification.setText(
                    "The user " + currentChatMate + " you are chatting with is offline now.");
                Timeline autoClear = new Timeline(
                    new KeyFrame(Duration.seconds(5), e -> Notification.clear()));
                autoClear.setCycleCount(1);
                autoClear.play();
                return;
            }

            messageOut.writeObject(message);
            messageOut.flush();
        }catch (NoSuchElementException | SocketException e) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Server closed");
            alert.setContentText("You can't send message to closed server.");
            alert.showAndWait();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    class OnlineUserListener implements Runnable{
        @Override
        public void run() {
            try {
            Socket userSocket = new Socket("localhost",134);
            PrintWriter out = new PrintWriter(userSocket.getOutputStream());
            Scanner input = new Scanner(userSocket.getInputStream());
            while(true){
                out.println("GET");
                out.flush();

                ArrayList<String> output = new ArrayList<>(5);
                String nextLine = null;
                while((nextLine = input.next())!=null){
                    if(nextLine.equals("OVER")){
                        break;
                    }
                    output.add(nextLine);
                }
                Platform.runLater(()->{
                    onlineUserList.getItems().clear();
                    onlineUserList.getItems().addAll(output);
                });
                Thread.sleep(100);
            }
            }catch(NoSuchElementException e){
                return;
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    class OnlineCntListener implements Runnable{
        @Override
        public void run() {
            try {
                Thread.sleep(1);
                onlineCntClient = new Socket("localhost",134);
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
                Platform.runLater(()->{
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setContentText("Server closed detected. Quit the application or wait for our fixing.");
                    alert.showAndWait();
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e){
                try {
                    onlineCntClient.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }
    class ReceiveListener implements Runnable{
        @Override
        public void run() throws IllegalStateException {
            try {
                while(!Thread.currentThread().isInterrupted()){
                    Thread.sleep(1);
                    Message receivedMessage = (Message) messageIn.readObject();
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
                        if(!userGroupSet.containsKey(to)){
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
                    Label timeLabel = new Label();

                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");

                    timeLabel.setText(formatter.format(new Date(msg.getTimestamp())));
                    timeLabel.setFont(new Font(8.0));
                    timeLabel.setUnderline(true);
                    timeLabel.setStyle("-fx-border-color: red; -fx-border-width: 1px");

                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

                    if (username.equals(msg.getSentBy())) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel,timeLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(timeLabel,nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }
}
