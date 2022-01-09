package com.beechat.network;

import androidx.room.PrimaryKey;

/***
 *  --- User ---
 *  The User class in DB.
 ***/
public class User {
    @PrimaryKey(autoGenerate = true)

    public static final int NODEID_SIZE = 12;
    public static final int PASS_HASH_SIZE = 64;

    private int id;
    private String logo;
    private String username;
    private String password;
    private byte[] dPubKey;
    private byte[] dPrivKey;
    private byte[] kPubKey;
    private byte[] kPrivKey;

    public User() {
        dPubKey = new byte[Dilithium.CRYPTO_PUBLICKEYBYTES];
        dPrivKey = new byte[Dilithium.CRYPTO_SECRETKEYBYTES];
    }

    public User(String username, String password, byte[] dpk,
        byte[] dsk, byte[] kpk, byte[] ksk, String logo
    ) {
        this.username = username;
        this.password = password;
        this.dPubKey = dpk;
        this.dPrivKey = dsk;
        this.kPubKey = kpk;
        this.kPrivKey = ksk;
        this.logo = logo;
    }

    public User(int id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public byte[] getDPubKey() {
        return this.dPubKey;
    }

    public void setDPubKey(byte[] _dPubKey) {
        this.dPubKey = _dPubKey;
    }

    public byte[] getDPrivKey() {
        return this.dPrivKey;
    }

    public void setDPrivKey(byte[] _dPrivKey) {
        this.dPrivKey = _dPrivKey;
    }

    public byte[] getKPrivKey() {
        return this.kPrivKey;
    }

    public void setKPrivKey(byte[] _kPrivKey) {
        this.kPrivKey = _kPrivKey;
    }

    public byte[] getKPubKey() {
        return this.kPubKey;
    }

    public void setKPubKey(byte[] _kPubKey) {
        this.kPubKey = _kPubKey;
    }

    public String getLogo() {
        return this.logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }
}
