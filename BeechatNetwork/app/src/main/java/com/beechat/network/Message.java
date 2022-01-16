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
public class Message extends ArrayList {
    Packet.Type type = Packet.Type.NONE;
    public short partNumber = -1;
    public short totalNumber = -1;
    byte[] secret = null;
    Cipher cipher = null;
    boolean ready = false;
    byte[] result = null;

    public Message() {
        super();
    }

    public Message(Packet.Type t, byte[] data, byte[] sk) {
        super();
        type = t;
        secret = sk;
        try {
            cipher = Cipher.getInstance("AES");
        } catch (Exception e){
            e.printStackTrace();
        }
        setData(data);
    }

    @Override
    public void clear() {
        type = Packet.Type.NONE;
        partNumber = -1;
        totalNumber = -1;
        ready = false;
        result = null;
        super.clear();
    }

    @Override
    public boolean add(Object obj) {
        if (!(obj instanceof Packet)) {
            return false;
        }
        Packet e = (Packet)obj;
        if (!e.isCorrect()) {
            return false;
        }
        Packet.Type temp = e.getType();
        if (temp != this.type && this.type != Packet.Type.NONE ) {
            return false;
        }
        this.type = temp;
        short packetNum = e.getPartNumber();
        if (packetNum != partNumber + 1) {
            return false;
        }
        partNumber++;
        short packetTotal = e.getTotalNumber();
        if (packetTotal < 1 || (packetTotal != totalNumber && totalNumber != -1)) {
            return false;
        }
        totalNumber = packetTotal;
        if (totalNumber == partNumber + 1) {
            ready = true;
            int len = 0;
            super.add(e);
            for (Packet pack : (ArrayList<Packet>)this) {
                len += pack.getData().length;
            }

            result = new byte[len];

            int pos = 0;
            for (Packet pack : (ArrayList<Packet>)this) {
                byte[] tempb = pack.getData();
                System.arraycopy(tempb, 0, result, pos, tempb.length);
                pos += tempb.length;
            }
            if (chipher != null) {
                try {
                    cipher.init(Cipher.DECRYPT_MODE, this.secret);
                    result = cipher.doFinal(result);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            return true;
        }
        return super.add(e);
    }

    public void send(XBeeDevice dev, RemoteXBeeDevice remote, Blake3 hasher) throws XBeeException {
        if (chipher != null) {
            try {
                cipher.init(Cipher.ENCRYPT_MODE, this.secret);
                result = cipher.doFinal(result);
            } catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }
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
              , (short)i
              , (short)(packetCount)
              , bs
              , hasher
            ).getData();

            if (remote != null) {
                dev.sendData(remote, toSend);
            } else {
                dev.sendBroadcastData(toSend);
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (MainScreen.replyReceived) {
                MainScreen.replyReceived = false;
            } else {
                i--;
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
        totalNumber = (short)(data.length / Packet.getMaxLen()
                    + (data.length % Packet.getMaxLen() != 0 ? 1 : 0))
        ;
        partNumber = (short)(totalNumber - 1);
    }

    public Packet.Type getType() {
        return this.type;
    }

    public void setType(Packet.Type t) {
        this.type = t;
    }
}
