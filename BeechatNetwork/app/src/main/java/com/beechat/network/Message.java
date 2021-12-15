package com.beechat.network;

import androidx.room.PrimaryKey;

/***
 *  --- Message ---
 *  The Message class in DB.
 ***/
public class Message {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String senderId;
    public String xbee_device_number_sender;
    public String receiverId;
    public String xbee_device_number_receiver;
    public String content;

    public Message() {}

    public Message(String senderId, String xbee_device_number_sender, String receiverId, String xbee_device_number_receiver, String content) {
        this.senderId = senderId;
        this.xbee_device_number_sender = xbee_device_number_sender;
        this.receiverId = receiverId;
        this.xbee_device_number_receiver = xbee_device_number_receiver;
        this.content = content;
    }

    public Message(int id, String senderId, String xbee_device_number_sender, String receiverId, String xbee_device_number_receiver, String content) {
        this.id = id;
        this.senderId = senderId;
        this.xbee_device_number_sender = xbee_device_number_sender;
        this.receiverId = receiverId;
        this.xbee_device_number_receiver = xbee_device_number_receiver;
        this.content = content;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSenderId() {
        return this.senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getXbeeSender() {
        return this.xbee_device_number_sender;
    }

    public void setXbeeSender(String xbee_device_number_sender) {
        this.xbee_device_number_sender = xbee_device_number_sender;
    }

    public String getReceiverId() {
        return this.receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getXbeeReceiver() {
        return this.xbee_device_number_receiver;
    }

    public void setXbeeReceiver(String xbee_device_number_receiver) {
        this.xbee_device_number_receiver = xbee_device_number_receiver;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}