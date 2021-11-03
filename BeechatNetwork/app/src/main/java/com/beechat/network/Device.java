package com.beechat.network;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

public class Device {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public String xbee_device_number;

    public Device() {}

    public Device(String xbee_device_number) {
        this.xbee_device_number = xbee_device_number;
    }
    public Device(int id, String xbee_device_number) {
        this.id = id;
        this.xbee_device_number = xbee_device_number;
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
}