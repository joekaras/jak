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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class Base64
{
  protected static final byte[] base64Chars = { 65, 66, 67, 68, 69, 70, 71, 72, 
    73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 
    97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 
    115, 116, 117, 118, 119, 120, 121, 122, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 
    43, 47 };
  protected static final byte[] reverseBase64Chars = new byte['ÿ'];
  
  static
  {
    for (int i = 0; i < reverseBase64Chars.length; i++) {
      reverseBase64Chars[i] = -1;
    }
    for (byte i = 0; i < base64Chars.length; i = (byte)(i + 1)) {
      reverseBase64Chars[base64Chars[i]] = i;
    }
  }
  
  private static void main(String[] args)
  {
    for (int i = 0; i < args.length; i++)
    {
      String encoded = encode(args[i]);
      String decoded = decode(encoded);
      System.out.println(args[i] + ": " + encoded + " " + decode(args[i]));
      if (!decoded.equals(args[i])) {
        System.err.println("Encoding then decoding " + args[i] + 
          " does not work. Decoded: '" + decoded + "'");
      }
    }
  }
  
  public static String encode(String string)
  {
    return new String(encode(string.getBytes()));
  }
  
  public static String encode(String string, String enc)
    throws UnsupportedEncodingException
  {
    return new String(encode(string.getBytes(enc)), enc);
  }
  
  public static byte[] encode(byte[] bytes)
  {
    ByteArrayInputStream in = new ByteArrayInputStream(bytes);
    
    int length = bytes.length;
    int mod;
    if ((mod = length % 3) != 0) {
      length += 3 - mod;
    }
    length = length * 4 / 3;
    ByteArrayOutputStream out = new ByteArrayOutputStream(length);
    try
    {
      encode(in, out, false);
    }
    catch (IOException localIOException) {}
    return out.toByteArray();
  }
  
  public static void encode(InputStream in, OutputStream out, boolean lineBreaks)
    throws IOException
  {
    int[] inBuffer = new int[3];
    int lineCount = 0;
    
    boolean done = false;
    while ((!done) && ((inBuffer[0] = in.read()) != -1))
    {
      inBuffer[1] = in.read();
      inBuffer[2] = in.read();

      out.write(base64Chars[(inBuffer[0] >> 2)]);
      if (inBuffer[1] != -1)
      {
        out.write(base64Chars[(inBuffer[0] << 4 & 0x30 | inBuffer[1] >> 4)]);
        if (inBuffer[2] != -1)
        {
          out.write(base64Chars[(inBuffer[1] << 2 & 0x3C | inBuffer[2] >> 6)]);
          
          out.write(base64Chars[(inBuffer[2] & 0x3F)]);
        }
        else
        {
          out.write(base64Chars[(inBuffer[1] << 2 & 0x3C)]);
          
          out.write(61);
          done = true;
        }
      }
      else
      {
        out.write(base64Chars[(inBuffer[0] << 4 & 0x30)]);
        
        out.write(61);
        out.write(61);
        done = true;
      }
      lineCount += 4;
      if ((lineBreaks) && (lineCount >= 76))
      {
        out.write(10);
        lineCount = 0;
      }
    }
  }
  
  public static String decode(String string)
  {
    return new String(decode(string.getBytes()));
  }
  
  public static String decode(String string, String enc)
    throws UnsupportedEncodingException
  {
    return new String(decode(string.getBytes(enc)), enc);
  }
  
  public static byte[] decode(byte[] bytes)
  {
    ByteArrayInputStream in = new ByteArrayInputStream(bytes);

    int length = bytes.length;
    int mod;
    if ((mod = length % 4) != 0) {
      length += 4 - mod;
    }
    length = length * 3 / 4;
    ByteArrayOutputStream out = new ByteArrayOutputStream(length);
    try
    {
      decode(in, out, false);
    }
    catch (IOException localIOException) {}
    return out.toByteArray();
  }
  
  private static final int readBase64(InputStream in, boolean throwExceptions)
    throws IOException
  {
    int read;
    do
    {
      read = in.read();
      if (read == -1) {
        return -1;
      }
      if ((throwExceptions) && (reverseBase64Chars[((byte)read)] == -1) && (read != 32) && 
        (read != 10) && (read != 13) && (read != 9) && (read != 12) && 
        (read != 61)) {
        throw new IOException("Unexpected Base64 character: " + read);
      }
      read = reverseBase64Chars[((byte)read)];
    } while (read == -1);
    return read;
  }
  
  public static void decode(InputStream in, OutputStream out, boolean throwExceptions)
    throws IOException
  {
    int[] inBuffer = new int[4];

    boolean done = false;
    while ((!done) && ((inBuffer[0] = readBase64(in, throwExceptions)) != -1) && 
      ((inBuffer[1] = readBase64(in, throwExceptions)) != -1))
    {
      inBuffer[2] = readBase64(in, throwExceptions);
      inBuffer[3] = readBase64(in, throwExceptions);

      out.write(inBuffer[0] << 2 | inBuffer[1] >> 4);
      if (inBuffer[2] != -1)
      {
        out.write(inBuffer[1] << 4 | inBuffer[2] >> 2);
        if (inBuffer[3] != -1) {
          out.write(inBuffer[2] << 6 | inBuffer[3]);
        } else {
          done = true;
        }
      }
      else
      {
        done = true;
      }
    }
  }
}
