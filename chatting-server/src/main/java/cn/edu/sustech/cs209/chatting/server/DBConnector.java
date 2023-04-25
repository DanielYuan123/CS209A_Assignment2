package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;

import javax.swing.plaf.nimbus.State;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class DBConnector {
    private static Connection connection = null;
    private static Statement statement = null;
    private static PreparedStatement preparedStatement = null;
    private static ResultSet resultSet = null;

    public DBConnector(){
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:chatting-server/src/main/resources/db/test.db");
            System.out.println("Connection established.");

        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
        // 连接到 SQLite 数据库
    }
    public static void main(String[] args) throws SQLException {
        DBConnector connector = new DBConnector();
        connector.registerUser("a","asd");

        if(connector.logInUser("a","asd") == 1){
            System.out.println("User exists.");
        }else{
            System.out.println("Register Successfully.");
        }
//        try {
//            // 加载 SQLite JDBC 驱动程序
////            Class.forName("org.sqlite.JDBC");
////            // 连接到 SQLite 数据库
////            connection = DriverManager.getConnection("jdbc:sqlite:chatting-server/src/main/resources/db/test.db");
////            System.out.println("Connection established.");
//
//        }
//        catch (ClassNotFoundException e) {
//            System.err.println("SQLite JDBC driver not found.");
//        }
//        catch (SQLException e) {
//            System.err.println("Error connecting to SQLite database: " + e.getMessage());
//        } finally {
//            try {
//                if (connection != null) {
//                    connection.close();
//                }
//            } catch (SQLException e) {
//                System.err.println("Error closing connection: " + e.getMessage());
//            }
//        }
    }

    public void disconnect(){
        try {
            connection.close();
            statement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public int logInUser(String userName, String password) throws SQLException {
        String sql = "SELECT * FROM user WHERE name = ?;";
        preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1,userName);
        resultSet = preparedStatement.executeQuery();
        boolean chatHistoryExist = false;
        boolean groupChatExist = false;
        if (resultSet.next()) {
            String correctPW = resultSet.getString("password");
            if(password.equals(correctPW)){
                Object chatHistory = resultSet.getBytes("chat_history");
                chatHistoryExist = !resultSet.wasNull();
                Object groupChat = resultSet.getBytes("group_chat_set");
                groupChatExist = !resultSet.wasNull();
                if(!chatHistoryExist && !groupChatExist){
                    return 1;
                } else if (chatHistoryExist && !groupChatExist) {
                    return 2;
                } else if (!chatHistoryExist && groupChatExist){
                    return 3;
                }else{
                    return 4;
                }
            }
        }
        return 0;
    }

    public boolean registerUser(String userName, String password) throws SQLException {
        String sql = "SELECT * FROM user WHERE name = ?;";
        preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1,userName);
        resultSet = preparedStatement.executeQuery();
        if(resultSet.next()){
            return false;
        }else{
            String sql1 = "INSERT INTO user VALUES (?,?,null,null);";
            preparedStatement = connection.prepareStatement(sql1);
            preparedStatement.setString(1,userName);
            preparedStatement.setString(2,password);
            preparedStatement.execute();
            return true;
        }
    }


    public void storeDialogAnd(HashMap<String,ArrayList<Message>> messages,HashMap<String, ArrayList<String>> chatGroup, String userName) throws SQLException, IOException {
        String sql = "UPDATE user SET chat_history = ?,group_chat_set = ? WHERE name = ?;";
        byte[] messageBytes = null;
        if(messages!=null){
            messageBytes = serialize(messages);
        }
        byte[] groupChatBytes = null;
        if(chatGroup!=null){
            groupChatBytes = serialize(chatGroup);
        }

        preparedStatement = connection.prepareStatement(sql);
        if(messages == null){
            preparedStatement.setNull(1,Types.BLOB);
        }else{
            preparedStatement.setBytes(1,messageBytes);
        }

        if(chatGroup == null){
            preparedStatement.setNull(2,Types.BLOB);
        }else{
            preparedStatement.setBytes(2,groupChatBytes);
        }


        preparedStatement.setString(3,userName);
        preparedStatement.executeUpdate();
    }

    public HashMap<String,ArrayList<Message>> readDialog(String userName) throws SQLException, IOException, ClassNotFoundException {
        String sql = "SELECT chat_history FROM user WHERE name = ?;";
        preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1,userName);

        resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) {
            byte[] objectBytes = resultSet.getBytes("chat_history");
            if(!resultSet.wasNull()){
                HashMap<String,ArrayList<Message>> messages = (HashMap<String,ArrayList<Message>>) deserialize(objectBytes);
                return messages;
            }else{
                return null;
            }
        } else {
            System.out.println("No person found with name 'Alice'.");
            return null;
        }
    }

    public HashMap<String,ArrayList<String>> readGroup(String userName) throws SQLException, IOException, ClassNotFoundException {
        String sql = "SELECT group_chat_set FROM user WHERE name = ?;";
        preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1,userName);

        resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) {
            byte[] objectBytes = resultSet.getBytes("group_chat_set");
            if(!resultSet.wasNull()){
                HashMap<String,ArrayList<String>> groups = (HashMap<String,ArrayList<String>>) deserialize(objectBytes);
                return groups;
            }else{
                return null;
            }
        } else {
            System.out.println("No person found with name 'Alice'.");
            return null;
        }
    }

    public static byte[] serialize(Serializable object) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(object);
        oos.close();
        return baos.toByteArray();
    }

    public static Object deserialize(byte[] objectBytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(objectBytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Object object = ois.readObject();
        ois.close();
        return object;
    }

}

