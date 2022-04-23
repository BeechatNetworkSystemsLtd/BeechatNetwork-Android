package com.beechat.network;

public class Kyber512 {

    public static final int KYBER_PUBLICKEYBYTES = 800;
    public static final int KYBER_SECRETKEYBYTES = 1632;
    public static final int KYBER_SSBYTES = 32;
    public static final int KYBER_CIPHERTEXTBYTES = 768;

    static {
        System.loadLibrary("kyber512");
    }

    // Declare a native methods of kyber512
    public static native int crypto_kem_keypair(byte []pk, byte []sk);
    public static native int crypto_kem_enc(byte []ct, byte []ss, byte []pk);
    public static native int crypto_kem_dec(byte []ss, byte []ct, byte []sk);

}

