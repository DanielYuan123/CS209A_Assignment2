package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable {

  private Long timestamp;

  private String sentBy;

  private String sendTo;

  private String data;

  private boolean isFile = false;

  private boolean isGroupMessage = false;

  private ArrayList<String> groupMembers = new ArrayList<>();

  public Message(Long timestamp, String sentBy, String sendTo, String data) {
    this.timestamp = timestamp;
    this.sentBy = sentBy;
    this.sendTo = sendTo;
    this.data = data;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public String getSentBy() {
    return sentBy;
  }

  public String getSendTo() {
    return sendTo;
  }

  public String getData() {
    return data;
  }

  public boolean isGroupMessage() {
    return isGroupMessage;
  }

  public void setIsGroupMessage() {
    this.isGroupMessage = true;
  }

  public void setGroupMembers(ArrayList<String> groupMembers) {
    this.groupMembers = groupMembers;
  }

  public ArrayList<String> getGroupMembers() {
    return groupMembers;
  }

  public void setIsFile(boolean a) {
    this.isFile = a;
  }

  public boolean getIsFile() {
    return this.isFile;
  }
}
