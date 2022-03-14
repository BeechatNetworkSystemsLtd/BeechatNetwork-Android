package com.beechat.network;

import java.util.*;

/**
 *  The Packet class of a part of Message.
 **/
public class Packet {

    public enum Type {
        NONE((byte)0),
        MESSAGE_DATA((byte)1),
        FILE_DATA((byte)2),
        DP_KEY((byte)3),
        SIGNED_HASH((byte)4),
        KP_KEY((byte)5),
        CHIPHER((byte)6),
        INFO((byte)7),
        FILE_NAME_DATA((byte)8);

        private byte value;
        private static Map map = new HashMap<>();

        private Type(byte value) {
            this.value = value;
        }
        static {
            for (Type pageType : Type.values()) {
                map.put(pageType.value, pageType);
            }
        }

        public static Type valueOf(byte pageType) {
            return (Type) map.get(pageType);
        }

        public byte getValue() {
            return value;
        }
    }

    boolean correct = false;
    byte type;
    byte[] hash = new byte[2];
    int partNumber = 0;
    int totalNumber = 0;
    byte[] data;
    Blake3 hasher;

    public Packet(Blake3 hasher) {
        this.hasher = hasher;
    }

    public Packet(byte[] data, Blake3 hasher) {
        this.hasher = hasher;
        setRaw(data);
    }

    public static int getMaxLen() {
        return 24;
    }

    public Packet(Type type, int num, int totalNumber, byte[] data, Blake3 hasher) {
        setData(type, num, totalNumber, data, hasher);
    }

    public void setData(Type type, int num, int totalNumber, byte[] data, Blake3 hasher) {
        this.hasher = hasher;
        if (type == Packet.Type.NONE) {
            return;
        }
        this.type = type.getValue();
        this.partNumber = num;
        this.totalNumber = totalNumber;

        if (this.hasher == null || data == null) {
            return;
        }
        this.hasher.clear();

        this.data = new byte[data.length + 11];
        this.data[0] = this.type;
        this.data[3] = (byte)(this.partNumber >> 24);
        this.data[4] = (byte)(this.partNumber >> 16);
        this.data[5] = (byte)(this.partNumber >> 8);
        this.data[6] = (byte)(this.partNumber & 0xFF);
        this.data[7] = (byte)(this.totalNumber >> 24);
        this.data[8] = (byte)(this.totalNumber >> 16);
        this.data[9] = (byte)(this.totalNumber >> 8);
        this.data[10] = (byte)(this.totalNumber & 0xFF);

        System.arraycopy(data, 0, this.data, 11, data.length);
        this.hasher.update(data, data.length);
        byte[] temp = this.hasher.finalize(2);

        this.data[1] = temp[0];
        this.data[2] = temp[1];
        correct = true;
    }


    public void setRaw(byte[] data) {
        int len = data.length;
        correct = false;
        if (len < 12 || hasher == null) {
            return;
        }

        this.hasher.clear();

        type = data[0];
        hash[0] = data[1];
        hash[1] = data[2];
        partNumber = (int)(((data[3] & 0xFF) << 24)
                         | ((data[4] & 0xFF) << 16)
                         | ((data[5] & 0xFF) << 8)
                         | ((data[6] & 0xFF) << 0));
        totalNumber = (int)(((data[7] & 0xFF) << 24)
                          | ((data[8] & 0xFF) << 16)
                          | ((data[9] & 0xFF) << 8)
                          | ((data[10] & 0xFF) << 0));
        this.data = new byte[len - 11];
        System.arraycopy(data, 11, this.data, 0, len - 11);
        hasher.update(this.data, len - 11);
        if (!Arrays.equals(hasher.finalize(2), hash)) {
            return;
        }

        correct = true;
    }

    public boolean isCorrect() {
        return correct;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public int getTotalNumber() {
        return totalNumber;
    }

    public byte[] getData() {
        return this.data;
    }

    public Type getType() {
        return Type.valueOf(this.type);
    }
}
