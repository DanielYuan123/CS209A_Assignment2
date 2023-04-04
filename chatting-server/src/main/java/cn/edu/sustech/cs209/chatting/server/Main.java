package cn.edu.sustech.cs209.chatting.server;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

public class Main {
    static UserSet userSet;

    public static void main(String[] args) throws IOException {
        System.out.println("Starting server");
        ServerSocket server = new ServerSocket(135);
        userSet = new UserSet();

        while(true){
            Socket socket = server.accept();
            handle han = new handle(userSet,socket);
            Thread thread = new Thread(han);
            thread.start();
        }
    }
}

class handle implements Runnable{
    UserSet userSet;
    Socket clientSocket;
    Scanner in;
    PrintWriter out;
    String userName;
    public handle(UserSet userset,Socket clientsocket) throws IOException {
        userSet = userset;
        clientSocket = clientsocket;
        in = new Scanner(clientSocket.getInputStream());
        out = new PrintWriter(clientSocket.getOutputStream());
    }

    public void execute(String command){
        switch(command){
            case "REGISTER":
                userName = in.next();
                if(userSet.userNameSet.contains(userName)){
                    out.println("DENY");
                    out.flush();
                }else{
                    userSet.addUser(userName,clientSocket);
                    userSet.userNameSet.add(userName);
                    out.println("OK");
                    out.flush();
                }
                break;
            case "CONNECT":

                break;
            case "GET":
                System.out.println(1);
                ArrayList<String> userList = userSet.getUserNameSet();
                for (int i = 0; i < userList.size(); i++) {
                    out.println(userList.get(i));
                    out.flush();
                }
                out.println();
                out.flush();
                System.out.println(2);
                break;
        }
    }
    @Override
    public void run() {
        try{
            while(true){
                String command = in.next();
                if(command.equals("QUIT")){
                    userSet.userQuit(userName);
                    return;
                }
                execute(command);
            }
        } finally {
            in.close();
            out.close();
            userSet.userQuit(userName);
        }
    }
}
class UserSet{
    ArrayList<String>userNameSet = new ArrayList<>(5);
    HashMap<String,Socket>clientServer = new HashMap<>();
    public UserSet() {}
    public synchronized void addUser(String userName, Socket client){
        userNameSet.add(userName);
        clientServer.put(userName,client);
    }

    public synchronized Socket getClient(String userName){
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
