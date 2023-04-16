package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;

import javax.swing.plaf.nimbus.State;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
public class DBConnector {
    private static Connection connection;
    private static Statement statement;
    private static PreparedStatement preparedStatement;
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
    public static void main(String[] args) {
        connection = null;
        try {
            // 加载 SQLite JDBC 驱动程序
            Class.forName("org.sqlite.JDBC");
            // 连接到 SQLite 数据库
            connection = DriverManager.getConnection("jdbc:sqlite:chatting-server/src/main/resources/db/test.db");
            System.out.println("Connection established.");

            ArrayList<Message> testArrayList = new ArrayList<>();
            testArrayList.add(new Message(System.currentTimeMillis(),"F","a","Hello"));
            testArrayList.add(new Message(System.currentTimeMillis(),"D","B","hi"));

            storeMessage(testArrayList,"a");

            ArrayList<Message> out = readMessage("a");

            for (Message message : out) {
                System.out.println("Message from "+message.getSentBy() + " to "+message.getSendTo());
            }

        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found.");
        } catch (SQLException e) {
            System.err.println("Error connecting to SQLite database: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    public boolean logInUser(String userName, String password) throws SQLException {
        String sql = "SELECT * FROM user WHERE name = ?;";
        preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1,userName);
        resultSet = preparedStatement.executeQuery();
        if(resultSet.getFetchSize()==0){
            return false;
        }
        if (resultSet.next()) {
            String correctPW = resultSet.getString("password");
            return password.equals(correctPW);
        }
        return false;
    }

    public boolean registerUser(String userName, String password) throws SQLException {
        String sql = "SELECT * FROM user WHERE name = ?;";
        preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1,userName);
        resultSet = preparedStatement.executeQuery();
        if(resultSet.getFetchSize() != 0){
            return false;
        }else{
            String sql1 = "INSERT INTO user VALUES (?,?,null);";
            preparedStatement = connection.prepareStatement(sql1);
            preparedStatement.setString(1,userName);
            preparedStatement.setString(2,password);
            preparedStatement.executeQuery();
            return true;
        }
    }

    public static void storeMessage(ArrayList<Message>messages,String userName) throws SQLException, IOException {
        String sql = "UPDATE user SET chat_history = ? WHERE name = ?;";
        byte[] objectBytes = serialize(messages);
        preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setBytes(1,objectBytes);
        preparedStatement.setString(2,userName);
        preparedStatement.executeUpdate();
    }

    public static ArrayList<Message> readMessage(String userName) throws SQLException, IOException, ClassNotFoundException {
        String sql = "SELECT chat_history FROM user WHERE name = ?;";
        preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1,userName);

        resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) {
            byte[] objectBytes = resultSet.getBytes("chat_history");
            ArrayList<Message> messages = (ArrayList<Message>) deserialize(objectBytes);
            return messages;
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

