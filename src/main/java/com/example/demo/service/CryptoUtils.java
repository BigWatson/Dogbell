package com.example.demo.service;

import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {
  private static final String AES = "AES";
  private static final String AES_GCM = "AES/GCM/NoPadding";
  private static final int GCM_TAG_LENGTH = 128;
  private static final int IV_LENGTH = 12;

  public static String encrypt(byte[] keyBytes, byte[] plaintext) throws Exception {
    SecretKey key = new SecretKeySpec(keyBytes, AES);
    byte[] iv = new byte[IV_LENGTH];
    SecureRandom sr = new SecureRandom();
    sr.nextBytes(iv);

    Cipher cipher = Cipher.getInstance(AES_GCM);
    GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
    cipher.init(Cipher.ENCRYPT_MODE, key, spec);
    byte[] cipherText = cipher.doFinal(plaintext);

    byte[] out = new byte[iv.length + cipherText.length];
    System.arraycopy(iv, 0, out, 0, iv.length);
    System.arraycopy(cipherText, 0, out, iv.length, cipherText.length);
    return Base64.getEncoder().encodeToString(out);
  }

  public static byte[] decrypt(byte[] keyBytes, String base64) throws Exception {
    byte[] all = Base64.getDecoder().decode(base64);
    byte[] iv = new byte[IV_LENGTH];
    System.arraycopy(all, 0, iv, 0, IV_LENGTH);
    int ctLen = all.length - IV_LENGTH;
    byte[] ct = new byte[ctLen];
    System.arraycopy(all, IV_LENGTH, ct, 0, ctLen);

    SecretKey key = new SecretKeySpec(keyBytes, AES);
    Cipher cipher = Cipher.getInstance(AES_GCM);
    GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
    cipher.init(Cipher.DECRYPT_MODE, key, spec);
    return cipher.doFinal(ct);
  }
}
