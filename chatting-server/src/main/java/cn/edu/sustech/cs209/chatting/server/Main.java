package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.SQLException;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {
    static UserSet userSet;
    static DBConnector connector;

    public static void main(String[] args) throws IOException {
        System.out.println("Starting server");
        ServerSocket server = new ServerSocket(134);
        userSet = new UserSet();
        connector = new DBConnector();

        while(true){
            Socket socket = server.accept();
            handle han = new handle(userSet,socket,connector);
            Thread thread = new Thread(han);
            thread.start();
        }
    }
}

class handle implements Runnable{
    DBConnector connector;
    UserSet userSet;
    Socket clientSocket;
    Scanner in;
    PrintWriter out;

    ObjectInputStream obIn;

    ObjectOutputStream obOut;
    String userName;
    public handle(UserSet userset,Socket clientsocket,DBConnector connect) throws IOException {
        userSet = userset;
        clientSocket = clientsocket;
        connector = connect;
        in = new Scanner(clientSocket.getInputStream());
        out = new PrintWriter(clientSocket.getOutputStream());
    }

    public void execute(String command){
        switch(command){
            case "GRASP":
                    try {
                        PrintStream onlineCntIn = new PrintStream(clientSocket.getOutputStream());
                        while (!clientSocket.isClosed()) {
                            onlineCntIn.println(userSet.userNameSet.size());
                            onlineCntIn.flush();
//                            System.out.println("Working");
                            Thread.sleep(20);
                        }
                    } catch(SocketException e){
                    } catch (IOException | InterruptedException e) {
                    }
                break;
            case "REGISTER":
                userName = in.next();
                if(userSet.userNameSet.contains(userName)){
                    out.println("DENY");
                    out.flush();
                }else{
                    userSet.addUser(userName,clientSocket);
                    out.println("OK");
                    out.flush();
                }
                break;
            case "MESSAGEREGISTER":
                userName = in.next();
                userSet.addUserMessage(userName,clientSocket);
                ObjectInputStream messageInputStream = null;
                try {
                    messageInputStream = new ObjectInputStream(clientSocket.getInputStream());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                while(true){
                    try {
                        String to = null;
                        Message serverMessage = null;
                        while(to == null){
                            serverMessage = (Message)messageInputStream.readObject();
                            to = serverMessage.getSendTo();
                        }
                        if(serverMessage.getSentBy().equals("SYSTEM")&&serverMessage.getSendTo().equals("SERVER")&&serverMessage.getData().equals("CLOSE")){
                            System.out.println(1);
                            Thread.currentThread().interrupt();
                            return;
                        }
                        if(serverMessage.isGroupMessage()){
                            ArrayList<String> usersSendTo = serverMessage.getGroupMembers();
                            ObjectOutputStream targetOutput;
                            for (int i = 0; i < usersSendTo.size(); i++) {
                                if(!usersSendTo.get(i).equals(userName)&&userSet.clientServer.containsKey(usersSendTo.get(i))){
                                    targetOutput = userSet.getInput(usersSendTo.get(i));
                                    targetOutput.writeObject(serverMessage);
                                    targetOutput.flush();
                                }
                            }
                            continue;
                        }
                        ObjectOutputStream targetOutput;
                        targetOutput = userSet.getInput(to);
                        targetOutput.writeObject(serverMessage);
                        targetOutput.flush();
                    } catch (IOException | ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            case "GET":

                ArrayList<String> userList = userSet.getUserNameSet();
                System.out.println(userName +" access the server now. Users now in the server:");
                for (String s : userList) {
                    System.out.println(s);
                }
                for (int i = 0; i < userList.size(); i++) {
                    out.println(userList.get(i));
                    out.flush();
                }
                out.println("OVER");
                out.flush();

                break;
//            case "LOGIN":
//                String name = in.next();
//                String password = in.next();
//
//                try {
//                    int result = -1;
//                    if((result = connector.logInUser(name,password)) != 0){
//                        out.println("Yes");
//                        out.flush();
//
//                        switch(result){
//                            //none
//                            case 1:
//                                out.println(1);
//                                out.flush();
//                                System.out.println("sent");
//                                break;
//                            //chat
//                            case 2:
//                                out.println(2);
//                                out.flush();
//
//                                obOut.writeObject(connector.readDialog(name));
//                                obOut.flush();
//                                break;
//                            //group
//                            case 3:
//                                out.println(3);
//                                out.flush();
//                                obOut.writeObject(connector.readGroup(name));
//                                obOut.flush();
//                                break;
//                            //both
//                            case 4:
//                                out.println(4);
//                                out.flush();
//                                obOut.writeObject(connector.readDialog(name));
//                                obOut.flush();
//
//                                obOut.writeObject(connector.readGroup(name));
//                                obOut.flush();
//                                break;
//                        }
//                    }else{
//                        out.println("No");
//                        out.flush();
//                    }
//                } catch (SQLException e) {
//                    throw new RuntimeException(e);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                } catch (ClassNotFoundException e) {
//                    throw new RuntimeException(e);
//                }
//                break;
        }
    }
    @Override
    public void run() {
        try{
            while(!Thread.currentThread().isInterrupted()){
                String command = in.next();
                if(command.equals("QUIT")){
                    userSet.userQuit(userName);
                    break;
                }
                if(command.equals("DISCONNECT")){
                    break;
                }
                execute(command);
            }
        } catch (NoSuchElementException e){
            
        }finally {
            System.out.println(2);
            try {
                clientSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            in.close();
            out.close();
            userSet.userQuit(userName);
        }
    }
}
class UserSet{
    ArrayList<String>userNameSet = new ArrayList<>(5);
    HashMap<String,ArrayList<Socket>>clientServer = new HashMap<>();

    HashMap<String,ObjectOutputStream> userInput = new HashMap<>();
    public UserSet() {}

    public synchronized ObjectOutputStream getInput(String userName){
        return userInput.get(userName);
    }
    public synchronized void addUser(String userName, Socket client){
        userNameSet.add(userName);
        clientServer.put(userName,new ArrayList<>());
        clientServer.get(userName).add(client);
    }

    public synchronized void addUserMessage(String userName, Socket messageSocket){
        try {
            clientServer.get(userName).add(messageSocket);
            userInput.put(userName,new ObjectOutputStream(messageSocket.getOutputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized ArrayList<Socket> getClient(String userName){
        return clientServer.get(userName);
    }

    public synchronized void userQuit(String userName){
        userNameSet.remove(userName);
        clientServer.remove(userName);
    }

    public synchronized ArrayList<String> getUserNameSet(){
        return userNameSet;
    }


}
