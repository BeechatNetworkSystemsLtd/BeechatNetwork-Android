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
    short partNumber = 0;
    short totalNumber = 0;
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
        return 30;
    }

    public Packet(Type type, short num, short totalNumber, byte[] data, Blake3 hasher) {
        setData(type, num, totalNumber, data, hasher);
    }

    public void setData(Type type, short num, short totalNumber, byte[] data, Blake3 hasher) {
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

        this.data = new byte[data.length + 7];
        this.data[0] = this.type;
        this.data[3] = (byte)(this.partNumber >> 8);
        this.data[4] = (byte)(this.partNumber & 0xFF);
        this.data[5] = (byte)(this.totalNumber >> 8);
        this.data[6] = (byte)(this.totalNumber & 0xFF);
        System.arraycopy(data, 0, this.data, 7, data.length);
        this.hasher.update(data, data.length);
        byte[] temp = this.hasher.finalize(2);

        this.data[1] = temp[0];
        this.data[2] = temp[1];
        correct = true;
    }


    public void setRaw(byte[] data) {
        int len = data.length;
        correct = false;
        if (len < 8 || hasher == null) {
            return;
        }

        this.hasher.clear();

        type = data[0];
        hash[0] = data[1];
        hash[1] = data[2];
        partNumber = (short)(((data[3] & 0xFF) << 8)
                           | ((data[4] & 0xFF) << 0));
        totalNumber = (short)(((data[5] & 0xFF) << 8)
                            | ((data[6] & 0xFF) << 0));
        this.data = new byte[len - 7];
        System.arraycopy(data, 7, this.data, 0, len - 7);
        hasher.update(this.data, len - 7);
        if (!Arrays.equals(hasher.finalize(2), hash)) {
            return;
        }

        correct = true;
    }

    public boolean isCorrect() {
        return correct;
    }

    public short getPartNumber() {
        return partNumber;
    }

    public short getTotalNumber() {
        return totalNumber;
    }

    public byte[] getData() {
        return this.data;
    }

    public Type getType() {
        return Type.valueOf(this.type);
    }
}
