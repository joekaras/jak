/*
 * ===================================================================
 * Copyright Notice! 
 * ------------------------------------------------------------------- 
 * This document is protected under the trade secret and copyright 
 * laws as the property of Fidelity National Information Services, Inc. 
 * Copying, reproduction or distribution should be limited and only to
 * employees with a "need to know" to do their job. 
 * Any disclosure of this document to third parties 
 * is strictly prohibited.
 *
 * copyright 2023 Fidelity National Information Services
 * All rights reserved worldwide.
 * ===================================================================
 */

package com.fis.security;

import com.certicom.ecc.scheme.CryptoTransform;
import com.certicom.ecc.system.CryptoManager;
import java.math.BigInteger;


/*
 * Performs AES Encryption
 */
public class AESEncryption
{
  private BlockCypherMode mode;
  private String key;
  private String vector;
  
  public static enum BlockCypherMode
  {
    ECB,  CBC,  CFB64,  OFB64;
  }
  
  /**
   * Constructor that defaults to CBC encryption.
   * @param key - Key to use for encryption
   * @param vector - Vector to use for encryption
   */
  public AESEncryption(String key, String vector)
  {
    this(key, vector, BlockCypherMode.CBC);
  }
  
  public AESEncryption(String key, String vector, BlockCypherMode mode)
  {
    this.key = key;
    this.vector = vector;
    this.mode = mode;
  }
  
  public static int segmentSize = 16;
  
  /**
   * Encrypts a string of data.
   * @param dataToEncrypt
   * @return Encrypted string.
   */
  public String encryptString(String dataToEncrypt)
  {	  
    byte[] encryptedbytes = new byte[99000];
    
    try
    {
      byte[] txtmsg = dataToEncrypt.getBytes();
      

      Object[] params = new Object[4];
      params[0] = this.mode.toString();
      params[1] = new Integer(128);
      params[2] = this.key.getBytes();
      params[3] = (this.vector != null ? this.vector.getBytes() : null);
      

      CryptoTransform encrypter = (CryptoTransform)CryptoManager.getInstance(CryptoTransform.TYPE, "AES");
      encrypter.init(3, params, null);
      
      int len = txtmsg.length;
      int bytechunkcount = 0;
      int i = 0;
      int remainder = 0;
      for (i = 0; i < len / segmentSize; i++) {
        encrypter.transform(txtmsg, i * segmentSize, segmentSize, encryptedbytes, i * segmentSize, false);
      }
      remainder = txtmsg.length % segmentSize;
      bytechunkcount = i;
      if (remainder != 0)
      {
        bytechunkcount++;
        String bytestr = new String(txtmsg, i * segmentSize, remainder);
        byte[] lastline = new byte[segmentSize];
        lastline = bytestr.getBytes();
        
        BigInteger bitxtmsg = new BigInteger(1, lastline);
        int shiftamt = 128 - 8 * remainder;
        bitxtmsg = bitxtmsg.shiftLeft(shiftamt);
        byte[] bytechunk = new byte[segmentSize];
        bytechunk = bitxtmsg.toByteArray();
        encrypter.transform(bytechunk, 0, segmentSize, encryptedbytes, i * segmentSize, false);
      }
      byte[] resizebyte = new byte[segmentSize * bytechunkcount];
      System.arraycopy(encryptedbytes, 0, resizebyte, 0, segmentSize * bytechunkcount);
      

      byte[] encodedbytes = Base64.encode(resizebyte);
      return new String(encodedbytes);
    }
    catch (Exception e) {}
    return "error";
  }
  
  /**
   * Decrypts base 64 encoded string
   * @param encodedString - encoded string to decrypt
   * @return - decoded and decrypted string
   */
  public String decryptString(String encodedString)
  {
    try
    {
      Object[] params = new Object[4];
      params[0] = this.mode.toString();
      params[1] = new Integer(128);
      params[2] = this.key.getBytes();
      params[3] = (this.vector != null ? this.vector.getBytes() : null);
      

      byte[] encryptbyte = Base64.decode(encodedString.getBytes());
      
      CryptoTransform decrypter = (CryptoTransform)CryptoManager.getInstance(CryptoTransform.TYPE, "AES");
      decrypter.init(4, params, null);
      
      byte[] plaintxt = new byte[encryptbyte.length];
      for (int j = 0; j < encryptbyte.length / segmentSize; j++) {
        decrypter.transform(encryptbyte, j * segmentSize, segmentSize, plaintxt, j * segmentSize, false);
      }
      String decryptedString = new String(plaintxt);
      return decryptedString.trim();
    }
    catch (Exception e) {}
    return "error";
  }
}
