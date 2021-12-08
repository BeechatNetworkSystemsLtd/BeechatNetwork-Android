package com.beechat.network;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class Contact {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public String user_id;
    public String xbee_device_number;
    public String name;

    public Contact() {}

    public Contact(String user_id, String xbee_device_number, String name) {
        this.user_id = user_id;
        this.xbee_device_number = xbee_device_number;
        this.name = name;
    }

    public Contact(int id,String user_id, String xbee_device_number, String name) {
        this.id = id;
        this.user_id = user_id;
        this.xbee_device_number = xbee_device_number;
        this.name = name;

    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserId() {
        return this.user_id;
    }

    public void setUserId(String user_id) {
        this.user_id = user_id;
    }

    public String getXbeeDeviceNumber() {
        return this.xbee_device_number;
    }

    public void setXbeeDeviceNumber(String xbee_device_number) {
        this.xbee_device_number = xbee_device_number;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}