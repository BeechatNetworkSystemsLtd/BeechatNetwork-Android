package com.beechat.network;

public class Dilithium {

    public static final int CRYPTO_PUBLICKEYBYTES = 1312;
    public static final int CRYPTO_SECRETKEYBYTES = 2528;
    public static final int CRYPTO_BYTES = 2420;

    static {
        System.loadLibrary("dilithium2");
    }

    // Declare a native methods
    public static native int crypto_sign_keypair(byte []pk, byte []sk);
    public static native int crypto_sign(byte []sm, byte []m, long mlen, byte []sk);
    public static native int crypto_sign_open(byte []m, byte []sm, long smlen, byte []pk);

}

