package com.beechat.network;

import java.util.*;
import com.digi.xbee.api.android.XBeeDevice;
import com.digi.xbee.api.RemoteXBeeDevice;
import com.digi.xbee.api.exceptions.XBeeException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;


/**
 *  The Message class of a message.
 **/
public class Message extends ArrayList<Packet> {
    Packet.Type type = Packet.Type.NONE;
    public int partNumber = -1;
    public int totalNumber = -1;
    boolean ready = false;
    byte[] result = null;

    public Message() {
        super();
    }

    public Message(Packet.Type t) {
        super();
        type = t;
        setData(new byte[1]);
    }

    public Message(Packet.Type t, byte[] data) {
        super();
        type = t;
        setData(data);
    }

    public void Clear() {
        type = Packet.Type.NONE;
        partNumber = -1;
        totalNumber = -1;
        ready = false;
        result = null;
        clear();
    }

    public boolean Add(Packet obj) {
        Packet e = obj;
        if (!e.isCorrect()) {
            return false;
        }
        Packet.Type temp = e.getType();
        if (temp != this.type && this.type != Packet.Type.NONE ) {
            return false;
        }
        this.type = temp;
        int packetNum = e.getPartNumber();
        if (packetNum != partNumber + 1) {
            return false;
        }
        partNumber++;
        int packetTotal = e.getTotalNumber();
        if (packetTotal < 1 || (packetTotal != totalNumber && totalNumber != -1)) {
            return false;
        }
        totalNumber = packetTotal;
        if (totalNumber == partNumber + 1) {
            ready = true;
            int len = 0;
            add(e);
            for (Packet pack : (ArrayList<Packet>)this) {
                len += pack.getData().length;
            }

            result = new byte[len];

            int pos = 0;
            for (int i = 0; i < size(); i++) {
                Packet pack = get(i);
                byte[] tempb = pack.getData();
                System.arraycopy(tempb, 0, result, pos, tempb.length);
                pos += tempb.length;
            }
            return true;
        }
        return add(e);
    }

    public void send(XBeeDevice dev, RemoteXBeeDevice remote, Blake3 hasher) throws XBeeException {
        int packetRemain = result.length % Packet.getMaxLen();
        int packetCount = result.length / Packet.getMaxLen() + (packetRemain != 0 ? 1 : 0);
        byte[] bs = new byte[Packet.getMaxLen()];

        for (int i = 0; i < packetCount; i++) {
            packetRemain = result.length - i * Packet.getMaxLen();
            if (packetRemain < Packet.getMaxLen()) {
                bs = new byte[packetRemain];
            } else {
                packetRemain = Packet.getMaxLen();
            }
            System.arraycopy(result, i * Packet.getMaxLen(), bs, 0, packetRemain);
            byte[] toSend = new Packet(
                this.type
              , (int)i
              , (int)(packetCount)
              , bs
              , hasher
            ).getData();

            if (remote != null) {
                dev.sendData(remote, toSend);
            } else {
                dev.sendBroadcastData(toSend);
            }
        }
    }

    public boolean isReady() {
        return this.ready;
    }

    public byte[] getData() {
        return result;
    }

    public void setData(byte[] data) {
        result = data;
        ready = true;
        totalNumber = (int)(data.length / Packet.getMaxLen()
                    + (data.length % Packet.getMaxLen() != 0 ? 1 : 0))
        ;
        partNumber = (int)(totalNumber - 1);
    }

    public Packet.Type getType() {
        return this.type;
    }

    public void setType(Packet.Type t) {
        this.type = t;
    }
}
