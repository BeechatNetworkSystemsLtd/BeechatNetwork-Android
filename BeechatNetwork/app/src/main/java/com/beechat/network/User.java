package com.beechat.network;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class User {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public String xbee_device_number;
    public String name;

    public User() {}

    public User(String xbee_device_number, String name) {
        this.xbee_device_number = xbee_device_number;
        this.name = name;
    }

    public User(String xbee_device_number) {
        this.xbee_device_number = xbee_device_number;
    }

    public User(int id, String xbee_device_number, String name) {
        this.id = id;
        this.xbee_device_number = xbee_device_number;
        this.name = name;

    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
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